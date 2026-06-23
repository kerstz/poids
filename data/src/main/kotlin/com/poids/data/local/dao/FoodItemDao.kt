package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poids.data.local.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {

    /** Insère ou met à jour (cache write-through). Retourne l'id de la ligne. */
    @Upsert
    suspend fun upsert(item: FoodItemEntity): Long

    @Query("SELECT * FROM food_item WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodItemEntity?

    @Query("SELECT * FROM food_item WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<FoodItemEntity?>

    /** Recherche par nom/marque (LIKE, insensible à la casse via NOCASE de SQLite). */
    @Query(
        """
        SELECT * FROM food_item
        WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'
        ORDER BY name
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int = 50): List<FoodItemEntity>
}
