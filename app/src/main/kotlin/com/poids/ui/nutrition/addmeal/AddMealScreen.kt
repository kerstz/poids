package com.poids.ui.nutrition.addmeal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.poids.domain.model.FoodItem
import com.poids.domain.model.MealType
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddMealViewModel = hiltViewModel(),
) {
    val food by viewModel.food.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    LaunchedEffect(form.saved) {
        if (form.saved) onDone()
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onPhotoPicked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter au journal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        val current = food
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        } else {
            MealForm(
                food = current,
                quantity = form.quantityG,
                mealType = form.mealType,
                photoPath = form.photoPath,
                error = form.error,
                onQuantityChange = viewModel::onQuantityChange,
                onMealTypeChange = viewModel::onMealTypeChange,
                onPickPhoto = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onSave = viewModel::save,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealForm(
    food: FoodItem,
    quantity: String,
    mealType: MealType,
    photoPath: String?,
    error: String?,
    onQuantityChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onPickPhoto: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qty = quantity.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(food.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            "${food.energyKcal100.roundToInt()} kcal / 100 g",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantité (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("≈ ${food.kcalFor(qty).roundToInt()} kcal", style = MaterialTheme.typography.titleMedium)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MealType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = mealType == type,
                    onClick = { onMealTypeChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, MealType.entries.size),
                ) { Text(shortMealLabel(type)) }
            }
        }

        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Photo du repas",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
        OutlinedButton(onClick = onPickPhoto) {
            Text(if (photoPath == null) "Ajouter une photo" else "Changer la photo")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Ajouter au journal")
        }
    }
}

private fun shortMealLabel(type: MealType): String = when (type) {
    MealType.BREAKFAST -> "P.déj"
    MealType.LUNCH -> "Déj"
    MealType.DINNER -> "Dîner"
    MealType.SNACK -> "Coll"
}
