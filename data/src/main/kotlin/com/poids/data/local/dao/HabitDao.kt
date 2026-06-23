package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.SleepLogEntity
import com.poids.data.local.entity.SubstanceLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SleepDao {

    @Insert
    suspend fun insert(entry: SleepLogEntity): Long

    @Delete
    suspend fun delete(entry: SleepLogEntity)

    @Query("SELECT * FROM sleep_log WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<SleepLogEntity>>
}

@Dao
interface SubstanceDao {

    @Insert
    suspend fun insert(entry: SubstanceLogEntity): Long

    @Delete
    suspend fun delete(entry: SubstanceLogEntity)

    @Query("SELECT * FROM substance_log ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<SubstanceLogEntity>>
}
