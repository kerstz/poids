package com.poids.domain.model

import java.time.LocalDate

/**
 * Agrégat quotidien de pas avec distance et calories estimées.
 *
 * [steps] est le total du jour, sport compris (conforme aux repères « pas/jour » :
 * Saint-Maurice JAMA 2020, Paluch Lancet PH 2022). [sessionSteps] = pas effectués
 * pendant une session GPS active ; ces pas sont déjà comptés en kcal par la session
 * (MET), donc le bilan énergétique exclut leur part calorique (anti double-comptage).
 */
data class StepDay(
    val date: LocalDate,
    val steps: Int,
    val distanceM: Double,
    val caloriesKcal: Double,
    val sessionSteps: Int = 0,
) {
    /** Pas hors session GPS — base des calories à porter au bilan énergétique. */
    val nonSessionSteps: Int get() = (steps - sessionSteps).coerceAtLeast(0)

    /** Calories des pas à porter au bilan (exclut les pas déjà comptés par une session). */
    val energyKcal: Double get() = if (steps > 0) caloriesKcal * nonSessionSteps / steps else 0.0
}
