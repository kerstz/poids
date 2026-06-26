package com.poids.domain.model

/**
 * Types d'activité physique avec leur valeur MET de référence.
 * MET = Metabolic Equivalent of Task. Valeurs alignées sur le **2024 Adult Compendium
 * of Physical Activities** (Herrmann/Willis/Ainsworth, J Sport Health Sci 2024;13(1):6-12) :
 * - marche plaisir 3.5 (17160) · marche rapide 4.8 (17200) · course 6 mph 9.3 (12050)
 * - vélo général 7.0 (01014) · rando 6.0 (17080) · natation loisir 6.0 (18310) · autre ~4.0
 * Ce sont des moyennes brutes : pour la marche/course, [com.poids.domain.calc.MetCalculator]
 * affine via la vitesse GPS, et l'app applique le MET net (MET−1) pour le bilan calorique.
 */
enum class ActivityType(val defaultMet: Double, val label: String) {
    WALKING(3.5, "Marche"),
    WALKING_BRISK(4.8, "Marche rapide"),
    RUNNING(9.3, "Course"),
    CYCLING(7.0, "Vélo"),
    HIKING(6.0, "Randonnée"),
    SWIMMING(6.0, "Natation"),
    OTHER(4.0, "Autre"),
}

/** Source d'une session d'activité : capteurs ou saisie manuelle. */
enum class ActivitySource { GPS, PEDOMETER, MANUAL }
