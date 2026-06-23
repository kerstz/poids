package com.poids.data.repository

import com.poids.data.local.entity.UserProfileEntity
import com.poids.data.local.entity.WeightEntryEntity
import com.poids.domain.model.UserProfile
import com.poids.domain.model.WeightEntry
import java.time.Instant

// --- UserProfile ---

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    heightCm = heightCm,
    weightKg = weightKg,
    birthDate = birthDate,
    sex = sex,
    activityLevel = activityLevel,
    goalType = goalType,
    targetWeightKg = targetWeightKg,
    weeklyRateKg = weeklyRateKg,
)

fun UserProfile.toEntity(updatedAt: Instant): UserProfileEntity = UserProfileEntity(
    id = UserProfileEntity.SINGLETON_ID,
    heightCm = heightCm,
    weightKg = weightKg,
    birthDate = birthDate,
    sex = sex,
    activityLevel = activityLevel,
    goalType = goalType,
    targetWeightKg = targetWeightKg,
    weeklyRateKg = weeklyRateKg,
    updatedAt = updatedAt,
)

// --- WeightEntry ---

fun WeightEntryEntity.toDomain(): WeightEntry = WeightEntry(
    id = id,
    date = date,
    weightKg = weightKg,
    note = note,
)

fun WeightEntry.toEntity(): WeightEntryEntity = WeightEntryEntity(
    id = id,
    date = date,
    weightKg = weightKg,
    note = note,
)
