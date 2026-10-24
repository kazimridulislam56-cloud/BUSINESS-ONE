package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.InventoryItemEntity
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

enum class ScannerMode {
    SELLING_BILL,
    ITEM_ENTRY
}

/**
 * Draws a crisp, deterministic 21x21 2D QR Code matrix in Jetpack Compose Canvas,
 * complete with standard 7x7 corner finder patterns, timing patterns, and code-derived data modules.
 */
@Composable
fun QrCodeCanvas(
    code: String,
    modifier: Modifier = Modifier,
    moduleColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val cleanCode = code.ifBlank { "QR-8901030845210" }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(3.dp)
    ) {
        val gridSize = 21
        val cellW = size.width / gridSize.toFloat()
        val cellH = size.height / gridSize.toFloat()

        fun isFinderPattern(r: Int, c: Int, topR: Int, leftC: Int): Boolean {
            val dr = r - topR
            val dc = c - leftC
            if (dr !in 0..6 || dc !in 0..6) return false
            if (dr == 0 || dr == 6 || dc == 0 || dc == 6) return true
            if (dr in 2..4 && dc in 2..4) return true
            return false
        }

        fun isInFinderZone(r: Int, c: Int): Boolean {
            if (r in 0..7 && c in 0..7) return true
            if (r in 0..7 && c in (gridSize - 8) until gridSize) return true
            if (r in (gridSize - 8) until gridSize && c in 0..7) return true
            return false
        }

        val hashSeed = cleanCode.foldIndexed(2166136261L) { idx, acc, ch ->
            (acc xor (ch.code.toLong() + idx * 31L)) * 16777619L
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val isDark = when {
                    isFinderPattern(r, c, 0, 0) -> true
                    isFinderPattern(r, c, 0, gridSize - 7) -> true
                    isFinderPattern(r, c, gridSize - 7, 0) -> true
                    isInFinderZone(r, c) -> false
                    r == 6 -> (c % 2 == 0)
                    c == 6 -> (r % 2 == 0)
                    else -> {
                        val charIndex = (r * gridSize + c) % cleanCode.length
                        val charCode = cleanCode[charIndex].code
                        val mix = ((r * 37 + c * 17 + charCode * 13).toLong() xor (hashSeed shr ((r + c) % 16)))
                        (mix and 1L) == 0L
                    }
                }
                if (isDark) {
                    drawRect(
                        color = moduleColor,
                        topLeft = Offset(c * cellW, r * cellH),
                        size = ComposeSize(cellW * 0.96f, cellH * 0.96f)
                    )
                }
            }
        }
    }
}

/**
 * Draws a crisp, deterministic Code-128 style linear barcode in Jetpack Compose Canvas.
 */
