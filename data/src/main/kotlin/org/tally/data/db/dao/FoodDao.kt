package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity

@Dao
interface FoodDao {
    @Query(
        "SELECT * FROM food WHERE archived_at IS NULL AND " +
            "(name LIKE '%' || :query || '%' ESCAPE '\\' OR brand LIKE '%' || :query || '%' ESCAPE '\\') " +
            "ORDER BY name COLLATE NOCASE LIMIT 200",
    )
    fun search(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun get(id: Long): FoodEntity?

    @Query("SELECT * FROM food WHERE barcode = :barcode AND archived_at IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun findByBarcode(barcode: String): FoodEntity?

    @Query("SELECT * FROM food WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcodeAny(barcode: String): FoodEntity?

    @Query("UPDATE food SET archived_at = NULL, updated_at = :at WHERE id = :id")
    suspend fun unarchive(id: Long, at: Long)

    @Query("SELECT * FROM food_portion WHERE food_id = :foodId ORDER BY id")
    suspend fun portionsFor(foodId: Long): List<FoodPortionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(food: FoodEntity): Long

    @Update
    suspend fun update(food: FoodEntity)

    @Insert
    suspend fun insertPortions(portions: List<FoodPortionEntity>)

    @Query("DELETE FROM food_portion WHERE food_id = :foodId")
    suspend fun deletePortionsFor(foodId: Long)

    @Query("UPDATE food SET archived_at = :at, updated_at = :at WHERE id = :id")
    suspend fun archive(id: Long, at: Long)

    @Transaction
    suspend fun insertWithPortions(food: FoodEntity, portions: List<FoodPortionEntity>): Long {
        val id = insert(food)
        insertPortions(portions.map { it.copy(id = 0, foodId = id) })
        return id
    }

    @Transaction
    suspend fun updateWithPortions(food: FoodEntity, portions: List<FoodPortionEntity>) {
        update(food)
        deletePortionsFor(food.id)
        insertPortions(portions.map { it.copy(id = 0, foodId = food.id) })
    }
}
