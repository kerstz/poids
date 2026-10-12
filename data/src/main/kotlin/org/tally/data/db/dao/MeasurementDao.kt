// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.MeasurementEntity

@Dao
interface MeasurementDao {
    @Query("SELECT id FROM measurement_type WHERE `key` = :key")
    suspend fun typeId(key: String): Long?

    @Query(
        "SELECT m.* FROM measurement m JOIN measurement_type t ON t.id = m.type_id " +
            "WHERE t.`key` = :key ORDER BY m.measured_at DESC",
    )
    fun observeByKey(key: String): Flow<List<MeasurementEntity>>

    @Query(
        "SELECT m.* FROM measurement m JOIN measurement_type t ON t.id = m.type_id " +
            "WHERE t.`key` = :key AND m.measured_at <= :instant ORDER BY m.measured_at DESC LIMIT 1",
    )
    suspend fun latestAtOrBefore(key: String, instant: Long): MeasurementEntity?

    @Query(
        "SELECT m.* FROM measurement m JOIN measurement_type t ON t.id = m.type_id " +
            "WHERE t.`key` = :key AND m.measured_at > :instant ORDER BY m.measured_at ASC LIMIT 1",
    )
    suspend fun firstAfter(key: String, instant: Long): MeasurementEntity?

    @Query("SELECT * FROM measurement WHERE id = :id")
    suspend fun get(id: Long): MeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeasurementEntity): Long

    @Update
    suspend fun update(entity: MeasurementEntity)

    @Query("DELETE FROM measurement WHERE id = :id")
    suspend fun delete(id: Long)
}
