// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.tally.app.R
import org.tally.coreui.theme.TallyTheme

private const val POLICY_URL = "https://github.com/kerstz/poids/blob/master/PRIVACY.md"

/** Confidentialité : description factuelle des données et des flux (sans avis juridique). */
@Composable
fun PrivacyScreen() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(TallyTheme.spacing.gutter),
    ) {
        Text(
            stringResource(R.string.privacy_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        PrivacyBlock(R.string.privacy_local_title, R.string.privacy_local_body)
        PrivacyBlock(R.string.privacy_data_title, R.string.privacy_data_body)
        PrivacyBlock(R.string.privacy_network_title, R.string.privacy_network_body)
        PrivacyBlock(R.string.privacy_camera_title, R.string.privacy_camera_body)
        PrivacyBlock(R.string.privacy_backup_title, R.string.privacy_backup_body)
        PrivacyBlock(R.string.privacy_rights_title, R.string.privacy_rights_body)
        TextButton(
            onClick = { uriHandler.openUri(POLICY_URL) },
            modifier = Modifier.padding(top = TallyTheme.spacing.l),
        ) {
            Text(stringResource(R.string.privacy_full_policy))
        }
    }
}

@Composable
private fun PrivacyBlock(titleRes: Int, bodyRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(top = TallyTheme.spacing.xl)
            .semantics { heading() },
    )
    Text(
        stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = TallyTheme.spacing.s),
    )
}
