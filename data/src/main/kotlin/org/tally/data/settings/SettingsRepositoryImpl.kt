// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.settings

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tally.core.time.AppClock
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.data.db.dao.AppSettingDao
import org.tally.data.db.entity.AppSettingEntity
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingDao,
    private val clock: AppClock,
) : SettingsRepository {

    override val unitPreferences: Flow<UnitPreferences> = dao.observeAll().map { rows ->
        val byKey = rows.associate { it.key to it.value }
        val defaults = UnitPreferences()
        UnitPreferences(
            mass = byKey[KEY_MASS].toEnumOr(defaults.mass),
            length = byKey[KEY_LENGTH].toEnumOr(defaults.length),
            distance = byKey[KEY_DISTANCE].toEnumOr(defaults.distance),
            energy = byKey[KEY_ENERGY].toEnumOr(defaults.energy),
        )
    }

    override val offEnabled: Flow<Boolean> = dao.observeAll().map { rows ->
        rows.firstOrNull { it.key == KEY_OFF_ENABLED }?.value?.toBooleanStrictOrNull() ?: true
    }

    override suspend fun setOffEnabled(enabled: Boolean) = put(KEY_OFF_ENABLED, enabled.toString())

    override suspend fun setMassUnit(unit: MassUnit) = put(KEY_MASS, unit.name)
    override suspend fun setLengthUnit(unit: LengthUnit) = put(KEY_LENGTH, unit.name)
    override suspend fun setDistanceUnit(unit: DistanceUnit) = put(KEY_DISTANCE, unit.name)
    override suspend fun setEnergyUnit(unit: EnergyUnit) = put(KEY_ENERGY, unit.name)

    private suspend fun put(key: String, value: String) =
        dao.put(AppSettingEntity(key, value, clock.nowMillis()))

    private companion object {
        const val KEY_OFF_ENABLED = "off.enabled"
        const val KEY_MASS = "unit.mass"
        const val KEY_LENGTH = "unit.length"
        const val KEY_DISTANCE = "unit.distance"
        const val KEY_ENERGY = "unit.energy"
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOr(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
