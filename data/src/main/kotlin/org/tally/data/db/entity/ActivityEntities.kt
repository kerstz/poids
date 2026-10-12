// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tally.domain.activity.ActivityType
import org.tally.domain.activity.SessionSource
import org.tally.domain.activity.SessionStatus

@Entity(
    tableName = "activity_session",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index("started_at"),
        Index("status"),
        Index("local_date"),
    ],
)
data class ActivitySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val type: ActivityType,
    val status: SessionStatus,
    val source: SessionSource,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "local_date") val localDate: Long,
    @ColumnInfo(name = "active_since") val activeSince: Long? = null,
    @ColumnInfo(name = "moving_ms") val movingMs: Long = 0,
    @ColumnInfo(name = "distance_m") val distanceM: Double = 0.0,
    @ColumnInfo(name = "elevation_gain_m") val elevationGainM: Double = 0.0,
    @ColumnInfo(name = "elevation_loss_m") val elevationLossM: Double = 0.0,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double? = null,
    @ColumnInfo(name = "energy_method") val energyMethod: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "track_point",
    primaryKeys = ["session_id", "seq"],
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TrackPointEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val seq: Int,
    val segment: Int,
    @ColumnInfo(name = "time_ms") val timeMs: Long,
    val lat: Double,
    val lon: Double,
    @ColumnInfo(name = "alt_m") val altM: Double? = null,
    @ColumnInfo(name = "h_acc_m") val hAccM: Double,
    @ColumnInfo(name = "speed_mps") val speedMps: Double? = null,
)
