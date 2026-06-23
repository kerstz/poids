package com.poids.domain.model

import java.time.LocalDate
import java.time.Period

/** Sexe biologique — utilisé par les formules métaboliques (Mifflin-St Jeor). */
enum class Sex { MALE, FEMALE }

/**
 * Niveau d'activité physique habituel → multiplicateur appliqué au BMR pour obtenir le TDEE.
 * Valeurs standard de l'industrie (Mifflin-St Jeor / Harris-Benedict).
 */
enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Sédentaire"),
    LIGHT(1.375, "Légèrement actif"),
    MODERATE(1.55, "Modérément actif"),
    ACTIVE(1.725, "Actif"),
    VERY_ACTIVE(1.9, "Très actif"),
}

/** Objectif de poids de l'utilisateur. */
enum class GoalType { LOSE, MAINTAIN, GAIN }

/**
 * Profil utilisateur. Stocké en un seul exemplaire (singleton applicatif).
 * Le poids courant vit dans l'historique [WeightEntry] ; on en garde une copie ici
 * pour les calculs immédiats (BMR/TDEE).
 */
data class UserProfile(
    val heightCm: Double,
    val weightKg: Double,
    val birthDate: LocalDate,
    val sex: Sex,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    /** Poids cible (null si MAINTAIN). */
    val targetWeightKg: Double? = null,
    /** Rythme visé en kg/semaine (valeur absolue, ex. 0.5). */
    val weeklyRateKg: Double = 0.5,
) {
    /** Âge en années révolues à la date donnée (injectée pour rester testable). */
    fun ageOn(today: LocalDate): Int = Period.between(birthDate, today).years
}