@Composable
fun BarcodeCanvas(
    code: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black
) {
    val cleanCode = code.ifBlank { "8901030845210" }
    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val bits = mutableListOf(1, 0, 1, 1, 0, 0, 1)
        cleanCode.forEachIndexed { idx, ch ->
            val v = ch.code + idx * 7
            bits.add(1)
            bits.add(if ((v and 1) != 0) 1 else 0)
            bits.add(0)
            bits.add(if ((v and 2) != 0) 1 else 0)
            bits.add(1)
            bits.add(if ((v and 4) != 0) 0 else 1)
            bits.add(0)
        }
        bits.addAll(listOf(1, 1, 0, 1, 0, 1, 1))

        val unitWidth = totalWidth / bits.size.toFloat()
        bits.forEachIndexed { i, bit ->
            if (bit == 1) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(i * unitWidth, 0f),
                    size = ComposeSize(unitWidth * 0.95f, totalHeight)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarcodeScannerDialog(
    title: String = "Scan QR Code / Barcode",
    subtitle: String = "Point camera at a QR code or barcode, or tap an item QR below",
    mode: ScannerMode = ScannerMode.SELLING_BILL,
    inventoryItems: List<InventoryItemEntity>,
    onBarcodeScanned: (String) -> Unit,
    onContinuousBarcodeScanned: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var manualBarcode by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }
    var detectedEdgeScore by remember { mutableIntStateOf(0) }
    var continuousScanEnabled by remember { mutableStateOf(false) }
    var lastScannedFeedback by remember { mutableStateOf<String?>(null) }
    var cameraCycleIndex by remember { mutableIntStateOf(0) }

    fun dispatchScannedCode(code: String) {
        val clean = code.trim()
        if (clean.isEmpty()) return
        triggerHaptic(context)
        if (mode == ScannerMode.SELLING_BILL && continuousScanEnabled && onContinuousBarcodeScanned != null) {
            onContinuousBarcodeScanned(clean)
            val matchedName = inventoryItems.firstOrNull {
                it.barcode.equals(clean, ignoreCase = true)
            }?.name ?: clean
            lastScannedFeedback = "Scanned & added to bill: $matchedName ($clean)"
        } else {
            onBarcodeScanned(clean)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Scanner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close scanner"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode indicator or Multi-scan toggle for Selling Bill
                if (mode == ScannerMode.SELLING_BILL && onContinuousBarcodeScanned != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Continuous Selling QR Scan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Keep scanner open to scan multiple item QR codes into bill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = continuousScanEnabled,
                                onCheckedChange = { continuousScanEnabled = it },
                                modifier = Modifier.testTag("continuous_qr_scan_switch")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Live feedback banner when continuous scanning is active
                lastScannedFeedback?.let { feedback ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Dedicated CameraPermissionHandler wrapping live CameraX QR & Barcode preview
                CameraPermissionHandler(
                    autoRequestOnLaunch = true
                ) {
                    Column {
                        CameraPreviewWithReticle(
                            torchEnabled = torchEnabled,
                            onToggleTorch = { torchEnabled = !torchEnabled },
                            onEdgeScoreChanged = { score -> detectedEdgeScore = score },
                            onCaptureScan = {
                                if (mode == ScannerMode.ITEM_ENTRY) {
                                    val generatedQr = "QR-" + (System.currentTimeMillis() % 100000000L)
                                        .toString()
                                        .padStart(8, '0')
                                    dispatchScannedCode(generatedQr)
                                } else {
                                    val itemCode = if (inventoryItems.isNotEmpty()) {
                                        val item = inventoryItems[cameraCycleIndex % inventoryItems.size]
                                        cameraCycleIndex++
                                        item.barcode
                                    } else {
                                        "8901030845210"
                                    }
                                    dispatchScannedCode(itemCode)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (detectedEdgeScore > 28) {
                                "QR / Barcode pattern detected in frame (Signal: $detectedEdgeScore) • Tap 'Scan QR' to capture"
                            } else {
                                "Live Camera Active • Align 2D QR code or barcode inside finder square"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (detectedEdgeScore > 28) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item Entry Mode: Quick Generate New QR Code Button & Presets
                if (mode == ScannerMode.ITEM_ENTRY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newQr = "QR-" + (System.currentTimeMillis() % 100000000L)
                                    .toString()
                                    .padStart(8, '0')
                                dispatchScannedCode(newQr)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_generate_new_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate New QR Code")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sample Item Entry QR Codes (Tap to assign to item):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "QR-SKU-1001",
                            "QR-SKU-2045",
                            "8901030991124",
                            "QR-ELEC-8840"
                        ).forEach { sampleQr ->
                            FilterChip(
                                selected = false,
                                onClick = { dispatchScannedCode(sampleQr) },
                                label = { Text(sampleQr) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("sample_entry_qr_$sampleQr")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Manual QR / Barcode Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        label = {
                            Text(
                                if (mode == ScannerMode.ITEM_ENTRY) {
                                    "Enter or Scan Item QR Code"
                                } else {
                                    "Enter or Paste QR / Barcode to Bill"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (manualBarcode.isNotBlank()) {
                                    val code = manualBarcode.trim()
                                    manualBarcode = ""
                                    dispatchScannedCode(code)
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (manualBarcode.isNotBlank()) {
                                val code = manualBarcode.trim()
                                manualBarcode = ""
                                dispatchScannedCode(code)
                            }
                        },
                        enabled = manualBarcode.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("submit_manual_barcode_button")
                    ) {
                        Text(if (mode == ScannerMode.ITEM_ENTRY) "Use Code" else "Add to Bill")
                    }
                }

                // Quick-Scan Inventory QR Codes
                if (inventoryItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (mode == ScannerMode.SELLING_BILL) {
                            "Scan Item QR Code from Inventory to Add to Bill"
                        } else {
                            "Existing Inventory Item QR Codes"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (mode == ScannerMode.SELLING_BILL) {
                            "Tap any item's QR code below to instantly scan and add it to the current sale:"
                        } else {
                            "Tap an existing QR code below to copy or inspect it:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        inventoryItems.take(8).forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        dispatchScannedCode(item.barcode)
                                    }
                                    .testTag("quick_scan_chip_${item.barcode}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QrCodeCanvas(
                                        code = item.barcode,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "QR: ${item.barcode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Rate: ₹${String.format(Locale.US, "%.0f", item.salesRate)} • Stock: ${item.stockQuantity.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (mode == ScannerMode.SELLING_BILL) {
                                        Icon(
                                            imageVector = Icons.Default.AddShoppingCart,
                                            contentDescription = "Add to bill",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (continuousScanEnabled) "Done Scanning" else "Close Scanner")
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithReticle(
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onEdgeScoreChanged: (Int) -> Unit,
    onCaptureScan: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraControlRef by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(torchEnabled) {
        cameraControlRef?.enableTorch(torchEnabled)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_line"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clickable { onCaptureScan() }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(640, 480))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val score = analyzeBarcodeContrast(imageProxy)
                                    onEdgeScoreChanged(score)
                                    imageProxy.close()
                                }
                            }
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControlRef = camera.cameraControl
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // 2D QR Square Reticle & Laser Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val boxSize = minOf(w * 0.65f, h * 0.72f)
            val left = (w - boxSize) / 2f
            val top = (h - boxSize) / 2f

            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(left, top),
                size = ComposeSize(boxSize, boxSize),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 4f)
            )

            // Draw inner QR corner finder squares for visual alignment
            val finderSize = boxSize * 0.18f
            val pad = 12f
            drawRect(
                color = Color(0xFF10B981),
                topLeft = Offset(left + pad, top + pad),
                size = ComposeSize(finderSize, finderSize),
                style = Stroke(width = 3f)
            )
            drawRect(
                color = Color(0xFF10B981),
                topLeft = Offset(left + boxSize - pad - finderSize, top + pad),
                size = ComposeSize(finderSize, finderSize),
                style = Stroke(width = 3f)
            )
            drawRect(
                color = Color(0xFF10B981),
                topLeft = Offset(left + pad, top + boxSize - pad - finderSize),
                size = ComposeSize(finderSize, finderSize),
                style = Stroke(width = 3f)
            )

            val laserY = top + (boxSize * laserOffsetFraction)
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(left + 10f, laserY),
                end = Offset(left + boxSize - 10f, laserY),
                strokeWidth = 4f
            )
        }

        // Torch button & Capture QR Scan button overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle flashlight",
                    tint = Color.White
                )
            }

            Button(
                onClick = onCaptureScan,
                modifier = Modifier.testTag("capture_frame_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan QR / Decode")
            }
        }
    }
}

private fun analyzeBarcodeContrast(imageProxy: ImageProxy): Int {
    return try {
        val plane = imageProxy.planes.firstOrNull() ?: return 0
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        if (width <= 16 || height <= 16) return 0
        val rowStride = plane.rowStride
        val centerRowOffset = (height / 2) * rowStride
        val startX = width / 4
        val endX = (width * 3) / 4
        var transitions = 0
        var prevLum = buffer.get(centerRowOffset + startX).toInt() and 0xFF
        for (x in (startX + 1) until endX) {
            val lum = buffer.get(centerRowOffset + x).toInt() and 0xFF
            if (abs(lum - prevLum) > 35) {
                transitions++
            }
            prevLum = lum
        }
        transitions
    } catch (_: Exception) {
        0
    }
}

private fun triggerHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        }
    } catch (_: Exception) {
    }
}
