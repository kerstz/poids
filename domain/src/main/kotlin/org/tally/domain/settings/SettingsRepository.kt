// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.settings

import kotlinx.coroutines.flow.Flow
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit

interface SettingsRepository {
    val unitPreferences: Flow<UnitPreferences>

    /** Requêtes réseau vers Open Food Facts autorisées (défaut : oui). */
    val offEnabled: Flow<Boolean>

    suspend fun setOffEnabled(enabled: Boolean)
    suspend fun setMassUnit(unit: MassUnit)
    suspend fun setLengthUnit(unit: LengthUnit)
    suspend fun setDistanceUnit(unit: DistanceUnit)
    suspend fun setEnergyUnit(unit: EnergyUnit)
}
