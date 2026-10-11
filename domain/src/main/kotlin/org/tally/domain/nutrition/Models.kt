package org.tally.domain.nutrition

import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource

/** Énergie (kcal) et macronutriments (g). */
data class Macros(
    val energyKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
) {
    operator fun plus(other: Macros) = Macros(
        energyKcal + other.energyKcal,
        proteinG + other.proteinG,
        carbsG + other.carbsG,
        fatG + other.fatG,
    )

    operator fun times(factor: Double) = Macros(
        energyKcal * factor,
        proteinG * factor,
        carbsG * factor,
        fatG * factor,
    )

    companion object {
        val ZERO = Macros()
    }
}

/** Aliment. [per100] = valeurs pour 100 g (ou 100 ml selon [baseUnit]). `id = 0` : nouvel aliment. */
data class Food(
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val source: FoodSource = FoodSource.USER,
    val baseUnit: BaseUnit = BaseUnit.G,
    val per100: Macros,
)

data class FoodPortion(
    val id: Long = 0,
    val label: String,
    /** Quantité en g ou ml. */
    val quantityBase: Double,
    val isDefault: Boolean = false,
)

data class FoodWithPortions(val food: Food, val portions: List<FoodPortion>)

data class MealSlot(
    val id: Long,
    val builtinKey: String?,
    val name: String?,
    val position: Int,
)

data class RecipeIngredient(
    val foodId: Long,
    val foodName: String,
    val quantityBase: Double,
    val per100: Macros,
) {
    val macros: Macros get() = NutritionCalc.macrosFor(per100, quantityBase)
}

data class Recipe(
    val id: Long = 0,
    val name: String,
    val servings: Double,
    val ingredients: List<RecipeIngredient>,
) {
    val total: Macros get() = ingredients.fold(Macros.ZERO) { acc, i -> acc + i.macros }
    val perServing: Macros get() = if (servings > 0) total * (1.0 / servings) else Macros.ZERO
}

/** Ligne de journal affichable. Les macros sont le snapshot figé à la saisie. */
data class JournalItem(
    val id: Long,
    val mealSlotId: Long,
    val name: String,
    /** g/ml pour un aliment ; nombre de portions pour une recette. */
    val quantityBase: Double,
    val baseUnit: BaseUnit?,
    val isRecipe: Boolean,
    val portionCount: Double?,
    val macros: Macros,
)

data class NewJournalEntry(
    val epochDay: Long,
    val mealSlotId: Long,
    val foodId: Long?,
    val recipeId: Long?,
    val portionId: Long?,
    val portionCount: Double?,
    val quantityBase: Double,
    val macros: Macros,
    val loggedAt: Long,
    val zoneId: String,
)

data class DayTargets(
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
)

object NutritionCalc {
    fun macrosFor(per100: Macros, quantityBase: Double): Macros = per100 * (quantityBase / 100.0)

    fun total(items: List<JournalItem>): Macros = items.fold(Macros.ZERO) { acc, i -> acc + i.macros }
}

object FoodValidation {
    fun isValid(food: Food): Boolean =
        food.name.isNotBlank() &&
            listOf(food.per100.energyKcal, food.per100.proteinG, food.per100.carbsG, food.per100.fatG)
                .all { it.isFinite() && it >= 0.0 }
}
