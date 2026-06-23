package com.poids.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.poids.domain.model.CalorieTargets
import com.poids.domain.model.UserProfile
import com.poids.domain.model.WeightEntry
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Poids") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val s = state) {
                HomeUiState.Loading -> CircularProgressIndicator()
                HomeUiState.NoProfile -> NoProfileContent(onCreate = viewModel::createSampleProfile)
                is HomeUiState.Ready -> ReadyContent(s.profile, s.latestWeight, s.targets)
            }
        }
    }
}

@Composable
private fun NoProfileContent(onCreate: () -> Unit) {
    Text(
        text = "Aucun profil configuré.",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = "L'écran de profil arrive à la prochaine feature. " +
            "Pour vérifier que la base est bien câblée, crée un profil d'exemple.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp),
    )
    Button(onClick = onCreate) { Text("Créer un profil d'exemple") }
}

@Composable
private fun ReadyContent(
    profile: UserProfile,
    latestWeight: WeightEntry?,
    targets: CalorieTargets,
) {
    Text("Bilan du jour", style = MaterialTheme.typography.headlineSmall)
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val weight = latestWeight?.weightKg ?: profile.weightKg
        Line("Poids actuel", "${weight} kg")
        Line("BMR", "${targets.bmr.roundToInt()} kcal")
        Line("TDEE", "${targets.tdee.roundToInt()} kcal")
        Line("Cible du jour", "${targets.dailyTarget.roundToInt()} kcal")
        Line("Déficit appliqué", "${targets.dailyDeltaKcal.roundToInt()} kcal")
    }
}

@Composable
private fun Line(label: String, value: String) {
    Text(
        text = "$label : $value",
        style = MaterialTheme.typography.bodyLarge,
    )
}
