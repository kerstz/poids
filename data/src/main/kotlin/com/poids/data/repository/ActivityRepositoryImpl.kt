package com.poids.data.repository

import com.poids.data.local.dao.ActivityDao
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.RoutePoint
import com.poids.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
}
