package com.poids.data.repository

import com.poids.data.local.dao.MealLogDao
import com.poids.data.local.entity.MealLogEntity
import com.poids.domain.model.LoggedMeal
import com.poids.domain.model.MealType
import com.poids.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class MealRepositoryImpl @Inject constructor(
    private val dao: MealLogDao,
) : MealRepository {

    override fun observeMealsForDate(date: LocalDate): Flow<List<LoggedMeal>> =
        dao.observeWithFoodBetween(
            start = date.atStartOfDay(),
            end = date.plusDays(1).atStartOfDay(),
        ).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addMeal(
        foodId: Long,
        mealType: MealType,
        quantityG: Double,
        dateTime: LocalDateTime,
        photoPath: String?,
    ): Long = dao.insert(
        MealLogEntity(
            dateTime = dateTime,
            mealType = mealType,
            foodItemId = foodId,
            quantityG = quantityG,
            photoPath = photoPath,
            note = null,
        ),
    )

    override suspend fun deleteMeal(id: Long) = dao.deleteById(id)
}
