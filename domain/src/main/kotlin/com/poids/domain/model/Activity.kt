package com.poids.domain.model

/**
 * Types d'activité physique avec leur valeur MET de référence.
 * MET = Metabolic Equivalent of Task (2024 Adult Compendium of Physical Activities,
 * Herrmann/Ainsworth et al.). Ces valeurs sont des moyennes : pour la marche/course,
 * [com.poids.domain.calc.MetCalculator] peut affiner via la vitesse GPS.
 */
enum class ActivityType(val defaultMet: Double, val label: String) {
    WALKING(3.5, "Marche"),
    WALKING_BRISK(4.3, "Marche rapide"),
    RUNNING(9.8, "Course"),
    CYCLING(8.0, "Vélo"),
    HIKING(6.0, "Randonnée"),
    SWIMMING(7.0, "Natation"),
    OTHER(4.0, "Autre"),
}

/** Source d'une session d'activité : capteurs ou saisie manuelle. */
enum class ActivitySource { GPS, PEDOMETER, MANUAL }
