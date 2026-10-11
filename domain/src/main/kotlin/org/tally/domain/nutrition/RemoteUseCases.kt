package org.tally.domain.nutrition

import org.tally.core.barcode.Gtin
import org.tally.domain.model.FoodSource

/** Résultat d'une recherche par code-barres : local d'abord, réseau ensuite. */
sealed interface BarcodeLookup {
    data class Local(val food: Food) : BarcodeLookup
    data class Remote(val food: RemoteFood) : BarcodeLookup
    data object NotFound : BarcodeLookup
    data class RateLimited(val retryAfterMs: Long) : BarcodeLookup
    data object Unavailable : BarcodeLookup
}

/** Ordre de résolution (docs/AUDIT.md §3.4) : aliment local → cache/réseau OFF → saisie manuelle. */
class ResolveBarcode(
    private val foods: FoodRepository,
    private val remote: FoodRemoteSource,
) {
    suspend operator fun invoke(barcode: String): BarcodeLookup {
        if (!Gtin.isValid(barcode)) return BarcodeLookup.NotFound
        val code = Gtin.normalize(barcode)
        foods.findByBarcode(code)?.let { return BarcodeLookup.Local(it) }
        return when (val result = remote.findByBarcode(code)) {
            is RemoteResult.Success -> BarcodeLookup.Remote(result.value)
            RemoteResult.NotFound -> BarcodeLookup.NotFound
            is RemoteResult.RateLimited -> BarcodeLookup.RateLimited(result.retryAfterMs)
            RemoteResult.Unavailable, RemoteResult.Disabled -> BarcodeLookup.Unavailable
        }
    }
}

/**
 * Copie un produit distant dans `food` (source OFF) pour qu'il reste utilisable hors ligne et
 * que le cache puisse être purgé sans casser l'historique. Dédoublonne par code-barres.
 *
 * Politique : un code-barres = une ligne. Si l'aliment existe mais est archivé, un seul `save`
 * le désarchive et écrase ce que fournit OFF (nom, marque, macros, détail, date) ; l'id, l'unité,
 * la densité et les portions sont conservés. Le journal est figé : l'historique ne bouge pas.
 */
class ImportRemoteFood(
    private val foods: FoodRepository,
    private val saveFood: SaveFood,
) {
    suspend operator fun invoke(remote: RemoteFood): Food? {
        val existing = foods.findByBarcodeAny(remote.barcode)
        if (existing != null && !existing.isArchived) return existing
        val servingPortion = remote.servingQuantityG
            ?.takeIf { it > 0 }
            ?.let { FoodPortion(label = "1 portion", quantityBase = it, isDefault = true) }
        if (existing != null) {
            val kept = foods.get(existing.id)?.portions.orEmpty()
            val refreshed = existing.copy(
                name = remote.name,
                brand = remote.brand,
                source = FoodSource.OFF,
                per100 = remote.per100,
                per100Detail = remote.per100Detail,
                offLastModified = remote.lastModifiedEpochSec,
                isArchived = false,
            )
            val portions = kept.ifEmpty { listOfNotNull(servingPortion) }
            saveFood(refreshed, portions) ?: return null
            return refreshed
        }
        val food = Food(
            name = remote.name,
            brand = remote.brand,
            barcode = remote.barcode,
            source = FoodSource.OFF,
            per100 = remote.per100,
            per100Detail = remote.per100Detail,
            offLastModified = remote.lastModifiedEpochSec,
        )
        val id = saveFood(food, listOfNotNull(servingPortion)) ?: return null
        return food.copy(id = id)
    }
}
