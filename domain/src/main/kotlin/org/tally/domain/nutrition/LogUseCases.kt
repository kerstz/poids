package org.tally.domain.nutrition

import java.time.ZoneId
import org.tally.core.time.AppClock

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
                epochDay = epochDay,
                mealSlotId = mealSlotId,
                foodId = food.id,
                recipeId = null,
                portionId = portionId,
                portionCount = portionCount,
                quantityBase = quantityBase,
                macros = NutritionCalc.macrosFor(food.per100, quantityBase),
                loggedAt = clock.nowMillis(),
                zoneId = zone().id,
            ),
        )
    }
}

/** Ajoute [servings] portions d'une recette. `quantityBase` = nombre de portions (recette). */
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
                epochDay = epochDay,
                mealSlotId = mealSlotId,
                foodId = null,
                recipeId = recipe.id,
                portionId = null,
                portionCount = servings,
                quantityBase = servings,
                macros = recipe.perServing * servings,
                loggedAt = clock.nowMillis(),
                zoneId = zone().id,
            ),
        )
    }
}
