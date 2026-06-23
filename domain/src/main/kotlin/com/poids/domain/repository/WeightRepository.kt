package com.poids.domain.repository

import com.poids.domain.model.WeightEntry
import kotlinx.coroutines.flow.Flow

/** Historique des pesées. */
interface WeightRepository {
    fun observeAll(): Flow<List<WeightEntry>>

    fun observeLatest(): Flow<WeightEntry?>

    suspend fun add(entry: WeightEntry): Long

    suspend fun delete(id: Long)
}
