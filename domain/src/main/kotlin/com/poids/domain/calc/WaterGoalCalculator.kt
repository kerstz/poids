package com.poids.domain.calc

import kotlin.math.roundToInt

/**
 * Objectif d'hydratation quotidien.
 *
 * Règle usuelle : ≈ 30-35 ml par kg de poids corporel (eau totale, boisson + aliments
 * confondus ; on vise ici la boisson). On retient 33 ml/kg comme milieu de fourchette.
 * ⚠️ Indicatif : varie selon climat, activité, santé. À présenter comme un repère.
 */
object WaterGoalCalculator {

    const val ML_PER_KG = 33

    /** Objectif par défaut quand le poids est inconnu (adulte « moyen »). */
    const val DEFAULT_GOAL_ML = 2000

    fun dailyGoalMl(weightKg: Double?): Int =
        weightKg?.takeIf { it > 0 }?.let { (it * ML_PER_KG).roundToInt() } ?: DEFAULT_GOAL_ML
}
