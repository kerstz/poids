package com.poids.domain.repository

import com.poids.domain.model.StepDay
import kotlinx.coroutines.flow.Flow

interface StepRepository {
    /** Le capteur TYPE_STEP_COUNTER existe-t-il sur l'appareil ? */
    val sensorAvailable: Boolean

    /** Pas du jour (0 si rien encore). */
    fun observeToday(): Flow<StepDay>

    /** Pas sur un intervalle (jours présents en base, du plus ancien au plus récent). */
    fun observeRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<StepDay>>

    /**
     * Collecte le compteur matériel et persiste le total du jour, en gérant le
     * passage à minuit et les reboots (compteur cumulatif remis à 0 au boot).
     * Suspend tant que non annulée — à lancer dans un scope lié à l'écran.
     */
    suspend fun trackUntilCancelled()
}
