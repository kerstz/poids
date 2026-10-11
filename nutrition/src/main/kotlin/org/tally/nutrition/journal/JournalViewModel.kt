package org.tally.nutrition.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

data class SlotSection(val slot: MealSlot, val items: List<JournalItem>, val total: Macros)

/** Entrée supprimée, réinsérable via [JournalViewModel.undo]. */
data class DeletedEntry(val name: String, val snapshot: NewJournalEntry)

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

    private val deletedEvents = MutableSharedFlow<DeletedEntry>(extraBufferCapacity = 8)
    val deleted: SharedFlow<DeletedEntry> = deletedEvents

    fun delete(item: JournalItem) {
        viewModelScope.launch {
            val snapshot = journal.snapshot(item.id)
            journal.delete(item.id)
            if (snapshot != null) deletedEvents.emit(DeletedEntry(item.name, snapshot))
        }
    }

    fun undo(entry: DeletedEntry) {
        viewModelScope.launch { journal.add(entry.snapshot) }
    }

    fun setEnergyTarget(kcal: Double) {
        viewModelScope.launch { goals.setEnergyTarget(kcal, fromEpochDay = day.value) }
    }
}
