package org.tally.core.time

import org.junit.Assert.assertTrue
import org.junit.Test

class AppClockTest {
    @Test
    fun systemClockIsPositiveAndMonotonicEnough() {
        val a = SystemAppClock.nowMillis()
        val b = SystemAppClock.nowMillis()
        assertTrue(a > 0)
        assertTrue(b >= a)
    }
}
