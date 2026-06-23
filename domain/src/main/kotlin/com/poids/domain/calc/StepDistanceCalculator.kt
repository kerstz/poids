package com.poids.domain.calc

import com.poids.domain.model.Sex

/**
 * Estime la distance parcourue à partir d'un nombre de pas et de la taille.
 *
 * Longueur de pas ≈ taille(cm) × facteur (0,415 homme / 0,413 femme).
 * ⚠️ La longueur de pas augmente avec la vitesse : précision ±5-10 % près de
 * l'allure de calibration, davantage aux extrêmes.
 */
object StepDistanceCalculator {

    fun stepLengthCm(heightCm: Double, sex: Sex): Double {
        val factor = when (sex) {
            Sex.MALE -> 0.415
            Sex.FEMALE -> 0.413
        }
        return heightCm * factor
    }

    /** Distance en mètres. */
    fun distanceMeters(steps: Int, heightCm: Double, sex: Sex): Double =
        steps * stepLengthCm(heightCm, sex) / 100.0
}
