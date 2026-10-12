// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException

/**
 * Écran de scan : demande CAMERA au premier affichage (l'écran n'est ouvert que par un appui sur « scan »),
 * lit un EAN-8 / EAN-13 / UPC-A, renvoie le code via [onResult] une seule fois.
 */
@Composable
internal fun ScanScreen(onResult: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(hasCamera) {
        if (hasCamera && !granted && !asked) launcher.launch(Manifest.permission.CAMERA)
    }

    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    if (hasCamera && granted) {
        LaunchedEffect(owner) {
            try {
                val provider = ProcessCameraProvider.awaitInstance(context)
                val preview = Preview.Builder().build().also { p ->
                    p.setSurfaceProvider { request -> surfaceRequest = request }
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val main = ContextCompat.getMainExecutor(context)
                analysis.setAnalyzer(
                    executor,
                    BarcodeAnalyzer(ZxingCppDecoder(), ScanStabilizer()) { code -> main.execute { onResult(code) } },
                )
                provider.unbindAll()
                camera = provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                cameraError = true
            }
        }
    }

    val permanentlyDenied = !granted && asked && context.findActivity()
        ?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true

    val state = when {
        !hasCamera -> ScanUiState.NoCamera
        cameraError -> ScanUiState.CameraError
        granted -> ScanUiState.Previewing(torchAvailable = camera?.cameraInfo?.hasFlashUnit() == true, torchOn = torchOn)
        asked -> ScanUiState.PermissionDenied(permanent = permanentlyDenied)
        else -> ScanUiState.NeedsPermission
    }

    ScanContent(
        state = state,
        onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        onToggleTorch = {
            torchOn = !torchOn
            camera?.cameraControl?.enableTorch(torchOn)
        },
        onBack = onBack,
        viewfinder = {
            surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
