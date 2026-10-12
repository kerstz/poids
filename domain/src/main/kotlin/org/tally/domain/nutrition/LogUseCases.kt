// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.nutrition

import java.time.ZoneId
import org.tally.core.time.AppClock
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.QuantityUnit

/** Ajoute un aliment au journal avec snapshot des nutriments (l'historique ne bouge plus ensuite). */
class LogFood(
    private val journal: JournalRepository,
    private val clock: AppClock,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    suspend operator fun invoke(
        epochDay: Long,
        mealSlotId: Long,
        food: Food,
        quantityBase: Double,
        portionId: Long? = null,
        portionCount: Double? = null,
    ): Long {
        require(quantityBase > 0 && quantityBase.isFinite()) { "quantité invalide" }
        return journal.add(
            NewJournalEntry(
                name = food.name,
                brand = food.brand,
                epochDay = epochDay,
                mealSlotId = mealSlotId,
                foodId = food.id,
                recipeId = null,
                portionId = portionId,
                portionCount = portionCount,
                quantityBase = quantityBase,
                quantityUnit = if (food.baseUnit == BaseUnit.ML) QuantityUnit.ML else QuantityUnit.G,
                macros = NutritionCalc.macrosFor(food.per100, quantityBase),
                detail = food.per100Detail * (quantityBase / 100.0),
                loggedAt = clock.nowMillis(),
                zoneId = zone().id,
            ),
        )
    }
}

/** Ajoute [servings] portions d'une recette (`quantityUnit = SERVING`). */
class LogRecipe(
    private val journal: JournalRepository,
    private val clock: AppClock,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    suspend operator fun invoke(
        epochDay: Long,
        mealSlotId: Long,
        recipe: Recipe,
        servings: Double,
    ): Long {
        require(servings > 0 && servings.isFinite()) { "portions invalides" }
        return journal.add(
            NewJournalEntry(
                name = recipe.name,
                brand = null,
                epochDay = epochDay,
                mealSlotId = mealSlotId,
                foodId = null,
                recipeId = recipe.id,
                portionId = null,
                portionCount = servings,
                quantityBase = servings,
                quantityUnit = QuantityUnit.SERVING,
                macros = recipe.perServing * servings,
                detail = recipe.perServingDetail * servings,
                loggedAt = clock.nowMillis(),
                zoneId = zone().id,
            ),
        )
    }
}

/**
 * Annulation d'une suppression : réinsère l'entrée figée. Une portion supprimée entre-temps
 * (l'édition d'un aliment recrée ses portions) n'est plus référencée, pour ne pas violer la clé étrangère.
 */
class RestoreJournalEntry(
    private val journal: JournalRepository,
    private val foods: FoodRepository,
) {
    suspend operator fun invoke(entry: NewJournalEntry): Long {
        val portionId = entry.portionId
        val stillThere = portionId == null ||
            entry.foodId?.let { foods.get(it) }?.portions?.any { it.id == portionId } == true
        return journal.add(if (stillThere) entry else entry.copy(portionId = null, portionCount = null))
    }
}
