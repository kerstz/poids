package com.poids.domain.repository

import com.poids.domain.model.LoggedMeal
import com.poids.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface MealRepository {
    fun observeMealsForDate(date: LocalDate): Flow<List<LoggedMeal>>

    suspend fun addMeal(
        foodId: Long,
        mealType: MealType,
        quantityG: Double,
        dateTime: LocalDateTime,
        photoPath: String?,
    ): Long

    suspend fun deleteMeal(id: Long)
}
