// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import org.tally.domain.data.BackupRepository
import org.tally.domain.data.CsvExporter
import org.tally.domain.data.DataWipeRepository
import org.tally.domain.data.RestoreError
import org.tally.domain.data.RestoreResult
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences

/** État de la section « Données » : une opération à la fois, résultat affiché en ligne. */
sealed interface DataStatus {
    data object Idle : DataStatus
    data object Busy : DataStatus
    data class BackupSaved(val rows: Int) : DataStatus
    data class Exported(val rows: Int) : DataStatus
    data class Restored(val rows: Int) : DataStatus
    data object Undone : DataStatus
    data object Wiped : DataStatus
    data object WipeFailed : DataStatus
    data class Failed(val error: RestoreError?) : DataStatus
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val backup: BackupRepository,
    private val csv: CsvExporter,
    private val wiper: DataWipeRepository,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val resolver: ContentResolver = context.contentResolver

    private val _data = MutableStateFlow<DataStatus>(DataStatus.Idle)
    val data: StateFlow<DataStatus> = _data

    fun exportBackup(uri: Uri) = write(uri) { out -> DataStatus.BackupSaved(backup.export(out).rows.values.sum()) }
    fun exportWeights(uri: Uri) = write(uri) { out -> DataStatus.Exported(csv.exportWeights(out)) }
    fun exportJournal(uri: Uri) = write(uri) { out -> DataStatus.Exported(csv.exportJournal(out)) }

    fun restore(uri: Uri) {
        if (_data.value == DataStatus.Busy) return
        _data.value = DataStatus.Busy
        viewModelScope.launch {
            _data.value = try {
                val input = resolver.openInputStream(uri) ?: throw IOException("flux indisponible")
                when (val r = input.use { backup.restore(it) }) {
                    is RestoreResult.Success -> DataStatus.Restored(r.rows.values.sum())
                    is RestoreResult.Rejected -> DataStatus.Failed(r.error)
                }
            } catch (_: IOException) {
                DataStatus.Failed(RestoreError.IO)
            } catch (_: SecurityException) {
                // Permission d'URI révoquée entre la sélection et la lecture.
                DataStatus.Failed(RestoreError.IO)
            } catch (_: IllegalArgumentException) {
                DataStatus.Failed(RestoreError.IO)
            }
        }
    }

    fun undoRestore() {
        if (_data.value == DataStatus.Busy) return
        _data.value = DataStatus.Busy
        viewModelScope.launch {
            _data.value = runCatching { backup.undoLastRestore() }.fold(
                onSuccess = { r ->
                    when (r) {
                        is RestoreResult.Success -> DataStatus.Undone
                        is RestoreResult.Rejected -> DataStatus.Failed(r.error)
                    }
                },
                onFailure = { if (it is CancellationException) throw it else DataStatus.Failed(RestoreError.IO) },
            )
        }
    }

    /** Effacement complet (RGPD art. 17). La confirmation explicite est demandée par l'écran. */
    fun wipe() {
        if (_data.value == DataStatus.Busy) return
        _data.value = DataStatus.Busy
        viewModelScope.launch {
            _data.value = runCatching { wiper.wipeAll() }.fold(
                onSuccess = { DataStatus.Wiped },
                onFailure = { if (it is CancellationException) throw it else DataStatus.WipeFailed },
            )
        }
    }

    private fun write(uri: Uri, block: suspend (OutputStream) -> DataStatus) {
        if (_data.value == DataStatus.Busy) return
        _data.value = DataStatus.Busy
        viewModelScope.launch {
            _data.value = try {
                val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("flux indisponible")
                out.use { block(it) }
            } catch (e: Exception) {
                if (e !is IOException && e !is SecurityException && e !is IllegalArgumentException) throw e
                // Évite de laisser un fichier à moitié écrit.
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
                DataStatus.Failed(null)
            }
        }
    }

    val unitPreferences: StateFlow<UnitPreferences> = repository.unitPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitPreferences())

    val offEnabled: StateFlow<Boolean> = repository.offEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setOffEnabled(enabled: Boolean) = viewModelScope.launch { repository.setOffEnabled(enabled) }

    fun setMass(unit: MassUnit) = viewModelScope.launch { repository.setMassUnit(unit) }
    fun setLength(unit: LengthUnit) = viewModelScope.launch { repository.setLengthUnit(unit) }
    fun setDistance(unit: DistanceUnit) = viewModelScope.launch { repository.setDistanceUnit(unit) }
    fun setEnergy(unit: EnergyUnit) = viewModelScope.launch { repository.setEnergyUnit(unit) }
}
