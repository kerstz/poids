package org.tally.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tally.core.time.AppClock
import org.tally.data.db.dao.OffCacheDao
import org.tally.data.db.entity.OffProductCacheEntity
import org.tally.domain.model.OffCacheStatus
import org.tally.domain.nutrition.FoodRemoteSource
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.NutrientDetail
import org.tally.domain.nutrition.RemoteFood
import org.tally.domain.nutrition.RemoteResult
import org.tally.domain.settings.SettingsRepository

/**
 * Open Food Facts avec cache Room (produits par code-barres) et limitation locale.
 * Limites publiées par OFF : 15 req/min (produit), 10 req/min (recherche), pas de
 * recherche au fil de la frappe. Marges : 12 et 8 req/min. Recherche texte non mise en cache.
 */
@Singleton
internal class CachedOffRemoteSource @Inject constructor(
    private val api: OffApi,
    private val cache: OffCacheDao,
    private val settings: SettingsRepository,
    private val clock: AppClock,
) : FoodRemoteSource {

    private val productLimiter = SlidingWindowRateLimiter(PRODUCT_PER_MINUTE, MINUTE_MS, clock::nowMillis)
    private val searchLimiter = SlidingWindowRateLimiter(SEARCH_PER_MINUTE, MINUTE_MS, clock::nowMillis)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun findByBarcode(barcode: String): RemoteResult<RemoteFood> {
        if (!settings.offEnabled.first()) return RemoteResult.Disabled
        val now = clock.nowMillis()

        cache.get(barcode)?.takeIf { it.expiresAt > now }?.let { entry ->
            when (entry.status) {
                OffCacheStatus.NOT_FOUND -> return RemoteResult.NotFound
                OffCacheStatus.FOUND -> entry.payloadJson?.let { payload ->
                    runCatching { json.decodeFromString<CachedProduct>(payload).toDomain() }
                        .getOrNull()
                        ?.let { return RemoteResult.Success(it) }
                }
            }
        }

        val wait = productLimiter.tryAcquire()
        if (wait > 0) return RemoteResult.RateLimited(wait)

        val result = api.product(barcode)
        when (result) {
            is RemoteResult.Success -> store(barcode, OffCacheStatus.FOUND, json.encodeToString(CachedProduct.from(result.value)), FOUND_TTL_MS)
            RemoteResult.NotFound -> store(barcode, OffCacheStatus.NOT_FOUND, null, NOT_FOUND_TTL_MS)
            is RemoteResult.RateLimited -> productLimiter.blockFor(result.retryAfterMs)
            else -> Unit
        }
        return result
    }

    override suspend fun search(query: String): RemoteResult<List<RemoteFood>> {
        if (!settings.offEnabled.first()) return RemoteResult.Disabled
        val q = query.trim()
        if (q.length < MIN_QUERY_LENGTH) return RemoteResult.Success(emptyList())

        val wait = searchLimiter.tryAcquire()
        if (wait > 0) return RemoteResult.RateLimited(wait)

        val result = api.search(q)
        if (result is RemoteResult.RateLimited) searchLimiter.blockFor(result.retryAfterMs)
        return result
    }

    private suspend fun store(barcode: String, status: OffCacheStatus, payload: String?, ttlMs: Long) {
        val now = clock.nowMillis()
        cache.purgeExpired(now)
        cache.put(
            OffProductCacheEntity(
                barcode = barcode,
                status = status,
                payloadJson = payload,
                fetchedAt = now,
                expiresAt = now + ttlMs,
            ),
        )
    }

    @Serializable
    private data class CachedProduct(
        val barcode: String,
        val name: String,
        val brand: String?,
        val kcal: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val servingG: Double?,
        val lastModified: Long?,
        val sugarsG: Double? = null,
        val satFatG: Double? = null,
        val fiberG: Double? = null,
        val saltG: Double? = null,
    ) {
        fun toDomain() = RemoteFood(
            barcode, name, brand, Macros(kcal, proteinG, carbsG, fatG), servingG, lastModified,
            NutrientDetail(sugarsG, satFatG, fiberG, saltG),
        )

        companion object {
            fun from(f: RemoteFood) = CachedProduct(
                f.barcode, f.name, f.brand,
                f.per100.energyKcal, f.per100.proteinG, f.per100.carbsG, f.per100.fatG,
                f.servingQuantityG, f.lastModifiedEpochSec,
                f.per100Detail.sugarsG, f.per100Detail.satFatG, f.per100Detail.fiberG, f.per100Detail.saltG,
            )
        }
    }

    internal companion object {
        const val MINUTE_MS = 60_000L
        const val PRODUCT_PER_MINUTE = 12
        const val SEARCH_PER_MINUTE = 8
        const val MIN_QUERY_LENGTH = 3
        const val FOUND_TTL_MS = 30L * 24 * 60 * 60 * 1000
        const val NOT_FOUND_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
