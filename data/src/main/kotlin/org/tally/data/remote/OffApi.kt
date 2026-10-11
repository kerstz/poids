package org.tally.data.remote

import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.RemoteFood
import org.tally.domain.nutrition.RemoteResult

/**
 * Client Open Food Facts. User-Agent obligatoire (docs OFF), HTTPS uniquement, aucune authentification.
 * Ne gère ni cache ni limitation : voir [CachedOffRemoteSource].
 */
internal class OffApi(
    private val client: OkHttpClient,
    private val userAgent: String,
    private val language: () -> String = { Locale.getDefault().language },
    private val productBase: String = "https://world.openfoodfacts.org/api/v2/product/",
    private val searchBase: String = "https://search.openfoodfacts.org/search",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun product(barcode: String): RemoteResult<RemoteFood> {
        val url = (productBase + barcode).toHttpUrl().newBuilder()
            .addQueryParameter("fields", FIELDS)
            .build()
        return execute(url.toString()) { body ->
            val root = json.parseToJsonElement(body).jsonObject
            if (root.num("status") != 1.0) return@execute RemoteResult.NotFound
            val product = root["product"] as? JsonObject ?: return@execute RemoteResult.NotFound
            product.toRemoteFood(fallbackCode = barcode)
                ?.let { RemoteResult.Success(it) }
                ?: RemoteResult.NotFound
        }
    }

    suspend fun search(query: String): RemoteResult<List<RemoteFood>> {
        val url = searchBase.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page_size", PAGE_SIZE.toString())
            .addQueryParameter("fields", FIELDS)
            .addQueryParameter("langs", language())
            .build()
        return execute(url.toString()) { body ->
            val hits = json.parseToJsonElement(body).jsonObject["hits"] as? JsonArray
                ?: return@execute RemoteResult.Unavailable
            RemoteResult.Success(hits.mapNotNull { (it as? JsonObject)?.toRemoteFood() })
        }
    }

    private suspend fun <T> execute(url: String, parse: (String) -> RemoteResult<T>): RemoteResult<T> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()
        return try {
            client.newCall(request).await().use { response ->
                when {
                    response.code == 429 || response.code == 503 ->
                        RemoteResult.RateLimited(retryAfterMs(response))
                    response.code == 404 -> RemoteResult.NotFound
                    !response.isSuccessful -> RemoteResult.Unavailable
                    else -> parse(response.body.string())
                }
            }
        } catch (_: IOException) {
            RemoteResult.Unavailable
        } catch (_: kotlinx.serialization.SerializationException) {
            RemoteResult.Unavailable
        } catch (_: IllegalArgumentException) {
            RemoteResult.Unavailable
        }
    }

    private fun retryAfterMs(response: Response): Long =
        response.header("Retry-After")?.toLongOrNull()?.times(1_000) ?: DEFAULT_RETRY_MS

    private companion object {
        const val FIELDS = "code,product_name,brands,nutriments,serving_quantity,last_modified_t"
        const val PAGE_SIZE = 20
        const val DEFAULT_RETRY_MS = 60_000L
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}

private fun JsonObject.str(key: String): String? = when (val e = this[key]) {
    is JsonPrimitive -> e.contentOrNull
    is JsonArray -> (e.firstOrNull() as? JsonPrimitive)?.contentOrNull
    else -> null
}

private fun JsonObject.num(key: String): Double? =
    (this[key] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

/** Produit OFF → modèle. Null si nom ou énergie manquants (inutilisable dans un journal). */
internal fun JsonObject.toRemoteFood(fallbackCode: String? = null): RemoteFood? {
    val code = str("code")?.takeIf { it.isNotBlank() } ?: fallbackCode ?: return null
    val name = str("product_name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val nutriments = this["nutriments"]?.asObject() ?: return null
    val kcal = nutriments.num("energy-kcal_100g")
        ?: nutriments.num("energy_100g")?.div(KJ_PER_KCAL)
        ?: return null
    val brand = when (val raw = this["brands"]) {
        is JsonArray -> raw.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.firstOrNull()
        is JsonPrimitive -> raw.contentOrNull?.split(',')?.firstOrNull()
        else -> null
    }?.trim()?.ifEmpty { null }
    return RemoteFood(
        barcode = code,
        name = name,
        brand = brand,
        per100 = Macros(
            energyKcal = kcal,
            proteinG = nutriments.num("proteins_100g") ?: 0.0,
            carbsG = nutriments.num("carbohydrates_100g") ?: 0.0,
            fatG = nutriments.num("fat_100g") ?: 0.0,
        ),
        servingQuantityG = num("serving_quantity")?.takeIf { it > 0 },
        lastModifiedEpochSec = num("last_modified_t")?.toLong(),
    )
}

private const val KJ_PER_KCAL = 4.184
