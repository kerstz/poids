package com.poids.domain.repository

import com.poids.domain.model.ActivitySession
import com.poids.domain.model.ActivityType
import com.poids.domain.model.RoutePoint
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeSessions(): Flow<List<ActivitySession>>

    /** Enregistre une session et sa trace. Retourne l'id de la session. */
    suspend fun saveSession(session: ActivitySession, route: List<RoutePoint>): Long

    /**
     * Saisie manuelle d'un effort non mesuré (source `MANUAL`, sans trace ni distance).
     * Compte directement dans les calories brûlées du jour. Retourne l'id.
     */
    suspend fun addManualSession(type: ActivityType, durationSec: Long, caloriesKcal: Double): Long
}
