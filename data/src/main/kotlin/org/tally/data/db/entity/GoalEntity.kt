// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tally.domain.model.GoalMode
import org.tally.domain.model.GoalType

/** Objectif historisé : valeur applicable = ligne `valid_from <= jour` la plus récente. */
@Entity(
    tableName = "goal",
    indices = [Index(value = ["type", "valid_from"], unique = true)],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: GoalType,
    val value: Double,
    val mode: GoalMode,
    @ColumnInfo(name = "valid_from") val validFromEpochDay: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
