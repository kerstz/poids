package com.poids.ui.profile

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.poids.R
import com.poids.ui.components.PoidsCard
import com.poids.ui.components.SegmentedSelector

/** Choix de langue : Système / Français / English (per-app locale). */
@Composable
fun LanguageSection() {
    val context = LocalContext.current

    val currentTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
    } else {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
    val current = currentTag.substringBefore('-').let { if (it == "fr" || it == "en") it else "" }

    val labelSystem = stringResource(R.string.language_system)
    val labelFr = stringResource(R.string.language_fr)
    val labelEn = stringResource(R.string.language_en)

    PoidsCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        SegmentedSelector(
            options = listOf("", "fr", "en"),
            selected = current,
            onSelect = { tag -> applyLanguage(context, tag) },
            label = { tag ->
                when (tag) {
                    "fr" -> labelFr
                    "en" -> labelEn
                    else -> labelSystem
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

private fun applyLanguage(context: android.content.Context, tag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = if (tag.isEmpty()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
    } else {
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
