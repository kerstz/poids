package com.poids.domain.calc

import com.poids.domain.model.ActivityLevel

/**
 * Dépense énergétique totale quotidienne (TDEE) = BMR × multiplicateur d'activité.
 */
object TdeeCalculator {

    fun fromBmr(bmr: Double, activityLevel: ActivityLevel): Double =
        bmr * activityLevel.multiplier
}
