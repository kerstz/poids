package com.poids.domain.repository

import com.poids.domain.model.ActivitySession
import com.poids.domain.model.RoutePoint
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeSessions(): Flow<List<ActivitySession>>

    /** Enregistre une session et sa trace. Retourne l'id de la session. */
    suspend fun saveSession(session: ActivitySession, route: List<RoutePoint>): Long
}
