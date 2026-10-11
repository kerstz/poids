package org.tally.coreui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.tally.coreui.theme.TallyTheme

/** État vide : toujours une action. */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TallyTheme.spacing.xl, vertical = TallyTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = TallyTheme.colors.onSurfaceVariant, modifier = Modifier.size(TallyTheme.dimens.iconL))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TallyTheme.colors.onSurface, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TallyTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Erreur réseau inline avec « Réessayer » ; annoncée par TalkBack. */
@Composable
fun ErrorBanner(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(TallyTheme.colors.surface2)
            .padding(start = TallyTheme.spacing.l, end = TallyTheme.spacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.s),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = TallyTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = TallyTheme.spacing.m),
        )
        TextButton(onClick = onRetry) { Text(retryLabel) }
    }
}
