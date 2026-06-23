package com.poids.ui.nutrition

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.poids.domain.model.LoggedMeal
import com.poids.domain.model.MealType
import com.poids.domain.repository.FoodLookupResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    onSearch: () -> Unit,
    onManual: () -> Unit,
    onAddMeal: (foodId: Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.onBarcodeScanned(it) }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.scanEvents.collect { result ->
            when (result) {
                is FoodLookupResult.Found -> onAddMeal(result.food.id)
                FoodLookupResult.NotFound ->
                    snackbar.showSnackbar("Produit inconnu d'Open Food Facts.")
                is FoodLookupResult.Error ->
                    snackbar.showSnackbar("Hors-ligne ou erreur : ${result.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Aujourd'hui : ${state.totalKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.titleLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSearch) { Text("Rechercher") }
                OutlinedButton(onClick = {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
                            setPrompt("Scanner un code-barres")
                            setBeepEnabled(false)
                            setOrientationLocked(false)
                        },
                    )
                }) { Text("Scanner") }
                OutlinedButton(onClick = onManual) { Text("Manuel") }
            }

            if (state.meals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun repas enregistré aujourd'hui.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.meals, key = { it.id }) { meal ->
                        MealRow(meal, onDelete = { viewModel.deleteMeal(meal.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: LoggedMeal, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            meal.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.food.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${mealTypeLabel(meal.mealType)} · ${meal.quantityG.roundToInt()} g · " +
                        "${meal.kcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

internal fun mealTypeLabel(type: MealType): String = when (type) {
    MealType.BREAKFAST -> "Petit-déj"
    MealType.LUNCH -> "Déjeuner"
    MealType.DINNER -> "Dîner"
    MealType.SNACK -> "Collation"
}
