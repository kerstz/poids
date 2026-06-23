package com.poids.domain.model

import java.time.LocalDate

/** Une pesée datée. L'historique de ces entrées trace la courbe de poids. */
data class WeightEntry(
    val id: Long = 0L,
    val date: LocalDate,
    val weightKg: Double,
    val note: String? = null,
)
