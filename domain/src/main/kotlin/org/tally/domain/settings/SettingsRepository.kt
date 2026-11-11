// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.settings

import kotlinx.coroutines.flow.Flow
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit

/** Thème d'affichage (docs/UI.md §1) : OLED par défaut, clair, ou suivant le système. */
enum class ThemeMode { OLED, LIGHT, SYSTEM }

interface SettingsRepository {
    val unitPreferences: Flow<UnitPreferences>

    /**
     * Requêtes réseau vers Open Food Facts autorisées. **Désactivé par défaut** (opt-in) : aucune
     * requête ne part tant que l'utilisateur n'a pas activé l'option (PRIVACY.md).
     */
    val offEnabled: Flow<Boolean>

    val themeMode: Flow<ThemeMode>

    suspend fun setOffEnabled(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setMassUnit(unit: MassUnit)
    suspend fun setLengthUnit(unit: LengthUnit)
    suspend fun setDistanceUnit(unit: DistanceUnit)
    suspend fun setEnergyUnit(unit: EnergyUnit)
}
