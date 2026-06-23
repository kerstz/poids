package com.poids.data.repository

import com.poids.data.local.dao.ActivityDao
import com.poids.data.local.dao.MealLogDao
import com.poids.data.local.dao.StepDao
import com.poids.domain.repository.DailyEnergyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class DailyEnergyRepositoryImpl @Inject constructor(
    private val mealLogDao: MealLogDao,
    private val activityDao: ActivityDao,
    private val stepDao: StepDao,
) : DailyEnergyRepository {

    override fun observeIntakeKcal(date: LocalDate): Flow<Double> =
        mealLogDao.observeIntakeKcal(
            start = date.atStartOfDay(),
            end = date.plusDays(1).atStartOfDay(),
        )

    /** Brûlées = sessions d'activité du jour + calories estimées des pas du jour. */
    override fun observeBurnedKcal(date: LocalDate): Flow<Double> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return combine(
            activityDao.observeBurnedKcal(start, end),
            stepDao.observeDay(date),
        ) { activityKcal, stepDay ->
            activityKcal + (stepDay?.caloriesKcal ?: 0.0)
        }
    }
}
