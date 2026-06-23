package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Agrégat quotidien de pas (une ligne par jour). Le compteur matériel
 * (TYPE_STEP_COUNTER) est cumulatif depuis le boot ; la gestion du baseline et des
 * reboots se fait dans la couche capteur, on ne persiste ici que le total du jour.
 */
@Entity(tableName = "step_day")
data class StepDayEntity(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
    val distanceM: Double,
    val caloriesKcal: Double,
)
