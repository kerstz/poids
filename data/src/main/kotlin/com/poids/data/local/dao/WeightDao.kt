package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_entry ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entry ORDER BY date DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>

    @Insert
    suspend fun insert(entry: WeightEntryEntity): Long

    @Query("DELETE FROM weight_entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}
