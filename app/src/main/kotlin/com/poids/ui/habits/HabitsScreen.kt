package com.poids.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.ui.components.PoidsCard
import com.poids.ui.theme.PoidsTheme
import com.poids.ui.weight.WeightTrendChart

private val dayLetters = listOf("L", "M", "M", "J", "V", "S", "D")

@Composable
fun HabitsScreen(
    onOpenSubstances: () -> Unit = {},
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSubstanceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Habitudes", style = MaterialTheme.typography.titleLarge)

        SleepCard(
            latestSummary = state.latestSleep?.let {
                "%02d:%02d → %02d:%02d".format(
                    it.bedTime.hour, it.bedTime.minute, it.wakeTime.hour, it.wakeTime.minute,
                )
            },
            durationMin = state.latestSleep?.durationMin,
            last7 = state.last7Sleep,
            onAdd = { showSleepDialog = true },
        )

        if (state.substanceTrackingEnabled) {
            SubstancesCard(
                alcohol = state.alcoholToday,
                tobacco = state.tobaccoToday,
                caffeine = state.caffeineToday,
                onAdd = { showSubstanceDialog = true },
                onOpenHistory = onOpenSubstances,
            )
        }

        PoidsCard(modifier = Modifier.fillMaxWidth()) {
            Text("Tendance poids", style = MaterialTheme.typography.titleMedium)
            if (state.weightSeries.size >= 2) {
                WeightTrendChart(
                    weightsChrono = state.weightSeries,
                    lineColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp),
                )
            } else {
                Text(
                    "Ajoute des pesées pour voir la tendance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showSleepDialog) {
        AddSleepDialog(
            onDismiss = { showSleepDialog = false },
            onConfirm = { bed, wake ->
                viewModel.addSleep(bed, wake)
                showSleepDialog = false
            },
        )
    }
    if (showSubstanceDialog) {
        SubstanceEntryDialog(
            onDismiss = { showSubstanceDialog = false },
            onConfirm = { input ->
                viewModel.addSubstance(input)
                showSubstanceDialog = false
            },
        )
    }
}

@Composable
private fun SleepCard(
    latestSummary: String?,
    durationMin: Int?,
    last7: List<Int>,
    onAdd: () -> Unit,
) {
    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sommeil", style = MaterialTheme.typography.titleMedium)
            Text(
                latestSummary ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (durationMin != null) {
            Text(
                "${durationMin / 60} h ${durationMin % 60}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        SleepBars(last7, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        TextButton(onClick = onAdd) { Text("+ Ajouter une nuit") }
    }
}

@Composable
private fun SleepBars(last7: List<Int>, modifier: Modifier = Modifier) {
    val maxMin = 600f // 10 h de référence
    Row(
        modifier = modifier.height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        last7.forEachIndexed { index, minutes ->
            val frac = (minutes / maxMin).coerceIn(0.04f, 1f)
            val isWeekend = index >= 5
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((70 * frac).dp)
                        .background(
                            if (isWeekend) MaterialTheme.colorScheme.primary else PoidsTheme.extra.track,
                            RoundedCornerShape(4.dp),
                        ),
                )
                Text(
                    dayLetters.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = PoidsTheme.extra.muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SubstancesCard(
    alcohol: Double,
    tobacco: Double,
    caffeine: Double,
    onAdd: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Substances · aujourd'hui", style = MaterialTheme.typography.titleMedium)
            Text(
                "Historique ›",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenHistory),
            )
        }
        SubstanceRow("Alcool", alcohol)
        SubstanceRow("Tabac", tobacco)
        SubstanceRow("Café", caffeine)
        TextButton(onClick = onAdd) { Text("+ Enregistrer") }
    }
}

@Composable
private fun SubstanceRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            if (amount == 0.0) "0" else amount.toInt().toString(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (amount == 0.0) PoidsTheme.extra.success else MaterialTheme.colorScheme.onSurface,
        )
    }
}
