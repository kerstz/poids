package org.tally.data.backup.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO du format de sauvegarde v1. Figés et indépendants des entités Room : toute évolution
 * passe par `formatVersion` 2 et un importeur v1 → v2. Enums en `String` (nom stable).
 */
@Serializable
data class ProfileV1(
    val id: Long,
    @SerialName("birth_date") val birthDate: Long?,
    val sex: String,
    @SerialName("height_m") val heightM: Double?,
    @SerialName("activity_level") val activityLevel: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class AppSettingV1(
    val key: String,
    val value: String,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class MealSlotV1(
    val id: Long,
    val name: String?,
    @SerialName("builtin_key") val builtinKey: String?,
    val position: Int,
    @SerialName("archived_at") val archivedAt: Long?,
)

@Serializable
data class MeasurementTypeV1(
    val id: Long,
    val key: String,
    val name: String?,
    val dimension: String,
    @SerialName("display_unit") val displayUnit: String?,
    @SerialName("is_enabled") val isEnabled: Boolean,
    val position: Int,
    @SerialName("archived_at") val archivedAt: Long?,
)

@Serializable
data class FoodV1(
    val id: Long,
    val name: String,
    val brand: String?,
    val barcode: String?,
    @SerialName("ciqual_code") val ciqualCode: Int?,
    val source: String,
    @SerialName("off_last_modified") val offLastModified: Long?,
    @SerialName("base_unit") val baseUnit: String,
    @SerialName("density_g_per_ml") val densityGPerMl: Double?,
    @SerialName("energy_kcal") val energyKcal: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("sugars_g") val sugarsG: Double?,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("sat_fat_g") val satFatG: Double?,
    @SerialName("fiber_g") val fiberG: Double?,
    @SerialName("salt_g") val saltG: Double?,
    @SerialName("archived_at") val archivedAt: Long?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class FoodPortionV1(
    val id: Long,
    @SerialName("food_id") val foodId: Long,
    val label: String,
    @SerialName("quantity_base") val quantityBase: Double,
    @SerialName("is_default") val isDefault: Boolean,
)

@Serializable
data class RecipeV1(
    val id: Long,
    val name: String,
    val servings: Double,
    @SerialName("cooked_weight_g") val cookedWeightG: Double?,
    val note: String?,
    @SerialName("archived_at") val archivedAt: Long?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class RecipeIngredientV1(
    val id: Long,
    @SerialName("recipe_id") val recipeId: Long,
    @SerialName("food_id") val foodId: Long,
    @SerialName("quantity_base") val quantityBase: Double,
    val position: Int,
)

@Serializable
data class GoalV1(
    val id: Long,
    val type: String,
    val value: Double,
    val mode: String,
    @SerialName("valid_from") val validFrom: Long,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class MeasurementV1(
    val id: Long,
    @SerialName("type_id") val typeId: Long,
    @SerialName("value_si") val valueSi: Double,
    @SerialName("measured_at") val measuredAt: Long,
    @SerialName("zone_id") val zoneId: String,
    @SerialName("group_id") val groupId: Long?,
    val source: String,
    val note: String?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class JournalEntryV1(
    val id: Long,
    val name: String,
    val brand: String?,
    @SerialName("local_date") val localDate: Long,
    @SerialName("meal_slot_id") val mealSlotId: Long,
    @SerialName("food_id") val foodId: Long?,
    @SerialName("recipe_id") val recipeId: Long?,
    @SerialName("portion_id") val portionId: Long?,
    @SerialName("portion_count") val portionCount: Double?,
    @SerialName("quantity_base") val quantityBase: Double,
    @SerialName("quantity_unit") val quantityUnit: String,
    @SerialName("energy_kcal") val energyKcal: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("sugars_g") val sugarsG: Double?,
    @SerialName("sat_fat_g") val satFatG: Double?,
    @SerialName("fiber_g") val fiberG: Double?,
    @SerialName("salt_g") val saltG: Double?,
    @SerialName("logged_at") val loggedAt: Long,
    @SerialName("zone_id") val zoneId: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class ManifestV1(
    val formatVersion: Int,
    val app: AppInfoV1,
    val dbSchemaVersion: Int,
    val createdAt: String,
    val tables: Map<String, TableInfoV1>,
    val attributions: List<AttributionV1> = emptyList(),
)

@Serializable
data class AppInfoV1(val versionName: String, val versionCode: Long)

@Serializable
data class TableInfoV1(val rows: Int, val sha256: String)

@Serializable
data class AttributionV1(val source: String, val license: String, val url: String)
