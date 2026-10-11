package org.tally.core.barcode

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
}
