package com.poids.ui.habits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.poids.domain.model.SubstanceKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** Entrée de consommation : nom de substance, dose, unité, date/heure, note. */
data class SubstanceEntryInput(
    val name: String,
    val kind: SubstanceKind,
    val amount: Double,
    val unit: String,
    val dateTime: LocalDateTime,
    val note: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstanceEntryDialog(
    initial: SubstanceEntryInput? = null,
    recent: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (SubstanceEntryInput) -> Unit,
) {
    val now = remember { LocalDateTime.now() }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var dose by remember { mutableStateOf(initial?.amount?.let(::fmtDose) ?: "1") }
    var unit by remember { mutableStateOf(initial?.unit ?: "mg") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.dateTime?.toLocalDate() ?: now.toLocalDate()) }
    var time by remember {
        mutableStateOf("%02d:%02d".format((initial?.dateTime ?: now).hour, (initial?.dateTime ?: now).minute))
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Chips : raccourcis fixes + substances déjà saisies (autocomplete).
    val fixed = listOf("Alcool" to "verres", "Tabac" to "cig.", "Café" to "tasses")
    val chips = (fixed + recent.filter { r -> fixed.none { it.first.equals(r.first, true) } })
        .distinctBy { it.first.lowercase() }
        .take(9)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Consommation" else "Modifier la prise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { (n, u) ->
                        AssistChip(
                            onClick = { name = n; unit = u },
                            label = { Text(n) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Substance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it; error = null },
                        label = { Text("Dose") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("Date : $date", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showDatePicker = true }) { Text("Changer") }
                }
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it; error = null },
                    label = { Text("Heure (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (contexte, ressenti…)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                val amount = dose.trim().replace(',', '.').toDoubleOrNull()
                val t = parseTime(time)
                when {
                    trimmed.isBlank() -> error = "Nom de substance requis."
                    amount == null || amount < 0 -> error = "Dose invalide."
                    t == null -> error = "Heure invalide (ex. 21:30)."
                    else -> onConfirm(
                        SubstanceEntryInput(
                            name = trimmed,
                            kind = inferKind(trimmed),
                            amount = amount,
                            unit = unit.trim().ifBlank { "u." },
                            dateTime = LocalDateTime.of(date, t),
                        ),
                    )
                }
            }) { Text(if (initial == null) "Ajouter" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) { DatePicker(state = state) }
    }
}

private fun inferKind(name: String): SubstanceKind {
    val n = name.lowercase()
    return when {
        "alcool" in n || "bière" in n || "biere" in n || "vin" in n -> SubstanceKind.ALCOHOL
        "tabac" in n || "cigaret" in n || "clope" in n -> SubstanceKind.TOBACCO
        "café" in n || "cafe" in n || "caféine" in n || "cafeine" in n -> SubstanceKind.CAFFEINE
        else -> SubstanceKind.OTHER
    }
}

private fun parseTime(value: String): LocalTime? = runCatching {
    val parts = value.trim().split(":")
    LocalTime.of(parts[0].toInt(), parts[1].toInt())
}.getOrNull()

private fun fmtDose(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
