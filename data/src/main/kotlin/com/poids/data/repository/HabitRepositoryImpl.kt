package com.poids.data.repository

import com.poids.data.local.dao.SleepDao
import com.poids.data.local.dao.SubstanceDao
import com.poids.data.local.entity.SleepLogEntity
import com.poids.data.local.entity.SubstanceLogEntity
import com.poids.domain.model.SleepLog
import com.poids.domain.model.SubstanceKind
import com.poids.domain.model.SubstanceLog
import com.poids.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val sleepDao: SleepDao,
    private val substanceDao: SubstanceDao,
) : HabitRepository {

    override fun observeSleep(start: LocalDate, end: LocalDate): Flow<List<SleepLog>> =
        sleepDao.observeRange(start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun addSleep(
        date: LocalDate,
        bedTime: LocalDateTime,
        wakeTime: LocalDateTime,
        quality: Int?,
        note: String?,
    ): Long = sleepDao.insert(
        SleepLogEntity(
            date = date,
            bedTime = bedTime,
            wakeTime = wakeTime,
            durationMin = Duration.between(bedTime, wakeTime).toMinutes().toInt(),
            quality = quality,
            note = note,
        ),
    )

    override suspend fun deleteSleep(id: Long) = sleepDao.deleteById(id)

    override fun observeSubstances(): Flow<List<SubstanceLog>> =
        substanceDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addSubstance(
        dateTime: LocalDateTime,
        kind: SubstanceKind,
        amount: Double,
        unit: String,
        label: String?,
        note: String?,
    ): Long = substanceDao.insert(
        SubstanceLogEntity(
            dateTime = dateTime,
            kind = kind,
            amount = amount,
            unit = unit,
            note = note,
            label = label,
        ),
    )

    override suspend fun updateSubstance(
        id: Long,
        dateTime: LocalDateTime,
        kind: SubstanceKind,
        amount: Double,
        unit: String,
        label: String?,
        note: String?,
    ) = substanceDao.update(id, dateTime, kind, amount, unit, label, note)

    override suspend fun deleteSubstance(id: Long) = substanceDao.deleteById(id)
}
