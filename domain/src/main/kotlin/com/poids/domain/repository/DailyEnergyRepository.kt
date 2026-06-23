package com.poids.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Énergie du jour, agrégée depuis les repas (ingérées) et l'activité (brûlées).
 * Renvoie 0 tant que les features nutrition (③) et activité (④) n'écrivent rien.
 */
interface DailyEnergyRepository {
    fun observeIntakeKcal(date: LocalDate): Flow<Double>
    fun observeBurnedKcal(date: LocalDate): Flow<Double>
}
