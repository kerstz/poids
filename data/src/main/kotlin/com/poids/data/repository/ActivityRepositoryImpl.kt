package com.poids.data.repository

import com.poids.data.local.dao.ActivityDao
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.ActivitySource
import com.poids.domain.model.ActivityType
import com.poids.domain.model.RoutePoint
import com.poids.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val dao: ActivityDao,
) : ActivityRepository {

    override fun observeSessions(): Flow<List<ActivitySession>> =
        dao.observeSessions().map { list -> list.map { it.toDomain() } }

    override suspend fun saveSession(session: ActivitySession, route: List<RoutePoint>): Long {
        val id = dao.insertSession(session.toEntity())
        if (route.isNotEmpty()) {
            dao.insertRoutePoints(route.map { it.toEntity(id) })
        }
        return id
    }

    override suspend fun addManualSession(
        type: ActivityType,
        durationSec: Long,
        caloriesKcal: Double,
    ): Long {
        val now = Instant.now()
        val session = ActivitySession(
            type = type,
            source = ActivitySource.MANUAL,
            startTime = now,
            endTime = now,
            durationSec = durationSec,
            distanceM = 0.0,
            avgSpeedMps = 0.0,
            elevationGainM = 0.0,
            caloriesKcal = caloriesKcal,
        )
        return dao.insertSession(session.toEntity())
    }
}
