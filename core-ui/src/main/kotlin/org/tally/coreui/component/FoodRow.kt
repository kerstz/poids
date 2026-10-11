package org.tally.coreui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import org.tally.coreui.icon.TallyIcons
import org.tally.coreui.theme.TallyTheme

/**
 * Ligne de journal : nom, détail (quantité), kcal. Un seul nœud TalkBack.
 * Si [onDelete] est fourni : swipe fin→début + action TalkBack [deleteLabel] ; l'appelant affiche le snackbar « Annuler ».
 */
@Composable
fun FoodRow(
    name: String,
    detail: String,
    kcal: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "",
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TallyTheme.colors.background)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = TallyTheme.dimens.rowMinHeight)
                .padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.m),
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = TallyTheme.colors.onSurface)
                Text(detail, style = TallyTheme.type.numberBody, color = TallyTheme.colors.onSurfaceVariant)
                if (secondary != null) {
                    Text(secondary, style = MaterialTheme.typography.bodySmall, color = TallyTheme.colors.onSurfaceVariant)
                }
            }
            Text(kcal, style = TallyTheme.type.numberBody, color = TallyTheme.colors.onSurface)
        }
    }
    if (onDelete == null) {
        Box(modifier.semantics(mergeDescendants = true) {}) { content() }
        return
    }
    val currentOnDelete = rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete.value()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier.semantics(mergeDescendants = true) {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { currentOnDelete.value(); true })
        },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(TallyTheme.colors.surface3)
                    .padding(horizontal = TallyTheme.spacing.gutter),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(TallyIcons.delete), contentDescription = null, tint = TallyTheme.colors.error)
            }
        },
    ) { content() }
    HorizontalDivider(color = TallyTheme.colors.divider, thickness = TallyTheme.dimens.divider)
}
