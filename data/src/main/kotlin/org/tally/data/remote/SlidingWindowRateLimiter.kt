package org.tally.data.remote

/**
 * Limiteur côté client (fenêtre glissante). N'attend jamais : retourne le délai à patienter,
 * l'appelant décide quoi afficher. Les marges sont choisies sous les limites publiées par OFF.
 */
class SlidingWindowRateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long,
    private val now: () -> Long,
) {
    private val stamps = ArrayDeque<Long>()
    private var blockedUntil = 0L

    /** 0 = requête autorisée (et comptée) ; sinon millisecondes à attendre. */
    @Synchronized
    fun tryAcquire(): Long {
        val t = now()
        if (t < blockedUntil) return blockedUntil - t
        while (stamps.isNotEmpty() && t - stamps.first() >= windowMs) stamps.removeFirst()
        if (stamps.size < maxRequests) {
            stamps.addLast(t)
            return 0
        }
        return windowMs - (t - stamps.first())
    }

    /** Bloque toute requête pendant [ms] (réponse 429/503 du serveur). */
    @Synchronized
    fun blockFor(ms: Long) {
        blockedUntil = maxOf(blockedUntil, now() + ms)
    }
}
