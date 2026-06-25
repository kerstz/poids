package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/** Une prise d'eau horodatée (suivi d'hydratation). */
@Entity(tableName = "water_log", indices = [Index("dateTime")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateTime: LocalDateTime,
    val amountMl: Int,
)
