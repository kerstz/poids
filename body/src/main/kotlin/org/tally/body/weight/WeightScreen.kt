// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.body.weight

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import org.tally.body.R

@Composable
fun WeightScreen(viewModel: WeightViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.weight_undo)

    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { entry ->
            val result = snackbar.showSnackbar(
                message = resources.getString(R.string.weight_deleted),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(entry)
        }
    }

    WeightContent(
        state = state,
        snackbarHostState = snackbar,
        editor = editor,
        onAdd = { editor = EditorTarget.New },
        onEdit = { editor = EditorTarget.Edit(it) },
        onDelete = viewModel::delete,
        onDismissEditor = { editor = null },
        onSave = { value, measuredAt ->
            val saved = viewModel.save((editor as? EditorTarget.Edit)?.entry?.id, value, measuredAt)
            if (saved) editor = null
            saved
        },
    )
}

internal fun safeZone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())

internal fun Long.toLocalDate(zoneId: String) = Instant.ofEpochMilli(this).atZone(safeZone(zoneId)).toLocalDate()
