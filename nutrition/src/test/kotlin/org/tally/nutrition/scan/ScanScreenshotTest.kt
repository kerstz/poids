// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.tally.coreui.theme.TallyTheme
import org.tally.nutrition.add.captureVariants

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-w411dp-h914dp-xxhdpi")
class ScanScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun screenshot(name: String, state: ScanUiState) = rule.captureVariants(name) {
        ScanContent(
            state = state,
            onRequestPermission = {}, onOpenSettings = {}, onToggleTorch = {}, onBack = {},
            // Le flux caméra n'existe pas en test : placeholder neutre.
            viewfinder = { Box(Modifier.fillMaxSize().background(TallyTheme.colors.surface1)) },
        )
    }

    @Test fun needsPermission() = screenshot("scan_needs_permission", ScanUiState.NeedsPermission)
    @Test fun deniedPermanent() = screenshot("scan_denied", ScanUiState.PermissionDenied(permanent = true))
    @Test fun noCamera() = screenshot("scan_no_camera", ScanUiState.NoCamera)
    @Test fun previewWithTorch() = screenshot("scan_preview_torch", ScanUiState.Previewing(torchAvailable = true, torchOn = false))
}
