package com.poids.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poids.R
import com.poids.reminder.ReminderSettingsViewModel
import com.poids.ui.components.PoidsCard

@Composable
fun ReminderSection(viewModel: ReminderSettingsViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    var showTimeDialog by remember { mutableStateOf(false) }

    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (config.enabled) {
                        stringResource(R.string.settings_reminder_daily_at, "%02d:%02d".format(config.hour, config.minute))
                    } else {
                        stringResource(R.string.settings_reminder_disabled)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = config.enabled, onCheckedChange = viewModel::setEnabled)
        }
        if (config.enabled) {
            TextButton(onClick = { showTimeDialog = true }, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.settings_reminder_change_time))
            }
        }
    }

    if (showTimeDialog) {
        ReminderTimeDialog(
            hour = config.hour,
            minute = config.minute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m ->
                viewModel.setTime(h, m)
                showTimeDialog = false
            },
        )
    }
}

@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var text by remember { mutableStateOf("%02d:%02d".format(hour, minute)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Heure du rappel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text("Heure (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parts = text.trim().split(":")
                val h = parts.getOrNull(0)?.toIntOrNull()
                val m = parts.getOrNull(1)?.toIntOrNull()
                if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                    error = "Heure invalide (ex. 08:00)."
                } else {
                    onConfirm(h, m)
                }
            }) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
