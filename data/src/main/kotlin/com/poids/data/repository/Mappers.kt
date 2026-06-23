package com.poids.data.repository

import com.poids.data.local.entity.ActivitySessionEntity
import com.poids.data.local.entity.FoodItemEntity
import com.poids.data.local.entity.RoutePointEntity
import com.poids.data.local.entity.StepDayEntity
import com.poids.data.local.entity.UserProfileEntity
import com.poids.data.local.entity.WeightEntryEntity
import com.poids.data.local.relation.MealWithFood
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.FoodItem
import com.poids.domain.model.LoggedMeal
import com.poids.domain.model.RoutePoint
import com.poids.domain.model.StepDay
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

// --- FoodItem ---

fun FoodItemEntity.toDomain(): FoodItem = FoodItem(
    id = id,
    barcode = barcode,
    name = name,
    brand = brand,
    source = source,
    energyKcal100 = energyKcal100,
    carbs100 = carbs100,
    sugars100 = sugars100,
    fat100 = fat100,
    satFat100 = satFat100,
    protein100 = protein100,
    salt100 = salt100,
    fiber100 = fiber100,
    servingSizeG = servingSizeG,
    nutriScore = nutriScore,
    novaGroup = novaGroup,
)

// --- ActivitySession / RoutePoint ---

fun ActivitySessionEntity.toDomain(): ActivitySession = ActivitySession(
    id = id,
    type = type,
    source = source,
    startTime = startTime,
    endTime = endTime,
    durationSec = durationSec,
    distanceM = distanceM,
    avgSpeedMps = avgSpeedMps,
    elevationGainM = elevationGainM,
    caloriesKcal = caloriesKcal,
)

fun ActivitySession.toEntity(): ActivitySessionEntity = ActivitySessionEntity(
    id = id,
    type = type,
    source = source,
    startTime = startTime,
    endTime = endTime,
    durationSec = durationSec,
    distanceM = distanceM,
    avgSpeedMps = avgSpeedMps,
    elevationGainM = elevationGainM,
    caloriesKcal = caloriesKcal,
    note = null,
)

fun RoutePoint.toEntity(sessionId: Long): RoutePointEntity = RoutePointEntity(
    sessionId = sessionId,
    lat = lat,
    lon = lon,
    altitudeM = altitudeM,
    speedMps = speedMps,
    timestamp = timestamp,
)

// --- StepDay ---

fun StepDayEntity.toDomain(): StepDay = StepDay(
    date = date,
    steps = steps,
    distanceM = distanceM,
    caloriesKcal = caloriesKcal,
)

// --- LoggedMeal ---

fun MealWithFood.toDomain(): LoggedMeal = LoggedMeal(
    id = meal.id,
    dateTime = meal.dateTime,
    mealType = meal.mealType,
    food = food.toDomain(),
    quantityG = meal.quantityG,
    photoPath = meal.photoPath,
)

fun FoodItem.toEntity(lastFetchedAt: Instant? = null): FoodItemEntity = FoodItemEntity(
    id = id,
    barcode = barcode,
    name = name,
    brand = brand,
    source = source,
    energyKcal100 = energyKcal100,
    carbs100 = carbs100,
    sugars100 = sugars100,
    fat100 = fat100,
    satFat100 = satFat100,
    protein100 = protein100,
    salt100 = salt100,
    fiber100 = fiber100,
    servingSizeG = servingSizeG,
    nutriScore = nutriScore,
    novaGroup = novaGroup,
    lastFetchedAt = lastFetchedAt,
)
