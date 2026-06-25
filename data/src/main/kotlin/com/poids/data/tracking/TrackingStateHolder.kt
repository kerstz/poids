package com.poids.data.tracking

import com.poids.domain.model.LiveActivityStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pont entre le service de suivi (qui écrit) et les lecteurs (UI + podomètre) pour
 * les stats en direct d'une session GPS en cours. `null` = aucune session active.
 *
 * Vit dans `:data` (et non `:app`) pour que [com.poids.data.repository.StepRepositoryImpl]
 * puisse savoir qu'une session tourne et figer l'accumulation des pas (anti double-comptage).
 */
@Singleton
class TrackingStateHolder @Inject constructor() {
    private val _stats = MutableStateFlow<LiveActivityStats?>(null)
    val stats: StateFlow<LiveActivityStats?> = _stats.asStateFlow()

    /** Une session GPS est-elle en cours ? */
    val sessionActive: Boolean get() = _stats.value != null

    fun update(stats: LiveActivityStats?) {
        _stats.value = stats
    }
}
