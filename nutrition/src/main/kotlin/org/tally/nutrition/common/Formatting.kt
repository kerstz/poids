package org.tally.nutrition.common

import java.util.Locale
import org.tally.domain.nutrition.Macros

/** Accepte la virgule décimale ; null si vide ou invalide. */
internal fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

internal fun formatNumber(value: Double, decimals: Int = 0): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)

internal fun formatKcal(macros: Macros): String = formatNumber(macros.energyKcal)
