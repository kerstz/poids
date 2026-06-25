package com.poids.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.ui.components.BalanceBar
import com.poids.ui.components.PoidsCard
import com.poids.ui.components.PrimaryButton
import com.poids.ui.components.ProgressTrack
import com.poids.ui.components.StatTile
import com.poids.ui.components.SuccessChip
import com.poids.ui.theme.PoidsTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onConfigureProfile: () -> Unit,
    onOpenWeight: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val s = state) {
            HomeUiState.Loading -> Unit
            HomeUiState.NoProfile -> NoProfile(onConfigureProfile)
            is HomeUiState.Ready -> Ready(s, onOpenWeight, viewModel::addWater)
        }
    }
}

@Composable
private fun NoProfile(onConfigure: () -> Unit) {
    Text("Bienvenue !", style = MaterialTheme.typography.titleLarge)
    Text(
        "Configure ton profil pour calculer ton métabolisme et ta cible calorique.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
    PrimaryButton("Configurer mon profil", onConfigure, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Ready(
    state: HomeUiState.Ready,
    onOpenWeight: () -> Unit,
    onAddWater: (Int) -> Unit,
) {
    val target = state.targets.dailyTarget
    val solde = (state.intakeKcal - state.burnedKcal - target).roundToInt()

    // En-tête
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Bilan du jour", style = MaterialTheme.typography.titleLarge)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${state.profile.sex.name.first()}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    // Carte balance énergétique
    PoidsCard(modifier = Modifier.fillMaxWidth(), padding = 20) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Bilan énergétique", style = MaterialTheme.typography.titleMedium)
            Text(
                "objectif ${state.targets.dailyDeltaKcal.roundToInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${if (solde > 0) "+" else ""}$solde kcal",
                style = MaterialTheme.typography.displaySmall,
            )
            if (solde <= 0) SuccessChip("Déficit ✓")
        }
        BalanceBar(
            label = "Ingéré",
            value = "${state.intakeKcal.roundToInt()} kcal",
            fraction = if (target > 0) (state.intakeKcal / target).toFloat() else 0f,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        BalanceBar(
            label = "Brûlé",
            value = "${state.burnedKcal.roundToInt()} kcal",
            fraction = if (target > 0) (state.burnedKcal / target).toFloat() else 0f,
            color = PoidsTheme.extra.teal,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Tuiles
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(
            label = "Pas",
            value = "${state.stepDay.steps}",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Distance",
            value = "${"%.1f".format(state.stepDay.distanceM / 1000)} km",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Actives",
            value = "${state.burnedKcal.roundToInt()}",
            sub = "kcal",
            modifier = Modifier.weight(1f),
        )
    }

    // Hydratation
    WaterSummaryCard(state.waterTodayMl, state.waterGoalMl, onAddWater)

    // Macros
    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Text("Macronutriments", style = MaterialTheme.typography.titleMedium)
        val totalMacro = (state.carbsG + state.proteinG + state.fatG).coerceAtLeast(1.0)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MacroColumn("Glucides", state.carbsG, (state.carbsG / totalMacro).toFloat(), PoidsTheme.extra.carbs, Modifier.weight(1f))
            MacroColumn("Protéines", state.proteinG, (state.proteinG / totalMacro).toFloat(), PoidsTheme.extra.protein, Modifier.weight(1f))
            MacroColumn("Lipides", state.fatG, (state.fatG / totalMacro).toFloat(), PoidsTheme.extra.fat, Modifier.weight(1f))
        }
    }

    // Pesée du jour
    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        val weight = state.latestWeight?.weightKg ?: state.profile.weightKg
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Poids actuel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$weight kg", style = MaterialTheme.typography.titleLarge)
            }
        }
        PrimaryButton("Pesée du jour", onOpenWeight, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun WaterSummaryCard(todayMl: Int, goalMl: Int, onAdd: (Int) -> Unit) {
    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hydratation", style = MaterialTheme.typography.titleMedium)
            Text(
                "${"%.2f".format(todayMl / 1000.0)} / ${"%.1f".format(goalMl / 1000.0)} L",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProgressTrack(
            fraction = if (goalMl > 0) (todayMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f,
            color = PoidsTheme.extra.teal,
            heightDp = 6,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = { onAdd(250) }, modifier = Modifier.weight(1f)) {
                Text("+25 cl")
            }
            FilledTonalButton(onClick = { onAdd(500) }, modifier = Modifier.weight(1f)) {
                Text("+50 cl")
            }
        }
    }
}

@Composable
private fun MacroColumn(
    label: String,
    grams: Double,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${grams.roundToInt()} g",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProgressTrack(
            fraction = fraction,
            color = color,
            heightDp = 5,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}
