// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.tally.data.db.entity.OffProductCacheEntity

@Dao
interface OffCacheDao {
    @Query("SELECT * FROM off_product_cache WHERE barcode = :barcode")
    suspend fun get(barcode: String): OffProductCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: OffProductCacheEntity)

    @Query("DELETE FROM off_product_cache WHERE expires_at < :now")
    suspend fun purgeExpired(now: Long): Int

    @Query("SELECT COUNT(*) FROM off_product_cache")
    suspend fun count(): Int
}
