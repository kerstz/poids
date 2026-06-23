package com.poids.domain.model

import java.time.Instant

/** Session d'activité enregistrée (résultat d'un suivi GPS ou saisie manuelle). */
data class ActivitySession(
    val id: Long = 0L,
    val type: ActivityType,
    val source: ActivitySource,
    val startTime: Instant,
    val endTime: Instant?,
    val durationSec: Long,
    val distanceM: Double,
    val avgSpeedMps: Double,
    val elevationGainM: Double,
    val caloriesKcal: Double,
)

/** Point d'une trace GPS. */
data class RoutePoint(
    val lat: Double,
    val lon: Double,
    val altitudeM: Double?,
    val speedMps: Double?,
    val timestamp: Instant,
)

/** Stats en direct d'une session en cours (publiées par le service de suivi). */
data class LiveActivityStats(
    val type: ActivityType,
    val startTime: Instant,
    val durationSec: Long,
    val distanceM: Double,
    val currentSpeedMps: Double,
    val elevationGainM: Double,
)
