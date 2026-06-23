package com.poids.domain.calc

import com.poids.domain.model.Sex

/**
 * Métabolisme de base (BMR) selon **Mifflin-St Jeor** — standard de l'industrie
 * (utilisé par MyFitnessPal et Cronometer), plus précis que Harris-Benedict.
 *
 * Homme : 10·poids(kg) + 6,25·taille(cm) − 5·âge + 5
 * Femme : 10·poids(kg) + 6,25·taille(cm) − 5·âge − 161
 *
 * @return BMR en kcal/jour.
 */
object BmrCalculator {

    fun mifflinStJeor(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: Sex,
    ): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
        }
    }
}
