package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.poids.domain.model.ActivityLevel
import com.poids.domain.model.GoalType
import com.poids.domain.model.Sex
import java.time.Instant
import java.time.LocalDate

/** Profil utilisateur — une seule ligne (id fixé à 0). */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val heightCm: Double,
    val weightKg: Double,
    val birthDate: LocalDate,
    val sex: Sex,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val targetWeightKg: Double?,
    val weeklyRateKg: Double,
    val updatedAt: Instant,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
