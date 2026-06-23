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

    /**
     * Recherche plein-texte via la table FTS4. `query` doit être un terme FTS
     * (ex. "yaourt*" pour le préfixe). Le join relie le rowid FTS à l'aliment.
     */
    @Query(
        """
        SELECT f.* FROM food_item f
        JOIN food_item_fts fts ON f.id = fts.rowid
        WHERE food_item_fts MATCH :query
        ORDER BY f.name
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int = 50): List<FoodItemEntity>
}
