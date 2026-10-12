// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.core.barcode

/** Validation GTIN (EAN-8, UPC-A 12, EAN-13, GTIN-14) : chiffres uniquement + clé de contrôle GS1. */
object Gtin {
    private val LENGTHS = setOf(8, 12, 13, 14)

    fun isValid(code: String): Boolean {
        if (code.length !in LENGTHS || !code.all { it in '0'..'9' }) return false
        val digits = code.map { it - '0' }
        val body = digits.dropLast(1).asReversed()
        val sum = body.withIndex().sumOf { (i, d) -> if (i % 2 == 0) d * 3 else d }
        return (10 - sum % 10) % 10 == digits.last()
    }

    /**
     * Forme canonique de stockage : GTIN-13. UPC-A (12) → `0` + code ; GTIN-14 commençant par `0` → 13 chiffres.
     * EAN-8 et le reste sont inchangés.
     */
    fun normalize(code: String): String = when {
        code.length == 12 && code.all { it in '0'..'9' } -> "0$code"
        code.length == 14 && code[0] == '0' && code.all { it in '0'..'9' } -> code.drop(1)
        else -> code
    }

    /** Heuristique de saisie : 8 à 14 chiffres, quel que soit le checksum. */
    fun looksLikeBarcode(text: String): Boolean = text.length in 8..14 && text.all { it in '0'..'9' }
}
