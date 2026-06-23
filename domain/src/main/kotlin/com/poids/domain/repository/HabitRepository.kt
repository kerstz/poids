package com.poids.domain.repository

import com.poids.domain.model.SleepLog
import com.poids.domain.model.SubstanceKind
import com.poids.domain.model.SubstanceLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface HabitRepository {
    // Sommeil
    fun observeSleep(start: LocalDate, end: LocalDate): Flow<List<SleepLog>>
    suspend fun addSleep(date: LocalDate, bedTime: LocalDateTime, wakeTime: LocalDateTime, quality: Int?, note: String?): Long
    suspend fun deleteSleep(id: Long)

    // Substances
    fun observeSubstances(): Flow<List<SubstanceLog>>
    suspend fun addSubstance(dateTime: LocalDateTime, kind: SubstanceKind, amount: Double, unit: String, label: String?, note: String?): Long
    suspend fun updateSubstance(id: Long, dateTime: LocalDateTime, kind: SubstanceKind, amount: Double, unit: String, label: String?): Unit
    suspend fun deleteSubstance(id: Long)
}
