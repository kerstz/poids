package org.tally.domain.nutrition

import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource
import org.tally.domain.model.QuantityUnit

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

/**
 * Nutriments optionnels (g). `null` = inconnu, distinct de 0. Une somme dont un terme est inconnu
 * est inconnue : on ne présente jamais un total partiel comme exact.
 */
data class NutrientDetail(
    val sugarsG: Double? = null,
    val satFatG: Double? = null,
    val fiberG: Double? = null,
    val saltG: Double? = null,
) {
    operator fun times(factor: Double) = NutrientDetail(
        sugarsG?.times(factor), satFatG?.times(factor), fiberG?.times(factor), saltG?.times(factor),
    )

    operator fun plus(other: NutrientDetail) = NutrientDetail(
        sum(sugarsG, other.sugarsG),
        sum(satFatG, other.satFatG),
        sum(fiberG, other.fiberG),
        sum(saltG, other.saltG),
    )

    private fun sum(a: Double?, b: Double?): Double? = if (a == null || b == null) null else a + b

    companion object {
        val UNKNOWN = NutrientDetail()
        /** Point de départ d'un cumul. */
        val ZERO = NutrientDetail(0.0, 0.0, 0.0, 0.0)
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
    val per100Detail: NutrientDetail = NutrientDetail.UNKNOWN,
    /** g/ml, pour convertir un aliment liquide ; non additionnable, donc hors [NutrientDetail]. */
    val densityGPerMl: Double? = null,
    /** Code Ciqual d'origine (`source = CIQUAL`) : un code = une ligne. */
    val ciqualCode: Int? = null,
    /** Date de dernière modification des données OFF (secondes epoch), si connue. */
    val offLastModified: Long? = null,
    /** Archivage logique : appliqué à l'écriture (`true` archive, `false` désarchive). */
    val isArchived: Boolean = false,
) {
    companion object
}

/**
 * Applique les champs éditables sur l'aliment [base] chargé (ou un aliment neuf si null), en conservant
 * code-barres, source, nutriments optionnels et densité que l'éditeur ne gère pas.
 */
fun Food.Companion.edited(
    base: Food?,
    id: Long,
    name: String,
    brand: String?,
    baseUnit: BaseUnit,
    per100: Macros,
): Food = (base ?: Food(id = id, name = name, per100 = per100))
    .copy(id = id, name = name, brand = brand, baseUnit = baseUnit, per100 = per100)

/** Entrée de la table de référence Ciqual (Anses), valeurs pour 100 g. */
data class CiqualFood(
    val code: Int,
    val name: String,
    val groupCode: String,
    val per100: Macros,
    val per100Detail: NutrientDetail = NutrientDetail.UNKNOWN,
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
    val per100Detail: NutrientDetail = NutrientDetail.UNKNOWN,
) {
    val macros: Macros get() = NutritionCalc.macrosFor(per100, quantityBase)
    val detail: NutrientDetail get() = per100Detail * (quantityBase / 100.0)
}

data class Recipe(
    val id: Long = 0,
    val name: String,
    val servings: Double,
    val ingredients: List<RecipeIngredient>,
) {
    val total: Macros get() = ingredients.fold(Macros.ZERO) { acc, i -> acc + i.macros }
    val perServing: Macros get() = if (servings > 0) total * (1.0 / servings) else Macros.ZERO

    /** Connu seulement si tous les ingrédients ont leurs nutriments optionnels. */
    val perServingDetail: NutrientDetail
        get() = if (servings > 0) {
            ingredients.fold(NutrientDetail.ZERO) { acc, i -> acc + i.detail } * (1.0 / servings)
        } else {
            NutrientDetail.UNKNOWN
        }
}

/** Ligne de journal affichable. Les macros sont le snapshot figé à la saisie. */
data class JournalItem(
    val id: Long,
    val mealSlotId: Long,
    val name: String,
    /** Exprimée dans [quantityUnit] : g/ml pour un aliment, nombre de portions pour une recette. */
    val quantityBase: Double,
    val quantityUnit: QuantityUnit,
    val isRecipe: Boolean,
    val portionCount: Double?,
    val macros: Macros,
    val detail: NutrientDetail = NutrientDetail.UNKNOWN,
)

data class NewJournalEntry(
    /** Nom figé à la saisie : renommer l'aliment ne réécrit pas l'historique. */
    val name: String,
    val brand: String?,
    val epochDay: Long,
    val mealSlotId: Long,
    val foodId: Long?,
    val recipeId: Long?,
    val portionId: Long?,
    val portionCount: Double?,
    val quantityBase: Double,
    val quantityUnit: QuantityUnit,
    val macros: Macros,
    val detail: NutrientDetail = NutrientDetail.UNKNOWN,
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

object RecipeValidation {
    fun isValid(recipe: Recipe): Boolean =
        recipe.name.isNotBlank() &&
            recipe.servings.isFinite() && recipe.servings > 0 &&
            recipe.ingredients.isNotEmpty() &&
            recipe.ingredients.all { it.quantityBase.isFinite() && it.quantityBase > 0 }
}

object FoodValidation {
    fun isValid(food: Food): Boolean =
        food.name.isNotBlank() &&
            listOf(food.per100.energyKcal, food.per100.proteinG, food.per100.carbsG, food.per100.fatG)
                .all { it.isFinite() && it >= 0.0 }
}
