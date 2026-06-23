package com.poids.domain.model

import java.time.LocalDate

/** Agrégat quotidien de pas avec distance et calories estimées. */
data class StepDay(
    val date: LocalDate,
    val steps: Int,
    val distanceM: Double,
    val caloriesKcal: Double,
)
