// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

enum class ActivityType { WALK, RUN }

enum class SessionStatus { RECORDING, PAUSED, FINISHED }

enum class SessionSource { RECORDED, MANUAL }

/** Session sportive. Distances en mètres, durées en ms, instants en epoch ms UTC. */
data class ActivitySession(
    val id: Long = 0,
    val uuid: String,
    val type: ActivityType,
    val status: SessionStatus,
    val source: SessionSource = SessionSource.RECORDED,
    val startedAt: Long,
    val endedAt: Long? = null,
    val zoneId: String,
    /** epochDay de `startedAt` dans `zoneId` (même convention que le journal). */
    val localDate: Long,
    /** Instant du dernier démarrage/reprise si RECORDING, sinon null. */
    val activeSince: Long? = null,
    /** Somme des intervalles de mouvement fermés. */
    val movingMs: Long = 0,
    val distanceM: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val elevationLossM: Double = 0.0,
    val energyKcal: Double? = null,
    val energyMethod: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Temps en mouvement à l'instant `now` (intervalle courant inclus). */
    fun movingMsAt(now: Long): Long {
        val since = activeSince ?: return movingMs
        return movingMs + (now - since).coerceAtLeast(0)
    }
}

data class TrackPoint(
    val seq: Int,
    val segment: Int,
    val timeMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double?,
    val hAccM: Double,
    val speedMps: Double?,
)

/** Fix GPS brut avant filtrage. */
data class GpsFix(
    val timeMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double?,
    val accuracyM: Double,
    val speedMps: Double?,
)

data class SessionAggregates(
    val distanceM: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
)
