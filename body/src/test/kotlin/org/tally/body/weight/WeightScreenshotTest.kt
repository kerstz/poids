// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.body.weight

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.tally.core.unit.MassUnit
import org.tally.domain.body.WeightEntry

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-w411dp-h914dp-xxhdpi")
class WeightScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun entry(id: Long, daysAgo: Long, kg: Double): WeightEntry {
        val day = LocalDate.of(2026, 10, 11).minusDays(daysAgo)
        return WeightEntry(id, kg, day.atTime(7, 30).toInstant(ZoneOffset.UTC).toEpochMilli(), "UTC")
    }

    private val entries = listOf(
        entry(6, 0, 78.4), entry(5, 2, 78.9), entry(4, 5, 79.3),
        entry(3, 9, 79.1), entry(2, 14, 80.2), entry(1, 20, 80.8),
    )

    private fun screenshot(name: String, state: WeightUiState, editor: EditorTarget? = null) =
        rule.captureVariants(name) {
            WeightContent(
                state = state,
                snackbarHostState = remember { SnackbarHostState() },
                editor = editor,
                onAdd = {},
                onEdit = {},
                onDelete = {},
                onDismissEditor = {},
                onSave = { _, _ -> true },
            )
        }

    @Test fun weightFilled() = screenshot("weight_filled", WeightUiState(entries, MassUnit.KG))

    @Test fun weightEmpty() = screenshot("weight_empty", WeightUiState())

    @Test fun weightEditor() = screenshot("weight_editor", WeightUiState(entries, MassUnit.KG), EditorTarget.Edit(entries.first()))
}
