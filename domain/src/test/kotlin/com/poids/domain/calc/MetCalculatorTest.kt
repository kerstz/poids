package com.poids.domain.calc

import com.google.common.truth.Truth.assertThat
import com.poids.domain.model.ActivityType
import org.junit.jupiter.api.Test

class MetCalculatorTest {

    @Test
    fun `1 heure de velo MET8 a 70kg = 560 kcal`() {
        val kcal = MetCalculator.caloriesBurned(met = 8.0, weightKg = 70.0, durationSeconds = 3600)
        assertThat(kcal).isWithin(0.01).of(560.0)
    }

    @Test
    fun `net retranche le metabolisme de repos`() {
        val kcal = MetCalculator.caloriesBurned(8.0, 70.0, 3600, net = true)
        // (8-1) * 70 * 1
        assertThat(kcal).isWithin(0.01).of(490.0)
    }

    @Test
    fun `surcharge ActivityType utilise le MET par defaut`() {
        val kcal = MetCalculator.caloriesBurned(ActivityType.RUNNING, 70.0, 1800)
        // 9.3 (Compendium 2024) * 70 * 0.5
        assertThat(kcal).isWithin(0.01).of(325.5)
    }

    @Test
    fun `metForSpeed course plus eleve que marche a meme vitesse`() {
        val walk = MetCalculator.metForSpeed(8.0, running = false)
        val run = MetCalculator.metForSpeed(8.0, running = true)
        assertThat(run).isGreaterThan(walk)
    }
}
