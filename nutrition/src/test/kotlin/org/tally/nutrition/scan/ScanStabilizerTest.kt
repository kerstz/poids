// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanStabilizerTest {
    private val ean = "3017620422003"

    @Test
    fun deliversOnlyAfterTwoIdenticalValidFramesThenNeverAgain() {
        val s = ScanStabilizer()
        assertNull(s.offer(ean))
        assertEquals(ean, s.offer(ean))
        assertNull(s.offer(ean))
        assertNull(s.offer(ean))
    }

    @Test
    fun invalidChecksumOrNoiseResetsTheStreak() {
        val s = ScanStabilizer()
        assertNull(s.offer("3017620422004")) // clé fausse
        assertNull(s.offer(ean))
        assertNull(s.offer(null))             // trame sans code : série remise à zéro
        assertNull(s.offer(ean))
        assertEquals(ean, s.offer(ean))
    }

    @Test
    fun differentCodesNeverCombine() {
        val s = ScanStabilizer()
        assertNull(s.offer(ean))
        assertNull(s.offer("73513537"))
        assertEquals("73513537", s.offer("73513537"))
    }
}
