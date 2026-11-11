// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.ThemeMode
import org.tally.domain.settings.UnitPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    /** Thème émis à la demande : simule une lecture Room qui n'a pas encore répondu. */
    private class SlowSettings : SettingsRepository {
        val theme = MutableSharedFlow<ThemeMode>(replay = 1)
        override val themeMode: Flow<ThemeMode> = theme
        override val unitPreferences: Flow<UnitPreferences> = emptyFlow()
        override val offEnabled: Flow<Boolean> = emptyFlow()
        override suspend fun setOffEnabled(enabled: Boolean) {}
        override suspend fun setThemeMode(mode: ThemeMode) {}
        override suspend fun setMassUnit(unit: MassUnit) {}
        override suspend fun setLengthUnit(unit: LengthUnit) {}
        override suspend fun setDistanceUnit(unit: DistanceUnit) {}
        override suspend fun setEnergyUnit(unit: EnergyUnit) {}
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun modeIsUnknownUntilStoredThemeIsRead() = runTest(dispatcher) {
        val settings = SlowSettings()
        val vm = ThemeViewModel(settings)
        // Pas de valeur OLED provisoire : l'activité ne doit rien dessiner.
        assertNull(vm.mode.value)

        settings.theme.emit(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.mode.value)
    }

    @Test fun modeIsCollectedWithoutAnySubscriber() = runTest(dispatcher) {
        val settings = SlowSettings()
        settings.theme.emit(ThemeMode.SYSTEM)
        // Lu par le pré-dessin de l'activité via `.value`, sans collecteur Compose.
        assertEquals(ThemeMode.SYSTEM, ThemeViewModel(settings).mode.value)
    }
}
