// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tally.domain.nutrition.DayTargets
import org.tally.domain.nutrition.GoalRepository
import org.tally.domain.nutrition.JournalItem
import org.tally.domain.nutrition.JournalRepository
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.MealSlot
import org.tally.domain.nutrition.MealSlotRepository
import org.tally.domain.nutrition.NewJournalEntry
import org.tally.domain.nutrition.NutritionCalc
import org.tally.domain.nutrition.RestoreJournalEntry

data class SlotSection(val slot: MealSlot, val items: List<JournalItem>, val total: Macros)

/** Entrée supprimée, réinsérable via [JournalViewModel.undo]. */
data class DeletedEntry(val name: String, val snapshot: NewJournalEntry)

/** Échec d'une action du journal, signalé à l'UI (snackbar). */
enum class JournalError { DELETE_FAILED, UNDO_FAILED }

data class JournalUiState(
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val sections: List<SlotSection> = emptyList(),
    val total: Macros = Macros.ZERO,
    val targets: DayTargets = DayTargets(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journal: JournalRepository,
    private val restoreEntry: RestoreJournalEntry,
    private val goals: GoalRepository,
    slots: MealSlotRepository,
) : ViewModel() {

    private val day = MutableStateFlow(LocalDate.now().toEpochDay())

    val uiState: StateFlow<JournalUiState> = day.flatMapLatest { epochDay ->
        combine(
            journal.observeDay(epochDay),
            slots.observeSlots(),
            goals.observeTargets(epochDay),
        ) { items, slotList, targets ->
            val byMeal = items.groupBy { it.mealSlotId }
            JournalUiState(
                epochDay = epochDay,
                sections = slotList.map { slot ->
                    val slotItems = byMeal[slot.id].orEmpty()
                    SlotSection(slot, slotItems, NutritionCalc.total(slotItems))
                },
                total = NutritionCalc.total(items),
                targets = targets,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun previousDay() = day.update { it - 1 }
    fun nextDay() = day.update { it + 1 }
    fun today() { day.value = LocalDate.now().toEpochDay() }

    // Channel plutôt que SharedFlow sans replay : un événement émis pendant une recréation n'est pas perdu.
    private val deletedEvents = Channel<DeletedEntry>(Channel.BUFFERED)
    val deleted: Flow<DeletedEntry> = deletedEvents.receiveAsFlow()

    private val errorEvents = Channel<JournalError>(Channel.BUFFERED)
    val errors: Flow<JournalError> = errorEvents.receiveAsFlow()

    fun delete(item: JournalItem) {
        viewModelScope.launch {
            try {
                val snapshot = journal.snapshot(item.id)
                journal.delete(item.id)
                if (snapshot != null) deletedEvents.send(DeletedEntry(item.name, snapshot))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorEvents.send(JournalError.DELETE_FAILED)
            }
        }
    }

    fun undo(entry: DeletedEntry) {
        viewModelScope.launch {
            try {
                restoreEntry(entry.snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorEvents.send(JournalError.UNDO_FAILED)
            }
        }
    }

    fun setEnergyTarget(kcal: Double) {
        viewModelScope.launch { goals.setEnergyTarget(kcal, fromEpochDay = day.value) }
    }
}
