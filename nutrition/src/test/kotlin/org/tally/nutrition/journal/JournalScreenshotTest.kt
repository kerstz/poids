package org.tally.nutrition.journal

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.tally.domain.model.QuantityUnit
import org.tally.domain.nutrition.DayTargets
import org.tally.domain.nutrition.JournalItem
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.MealSlot

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-w411dp-h914dp-xxhdpi")
class JournalScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val day = LocalDate.of(2026, 10, 11).toEpochDay()

    private fun item(id: Long, slot: Long, name: String, qty: Double, unit: QuantityUnit, macros: Macros) =
        JournalItem(id, slot, name, qty, unit, isRecipe = false, portionCount = null, macros = macros)

    private val slots = listOf(
        MealSlot(1, "breakfast", null, 0),
        MealSlot(2, "lunch", null, 1),
        MealSlot(3, "dinner", null, 2),
        MealSlot(4, "snack", null, 3),
    )

    private fun filled(): JournalUiState {
        val breakfast = listOf(
            item(1, 1, "Flocons d'avoine", 60.0, QuantityUnit.G, Macros(228.0, 8.0, 40.0, 4.0)),
            item(2, 1, "Lait demi-écrémé", 250.0, QuantityUnit.ML, Macros(115.0, 8.5, 12.0, 4.0)),
        )
        val lunch = listOf(
            item(3, 2, "Poulet rôti avec une garniture de légumes de saison", 180.0, QuantityUnit.G, Macros(430.0, 48.0, 6.0, 24.0)),
            item(4, 2, "Riz basmati cuit", 150.0, QuantityUnit.G, Macros(195.0, 4.0, 42.0, 0.5)),
        )
        val sections = listOf(
            SlotSection(slots[0], breakfast, Macros(343.0, 16.5, 52.0, 8.0)),
            SlotSection(slots[1], lunch, Macros(625.0, 52.0, 48.0, 24.5)),
            SlotSection(slots[2], emptyList(), Macros.ZERO),
            SlotSection(slots[3], emptyList(), Macros.ZERO),
        )
        return JournalUiState(
            epochDay = day,
            sections = sections,
            total = Macros(968.0, 68.5, 100.0, 32.5),
            targets = DayTargets(energyKcal = 2200.0),
        )
    }

    private fun empty() = JournalUiState(
        epochDay = day,
        sections = slots.map { SlotSection(it, emptyList(), Macros.ZERO) },
    )

    private fun screenshot(name: String, state: JournalUiState, editingTarget: Boolean = false) =
        rule.captureVariants(name) {
            JournalContent(
                state = state,
                snackbarHostState = remember { SnackbarHostState() },
                editingTarget = editingTarget,
                onPreviousDay = {},
                onNextDay = {},
                onToday = {},
                onAddEntry = {},
                onDelete = {},
                onEditTarget = {},
                onDismissTarget = {},
                onSaveTarget = {},
            )
        }

    @Test fun journalFilled() = screenshot("journal_filled", filled())

    @Test fun journalEmpty() = screenshot("journal_empty", empty())

    @Test fun journalTargetSheet() = screenshot("journal_target_sheet", filled(), editingTarget = true)
}
