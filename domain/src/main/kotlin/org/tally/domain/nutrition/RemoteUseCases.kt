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
        foods.findByBarcode(barcode)?.let { return BarcodeLookup.Local(it) }
        return when (val result = remote.findByBarcode(barcode)) {
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
 * Politique : un code-barres = une ligne. Si l'aliment existe mais est archivé, il est désarchivé et
 * rafraîchi avec les données OFF (nom, marque, macros), en conservant ses portions et son id : le
 * journal est figé, l'historique ne bouge pas.
 */
class ImportRemoteFood(
    private val foods: FoodRepository,
    private val saveFood: SaveFood,
) {
    suspend operator fun invoke(remote: RemoteFood): Food? {
        foods.findByBarcodeAny(remote.barcode)?.let { existing ->
            if (!existing.isArchived) return existing
            val portions = foods.get(existing.id)?.portions.orEmpty()
            val refreshed = existing.copy(
                name = remote.name,
                brand = remote.brand,
                source = FoodSource.OFF,
                per100 = remote.per100,
                isArchived = false,
            )
            if (saveFood(refreshed, portions) == null) return null
            foods.unarchive(existing.id)
            return refreshed
        }
        val food = Food(
            name = remote.name,
            brand = remote.brand,
            barcode = remote.barcode,
            source = FoodSource.OFF,
            per100 = remote.per100,
        )
        val portions = remote.servingQuantityG
            ?.takeIf { it > 0 }
            ?.let { listOf(FoodPortion(label = "1 portion", quantityBase = it, isDefault = true)) }
            .orEmpty()
        val id = saveFood(food, portions) ?: return null
        return food.copy(id = id)
    }
}
