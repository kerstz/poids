// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.model

enum class Sex { FEMALE, MALE, UNSPECIFIED }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }

enum class GoalType { ENERGY_KCAL, PROTEIN_G, CARBS_G, FAT_G, BODY_WEIGHT_KG }

enum class GoalMode { FIXED, COMPUTED }

enum class FoodSource { USER, OFF, IMPORT, CIQUAL }

/** Base de quantité d'un aliment : valeurs nutritionnelles pour 100 g ou 100 ml. */
enum class BaseUnit { G, ML }

enum class MeasurementDimension { MASS, LENGTH, PERCENT, ENERGY, COUNT }

enum class MeasurementSource { MANUAL, BLE, IMPORT }

enum class OffCacheStatus { FOUND, NOT_FOUND }

/** Unité de `journal_entry.quantity_base` : g/ml pour un aliment, SERVING pour une recette. */
enum class QuantityUnit { G, ML, SERVING }
