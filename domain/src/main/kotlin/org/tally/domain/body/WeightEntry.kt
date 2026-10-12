// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.body

/** Pesée. Valeur en kg (SI) ; instants en epoch ms UTC + zone de saisie. */
data class WeightEntry(
    val id: Long,
    val valueKg: Double,
    val measuredAt: Long,
    val zoneId: String,
    val note: String? = null,
)

object WeightValidation {
    const val MIN_KG = 20.0
    const val MAX_KG = 500.0

    fun isValid(kg: Double): Boolean = kg.isFinite() && kg in MIN_KG..MAX_KG
}
