// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.MealSlotEntity

@Dao
interface MealSlotDao {
    @Query("SELECT * FROM meal_slot WHERE archived_at IS NULL ORDER BY position")
    fun observeSlots(): Flow<List<MealSlotEntity>>
}
