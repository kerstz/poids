package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface WaterDao {

    @Insert
    suspend fun insert(entry: WaterLogEntity): Long

    @Query("SELECT * FROM water_log WHERE dateTime >= :start AND dateTime < :end ORDER BY dateTime")
    fun observeRange(start: LocalDateTime, end: LocalDateTime): Flow<List<WaterLogEntity>>

    /** Supprime la dernière prise de l'intervalle (annulation du dernier ajout). */
    @Query(
        """
        DELETE FROM water_log WHERE id = (
            SELECT id FROM water_log
            WHERE dateTime >= :start AND dateTime < :end
            ORDER BY dateTime DESC LIMIT 1
        )
        """,
    )
    suspend fun deleteLastInRange(start: LocalDateTime, end: LocalDateTime)
}
