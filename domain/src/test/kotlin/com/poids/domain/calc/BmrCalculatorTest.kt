package com.poids.domain.calc

import com.google.common.truth.Truth.assertThat
import com.poids.domain.model.Sex
import org.junit.jupiter.api.Test

class BmrCalculatorTest {

    @Test
    fun `homme 80kg 180cm 30ans`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        val bmr = BmrCalculator.mifflinStJeor(80.0, 180.0, 30, Sex.MALE)
        assertThat(bmr).isWithin(0.01).of(1780.0)
    }

    @Test
    fun `femme 80kg 180cm 30ans`() {
        // 10*80 + 6.25*180 - 5*30 - 161 = 1614
        val bmr = BmrCalculator.mifflinStJeor(80.0, 180.0, 30, Sex.FEMALE)
        assertThat(bmr).isWithin(0.01).of(1614.0)
    }

    @Test
    fun `ecart homme-femme vaut 166`() {
        val h = BmrCalculator.mifflinStJeor(70.0, 170.0, 40, Sex.MALE)
        val f = BmrCalculator.mifflinStJeor(70.0, 170.0, 40, Sex.FEMALE)
        assertThat(h - f).isWithin(0.01).of(166.0)
    }
}
