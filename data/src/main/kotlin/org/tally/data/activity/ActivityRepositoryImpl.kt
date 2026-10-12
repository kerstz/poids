// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.activity

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tally.data.db.dao.ActivityDao
import org.tally.data.db.entity.ActivitySessionEntity
import org.tally.data.db.entity.TrackPointEntity
import org.tally.domain.activity.ActivityRepository
import org.tally.domain.activity.ActivitySession
import org.tally.domain.activity.SessionAggregates
import org.tally.domain.activity.TrackPoint

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val dao: ActivityDao,
) : ActivityRepository {

    override fun observeSessions(): Flow<List<ActivitySession>> =
        dao.observeSessions().map { rows -> rows.map { it.toDomain() } }

    override fun observeSession(id: Long): Flow<ActivitySession?> =
        dao.observeSession(id).map { it?.toDomain() }

    override suspend fun activeSession(): ActivitySession? = dao.active()?.toDomain()

    override suspend fun get(id: Long): ActivitySession? = dao.get(id)?.toDomain()

    override suspend fun points(sessionId: Long): List<TrackPoint> =
        dao.points(sessionId).map { it.toDomain() }

    override suspend fun lastPoint(sessionId: Long): TrackPoint? = dao.lastPoint(sessionId)?.toDomain()

    override suspend fun create(session: ActivitySession): Long = dao.insert(session.toEntity())

    override suspend fun update(session: ActivitySession) = dao.update(session.toEntity())

    override suspend fun appendBatch(
        sessionId: Long,
        points: List<TrackPoint>,
        aggregates: SessionAggregates,
        now: Long,
    ) = dao.appendBatch(
        sessionId,
        points.map { it.toEntity(sessionId) },
        aggregates.distanceM,
        aggregates.elevationGainM,
        aggregates.elevationLossM,
        now,
    )

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun restore(session: ActivitySession, points: List<TrackPoint>) =
        dao.restore(session.toEntity(), points.map { it.toEntity(session.id) })

    override suspend fun finishedWithoutEnergy(): List<ActivitySession> =
        dao.finishedWithoutEnergy().map { it.toDomain() }
}

internal fun ActivitySessionEntity.toDomain() = ActivitySession(
    id = id, uuid = uuid, type = type, status = status, source = source,
    startedAt = startedAt, endedAt = endedAt, zoneId = zoneId, localDate = localDate,
    activeSince = activeSince, movingMs = movingMs, distanceM = distanceM,
    elevationGainM = elevationGainM, elevationLossM = elevationLossM,
    energyKcal = energyKcal, energyMethod = energyMethod, note = note,
    createdAt = createdAt, updatedAt = updatedAt,
)

internal fun ActivitySession.toEntity() = ActivitySessionEntity(
    id = id, uuid = uuid, type = type, status = status, source = source,
    startedAt = startedAt, endedAt = endedAt, zoneId = zoneId, localDate = localDate,
    activeSince = activeSince, movingMs = movingMs, distanceM = distanceM,
    elevationGainM = elevationGainM, elevationLossM = elevationLossM,
    energyKcal = energyKcal, energyMethod = energyMethod, note = note,
    createdAt = createdAt, updatedAt = updatedAt,
)

internal fun TrackPointEntity.toDomain() = TrackPoint(
    seq = seq, segment = segment, timeMs = timeMs, lat = lat, lon = lon,
    altM = altM, hAccM = hAccM, speedMps = speedMps,
)

internal fun TrackPoint.toEntity(sessionId: Long) = TrackPointEntity(
    sessionId = sessionId, seq = seq, segment = segment, timeMs = timeMs, lat = lat, lon = lon,
    altM = altM, hAccM = hAccM, speedMps = speedMps,
)
