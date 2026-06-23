package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poids.domain.model.ActivitySource
import com.poids.domain.model.ActivityType
import java.time.Instant

/** Une session d'activité (marche, course, vélo…). */
@Entity(tableName = "activity_session")
data class ActivitySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: ActivityType,
    val source: ActivitySource,
    val startTime: Instant,
    val endTime: Instant?,
    val durationSec: Long,
    val distanceM: Double,
    val avgSpeedMps: Double,
    val elevationGainM: Double,
    val caloriesKcal: Double,
    val note: String?,
)

/**
 * Point GPS d'une trace, rattaché à une session. Supprimé en cascade avec la session.
 * Altitude issue du baromètre quand disponible (plus fiable que l'altitude GPS).
 */
@Entity(
    tableName = "route_point",
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val lat: Double,
    val lon: Double,
    val altitudeM: Double?,
    val speedMps: Double?,
    val timestamp: Instant,
)
