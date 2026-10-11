package org.tally.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.domain.model.QuantityUnit

data class JournalRow(
    val id: Long,
    @ColumnInfo(name = "meal_slot_id") val mealSlotId: Long,
    @ColumnInfo(name = "recipe_id") val recipeId: Long?,
    val name: String,
    @ColumnInfo(name = "quantity_base") val quantityBase: Double,
    @ColumnInfo(name = "quantity_unit") val quantityUnit: QuantityUnit,
    @ColumnInfo(name = "portion_count") val portionCount: Double?,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
    @ColumnInfo(name = "sugars_g") val sugarsG: Double?,
    @ColumnInfo(name = "sat_fat_g") val satFatG: Double?,
    @ColumnInfo(name = "fiber_g") val fiberG: Double?,
    @ColumnInfo(name = "salt_g") val saltG: Double?,
)

@Dao
interface JournalDao {
    @Query(
        "SELECT j.id, j.meal_slot_id, j.recipe_id, j.name, " +
            "j.quantity_base, j.quantity_unit, j.portion_count, j.energy_kcal, j.protein_g, j.carbs_g, j.fat_g, " +
            "j.sugars_g, j.sat_fat_g, j.fiber_g, j.salt_g " +
            "FROM journal_entry j " +
            "WHERE j.local_date = :epochDay ORDER BY j.logged_at, j.id",
    )
    fun observeDay(epochDay: Long): Flow<List<JournalRow>>

    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Query("SELECT * FROM journal_entry WHERE id = :id")
    suspend fun get(id: Long): JournalEntryEntity?

    @Query("DELETE FROM journal_entry WHERE id = :id")
    suspend fun delete(id: Long)
}
