package com.poids.domain.calc

import com.poids.domain.model.ActivityLevel
import com.poids.domain.model.CalorieTargets
import com.poids.domain.model.GoalType
import com.poids.domain.model.Sex
import com.poids.domain.model.UserProfile
import java.time.LocalDate

/**
 * Dérive BMR → TDEE de base → cible calorique quotidienne à partir du profil et de l'objectif.
 *
 * **Architecture énergétique (modèle additif, « Option A ») :** le TDEE de la cible utilise
 * une base **sédentaire** (BMR × 1.2 = métabolisme + vie quotidienne hors sport). L'activité
 * réelle (pas + séances GPS, en MET net) est mesurée et ajoutée séparément aux calories
 * brûlées du jour. L'activité n'est donc comptée qu'**une fois** : on évite le double-comptage
 * d'un multiplicateur qui inclurait déjà l'exercice (FAO/WHO/UNU 2004 ; cf
 * docs/research/depense-energetique-bmr-tdee.md, Q3).
 *
 * Conversion énergie ⇄ masse : **7700 kcal ≈ 1 kg** de masse grasse.
 * Déficit/surplus quotidien = rythme hebdo (kg) × 7700 / 7.
 *
 * Garde-fou : la cible ne descend jamais sous un plancher de sécurité
 * (1200 kcal femme / 1500 kcal homme) pour éviter un déficit dangereux.
 */
object CalorieTargetCalculator {

    private const val KCAL_PER_KG = 7700.0

    /** Base d'activité du TDEE cible (sédentaire) — l'activité mesurée s'ajoute par-dessus. */
    private val BASE_ACTIVITY = ActivityLevel.SEDENTARY

    fun minSafeIntake(sex: Sex): Double = when (sex) {
        Sex.FEMALE -> 1200.0
        Sex.MALE -> 1500.0
    }

    fun compute(profile: UserProfile, today: LocalDate): CalorieTargets {
        val bmr = BmrCalculator.mifflinStJeor(
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            ageYears = profile.ageOn(today),
            sex = profile.sex,
        )
        val tdee = TdeeCalculator.fromBmr(bmr, BASE_ACTIVITY)

        val dailyDelta = when (profile.goalType) {
            GoalType.MAINTAIN -> 0.0
            GoalType.LOSE -> -(profile.weeklyRateKg * KCAL_PER_KG / 7.0)
            GoalType.GAIN -> +(profile.weeklyRateKg * KCAL_PER_KG / 7.0)
        }

        val rawTarget = tdee + dailyDelta
        val floor = minSafeIntake(profile.sex)
        val dailyTarget = rawTarget.coerceAtLeast(floor)

        return CalorieTargets(
            bmr = bmr,
            tdee = tdee,
            dailyTarget = dailyTarget,
            // delta réellement appliqué après le plancher de sécurité
            dailyDeltaKcal = dailyTarget - tdee,
        )
    }
}
