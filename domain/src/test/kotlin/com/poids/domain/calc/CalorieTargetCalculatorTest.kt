package com.poids.domain.calc

import com.google.common.truth.Truth.assertThat
import com.poids.domain.model.ActivityLevel
import com.poids.domain.model.GoalType
import com.poids.domain.model.Sex
import com.poids.domain.model.UserProfile
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CalorieTargetCalculatorTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun profile(
        goal: GoalType,
        rate: Double = 0.5,
        weight: Double = 80.0,
        sex: Sex = Sex.MALE,
    ) = UserProfile(
        heightCm = 180.0,
        weightKg = weight,
        birthDate = LocalDate.of(1996, 1, 1), // 30 ans au 2026-01-01
        sex = sex,
        activityLevel = ActivityLevel.MODERATE,
        goalType = goal,
        targetWeightKg = if (goal == GoalType.LOSE) 75.0 else null,
        weeklyRateKg = rate,
    )

    @Test
    fun `maintain donne cible egale au TDEE`() {
        val t = CalorieTargetCalculator.compute(profile(GoalType.MAINTAIN), today)
        // BMR 1780 * 1.55 = 2759
        assertThat(t.bmr).isWithin(0.01).of(1780.0)
        assertThat(t.tdee).isWithin(0.01).of(2759.0)
        assertThat(t.dailyTarget).isWithin(0.01).of(2759.0)
        assertThat(t.dailyDeltaKcal).isWithin(0.01).of(0.0)
    }

    @Test
    fun `perte 0_5kg par semaine applique un deficit d_environ 550 kcal`() {
        val t = CalorieTargetCalculator.compute(profile(GoalType.LOSE, rate = 0.5), today)
        // 0.5 * 7700 / 7 = 550
        assertThat(t.dailyDeltaKcal).isWithin(0.01).of(-550.0)
        assertThat(t.dailyTarget).isWithin(0.01).of(2759.0 - 550.0)
    }

    @Test
    fun `plancher de securite empeche une cible trop basse`() {
        // Petit poids + rythme agressif → la cible brute passe sous 1500 (homme)
        val t = CalorieTargetCalculator.compute(
            profile(GoalType.LOSE, rate = 2.0, weight = 50.0),
            today,
        )
        assertThat(t.dailyTarget).isAtLeast(CalorieTargetCalculator.minSafeIntake(Sex.MALE))
    }
}
