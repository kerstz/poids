package com.poids.data.repository

import com.poids.data.local.dao.WaterDao
import com.poids.data.local.entity.WaterLogEntity
import com.poids.domain.model.WaterLog
import com.poids.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val dao: WaterDao,
) : WaterRepository {

    override fun observeForDate(date: LocalDate): Flow<List<WaterLog>> =
        dao.observeRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            .map { list -> list.map { WaterLog(it.id, it.dateTime, it.amountMl) } }

    override suspend fun addWater(amountMl: Int): Long =
        dao.insert(WaterLogEntity(dateTime = LocalDateTime.now(), amountMl = amountMl))

    override suspend fun deleteLastForDate(date: LocalDate) {
        dao.deleteLastInRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
    }
}
