package org.tally.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.tally.data.db.dao.AppSettingDao
import org.tally.data.db.dao.BackupDao
import org.tally.data.db.dao.CiqualDao
import org.tally.data.db.dao.FoodDao
import org.tally.data.db.dao.GoalDao
import org.tally.data.db.dao.JournalDao
import org.tally.data.db.dao.MealSlotDao
import org.tally.data.db.dao.OffCacheDao
import org.tally.data.db.dao.MeasurementDao
import org.tally.data.db.dao.RecipeDao
import org.tally.data.db.entity.AppSettingEntity
import org.tally.data.db.entity.CiqualFoodEntity
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity
import org.tally.data.db.entity.GoalEntity
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.data.db.entity.MealSlotEntity
import org.tally.data.db.entity.MeasurementEntity
import org.tally.data.db.entity.MeasurementTypeEntity
import org.tally.data.db.entity.OffProductCacheEntity
import org.tally.data.db.entity.ProfileEntity
import org.tally.data.db.entity.RecipeEntity
import org.tally.data.db.entity.RecipeIngredientEntity

@Database(
    entities = [
        ProfileEntity::class,
        GoalEntity::class,
        MealSlotEntity::class,
        FoodEntity::class,
        FoodPortionEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        JournalEntryEntity::class,
        MeasurementTypeEntity::class,
        MeasurementEntity::class,
        AppSettingEntity::class,
        OffProductCacheEntity::class,
        CiqualFoodEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao
    abstract fun journalDao(): JournalDao
    abstract fun mealSlotDao(): MealSlotDao
    abstract fun goalDao(): GoalDao
    abstract fun offCacheDao(): OffCacheDao
    abstract fun ciqualDao(): CiqualDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "tally.db"

        val ALL_MIGRATIONS = arrayOf(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)

        /** Données de référence insérées à la création : créneaux de repas et types de mesure. */
        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                listOf("breakfast", "lunch", "dinner", "snack").forEachIndexed { index, key ->
                    db.execSQL(
                        "INSERT INTO meal_slot (name, builtin_key, position) VALUES (NULL, ?, ?)",
                        arrayOf<Any>(key, index),
                    )
                }
                listOf(
                    Triple("builtin.weight", "MASS", 0),
                    Triple("builtin.body_fat", "PERCENT", 1),
                    Triple("builtin.waist", "LENGTH", 2),
                    Triple("builtin.hips", "LENGTH", 3),
                ).forEach { (key, dimension, position) ->
                    db.execSQL(
                        "INSERT INTO measurement_type (`key`, dimension, is_enabled, position) VALUES (?, ?, 1, ?)",
                        arrayOf<Any>(key, dimension, position),
                    )
                }
            }
        }
    }
}
