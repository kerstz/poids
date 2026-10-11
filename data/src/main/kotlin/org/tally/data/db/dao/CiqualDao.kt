package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import org.tally.data.db.entity.CiqualFoodEntity

@Dao
interface CiqualDao {
    @Query(
        "SELECT * FROM ciqual_food WHERE name_norm LIKE '%' || :query || '%' ESCAPE '\\' " +
            "ORDER BY length(name), name LIMIT 50",
    )
    suspend fun search(query: String): List<CiqualFoodEntity>

    @Query("SELECT COUNT(*) FROM ciqual_food")
    suspend fun count(): Int

    @Query("DELETE FROM ciqual_food")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<CiqualFoodEntity>)

    @Transaction
    suspend fun replaceAll(items: List<CiqualFoodEntity>) {
        deleteAll()
        insertAll(items)
    }
}
