package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Table de référence Ciqual (Anses) chargée depuis l'asset ; jetable, exclue de la sauvegarde. */
@Entity(tableName = "ciqual_food")
data class CiqualFoodEntity(
    @PrimaryKey val code: Int,
    val name: String,
    /** Nom minuscule sans accents, pour la recherche. */
    @ColumnInfo(name = "name_norm") val nameNorm: String,
    @ColumnInfo(name = "group_code") val groupCode: String,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
    @ColumnInfo(name = "sugars_g") val sugarsG: Double?,
    @ColumnInfo(name = "sat_fat_g") val satFatG: Double?,
    @ColumnInfo(name = "fiber_g") val fiberG: Double?,
    @ColumnInfo(name = "salt_g") val saltG: Double?,
)
