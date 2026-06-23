package com.poids.ui.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.ui.components.BarChart
import com.poids.ui.components.PoidsCard
import com.poids.ui.theme.PoidsTheme
import com.poids.ui.weight.WeightTrendChart
import java.time.LocalDate
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayLabels = remember7DayLabels()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tendances") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Poids
            PoidsCard(modifier = Modifier.fillMaxWidth()) {
                Row3(
                    "Poids · 30 j",
                    state.weightChange?.let {
                        "${if (it <= 0) "−" else "+"}${"%.1f".format(abs(it))} kg"
                    },
                    if (state.weightChange != null && state.weightChange!! <= 0) {
                        PoidsTheme.extra.teal
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                if (state.weightSeries.size >= 2) {
                    WeightTrendChart(
                        weightsChrono = state.weightSeries,
                        lineColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(130.dp).padding(top = 12.dp),
                    )
                } else {
                    Empty("Ajoute des pesées pour voir la courbe.")
                }
            }

            // Pas
            PoidsCard(modifier = Modifier.fillMaxWidth()) {
                Text("Pas · 7 j", style = MaterialTheme.typography.titleMedium)
                if (state.steps7.any { it > 0 }) {
                    BarChart(
                        values = state.steps7.map { it.toFloat() },
                        labels = dayLabels,
                        barColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp),
                    )
                } else {
                    Empty("Aucun pas enregistré cette semaine.")
                }
            }

            // Sommeil
            PoidsCard(modifier = Modifier.fillMaxWidth()) {
                Text("Sommeil · 7 j", style = MaterialTheme.typography.titleMedium)
                if (state.sleep7.any { it > 0 }) {
                    BarChart(
                        values = state.sleep7.map { it / 60f },
                        labels = dayLabels,
                        barColor = PoidsTheme.extra.teal,
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp),
                    )
                } else {
                    Empty("Aucune nuit enregistrée cette semaine.")
                }
            }
        }
    }
}

@Composable
private fun Row3(title: String, value: String?, valueColor: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (value != null) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
        }
    }
}

@Composable
private fun Empty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun remember7DayLabels(): List<String> {
    val start = LocalDate.now().minusDays(6)
    return (0..6).map { offset ->
        when (start.plusDays(offset.toLong()).dayOfWeek.value) {
            1 -> "L"; 2 -> "M"; 3 -> "M"; 4 -> "J"; 5 -> "V"; 6 -> "S"; else -> "D"
        }
    }
}
