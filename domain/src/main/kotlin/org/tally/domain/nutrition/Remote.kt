package org.tally.domain.nutrition

/** Produit issu d'une base distante (Open Food Facts). Valeurs pour 100 g. */
data class RemoteFood(
    val barcode: String,
    val name: String,
    val brand: String?,
    val per100: Macros,
    /** Taille de portion déclarée, en g, si connue. */
    val servingQuantityG: Double?,
    val lastModifiedEpochSec: Long?,
)

sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>
    /** Code inconnu ou produit sans données nutritionnelles exploitables. */
    data object NotFound : RemoteResult<Nothing>
    /** Limite de requêtes (locale ou HTTP 429/503) : réessayer après [retryAfterMs]. */
    data class RateLimited(val retryAfterMs: Long) : RemoteResult<Nothing>
    /** Réseau indisponible, délai dépassé ou réponse invalide. */
    data object Unavailable : RemoteResult<Nothing>
    /** L'utilisateur a désactivé les requêtes réseau (`off.enabled`). */
    data object Disabled : RemoteResult<Nothing>
}

interface FoodRemoteSource {
    suspend fun findByBarcode(barcode: String): RemoteResult<RemoteFood>
    suspend fun search(query: String): RemoteResult<List<RemoteFood>>
}
