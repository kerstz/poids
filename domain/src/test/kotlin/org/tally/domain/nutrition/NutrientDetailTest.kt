// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.nutrition

import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tally.core.time.AppClock

class NutrientDetailTest {
    @Test
    fun sumIsUnknownWhenAnyTermIsUnknown() {
        val a = NutrientDetail(sugarsG = 2.0, saltG = 1.0)
        val b = NutrientDetail(sugarsG = 3.0)
        val sum = a + b
        assertEquals(5.0, sum.sugarsG!!, 0.0)
        assertNull(sum.saltG)
        assertNull(sum.fiberG)
    }

    @Test
    fun timesKeepsUnknownAndZeroIsNeutral() {
        val d = NutrientDetail(sugarsG = 4.0) * 0.5
        assertEquals(2.0, d.sugarsG!!, 0.0)
        assertNull(d.satFatG)
        assertEquals(NutrientDetail(1.0, 2.0, 3.0, 4.0), NutrientDetail.ZERO + NutrientDetail(1.0, 2.0, 3.0, 4.0))
    }

    @Test
    fun recipeDetailIsKnownOnlyIfAllIngredientsAreKnown() {
        val known = RecipeIngredient(1, "A", 200.0, Macros(100.0), NutrientDetail(5.0, 1.0, 2.0, 0.5))
        val unknown = RecipeIngredient(2, "B", 100.0, Macros(50.0))
        val full = Recipe(1, "R", 2.0, listOf(known))
        assertEquals(NutrientDetail(5.0, 1.0, 2.0, 0.5), full.perServingDetail) // 10 g/200 g ÷ 2 portions
        assertNull(Recipe(1, "R", 2.0, listOf(known, unknown)).perServingDetail.sugarsG)
    }

    private class Capture : JournalRepository {
        var last: NewJournalEntry? = null
        override fun observeDay(epochDay: Long) = kotlinx.coroutines.flow.emptyFlow<List<JournalItem>>()
        override suspend fun add(entry: NewJournalEntry): Long { last = entry; return 1 }
        override suspend fun delete(id: Long) = Unit
    }

    @Test
    fun logFoodAndLogRecipeScaleDetailIntoSnapshot() = runTest {
        val clock = object : AppClock { override fun nowMillis() = 1L }
        val journal = Capture()
        val food = Food(1, "Yaourt", "Marque", per100 = Macros(60.0), per100Detail = NutrientDetail(4.0, 2.0, 0.0, 0.1))
        LogFood(journal, clock) { ZoneId.of("UTC") }(0, 1, food, 250.0)
        assertEquals(10.0, journal.last!!.detail.sugarsG!!, 1e-9)
        assertEquals("Yaourt", journal.last!!.name)
        assertEquals("Marque", journal.last!!.brand)

        val recipe = Recipe(3, "Dessert", 2.0, listOf(RecipeIngredient(1, "Yaourt", 200.0, Macros(60.0), food.per100Detail)))
        LogRecipe(journal, clock) { ZoneId.of("UTC") }(0, 1, recipe, 1.0)
        assertEquals(4.0, journal.last!!.detail.sugarsG!!, 1e-9) // 8 g au total ÷ 2 × 1 portion
        assertEquals("Dessert", journal.last!!.name)
    }
}
