package com.example.util

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class CameraPermissionStatus {
    GRANTED,
    SHOULD_SHOW_RATIONALE,
    DENIED
}

/**
 * Reusable Jetpack Compose Camera Permission Handler for Barcode & QR Scanning.
 *
 * - Automatically checks `Manifest.permission.CAMERA` on launch and whenever the Activity resumes
 *   (e.g., returning from Android System Settings).
 * - Distinguishes between initial request, educational rationale (`shouldShowRequestPermissionRationale`),
 *   and permanently denied states.
 * - Provides direct actions to request runtime permission or open App Details Settings.
 */
@Composable
fun CameraPermissionHandler(
    autoRequestOnLaunch: Boolean = true,
    onPermissionResult: (Boolean) -> Unit = {},
    grantedContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var permissionStatus by remember {
        mutableStateOf(evaluateCameraPermissionStatus(context))
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRequestedOnce = true
        permissionStatus = if (isGranted) {
            CameraPermissionStatus.GRANTED
        } else {
            evaluateCameraPermissionStatus(context)
        }
        onPermissionResult(isGranted)
    }

    // Re-evaluate camera permission when returning from system settings or resuming lifecycle
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val updated = evaluateCameraPermissionStatus(context)
                permissionStatus = updated
                onPermissionResult(updated == CameraPermissionStatus.GRANTED)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(autoRequestOnLaunch) {
        if (autoRequestOnLaunch &&
            permissionStatus != CameraPermissionStatus.GRANTED &&
            !hasRequestedOnce
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (permissionStatus == CameraPermissionStatus.GRANTED) {
        grantedContent()
    } else {
        CameraPermissionRationaleCard(
            status = permissionStatus,
            hasRequestedOnce = hasRequestedOnce,
            onRequestPermission = {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onOpenAppSettings = {
                openApplicationDetailsSettings(context)
            }
        )
    }
}

@Composable
private fun CameraPermissionRationaleCard(
    status: CameraPermissionStatus,
    hasRequestedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("camera_permission_handler_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Access Required",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp)
                )
            }

            Text(
                text = when {
                    status == CameraPermissionStatus.SHOULD_SHOW_RATIONALE ->
                        "Camera Permission Needed for Barcode/QR Scanning"
                    hasRequestedOnce ->
                        "Camera Access Blocked or Denied"
                    else ->
                        "Enable Camera for Instant Barcode/QR Scanning"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    status == CameraPermissionStatus.SHOULD_SHOW_RATIONALE ->
                        "LedgerFlow POS uses your camera exclusively to scan product barcodes and QR codes for instant billing and stock lookup."
                    hasRequestedOnce ->
                        "Camera permission was denied. You can request permission again or enable Camera access in Android App Settings."
                    else ->
                        "Grant camera access to scan 1D barcodes and 2D QR codes directly into sales invoices and inventory."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grant_camera_permission_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Allow Camera")
                }

                if (hasRequestedOnce || status == CameraPermissionStatus.SHOULD_SHOW_RATIONALE) {
                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_app_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("App Settings")
                    }
                }
            }
        }
    }
}

fun evaluateCameraPermissionStatus(context: Context): CameraPermissionStatus {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) return CameraPermissionStatus.GRANTED

    val activity = context.findActivity()
    if (activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    ) {
        return CameraPermissionStatus.SHOULD_SHOW_RATIONALE
    }

    return CameraPermissionStatus.DENIED
}

fun openApplicationDetailsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
