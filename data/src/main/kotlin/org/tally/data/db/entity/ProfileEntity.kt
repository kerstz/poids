// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.tally.domain.model.ActivityLevel
import org.tally.domain.model.Sex

/** Singleton (id = 1). Aucun champ poids : source unique = `measurement`. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = SINGLETON_ID,
    @ColumnInfo(name = "birth_date") val birthDateEpochDay: Long?,
    val sex: Sex,
    @ColumnInfo(name = "height_m") val heightM: Double?,
    @ColumnInfo(name = "activity_level") val activityLevel: ActivityLevel,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}
