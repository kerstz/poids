// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tally.domain.model.MeasurementDimension
import org.tally.domain.model.MeasurementSource

@Entity(
    tableName = "measurement_type",
    indices = [Index(value = ["key"], unique = true)],
)
data class MeasurementTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** `builtin.weight`, `builtin.waist`, `custom.<uuid>`… */
    val key: String,
    val name: String? = null,
    val dimension: MeasurementDimension,
    @ColumnInfo(name = "display_unit") val displayUnit: String? = null,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    val position: Int,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
)

/** Format long : 1 ligne = 1 valeur numérique en unité SI. Le poids n'existe que ici. */
@Entity(
    tableName = "measurement",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["type_id", "measured_at"]),
        Index("group_id"),
        Index(value = ["type_id", "measured_at", "source"], unique = true),
    ],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "type_id") val typeId: Long,
    @ColumnInfo(name = "value_si") val valueSi: Double,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "group_id") val groupId: Long? = null,
    val source: MeasurementSource,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
