// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.settings

import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit

data class UnitPreferences(
    val mass: MassUnit = MassUnit.KG,
    val length: LengthUnit = LengthUnit.CM,
    val distance: DistanceUnit = DistanceUnit.KM,
    val energy: EnergyUnit = EnergyUnit.KCAL,
)
