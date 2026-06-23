package com.poids.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.domain.model.DailyEnergyBalance
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConfigureProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenNutrition: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Poids") },
                actions = {
                    if (state is HomeUiState.Ready) {
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Filled.Edit, contentDescription = "Modifier le profil")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            HomeUiState.Loading -> Centered(padding) { CircularProgressIndicator() }
            HomeUiState.NoProfile -> Centered(padding) { NoProfileContent(onConfigureProfile) }
            is HomeUiState.Ready ->
                ReadyContent(s, onOpenWeight, onOpenNutrition, Modifier.padding(padding))
        }
    }
}

@Composable
private fun Centered(
    padding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun NoProfileContent(onConfigure: () -> Unit) {
    Text("Bienvenue !", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        text = "Configure ton profil pour calculer ton métabolisme et ta cible calorique.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp),
    )
    Button(onClick = onConfigure) { Text("Configurer mon profil") }
}

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    onOpenWeight: () -> Unit,
    onOpenNutrition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val weight = state.latestWeight?.weightKg ?: state.profile.weightKg

        SectionCard("Poids") {
            Line("Poids actuel", "$weight kg")
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenWeight) { Text("Mes pesées") }
                Button(onClick = onOpenNutrition) { Text("Nutrition") }
            }
        }

        SectionCard("Objectif calorique") {
            Line("BMR", "${state.targets.bmr.roundToInt()} kcal")
            Line("TDEE", "${state.targets.tdee.roundToInt()} kcal")
            Line("Cible du jour", "${state.targets.dailyTarget.roundToInt()} kcal")
            Line("Écart appliqué", "${state.targets.dailyDeltaKcal.roundToInt()} kcal")
        }

        BalanceCard(state.balance)
    }
}

@Composable
private fun BalanceCard(balance: DailyEnergyBalance) {
    SectionCard("Bilan du jour") {
        Line("Ingérées", "${balance.intakeKcal.roundToInt()} kcal")
        Line("Brûlées", "${balance.burnedKcal.roundToInt()} kcal")
        Line("Cible", "${balance.targetKcal.roundToInt()} kcal")
        val net = balance.netVsTarget.roundToInt()
        val color = if (net <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Text(
            text = "Solde vs cible : $net kcal",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "Ingérées et brûlées seront alimentées par les features nutrition et activité.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
