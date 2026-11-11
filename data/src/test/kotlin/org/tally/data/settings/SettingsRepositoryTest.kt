// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.MassUnit
import org.tally.data.db.TallyDatabase
import org.tally.domain.settings.ThemeMode
import org.tally.domain.settings.UnitPreferences

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {
    private lateinit var db: TallyDatabase
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(TallyDatabase.seedCallback)
            .build()
        repository = SettingsRepositoryImpl(db.appSettingDao(), object : AppClock {
            override fun nowMillis() = 1_000L
        })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun defaultsWhenNothingStored() = runTest {
        assertEquals(UnitPreferences(), repository.unitPreferences.first())
        // Opt-in : aucune requête Open Food Facts sans geste explicite (PRIVACY.md).
        assertEquals(false, repository.offEnabled.first())
        assertEquals(ThemeMode.OLED, repository.themeMode.first())
    }

    @Test
    fun persistsOffOptInAndTheme() = runTest {
        repository.setOffEnabled(true)
        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(true, repository.offEnabled.first())
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
        repository.setOffEnabled(false)
        assertEquals(false, repository.offEnabled.first())
    }

    @Test
    fun unknownStoredThemeFallsBackToOled() = runTest {
        db.appSettingDao().put(org.tally.data.db.entity.AppSettingEntity(SettingsRepositoryImpl.KEY_THEME, "SEPIA", 1L))
        assertEquals(ThemeMode.OLED, repository.themeMode.first())
    }

    @Test
    fun persistsUnitChoices() = runTest {
        repository.setMassUnit(MassUnit.LB)
        repository.setEnergyUnit(EnergyUnit.KJ)
        val prefs = repository.unitPreferences.first()
        assertEquals(MassUnit.LB, prefs.mass)
        assertEquals(EnergyUnit.KJ, prefs.energy)
    }

    @Test
    fun seedCreatesMealSlotsAndMeasurementTypes() {
        val cursor = db.openHelper.readableDatabase
        cursor.query("SELECT COUNT(*) FROM meal_slot").use {
            it.moveToFirst(); assertEquals(4, it.getInt(0))
        }
        cursor.query("SELECT COUNT(*) FROM measurement_type WHERE `key` = 'builtin.weight'").use {
            it.moveToFirst(); assertEquals(1, it.getInt(0))
        }
    }
}
