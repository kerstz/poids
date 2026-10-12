// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.core.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GtinTest {
    @Test fun validEan13() = assertTrue(Gtin.isValid("3017620422003"))
    @Test fun validEan8() = assertTrue(Gtin.isValid("73513537"))
    @Test fun validUpcA() = assertTrue(Gtin.isValid("036000291452"))
    @Test fun wrongCheckDigit() = assertFalse(Gtin.isValid("3017620422004"))
    @Test fun wrongLength() = assertFalse(Gtin.isValid("12345"))
    @Test fun nonDigits() = assertFalse(Gtin.isValid("30176204220A3"))
    @Test fun looksLikeBarcodeIgnoresChecksum() = assertTrue(Gtin.looksLikeBarcode("1234567890123"))
    @Test fun lettersAreNotBarcode() = assertFalse(Gtin.looksLikeBarcode("avoine"))

    @Test
    fun normalizeGivesGtin13ForUpcAndLeadingZeroGtin14() {
        assertEquals("0036000291452", Gtin.normalize("036000291452"))
        assertEquals("3017620422003", Gtin.normalize("03017620422003"))
        assertEquals("3017620422003", Gtin.normalize("3017620422003"))
        assertEquals("73513537", Gtin.normalize("73513537"))
        assertEquals(true, Gtin.isValid(Gtin.normalize("036000291452")))
    }
}
