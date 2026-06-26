package com.poids.domain.calc

import kotlin.math.roundToInt

/**
 * Objectif d'hydratation quotidien — part **BOISSON** (ce que l'app trace).
 *
 * Le « 30-35 ml/kg » des références cliniques estime l'**eau TOTALE** (boisson + aliments
 * + eau métabolique). Or les apports d'eau totale ne viennent qu'à ~75-80 % des boissons
 * (IOM/NASEM 2004 ; EFSA 2010). On part donc de 33 ml/kg (eau totale) puis on applique
 * un facteur boisson de 0,75 → ≈ 25 ml/kg de boisson, et on borne le résultat.
 *
 * Références : EFSA 2010 (eau totale 2,0 L F / 2,5 L H) ; NASEM/IOM 2004 ; ANSES.
 * ⚠️ Repère indicatif (varie selon climat, activité, santé), jamais un avis médical.
 */
object WaterGoalCalculator {

    /** Eau totale par kg (milieu de la fourchette clinique 30-35). */
    const val ML_PER_KG_TOTAL = 33

    /** Part de l'eau totale apportée par les boissons (le reste vient des aliments). */
    const val BEVERAGE_FACTOR = 0.75

    /** Objectif par défaut quand le poids est inconnu (≈ EFSA femme adulte). */
    const val DEFAULT_GOAL_ML = 2000

    /** Bornes de sécurité : évite une cible absurde (petit/grand poids) ou risquée. */
    const val MIN_GOAL_ML = 1200
    const val MAX_GOAL_ML = 3500

    fun dailyGoalMl(weightKg: Double?): Int =
        weightKg?.takeIf { it > 0 }?.let {
            (it * ML_PER_KG_TOTAL * BEVERAGE_FACTOR).roundToInt().coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)
        } ?: DEFAULT_GOAL_ML
}
