// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.body

/**
 * Poids connu à un instant : dernière pesée ≤ instant, sinon première pesée postérieure,
 * sinon null (l'appelant invite à saisir un poids).
 */
class GetWeightAt(private val repository: WeightRepository) {
    suspend operator fun invoke(instant: Long): Double? =
        (repository.latestAtOrBefore(instant) ?: repository.firstAfter(instant))?.valueKg
}
