// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.coreui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.tally.coreui.theme.TallyTheme

/** Carte plate : palier surface1, pas d'ombre. */
@Composable
fun TallyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clip = Modifier.clip(MaterialTheme.shapes.medium).background(TallyTheme.colors.surface1)
    val click = if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(clip).then(click).padding(TallyTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.s),
        content = content,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TallyTheme.dimens.touchTarget)
            .padding(start = TallyTheme.spacing.gutter, end = if (action != null) TallyTheme.spacing.xs else TallyTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.s),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TallyTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (trailing != null) {
            Text(text = trailing, style = TallyTheme.type.numberBody, color = TallyTheme.colors.onSurfaceVariant)
        }
        action?.invoke()
    }
}
