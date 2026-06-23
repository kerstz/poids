package com.poids.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.poids.domain.model.SubstanceKind
import com.poids.ui.components.SegmentedSelector
import java.time.LocalTime

@Composable
fun AddSleepDialog(
    onDismiss: () -> Unit,
    onConfirm: (bedTime: LocalTime, wakeTime: LocalTime) -> Unit,
) {
    var bed by remember { mutableStateOf("23:00") }
    var wake by remember { mutableStateOf("07:00") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuit de sommeil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bed,
                    onValueChange = { bed = it; error = null },
                    label = { Text("Coucher (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = wake,
                    onValueChange = { wake = it; error = null },
                    label = { Text("Réveil (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = parseTime(bed)
                val w = parseTime(wake)
                if (b == null || w == null) error = "Heure invalide (ex. 23:30)." else onConfirm(b, w)
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun parseTime(value: String): LocalTime? = runCatching {
    val parts = value.trim().split(":")
    LocalTime.of(parts[0].toInt(), parts[1].toInt())
}.getOrNull()
