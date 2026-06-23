package com.poids.data.repository

import com.poids.data.local.dao.WeightDao
import com.poids.domain.model.WeightEntry
import com.poids.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightDao,
) : WeightRepository {

    override fun observeAll(): Flow<List<WeightEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeLatest(): Flow<WeightEntry?> =
        dao.observeLatest().map { it?.toDomain() }

    override suspend fun add(entry: WeightEntry): Long = dao.insert(entry.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
