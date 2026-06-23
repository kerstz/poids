package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "weight_entry", indices = [Index("date")])
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: LocalDate,
    val weightKg: Double,
    val note: String?,
)
