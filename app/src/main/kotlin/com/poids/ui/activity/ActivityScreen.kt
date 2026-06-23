package com.poids.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.ActivityType
import com.poids.domain.model.LiveActivityStats
import com.poids.domain.model.StepDay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val stepDay by viewModel.stepDay.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val live by viewModel.liveStats.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(ActivityType.WALKING) }

    val stepPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.startStepTracking() }

    LaunchedEffect(Unit) {
        if (!viewModel.stepSensorAvailable) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startStepTracking() else stepPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    val gpsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.startGps(selectedType)
    }

    fun startGps() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) {
            viewModel.startGps(selectedType)
        } else {
            val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms += Manifest.permission.POST_NOTIFICATIONS
            }
            gpsPermLauncher.launch(perms.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activité") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepCard(viewModel.stepSensorAvailable, stepDay)
            GpsCard(
                live = live,
                selectedType = selectedType,
                onTypeChange = { selectedType = it },
                onStart = ::startGps,
                onStop = viewModel::stopGps,
            )
            if (sessions.isNotEmpty()) {
                Text("Historique", style = MaterialTheme.typography.titleLarge)
                sessions.forEach { SessionRow(it) }
            }
        }
    }
}

@Composable
private fun StepCard(sensorAvailable: Boolean, stepDay: StepDay) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Pas aujourd'hui", style = MaterialTheme.typography.titleLarge)
            if (!sensorAvailable) {
                Text(
                    "Aucun podomètre détecté sur cet appareil.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text("${stepDay.steps} pas", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${"%.2f".format(stepDay.distanceM / 1000)} km · " +
                        "${stepDay.caloriesKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpsCard(
    live: LiveActivityStats?,
    selectedType: ActivityType,
    onTypeChange: (ActivityType) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Suivi GPS", style = MaterialTheme.typography.titleLarge)

            if (live == null) {
                ActivityTypeDropdown(selectedType, onTypeChange)
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Démarrer")
                }
            } else {
                Text(live.type.label, style = MaterialTheme.typography.titleMedium)
                Text("Durée : ${formatDuration(live.durationSec)}")
                Text("Distance : ${"%.2f".format(live.distanceM / 1000)} km")
                Text("Vitesse : ${"%.1f".format(live.currentSpeedMps * 3.6)} km/h")
                Text("Dénivelé + : ${live.elevationGainM.roundToInt()} m")
                Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text("Arrêter")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTypeDropdown(selected: ActivityType, onSelected: (ActivityType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type d'activité") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActivityType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: ActivitySession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(session.type.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${"%.2f".format(session.distanceM / 1000)} km · " +
                    "${formatDuration(session.durationSec)} · " +
                    "${session.caloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
