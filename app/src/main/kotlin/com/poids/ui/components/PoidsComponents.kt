package com.poids.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poids.ui.theme.PoidsTheme

/** Carte douce : ombre légère en clair, fine bordure en sombre. */
@Composable
fun PoidsCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (dark) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        shadowElevation = if (dark) 0.dp else 2.dp,
    ) {
        Column(modifier = Modifier.padding(padding.dp), content = content)
    }
}

/** Bouton primaire pleine largeur, 54 dp, coins 16 dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().height(54.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
    }
}

/** Tuile statistique : label capitale discret + grande valeur + sous-texte. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    PoidsCard(modifier = modifier, padding = 14) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PoidsTheme.extra.muted,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (sub != null) {
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Barre horizontale label + valeur + progression (balance énergétique). */
@Composable
fun BalanceBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        ProgressTrack(
            fraction = fraction,
            color = color,
            heightDp = 10,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

/** Piste de progression arrondie. */
@Composable
fun ProgressTrack(
    fraction: Float,
    color: Color,
    heightDp: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .background(PoidsTheme.extra.track, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, shape),
        )
    }
}

/** Puce de statut (succès). */
@Composable
fun SuccessChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = PoidsTheme.extra.successContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = PoidsTheme.extra.onSuccessContainer,
        )
    }
}

/** Sélecteur segmenté à choix unique, style « Sérénité ». */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable { onSelect(option) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.5.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
