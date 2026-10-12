// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tally.domain.model.OffCacheStatus

/** Cache jetable (exclu du backup). Un aliment ajouté au journal est copié dans `food`. */
@Entity(
    tableName = "off_product_cache",
    indices = [Index("expires_at")],
)
data class OffProductCacheEntity(
    @PrimaryKey val barcode: String,
    val status: OffCacheStatus,
    @ColumnInfo(name = "payload_json") val payloadJson: String?,
    val etag: String? = null,
    @ColumnInfo(name = "last_modified") val lastModified: String? = null,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
)
