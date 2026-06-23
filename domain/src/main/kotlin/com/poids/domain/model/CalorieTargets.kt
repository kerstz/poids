package com.poids.domain.model

/**
 * Cibles caloriques quotidiennes dérivées du profil et de l'objectif.
 *
 * @param bmr Métabolisme de base (kcal/jour).
 * @param tdee Dépense énergétique totale (kcal/jour) = BMR × multiplicateur d'activité.
 * @param dailyTarget Apport calorique cible (kcal/jour) = TDEE ± déficit/surplus.
 * @param dailyDeltaKcal Écart appliqué (négatif = déficit, positif = surplus).
 */
data class CalorieTargets(
    val bmr: Double,
    val tdee: Double,
    val dailyTarget: Double,
    val dailyDeltaKcal: Double,
)

/**
 * Bilan calorique d'une journée : ce qui rentre vs ce qui sort.
 *
 * @param intakeKcal Calories ingérées (repas).
 * @param burnedKcal Calories brûlées par l'activité (au-delà du TDEE de base).
 * @param targetKcal Cible du jour.
 */
data class DailyEnergyBalance(
    val intakeKcal: Double,
    val burnedKcal: Double,
    val targetKcal: Double,
) {
    /** Solde net vs cible. Négatif = sous la cible (déficit atteint). */
    val netVsTarget: Double get() = intakeKcal - burnedKcal - targetKcal
}
