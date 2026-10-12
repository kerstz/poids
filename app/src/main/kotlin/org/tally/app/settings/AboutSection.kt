// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tally.app.R
import org.tally.coreui.theme.TallyTheme

private const val PROJECT_URL = "https://github.com/kerstz/poids"

@Serializable
private data class LicenseEntry(val group: String, val name: String, val license: String)

private fun loadLicenses(context: Context): List<LicenseEntry> = runCatching {
    val raw = context.assets.open("licenses.json").bufferedReader().use { it.readText() }
    Json.decodeFromString<List<LicenseEntry>>(raw)
}.getOrDefault(emptyList())

/** « À propos » : version, licence, dépôt, sources de données et bibliothèques (assets/licenses.json). */
@Composable
internal fun AboutSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val libraries = remember { loadLicenses(context) }
    var showLibraries by remember { mutableStateOf(false) }

    Text(
        stringResource(R.string.about_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = TallyTheme.spacing.xl),
    )
    Text(
        stringResource(R.string.about_version, version),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = TallyTheme.spacing.s),
    )
    Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = { uriHandler.openUri(PROJECT_URL) }) {
        Text(stringResource(R.string.about_source_code))
    }
    Text(stringResource(R.string.about_data_ciqual), style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = { showLibraries = !showLibraries }) {
        Text(stringResource(if (showLibraries) R.string.about_hide_libraries else R.string.about_show_libraries))
    }
    if (showLibraries) {
        Column {
            libraries.forEach { entry ->
                Text(
                    "${entry.name} — ${entry.license}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = TallyTheme.spacing.xs),
                )
            }
        }
    }
}
