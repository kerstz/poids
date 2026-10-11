package org.tally.nutrition.journal

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import org.tally.nutrition.R

@Composable
fun JournalScreen(
    onAddEntry: (mealSlotId: Long, epochDay: Long) -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTarget by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val context = LocalContext.current
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { entry ->
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.journal_deleted, entry.name),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(entry)
        }
    }

    JournalContent(
        state = state,
        snackbarHostState = snackbar,
        editingTarget = editingTarget,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onToday = viewModel::today,
        onAddEntry = { slotId -> onAddEntry(slotId ?: state.suggestedSlotId(LocalTime.now().hour), state.epochDay) },
        onDelete = viewModel::delete,
        onEditTarget = { editingTarget = true },
        onDismissTarget = { editingTarget = false },
        onSaveTarget = {
            viewModel.setEnergyTarget(it)
            editingTarget = false
        },
    )
}

/** Créneau proposé par le bouton principal selon l'heure ; premier créneau à défaut. */
internal fun JournalUiState.suggestedSlotId(hour: Int): Long {
    val key = when {
        hour < 10 -> "breakfast"
        hour < 15 -> "lunch"
        hour < 18 -> "snack"
        else -> "dinner"
    }
    return (sections.firstOrNull { it.slot.builtinKey == key } ?: sections.firstOrNull())?.slot?.id ?: 0L
}
