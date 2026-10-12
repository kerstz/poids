// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.GoalEntity
import org.tally.domain.model.GoalType

@Dao
interface GoalDao {
    /** Dernier objectif de ce type applicable au jour donné (`valid_from <= jour`). */
    @Query("SELECT * FROM goal WHERE type = :type AND valid_from <= :epochDay ORDER BY valid_from DESC LIMIT 1")
    fun observeLatest(type: GoalType, epochDay: Long): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity): Long
}
