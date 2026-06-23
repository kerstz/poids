package com.poids.tracking

import com.poids.domain.model.LiveActivityStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pont entre le service de suivi (qui écrit) et l'UI (qui lit) pour les stats en
 * direct d'une session GPS en cours. `null` = aucune session active.
 */
@Singleton
class TrackingStateHolder @Inject constructor() {
    private val _stats = MutableStateFlow<LiveActivityStats?>(null)
    val stats: StateFlow<LiveActivityStats?> = _stats.asStateFlow()

    fun update(stats: LiveActivityStats?) {
        _stats.value = stats
    }
}
