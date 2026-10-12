// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.backup

import org.tally.data.backup.v1.AppSettingV1
import org.tally.data.backup.v1.FoodPortionV1
import org.tally.data.backup.v1.FoodV1
import org.tally.data.backup.v1.GoalV1
import org.tally.data.backup.v1.JournalEntryV1
import org.tally.data.backup.v1.MealSlotV1
import org.tally.data.backup.v1.MeasurementTypeV1
import org.tally.data.backup.v1.MeasurementV1
import org.tally.data.backup.v1.ProfileV1
import org.tally.data.backup.v1.RecipeIngredientV1
import org.tally.data.backup.v1.RecipeV1
import org.tally.data.db.entity.AppSettingEntity
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity
import org.tally.data.db.entity.GoalEntity
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.data.db.entity.MealSlotEntity
import org.tally.data.db.entity.MeasurementEntity
import org.tally.data.db.entity.MeasurementTypeEntity
import org.tally.data.db.entity.ProfileEntity
import org.tally.data.db.entity.RecipeEntity
import org.tally.data.db.entity.RecipeIngredientEntity
import org.tally.domain.model.ActivityLevel
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource
import org.tally.domain.model.GoalMode
import org.tally.domain.model.GoalType
import org.tally.domain.model.MeasurementDimension
import org.tally.domain.model.MeasurementSource
import org.tally.domain.model.QuantityUnit
import org.tally.domain.model.Sex

/** Valeur d'enum inconnue : la ligne est rejetée (INVALID_ROW), jamais devinée. */
private inline fun <reified E : Enum<E>> enumOf(raw: String): E =
    enumValues<E>().firstOrNull { it.name == raw } ?: throw IllegalArgumentException("${E::class.simpleName}=$raw")

internal fun ProfileEntity.toDto() = ProfileV1(id, birthDateEpochDay, sex.name, heightM, activityLevel.name, createdAt, updatedAt)
internal fun ProfileV1.toEntity() = ProfileEntity(id, birthDate, enumOf<Sex>(sex), heightM, enumOf<ActivityLevel>(activityLevel), createdAt, updatedAt)

internal fun AppSettingEntity.toDto() = AppSettingV1(key, value, updatedAt)
internal fun AppSettingV1.toEntity() = AppSettingEntity(key, value, updatedAt)

internal fun MealSlotEntity.toDto() = MealSlotV1(id, name, builtinKey, position, archivedAt)
internal fun MealSlotV1.toEntity() = MealSlotEntity(id, name, builtinKey, position, archivedAt)

internal fun MeasurementTypeEntity.toDto() =
    MeasurementTypeV1(id, key, name, dimension.name, displayUnit, isEnabled, position, archivedAt)
internal fun MeasurementTypeV1.toEntity() =
    MeasurementTypeEntity(id, key, name, enumOf<MeasurementDimension>(dimension), displayUnit, isEnabled, position, archivedAt)

internal fun FoodEntity.toDto() = FoodV1(
    id, name, brand, barcode, ciqualCode, source.name, offLastModified, baseUnit.name, densityGPerMl,
    energyKcal, proteinG, carbsG, sugarsG, fatG, satFatG, fiberG, saltG, archivedAt, createdAt, updatedAt,
)
internal fun FoodV1.toEntity() = FoodEntity(
    id = id, name = name, brand = brand, barcode = barcode, ciqualCode = ciqualCode,
    source = enumOf<FoodSource>(source), offLastModified = offLastModified, baseUnit = enumOf<BaseUnit>(baseUnit),
    densityGPerMl = densityGPerMl, energyKcal = energyKcal, proteinG = proteinG, carbsG = carbsG,
    sugarsG = sugarsG, fatG = fatG, satFatG = satFatG, fiberG = fiberG, saltG = saltG,
    archivedAt = archivedAt, createdAt = createdAt, updatedAt = updatedAt,
)

internal fun FoodPortionEntity.toDto() = FoodPortionV1(id, foodId, label, quantityBase, isDefault)
internal fun FoodPortionV1.toEntity() = FoodPortionEntity(id, foodId, label, quantityBase, isDefault)

internal fun RecipeEntity.toDto() = RecipeV1(id, name, servings, cookedWeightG, note, archivedAt, createdAt, updatedAt)
internal fun RecipeV1.toEntity() = RecipeEntity(id, name, servings, cookedWeightG, note, archivedAt, createdAt, updatedAt)

internal fun RecipeIngredientEntity.toDto() = RecipeIngredientV1(id, recipeId, foodId, quantityBase, position)
internal fun RecipeIngredientV1.toEntity() = RecipeIngredientEntity(id, recipeId, foodId, quantityBase, position)

internal fun GoalEntity.toDto() = GoalV1(id, type.name, value, mode.name, validFromEpochDay, createdAt)
internal fun GoalV1.toEntity() = GoalEntity(id, enumOf<GoalType>(type), value, enumOf<GoalMode>(mode), validFrom, createdAt)

internal fun MeasurementEntity.toDto() =
    MeasurementV1(id, typeId, valueSi, measuredAt, zoneId, groupId, source.name, note, createdAt, updatedAt)
internal fun MeasurementV1.toEntity() =
    MeasurementEntity(id, typeId, valueSi, measuredAt, zoneId, groupId, enumOf<MeasurementSource>(source), note, createdAt, updatedAt)

internal fun JournalEntryEntity.toDto() = JournalEntryV1(
    id, name, brand, localEpochDay, mealSlotId, foodId, recipeId, portionId, portionCount, quantityBase,
    quantityUnit.name, energyKcal, proteinG, carbsG, fatG, sugarsG, satFatG, fiberG, saltG, loggedAt, zoneId,
    createdAt, updatedAt,
)
internal fun JournalEntryV1.toEntity() = JournalEntryEntity(
    id = id, name = name, brand = brand, localEpochDay = localDate, mealSlotId = mealSlotId, foodId = foodId,
    recipeId = recipeId, portionId = portionId, portionCount = portionCount, quantityBase = quantityBase,
    quantityUnit = enumOf<QuantityUnit>(quantityUnit), energyKcal = energyKcal, proteinG = proteinG,
    carbsG = carbsG, fatG = fatG, sugarsG = sugarsG, satFatG = satFatG, fiberG = fiberG, saltG = saltG,
    loggedAt = loggedAt, zoneId = zoneId, createdAt = createdAt, updatedAt = updatedAt,
)
