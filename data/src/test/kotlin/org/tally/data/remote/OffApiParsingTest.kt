// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OffApiParsingTest {
    private fun parse(nutriments: String) = Json.parseToJsonElement(
        """{"code":"123","product_name":"P","nutriments":{$nutriments}}""",
    ).jsonObject.toRemoteFood()

    @Test
    fun negativeDetailFieldsBecomeNull() {
        val food = parse(
            """"energy-kcal_100g":100,"sugars_100g":-1,"saturated-fat_100g":-0.5,"fiber_100g":-2,"salt_100g":-3""",
        )
        assertNotNull(food)
        val d = food!!.per100Detail
        assertNull(d.sugarsG); assertNull(d.satFatG); assertNull(d.fiberG); assertNull(d.saltG)
    }

    @Test
    fun zeroAndPositiveDetailFieldsAreKept() {
        val d = parse(""""energy-kcal_100g":100,"sugars_100g":0,"salt_100g":1.5""")!!.per100Detail
        assertEquals(0.0, d.sugarsG!!, 0.0)
        assertEquals(1.5, d.saltG!!, 0.0)
    }

    @Test
    fun negativeMacroIsTreatedAsMissingAndNegativeEnergyDropsTheProduct() {
        assertEquals(0.0, parse(""""energy-kcal_100g":100,"proteins_100g":-4""")!!.per100.proteinG, 0.0)
        assertNull(parse(""""energy-kcal_100g":-5"""))
    }
}
