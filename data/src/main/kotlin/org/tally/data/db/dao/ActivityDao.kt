// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.ActivitySessionEntity
import org.tally.data.db.entity.TrackPointEntity

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_session ORDER BY started_at DESC")
    fun observeSessions(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_session WHERE id = :id")
    fun observeSession(id: Long): Flow<ActivitySessionEntity?>

    @Query("SELECT * FROM activity_session WHERE id = :id")
    suspend fun get(id: Long): ActivitySessionEntity?

    @Query("SELECT * FROM activity_session WHERE status <> 'FINISHED' ORDER BY started_at DESC LIMIT 1")
    suspend fun active(): ActivitySessionEntity?

    @Query(
        "SELECT * FROM activity_session WHERE status = 'FINISHED' AND energy_kcal IS NULL " +
            "ORDER BY started_at DESC",
    )
    suspend fun finishedWithoutEnergy(): List<ActivitySessionEntity>

    @Query("SELECT * FROM track_point WHERE session_id = :sessionId ORDER BY seq ASC")
    suspend fun points(sessionId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_point WHERE session_id = :sessionId ORDER BY seq DESC LIMIT 1")
    suspend fun lastPoint(sessionId: Long): TrackPointEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ActivitySessionEntity): Long

    @Update
    suspend fun update(entity: ActivitySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query(
        "UPDATE activity_session SET distance_m = :distanceM, elevation_gain_m = :gainM, " +
            "elevation_loss_m = :lossM, updated_at = :now WHERE id = :id",
    )
    suspend fun updateAggregates(id: Long, distanceM: Double, gainM: Double, lossM: Double, now: Long)

    @Transaction
    suspend fun appendBatch(
        id: Long,
        points: List<TrackPointEntity>,
        distanceM: Double,
        gainM: Double,
        lossM: Double,
        now: Long,
    ) {
        insertPoints(points)
        updateAggregates(id, distanceM, gainM, lossM, now)
    }

    @Transaction
    suspend fun restore(session: ActivitySessionEntity, points: List<TrackPointEntity>) {
        insert(session)
        insertPoints(points)
    }

    @Query("DELETE FROM activity_session WHERE id = :id")
    suspend fun delete(id: Long)
}
