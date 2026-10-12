// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.core.unit

/**
 * Unités d'affichage. Stockage toujours en SI (kg, m, kcal) ; conversion ici uniquement.
 * `perSi` = combien d'unités d'affichage pour 1 unité SI.
 */
enum class MassUnit(val perSi: Double) {
    KG(1.0),
    LB(1.0 / 0.45359237),
    ST(1.0 / 6.35029318);

    fun fromSi(kg: Double): Double = kg * perSi
    fun toSi(value: Double): Double = value / perSi
}

enum class LengthUnit(val perSi: Double) {
    CM(100.0),
    IN(1.0 / 0.0254);

    fun fromSi(meters: Double): Double = meters * perSi
    fun toSi(value: Double): Double = value / perSi
}

enum class DistanceUnit(val perSi: Double) {
    KM(1.0 / 1000.0),
    MI(1.0 / 1609.344);

    fun fromSi(meters: Double): Double = meters * perSi
    fun toSi(value: Double): Double = value / perSi
}

/** Référence de stockage = kcal (exception au tout-SI, voir docs/AUDIT.md §4.1). */
enum class EnergyUnit(val perKcal: Double) {
    KCAL(1.0),
    KJ(4.184);

    fun fromKcal(kcal: Double): Double = kcal * perKcal
    fun toKcal(value: Double): Double = value / perKcal
}
