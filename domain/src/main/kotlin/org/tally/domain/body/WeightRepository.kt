package org.tally.domain.body

import kotlinx.coroutines.flow.Flow

/** Source unique du poids (module `:body`). Les autres modules passent par [GetWeightAt]. */
interface WeightRepository {
    /** Pesées triées de la plus récente à la plus ancienne. */
    fun observeEntries(): Flow<List<WeightEntry>>

    suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String? = null): Long
    suspend fun update(entry: WeightEntry)
    suspend fun delete(id: Long)

    suspend fun latestAtOrBefore(instant: Long): WeightEntry?
    suspend fun firstAfter(instant: Long): WeightEntry?
}
