package com.poids.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.poids.domain.model.WeightEntry
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onBack: () -> Unit,
    viewModel: WeightViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes pesées") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une pesée")
            }
        },
    ) { padding ->
        if (state.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aucune pesée. Touche + pour en ajouter une.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.entries.size >= 2) {
                    item { TrendCard(state.entries) }
                }
                item { SummaryCard(state.latest, state.totalChangeKg) }
                items(state.entries, key = { it.id }) { entry ->
                    WeightRow(entry, onDelete = { viewModel.delete(entry.id) })
                }
            }
        }
    }

    if (showAdd) {
        AddWeightDialog(
            onDismiss = { showAdd = false },
            onConfirm = { weight, date, note ->
                viewModel.addWeight(weight, date, note)
                showAdd = false
            },
        )
    }
}

@Composable
private fun TrendCard(entriesDesc: List<WeightEntry>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tendance", style = MaterialTheme.typography.titleMedium)
            WeightTrendChart(
                weightsChrono = entriesDesc.reversed().map { it.weightKg },
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SummaryCard(latest: WeightEntry?, totalChangeKg: Double?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Poids actuel : ${latest?.weightKg ?: "—"} kg", style = MaterialTheme.typography.titleMedium)
            if (totalChangeKg != null) {
                val sign = if (totalChangeKg <= 0) "−" else "+"
                Text(
                    "Variation totale : $sign${"%.1f".format(abs(totalChangeKg))} kg",
                    color = if (totalChangeKg <= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightRow(entry: WeightEntry, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${entry.weightKg} kg", style = MaterialTheme.typography.bodyLarge)
                Text(entry.date.toString(), style = MaterialTheme.typography.bodySmall)
                entry.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            }
        }
    }
}
