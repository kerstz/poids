package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.MealLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MealLogDao {

    @Insert
    suspend fun insert(entry: MealLogEntity): Long

    @Delete
    suspend fun delete(entry: MealLogEntity)

    @Query("SELECT * FROM meal_log WHERE dateTime >= :start AND dateTime < :end ORDER BY dateTime")
    fun observeBetween(start: LocalDateTime, end: LocalDateTime): Flow<List<MealLogEntity>>

    /** Total des calories ingérées sur l'intervalle (kcal = énergie/100 × quantité). */
    @Query(
        """
        SELECT COALESCE(SUM(f.energyKcal100 * m.quantityG / 100.0), 0.0)
        FROM meal_log m
        JOIN food_item f ON f.id = m.foodItemId
        WHERE m.dateTime >= :start AND m.dateTime < :end
        """,
    )
    fun observeIntakeKcal(start: LocalDateTime, end: LocalDateTime): Flow<Double>
}
