package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poids.domain.model.MealType
import java.time.LocalDateTime

/**
 * Un aliment consommé, daté et rattaché à un type de repas. La photo éventuelle
 * est stockée sur disque (filesDir) ; on ne garde ici que son chemin relatif.
 */
@Entity(
    tableName = "meal_log",
    foreignKeys = [
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("foodItemId"), Index("dateTime")],
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateTime: LocalDateTime,
    val mealType: MealType,
    val foodItemId: Long,
    val quantityG: Double,
    val photoPath: String?,
    val note: String?,
)
