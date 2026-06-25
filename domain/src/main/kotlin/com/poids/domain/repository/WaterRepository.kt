package com.poids.domain.repository

import com.poids.domain.model.WaterLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WaterRepository {
    /** Prises d'eau d'un jour, de la plus ancienne à la plus récente. */
    fun observeForDate(date: LocalDate): Flow<List<WaterLog>>

    /** Ajoute une prise d'eau horodatée à maintenant. Retourne l'id. */
    suspend fun addWater(amountMl: Int): Long

    /** Supprime la dernière prise du jour (annulation). */
    suspend fun deleteLastForDate(date: LocalDate)
}
