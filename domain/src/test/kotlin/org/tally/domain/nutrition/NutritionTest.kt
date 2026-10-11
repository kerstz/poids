package org.tally.domain.nutrition

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tally.core.time.AppClock

class NutritionTest {
    private val oats = Macros(energyKcal = 370.0, proteinG = 13.0, carbsG = 60.0, fatG = 7.0)
    private val milk = Macros(energyKcal = 46.0, proteinG = 3.4, carbsG = 4.8, fatG = 1.5)

    @Test
    fun macrosScaleWithQuantity() {
        val m = NutritionCalc.macrosFor(oats, 50.0)
        assertEquals(185.0, m.energyKcal, 1e-9)
        assertEquals(6.5, m.proteinG, 1e-9)
    }

    @Test
    fun recipePerServingIsTotalOverServings() {
        val recipe = Recipe(
            name = "Porridge",
            servings = 2.0,
            ingredients = listOf(
                RecipeIngredient(1, "Avoine", 80.0, oats),
                RecipeIngredient(2, "Lait", 300.0, milk),
            ),
        )
        assertEquals(370.0 * 0.8 + 46.0 * 3.0, recipe.total.energyKcal, 1e-9)
        assertEquals(recipe.total.energyKcal / 2, recipe.perServing.energyKcal, 1e-9)
    }

    @Test
    fun foodValidationRejectsBlankNameAndNegatives() {
        assertTrue(FoodValidation.isValid(Food(name = "Pomme", per100 = Macros(52.0, 0.3, 14.0, 0.2))))
        assertFalse(FoodValidation.isValid(Food(name = " ", per100 = Macros())))
        assertFalse(FoodValidation.isValid(Food(name = "X", per100 = Macros(energyKcal = -1.0))))
    }

    @Test
    fun logFoodStoresSnapshot() = runTest {
        var captured: NewJournalEntry? = null
        val journal = object : JournalRepository {
            override fun observeDay(epochDay: Long): Flow<List<JournalItem>> = emptyFlow()
            override suspend fun add(entry: NewJournalEntry): Long { captured = entry; return 1 }
            override suspend fun delete(id: Long) = Unit
        }
        val clock = object : AppClock { override fun nowMillis() = 42L }
        LogFood(journal, clock)(
            epochDay = 100, mealSlotId = 1, food = Food(id = 7, name = "Avoine", per100 = oats), quantityBase = 40.0,
        )
        val entry = captured!!
        assertEquals(7L, entry.foodId)
        assertEquals(148.0, entry.macros.energyKcal, 1e-9)
        assertEquals(42L, entry.loggedAt)
    }
}
