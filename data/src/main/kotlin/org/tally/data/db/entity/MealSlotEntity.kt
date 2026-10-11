package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_slot",
    indices = [Index(value = ["builtin_key"], unique = true)],
)
data class MealSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null = libellé issu de `builtin_key` (i18n). */
    val name: String?,
    @ColumnInfo(name = "builtin_key") val builtinKey: String?,
    val position: Int,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
)
