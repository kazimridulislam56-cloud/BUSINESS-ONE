package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BusinessAndAppSettings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.BarcodeCanvas
import com.example.util.ThermalPrinterManager
import kotlinx.coroutines.launch

@Composable
fun ThermalReceiptModal(
    invoice: SalesInvoiceEntity,
    lineItems: List<InvoiceLineItemEntity>,
    settings: BusinessAndAppSettings,
    onPaperWidthChange: (String) -> Unit,
    onExportPdfClick: () -> Unit,
    onStatusMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    val receiptLines = remember(invoice, lineItems, settings) {
        ThermalPrinterManager.formatThermalReceiptText(invoice, lineItems, settings)
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isPrinting = true
            scope.launch {
                val res = ThermalPrinterManager.printOverBluetooth(
                    context = context,
                    printerAddress = settings.selectedPrinterAddress,
                    receiptLines = receiptLines
                )
                isPrinting = false
                onStatusMessage(
                    res.getOrElse {
                        "Thermal Print Preview Ready (${settings.printerPaperWidth}): ${it.localizedMessage ?: "Connect printer in Settings"}"
                    }
                )
            }
        } else {
            onStatusMessage("Bluetooth permission is required to connect to thermal printers.")
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
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Thermal Receipt Preview",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Thermal Receipt (${settings.printerPaperWidth})",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Printer: ${settings.selectedPrinterName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_modal_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Paper Width Switcher: 58mm (2-inch) vs 80mm (3-inch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paper Roll:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FilterChip(
                        selected = settings.printerPaperWidth == "58mm",
                        onClick = { onPaperWidthChange("58mm") },
                        label = { Text("58mm (2-Inch)") },
                        modifier = Modifier.testTag("paper_58mm_chip")
                    )
                    FilterChip(
                        selected = settings.printerPaperWidth == "80mm",
                        onClick = { onPaperWidthChange("80mm") },
                        label = { Text("80mm (3-Inch)") },
                        modifier = Modifier.testTag("paper_80mm_chip")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Realistic Thermal Paper Roll Sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFDF7))
                        .border(1.dp, Color(0xFFD6D3C9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        receiptLines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = if (settings.printerPaperWidth == "80mm") 11.sp else 12.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF18181B),
                                fontWeight = if (line.contains("GRAND TOTAL") || line.contains("INVOICE")) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }

                        if (settings.printBarcodeFooter) {
                            Spacer(modifier = Modifier.height(12.dp))
                            BarcodeCanvas(
                                code = invoice.billNumber,
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(44.dp),
                                barColor = Color(0xFF18181B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = invoice.billNumber,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF18181B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Bluetooth Thermal Print + PDF Export
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdfClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                !ThermalPrinterManager.hasBluetoothPermission(context)
                            ) {
                                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                isPrinting = true
                                scope.launch {
                                    val res = ThermalPrinterManager.printOverBluetooth(
                                        context = context,
                                        printerAddress = settings.selectedPrinterAddress,
                                        receiptLines = receiptLines
                                    )
                                    isPrinting = false
                                    onStatusMessage(
                                        res.getOrElse {
                                            "ESC/POS Stream Ready (${settings.printerPaperWidth}): ${it.localizedMessage}"
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isPrinting,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("bluetooth_print_button")
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print (${settings.printerPaperWidth})")
                        }
                    }
                }
            }
        }
    }
}
