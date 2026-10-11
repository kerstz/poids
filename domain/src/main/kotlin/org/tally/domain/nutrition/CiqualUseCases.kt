package org.tally.domain.nutrition

import org.tally.domain.model.FoodSource

/**
 * Copie une entrée Ciqual dans `food` (source CIQUAL) quand l'utilisateur l'utilise, pour que le
 * journal et les recettes restent indépendants de la table de référence. Un code = une ligne ;
 * un aliment archivé est désarchivé, ses éditions et ses portions sont conservées.
 */
class ImportCiqualFood(
    private val foods: FoodRepository,
    private val saveFood: SaveFood,
) {
    suspend operator fun invoke(item: CiqualFood): Food? {
        foods.findByCiqualCode(item.code)?.let { existing ->
            if (!existing.isArchived) return existing
            val refreshed = existing.copy(isArchived = false)
            saveFood(refreshed, foods.get(existing.id)?.portions.orEmpty()) ?: return null
            return refreshed
        }
        val food = Food(
            name = item.name,
            source = FoodSource.CIQUAL,
            per100 = item.per100,
            per100Detail = item.per100Detail,
            ciqualCode = item.code,
        )
        val id = saveFood(food, emptyList()) ?: return null
        return food.copy(id = id)
    }
}
