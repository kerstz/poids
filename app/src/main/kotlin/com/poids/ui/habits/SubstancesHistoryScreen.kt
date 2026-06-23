package com.poids.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.domain.model.SubstanceLog
import com.poids.ui.components.BarChart
import com.poids.ui.components.PoidsCard
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstancesHistoryScreen(
    onBack: () -> Unit,
    viewModel: SubstanceHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedName by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SubstanceLog?>(null) }

    LaunchedEffect(state.names) {
        if (selectedName !in state.names) selectedName = state.names.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suivi de conso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.names.isEmpty()) {
                item {
                    Text(
                        "Aucune conso enregistrée. Touche + pour ajouter (substance, dose, heure).",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.names.forEach { name ->
                            FilterChip(
                                selected = selectedName == name,
                                onClick = { selectedName = name },
                                label = { Text(name) },
                            )
                        }
                    }
                }
                item {
                    val name = selectedName
                    PoidsCard(modifier = Modifier.fillMaxWidth()) {
                        Text("${name ?: ""} · 12 mois", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Ce mois : ${fmt(state.totalMonthByName[name] ?: 0.0)} · " +
                                "Cette année : ${fmt(state.totalYearByName[name] ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        val values = state.monthlyByName[name].orEmpty()
                        if (values.any { it > 0f }) {
                            BarChart(
                                values = values,
                                labels = state.monthLabels,
                                barColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().height(130.dp).padding(top = 12.dp),
                                barAreaHeightDp = 100,
                            )
                        }
                    }
                }
                item { Text("Historique", style = MaterialTheme.typography.titleMedium) }
                items(state.entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onEdit = { editing = entry },
                        onDelete = { viewModel.delete(entry.id) },
                    )
                }
            }
        }
    }

    if (showAdd) {
        SubstanceEntryDialog(
            onDismiss = { showAdd = false },
            onConfirm = { input -> viewModel.addSubstance(input); showAdd = false },
        )
    }
    editing?.let { entry ->
        SubstanceEntryDialog(
            initial = SubstanceEntryInput(
                name = displayName(entry),
                kind = entry.kind,
                amount = entry.amount,
                unit = entry.unit,
                dateTime = entry.dateTime,
            ),
            onDismiss = { editing = null },
            onConfirm = { input -> viewModel.updateSubstance(entry.id, input); editing = null },
        )
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")

@Composable
private fun EntryRow(entry: SubstanceLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    PoidsCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), padding = 0) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "${displayName(entry)} · ${fmt(entry.amount)} ${entry.unit}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    entry.dateTime.format(dateFmt),
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

private fun fmt(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
