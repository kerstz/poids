package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.MealLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MealLogDao {

    @Insert
    suspend fun insert(entry: MealLogEntity): Long

    @Delete
    suspend fun delete(entry: MealLogEntity)

    @Query("SELECT * FROM meal_log WHERE dateTime >= :start AND dateTime < :end ORDER BY dateTime")
    fun observeBetween(start: LocalDateTime, end: LocalDateTime): Flow<List<MealLogEntity>>
}
