// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.coreui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.remember
import org.tally.coreui.theme.TallyTheme

/** Champ décimal : accepte « , » et « . » ; le parsing reste côté appelant. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == ',' || it == '.' }) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(unit, color = TallyTheme.colors.onSurfaceVariant) },
        textStyle = TallyTheme.type.numberBody,
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
    )
}

/**
 * Saisie d'une quantité en bottom sheet. [units] vide ⇒ unité unique [unit] (ex. objectif en kcal).
 * [preview] : aperçu calculé par l'appelant (ex. « 150 g = 78 kcal »).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuantitySheet(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    units: List<String> = emptyList(),
    onUnitChange: (String) -> Unit = {},
    error: String? = null,
    confirmEnabled: Boolean = true,
    preview: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = TallyTheme.colors.surface2,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.l),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TallyTheme.colors.onSurface)
            NumberField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                unit = unit,
                error = error,
                modifier = Modifier.focusRequester(focus),
                onImeAction = { if (confirmEnabled) onConfirm() },
            )
            if (units.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.s)) {
                    units.forEach { u ->
                        FilterChip(selected = u == unit, onClick = { onUnitChange(u) }, label = { Text(u) })
                    }
                }
            }
            preview?.invoke()
            Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(confirmLabel)
            }
        }
    }
}
