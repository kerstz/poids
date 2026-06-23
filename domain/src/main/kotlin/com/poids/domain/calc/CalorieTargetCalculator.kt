package com.poids.domain.calc

import com.poids.domain.model.CalorieTargets
import com.poids.domain.model.GoalType
import com.poids.domain.model.Sex
import com.poids.domain.model.UserProfile
import java.time.LocalDate

/**
 * Dérive BMR → TDEE → cible calorique quotidienne à partir du profil et de l'objectif.
 *
 * Conversion énergie ⇄ masse : **7700 kcal ≈ 1 kg** de masse grasse.
 * Déficit/surplus quotidien = rythme hebdo (kg) × 7700 / 7.
 *
 * Garde-fou : la cible ne descend jamais sous un plancher de sécurité
 * (1200 kcal femme / 1500 kcal homme) pour éviter un déficit dangereux.
 */
object CalorieTargetCalculator {

    private const val KCAL_PER_KG = 7700.0

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
        val tdee = TdeeCalculator.fromBmr(bmr, profile.activityLevel)

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
