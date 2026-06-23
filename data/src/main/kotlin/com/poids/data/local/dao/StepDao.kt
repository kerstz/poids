package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poids.data.local.entity.StepDayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StepDao {

    @Upsert
    suspend fun upsert(day: StepDayEntity)

    @Query("SELECT * FROM step_day WHERE date = :date LIMIT 1")
    fun observeDay(date: LocalDate): Flow<StepDayEntity?>

    @Query("SELECT * FROM step_day WHERE date >= :start AND date <= :end ORDER BY date")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<StepDayEntity>>
}
