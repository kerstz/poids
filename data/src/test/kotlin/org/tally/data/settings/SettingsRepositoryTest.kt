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
