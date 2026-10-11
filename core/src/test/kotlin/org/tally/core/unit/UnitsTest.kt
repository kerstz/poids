package org.tally.core.unit

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    private val eps = 1e-6

    @Test fun kgToLb() = assertEquals(2.2046226, MassUnit.LB.fromSi(1.0), 1e-6)
    @Test fun lbRoundTrip() = assertEquals(80.0, MassUnit.LB.toSi(MassUnit.LB.fromSi(80.0)), eps)
    @Test fun stoneIs14Pounds() = assertEquals(14.0, MassUnit.LB.fromSi(MassUnit.ST.toSi(1.0)), 1e-6)
    @Test fun metersToCm() = assertEquals(175.0, LengthUnit.CM.fromSi(1.75), eps)
    @Test fun inchesToMeters() = assertEquals(0.0254, LengthUnit.IN.toSi(1.0), eps)
    @Test fun metersToKm() = assertEquals(5.0, DistanceUnit.KM.fromSi(5000.0), eps)
    @Test fun milesToMeters() = assertEquals(1609.344, DistanceUnit.MI.toSi(1.0), eps)
    @Test fun kcalToKj() = assertEquals(418.4, EnergyUnit.KJ.fromKcal(100.0), eps)
    @Test fun kjRoundTrip() = assertEquals(2000.0, EnergyUnit.KJ.toKcal(EnergyUnit.KJ.fromKcal(2000.0)), eps)
}
