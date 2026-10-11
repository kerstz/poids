package org.tally.data.body

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tally.core.time.AppClock
import org.tally.data.db.dao.MeasurementDao
import org.tally.data.db.entity.MeasurementEntity
import org.tally.domain.body.WeightEntry
import org.tally.domain.body.WeightRepository
import org.tally.domain.model.MeasurementSource

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val dao: MeasurementDao,
    private val clock: AppClock,
) : WeightRepository {

    override fun observeEntries(): Flow<List<WeightEntry>> =
        dao.observeByKey(WEIGHT_KEY).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String?): Long {
        val typeId = requireNotNull(dao.typeId(WEIGHT_KEY)) { "measurement_type $WEIGHT_KEY manquant" }
        val now = clock.nowMillis()
        return dao.insert(
            MeasurementEntity(
                typeId = typeId,
                valueSi = valueKg,
                measuredAt = measuredAt,
                zoneId = zoneId,
                source = MeasurementSource.MANUAL,
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun update(entry: WeightEntry) {
        val existing = dao.get(entry.id) ?: return
        dao.update(
            existing.copy(
                valueSi = entry.valueKg,
                measuredAt = entry.measuredAt,
                zoneId = entry.zoneId,
                note = entry.note,
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun latestAtOrBefore(instant: Long): WeightEntry? =
        dao.latestAtOrBefore(WEIGHT_KEY, instant)?.toDomain()

    override suspend fun firstAfter(instant: Long): WeightEntry? =
        dao.firstAfter(WEIGHT_KEY, instant)?.toDomain()

    private fun MeasurementEntity.toDomain() = WeightEntry(id, valueSi, measuredAt, zoneId, note)

    private companion object {
        const val WEIGHT_KEY = "builtin.weight"
    }
}
