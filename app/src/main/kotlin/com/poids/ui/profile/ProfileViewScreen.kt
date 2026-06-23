package com.poids.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.domain.model.Sex
import com.poids.ui.components.PoidsCard
import com.poids.ui.components.PrimaryButton
import com.poids.ui.components.StatTile
import com.poids.ui.theme.PoidsTheme
import kotlin.math.roundToInt

@Composable
fun ProfileViewScreen(
    onEdit: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenTrends: () -> Unit,
    viewModel: ProfileSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val s = state) {
            ProfileSummaryState.Loading -> Unit
            ProfileSummaryState.Empty -> {
                Text("Profil", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Aucun profil. Configure-le pour démarrer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton("Configurer mon profil", onEdit)
            }
            is ProfileSummaryState.Ready -> ReadyProfile(s, onEdit, onOpenWeight, onOpenTrends)
        }
    }
}

@Composable
private fun ReadyProfile(
    state: ProfileSummaryState.Ready,
    onEdit: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenTrends: () -> Unit,
) {
    val weight = state.latestWeight?.weightKg ?: state.profile.weightKg

    // En-tête
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${state.profile.sex.name.first()}",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Column {
            Text("Mon profil", style = MaterialTheme.typography.titleLarge)
            Text(
                "${state.ageYears} ans · ${if (state.profile.sex == Sex.FEMALE) "Femme" else "Homme"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // Tuiles
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile("Taille", "${state.profile.heightCm.roundToInt()} cm", Modifier.weight(1f))
        StatTile("Poids", "$weight kg", Modifier.weight(1f))
        StatTile(
            "Objectif",
            state.profile.targetWeightKg?.let { "${it.roundToInt()}" } ?: "—",
            Modifier.weight(1f),
            valueColor = MaterialTheme.colorScheme.primary,
        )
    }

    // Énergie
    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Text("Énergie", style = MaterialTheme.typography.titleMedium)
        EnergyRow("BMR", "${state.targets.bmr.roundToInt()} kcal")
        EnergyRow("TDEE", "${state.targets.tdee.roundToInt()} kcal")
        EnergyRow(
            "Déficit cible / jour",
            "${state.targets.dailyDeltaKcal.roundToInt()} kcal",
            valueColor = MaterialTheme.colorScheme.primary,
            bold = true,
        )
    }

    // Réglages
    PoidsCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
        SettingRow("Modifier le profil", onEdit)
        SettingRow("Mes pesées", onOpenWeight)
        SettingRow("Tendances", onOpenTrends)
    }

    // Rappel de pesée (configurable)
    ReminderSection()

    // Suivi de substances (optionnel)
    SubstanceTrackingSection()

    // Bannière vie privée
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PoidsTheme.extra.successContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Text(
            "100 % hors-ligne · toutes tes données restent sur l'appareil",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = PoidsTheme.extra.onSuccessContainer,
        )
    }
}

@Composable
private fun EnergyRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold),
            color = valueColor,
        )
    }
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
