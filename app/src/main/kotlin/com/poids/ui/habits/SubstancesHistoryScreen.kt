package com.poids.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.domain.model.SubstanceKind
import com.poids.domain.model.SubstanceLog
import com.poids.ui.components.BarChart
import com.poids.ui.components.PoidsCard
import com.poids.ui.components.SegmentedSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstancesHistoryScreen(
    onBack: () -> Unit,
    viewModel: SubstanceHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedKind by remember { mutableStateOf(SubstanceKind.CAFFEINE) }
    var showAdd by remember { mutableStateOf(false) }

    val kinds = listOf(SubstanceKind.ALCOHOL, SubstanceKind.TOBACCO, SubstanceKind.CAFFEINE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Substances") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Enregistrer")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SegmentedSelector(
                    options = kinds,
                    selected = selectedKind,
                    onSelect = { selectedKind = it },
                    label = ::kindLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                PoidsCard(modifier = Modifier.fillMaxWidth()) {
                    val month = state.totalThisMonth[selectedKind] ?: 0.0
                    val year = state.totalThisYear[selectedKind] ?: 0.0
                    Text("${kindLabel(selectedKind)} · 12 mois", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ce mois : ${fmt(month)} · Cette année : ${fmt(year)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    val values = state.monthly[selectedKind].orEmpty()
                    if (values.any { it > 0f }) {
                        BarChart(
                            values = values,
                            labels = state.monthLabels,
                            barColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(130.dp).padding(top = 12.dp),
                            barAreaHeightDp = 100,
                        )
                    } else {
                        Text(
                            "Aucune conso enregistrée sur 12 mois.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            item { Text("Historique", style = MaterialTheme.typography.titleMedium) }

            items(state.entries, key = { it.id }) { entry ->
                SubstanceEntryRow(entry, onDelete = { viewModel.delete(entry.id) })
            }
        }
    }

    if (showAdd) {
        AddSubstanceDialog(
            onDismiss = { showAdd = false },
            onConfirm = { kind, amount, unit ->
                viewModel.addSubstance(kind, amount, unit)
                showAdd = false
            },
        )
    }
}

@Composable
private fun SubstanceEntryRow(entry: SubstanceLog, onDelete: () -> Unit) {
    PoidsCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "${kindLabel(entry.kind)} · ${fmt(entry.amount)} ${entry.unit}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    entry.dateTime.toLocalDate().toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

private fun kindLabel(kind: SubstanceKind): String = when (kind) {
    SubstanceKind.ALCOHOL -> "Alcool"
    SubstanceKind.TOBACCO -> "Tabac"
    SubstanceKind.CAFFEINE -> "Café"
    SubstanceKind.OTHER -> "Autre"
}

private fun fmt(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
