// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import org.tally.data.db.entity.AppSettingEntity
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity
import org.tally.data.db.entity.GoalEntity
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.data.db.entity.MealSlotEntity
import org.tally.data.db.entity.MeasurementEntity
import org.tally.data.db.entity.MeasurementTypeEntity
import org.tally.data.db.entity.ProfileEntity
import org.tally.data.db.entity.RecipeEntity
import org.tally.data.db.entity.RecipeIngredientEntity

/** Toutes les données utilisateur (hors caches jetables), dans l'ordre des FK. */
data class BackupData(
    val profile: List<ProfileEntity>,
    val appSettings: List<AppSettingEntity>,
    val mealSlots: List<MealSlotEntity>,
    val measurementTypes: List<MeasurementTypeEntity>,
    val foods: List<FoodEntity>,
    val foodPortions: List<FoodPortionEntity>,
    val recipes: List<RecipeEntity>,
    val recipeIngredients: List<RecipeIngredientEntity>,
    val goals: List<GoalEntity>,
    val measurements: List<MeasurementEntity>,
    val journalEntries: List<JournalEntryEntity>,
) {
    fun counts(): Map<String, Int> = linkedMapOf(
        "profile" to profile.size,
        "app_setting" to appSettings.size,
        "meal_slot" to mealSlots.size,
        "measurement_type" to measurementTypes.size,
        "food" to foods.size,
        "food_portion" to foodPortions.size,
        "recipe" to recipes.size,
        "recipe_ingredient" to recipeIngredients.size,
        "goal" to goals.size,
        "measurement" to measurements.size,
        "journal_entry" to journalEntries.size,
    )
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM profile ORDER BY id") suspend fun profile(): List<ProfileEntity>
    @Query("SELECT * FROM app_setting WHERE `key` NOT IN (:excluded) ORDER BY `key`")
    suspend fun appSettings(excluded: List<String>): List<AppSettingEntity>
    @Query("SELECT * FROM meal_slot ORDER BY id") suspend fun mealSlots(): List<MealSlotEntity>
    @Query("SELECT * FROM measurement_type ORDER BY id") suspend fun measurementTypes(): List<MeasurementTypeEntity>
    @Query("SELECT * FROM food ORDER BY id") suspend fun foods(): List<FoodEntity>
    @Query("SELECT * FROM food_portion ORDER BY id") suspend fun foodPortions(): List<FoodPortionEntity>
    @Query("SELECT * FROM recipe ORDER BY id") suspend fun recipes(): List<RecipeEntity>
    @Query("SELECT * FROM recipe_ingredient ORDER BY id") suspend fun recipeIngredients(): List<RecipeIngredientEntity>
    @Query("SELECT * FROM goal ORDER BY id") suspend fun goals(): List<GoalEntity>
    @Query("SELECT * FROM measurement ORDER BY id") suspend fun measurements(): List<MeasurementEntity>
    @Query("SELECT * FROM journal_entry ORDER BY id") suspend fun journalEntries(): List<JournalEntryEntity>

    @Query("DELETE FROM journal_entry") suspend fun deleteJournal()
    @Query("DELETE FROM measurement") suspend fun deleteMeasurements()
    @Query("DELETE FROM goal") suspend fun deleteGoals()
    @Query("DELETE FROM recipe_ingredient") suspend fun deleteIngredients()
    @Query("DELETE FROM recipe") suspend fun deleteRecipes()
    @Query("DELETE FROM food_portion") suspend fun deletePortions()
    @Query("DELETE FROM food") suspend fun deleteFoods()
    @Query("DELETE FROM measurement_type") suspend fun deleteMeasurementTypes()
    @Query("DELETE FROM meal_slot") suspend fun deleteMealSlots()
    @Query("DELETE FROM app_setting WHERE `key` NOT IN (:excluded)") suspend fun deleteSettings(excluded: List<String>)
    @Query("DELETE FROM profile") suspend fun deleteProfile()

    @Insert suspend fun insertProfile(items: List<ProfileEntity>)
    @Insert suspend fun insertSettings(items: List<AppSettingEntity>)
    @Insert suspend fun insertMealSlots(items: List<MealSlotEntity>)
    @Insert suspend fun insertMeasurementTypes(items: List<MeasurementTypeEntity>)
    @Insert suspend fun insertFoods(items: List<FoodEntity>)
    @Insert suspend fun insertPortions(items: List<FoodPortionEntity>)
    @Insert suspend fun insertRecipes(items: List<RecipeEntity>)
    @Insert suspend fun insertIngredients(items: List<RecipeIngredientEntity>)
    @Insert suspend fun insertGoals(items: List<GoalEntity>)
    @Insert suspend fun insertMeasurements(items: List<MeasurementEntity>)
    @Insert suspend fun insertJournal(items: List<JournalEntryEntity>)

    @Query("SELECT COUNT(*) FROM profile") suspend fun countProfile(): Int
    @Query("SELECT COUNT(*) FROM food") suspend fun countFoods(): Int
    @Query("SELECT COUNT(*) FROM journal_entry") suspend fun countJournal(): Int
    @Query("SELECT COUNT(*) FROM measurement") suspend fun countMeasurements(): Int

    /**
     * Remplace toutes les données dans une seule transaction : la moindre exception annule tout.
     * Ordre inverse des FK pour supprimer, ordre des FK pour insérer ; `excluded` = réglages techniques
     * de l'appareil, ni effacés ni écrits.
     */
    @Transaction
    suspend fun replaceAll(data: BackupData, excluded: List<String>) {
        deleteJournal(); deleteMeasurements(); deleteGoals(); deleteIngredients(); deleteRecipes()
        deletePortions(); deleteFoods(); deleteMeasurementTypes(); deleteMealSlots()
        deleteSettings(excluded); deleteProfile()
        data.profile.chunked(CHUNK).forEach { insertProfile(it) }
        data.appSettings.chunked(CHUNK).forEach { insertSettings(it) }
        data.mealSlots.chunked(CHUNK).forEach { insertMealSlots(it) }
        data.measurementTypes.chunked(CHUNK).forEach { insertMeasurementTypes(it) }
        data.foods.chunked(CHUNK).forEach { insertFoods(it) }
        data.foodPortions.chunked(CHUNK).forEach { insertPortions(it) }
        data.recipes.chunked(CHUNK).forEach { insertRecipes(it) }
        data.recipeIngredients.chunked(CHUNK).forEach { insertIngredients(it) }
        data.goals.chunked(CHUNK).forEach { insertGoals(it) }
        data.measurements.chunked(CHUNK).forEach { insertMeasurements(it) }
        data.journalEntries.chunked(CHUNK).forEach { insertJournal(it) }
        check(countFoods() == data.foods.size && countJournal() == data.journalEntries.size &&
            countMeasurements() == data.measurements.size && countProfile() == data.profile.size) { "COUNT_MISMATCH" }
    }

    private companion object {
        const val CHUNK = 500
    }
}
