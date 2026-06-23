package com.poids.domain.calc

import com.poids.domain.model.ActivityType

/**
 * Calories brûlées par une activité, via les MET.
 *
 * Formule : **kcal = MET × poids(kg) × durée(heures)**
 * (équivaut à 1 MET = 1 kcal/kg/h).
 *
 * `net = true` retranche le métabolisme de repos (MET − 1) pour ne compter que la
 * dépense *au-delà* du repos — utile quand on l'ajoute à un TDEE déjà calculé.
 *
 * ⚠️ Estimation : marge d'erreur typique ±20-30 % vs calorimétrie. À présenter
 * comme une estimation, jamais comme une mesure exacte.
 */
object MetCalculator {

    fun caloriesBurned(
        met: Double,
        weightKg: Double,
        durationSeconds: Long,
        net: Boolean = false,
    ): Double {
        val hours = durationSeconds / 3600.0
        val effectiveMet = if (net) (met - 1.0).coerceAtLeast(0.0) else met
        return effectiveMet * weightKg * hours
    }

    fun caloriesBurned(
        activity: ActivityType,
        weightKg: Double,
        durationSeconds: Long,
        net: Boolean = false,
    ): Double = caloriesBurned(activity.defaultMet, weightKg, durationSeconds, net)

    /**
     * MET affiné pour marche/course à partir de la vitesse (équations ACSM, terrain plat).
     * À utiliser quand une session GPS fournit une vitesse moyenne fiable.
     *
     * @param speedKmh vitesse moyenne en km/h.
     * @param running true pour la course (sinon marche).
     */
    fun metForSpeed(speedKmh: Double, running: Boolean): Double {
        val metersPerMin = speedKmh * 1000.0 / 60.0
        // VO2 (mL/kg/min) ; pente nulle
        val vo2 = if (running) {
            0.2 * metersPerMin + 3.5
        } else {
            0.1 * metersPerMin + 3.5
        }
        // 1 MET = 3,5 mL/kg/min
        return (vo2 / 3.5).coerceAtLeast(1.0)
    }
}
