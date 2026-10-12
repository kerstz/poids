// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource

class FoodEditTest {
    @Test
    fun editKeepsFieldsTheEditorDoesNotHandle() {
        val stored = Food(
            id = 7, name = "Yaourt", brand = "Marque", barcode = "3017620422003", source = FoodSource.OFF,
            baseUnit = BaseUnit.ML, per100 = Macros(60.0, 4.0, 5.0, 3.0),
            per100Detail = NutrientDetail(4.5, 2.0, 0.5, 0.1), densityGPerMl = 1.03,
        )
        val edited = Food.edited(stored, 7, "Yaourt nature", null, BaseUnit.ML, Macros(61.0, 4.0, 5.0, 3.0))
        assertEquals("Yaourt nature", edited.name)
        assertEquals(61.0, edited.per100.energyKcal, 0.0)
        assertEquals("3017620422003", edited.barcode)
        assertEquals(FoodSource.OFF, edited.source)
        assertEquals(NutrientDetail(4.5, 2.0, 0.5, 0.1), edited.per100Detail)
        assertEquals(1.03, edited.densityGPerMl!!, 0.0)
    }
}
