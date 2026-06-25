package com.poids.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.poids.data.local.converter.Converters
import com.poids.data.local.dao.ActivityDao
import com.poids.data.local.dao.FoodItemDao
import com.poids.data.local.dao.MealLogDao
import com.poids.data.local.dao.SleepDao
import com.poids.data.local.dao.StepDao
import com.poids.data.local.dao.SubstanceDao
import com.poids.data.local.dao.UserProfileDao
import com.poids.data.local.dao.WaterDao
import com.poids.data.local.dao.WeightDao
import com.poids.data.local.entity.ActivitySessionEntity
import com.poids.data.local.entity.FoodItemEntity
import com.poids.data.local.entity.MealLogEntity
import com.poids.data.local.entity.RoutePointEntity
import com.poids.data.local.entity.SleepLogEntity
import com.poids.data.local.entity.StepDayEntity
import com.poids.data.local.entity.SubstanceLogEntity
import com.poids.data.local.entity.UserProfileEntity
import com.poids.data.local.entity.WaterLogEntity
import com.poids.data.local.entity.WeightEntryEntity

@Database(
    entities = [
        UserProfileEntity::class,
        WeightEntryEntity::class,
        FoodItemEntity::class,
        MealLogEntity::class,
        ActivitySessionEntity::class,
        RoutePointEntity::class,
        StepDayEntity::class,
        SleepLogEntity::class,
        SubstanceLogEntity::class,
        WaterLogEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PoidsDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun activityDao(): ActivityDao
    abstract fun stepDao(): StepDao
    abstract fun sleepDao(): SleepDao
    abstract fun substanceDao(): SubstanceDao
    abstract fun waterDao(): WaterDao

    companion object {
        const val NAME = "poids.db"

        /** v2 : ajout du nom libre de substance (suivi de conso). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE substance_log ADD COLUMN label TEXT")
            }
        }

        /** v3 : table d'hydratation (suivi de conso d'eau). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS water_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "dateTime INTEGER NOT NULL, " +
                        "amountMl INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_water_log_dateTime ON water_log (dateTime)")
            }
        }
    }
}
