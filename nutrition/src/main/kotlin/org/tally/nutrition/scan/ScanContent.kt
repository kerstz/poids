package org.tally.nutrition.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.tally.coreui.component.EmptyState
import org.tally.coreui.icon.TallyIcons
import org.tally.coreui.theme.TallyTheme
import org.tally.nutrition.R

/** États de l'écran de scan (sans état Compose : testable en capture). */
internal sealed interface ScanUiState {
    data object NeedsPermission : ScanUiState
    /** [permanent] : « Ne plus demander » ; seul le réglage système peut rouvrir la demande. */
    data class PermissionDenied(val permanent: Boolean) : ScanUiState
    data object NoCamera : ScanUiState
    data class Previewing(val torchAvailable: Boolean, val torchOn: Boolean) : ScanUiState
    data object CameraError : ScanUiState
}

@Composable
internal fun ScanContent(
    state: ScanUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTorch: () -> Unit,
    onBack: () -> Unit,
    viewfinder: @Composable () -> Unit,
) {
    when (state) {
        ScanUiState.NeedsPermission -> StateMessage(
            R.string.scan_permission_title, R.string.scan_permission_body,
            R.string.scan_permission_action, onRequestPermission,
        )
        is ScanUiState.PermissionDenied -> StateMessage(
            R.string.scan_denied_title, R.string.scan_denied_body,
            if (state.permanent) R.string.scan_open_settings else R.string.scan_permission_action,
            if (state.permanent) onOpenSettings else onRequestPermission,
        )
        ScanUiState.NoCamera -> StateMessage(R.string.scan_no_camera_title, R.string.scan_no_camera_body, R.string.action_back, onBack)
        ScanUiState.CameraError -> StateMessage(R.string.scan_error_title, R.string.scan_error_body, R.string.action_back, onBack)
        is ScanUiState.Previewing -> Box(Modifier.fillMaxSize()) {
            viewfinder()
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(TallyTheme.spacing.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.scan_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TallyTheme.colors.onSurface,
                )
                if (state.torchAvailable) {
                    TextButton(onClick = onToggleTorch) {
                        Text(stringResource(if (state.torchOn) R.string.scan_torch_off else R.string.scan_torch_on))
                    }
                }
            }
        }
    }
}

@Composable
private fun StateMessage(title: Int, body: Int, action: Int, onAction: () -> Unit) {
    EmptyState(
        icon = painterResource(TallyIcons.scan),
        title = stringResource(title),
        body = stringResource(body),
        actionLabel = stringResource(action),
        onAction = onAction,
    )
}
