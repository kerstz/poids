// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.wipe

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.data.db.TallyDatabase
import org.tally.data.db.entity.AppSettingEntity
import org.tally.data.db.entity.CiqualFoodEntity
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.GoalEntity
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.data.db.entity.MeasurementEntity
import org.tally.domain.model.OffCacheStatus
import org.tally.data.db.entity.OffProductCacheEntity
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource
import org.tally.domain.model.GoalMode
import org.tally.domain.model.GoalType
import org.tally.domain.model.MeasurementSource
import org.tally.domain.model.QuantityUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataWipeRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: TallyDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
            .allowMainThreadQueries().addCallback(TallyDatabase.seedCallback).build()
    }

    @After
    fun tearDown() = db.close()

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun wipeAllClearsUserTablesKeepsReferenceDataAndDeletesSafetyFile() = runTest {
        val dao = db.backupDao()
        val weight = dao.measurementTypes().first { it.key == "builtin.weight" }.id
        val slot = dao.mealSlots().first().id
        val sql = db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO profile (id, birth_date, sex, height_m, activity_level, created_at, updated_at) VALUES (1, 9000, 'FEMALE', 1.68, 'LIGHT', 1, 2)")
        dao.insertSettings(listOf(AppSettingEntity("units.mass", "KG", 5), AppSettingEntity("ciqual.version", "2020", 6)))
        dao.insertFoods(listOf(FoodEntity(id = 1, name = "Pomme", source = FoodSource.USER, baseUnit = BaseUnit.G, energyKcal = 52.0, proteinG = 0.3, carbsG = 14.0, fatG = 0.2, createdAt = 1, updatedAt = 1)))
        dao.insertGoals(listOf(GoalEntity(1, GoalType.ENERGY_KCAL, 2000.0, GoalMode.FIXED, 19000, 1)))
        dao.insertMeasurements(listOf(MeasurementEntity(1, weight, 72.0, 1_759_000_000_000, "UTC", null, MeasurementSource.MANUAL, null, 1, 1)))
        dao.insertJournal(
            listOf(
                JournalEntryEntity(
                    id = 1, name = "Pomme", brand = null, localEpochDay = 20000, mealSlotId = slot, foodId = 1, recipeId = null,
                    portionId = null, portionCount = null, quantityBase = 100.0, quantityUnit = QuantityUnit.G,
                    energyKcal = 52.0, proteinG = 0.3, carbsG = 14.0, fatG = 0.2, sugarsG = null, satFatG = null,
                    loggedAt = 1_759_000_000_000, zoneId = "UTC", createdAt = 1, updatedAt = 1,
                ),
            ),
        )
        db.offCacheDao().put(OffProductCacheEntity("3017620422003", OffCacheStatus.FOUND, "{}", null, null, 1, 2))
        db.ciqualDao().insertAll(listOf(CiqualFoodEntity(1, "Pomme", "pomme", "01", 52.0, 0.3, 14.0, 0.2, null, null, null, null)))
        val ciqualBefore = count("ciqual_food")
        val safety = File(File(context.filesDir, "backup"), "pre-restore.zip").also { it.parentFile!!.mkdirs(); it.writeText("zip") }

        DataWipeRepositoryImpl(context, db).wipeAll()

        listOf("journal_entry", "measurement", "food", "food_portion", "recipe", "recipe_ingredient", "goal", "profile",
            "off_product_cache", "activity_session", "track_point").forEach { assertEquals(it, 0, count(it)) }
        assertEquals(ciqualBefore, count("ciqual_food"))
        assertEquals(listOf("ciqual.version"), dao.appSettings(emptyList()).map { it.key })
        assertEquals(listOf("breakfast", "lunch", "dinner", "snack"), dao.mealSlots().map { it.builtinKey })
        assertEquals(4, dao.measurementTypes().size)
        assertTrue(dao.measurementTypes().any { it.key == "builtin.weight" })
        assertFalse(safety.exists())
    }
}
