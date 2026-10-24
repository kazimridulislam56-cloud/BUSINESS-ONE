package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BusinessAndAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PairedPrinterDevice(
    val name: String,
    val address: String
)

object ThermalPrinterManager {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<PairedPrinterDevice> {
        if (!hasBluetoothPermission(context)) return emptyList()
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices?.map { device: BluetoothDevice ->
                PairedPrinterDevice(
                    name = device.name ?: "Bluetooth Printer",
                    address = device.address
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Formats a complete thermal receipt in plain text for 58mm (32 chars) or 80mm (48 chars)
     * so it can be both previewed visually in the app and transmitted over ESC/POS Bluetooth.
     */
    fun formatThermalReceiptText(
        invoice: SalesInvoiceEntity,
        lineItems: List<InvoiceLineItemEntity>,
        settings: BusinessAndAppSettings
    ): List<String> {
        val width = if (settings.printerPaperWidth == "80mm") 48 else 32
        val divider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)
        val lines = mutableListOf<String>()

        fun center(text: String): String {
            if (text.length >= width) return text.take(width)
            val pad = (width - text.length) / 2
            return " ".repeat(pad) + text
        }

        fun rowLeftRight(left: String, right: String): String {
            val maxLeft = (width - right.length - 1).coerceAtLeast(1)
            val trimmedLeft = if (left.length > maxLeft) left.take(maxLeft) else left
            val spaces = (width - trimmedLeft.length - right.length).coerceAtLeast(1)
            return trimmedLeft + " ".repeat(spaces) + right
        }

        val cur = settings.currencySymbol
        val dtFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        if (settings.printBusinessHeader) {
            lines.add(center(settings.businessName.uppercase()))
            if (settings.businessCategory.isNotBlank()) {
                lines.add(center(settings.businessCategory))
            }
            if (settings.businessAddress.isNotBlank()) {
                settings.businessAddress.chunked(width).forEach { lines.add(center(it.trim())) }
            }
            if (settings.businessPhone.isNotBlank()) {
                lines.add(center("Tel: ${settings.businessPhone}"))
            }
            if (settings.gstEnabled && settings.gstinNumber.isNotBlank()) {
                lines.add(center("GSTIN: ${settings.gstinNumber}"))
            }
            lines.add(doubleDivider)
        }

        lines.add(center(if (invoice.gstEnabled) "TAX INVOICE" else "RETAIL INVOICE"))
        lines.add(divider)
        lines.add(rowLeftRight("Bill No:", invoice.billNumber))
        lines.add(rowLeftRight("Date:", dtFormat.format(Date(invoice.timestamp))))
        lines.add(rowLeftRight("Pay Mode:", invoice.paymentMode))

        if (settings.printCustomerDetails) {
            lines.add(rowLeftRight("Customer:", invoice.customerName.ifBlank { "Walk-in" }))
            if (invoice.customerMobile.isNotBlank()) {
                lines.add(rowLeftRight("Mobile:", invoice.customerMobile))
            }
        }

        lines.add(divider)
        lines.add(rowLeftRight("ITEM (QTY x RATE)", "AMOUNT"))
        lines.add(divider)

        lineItems.forEachIndexed { index, item ->
            val hsnTag = if (settings.printHsnColumn && item.hsnCode.isNotBlank()) " [HSN:${item.hsnCode}]" else ""
            val titleLine = "${index + 1}. ${item.itemName}$hsnTag"
            titleLine.chunked(width).forEach { lines.add(it) }

            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity)
            val gstTag = if (invoice.gstEnabled && item.gstRate > 0) " (${item.gstRate}% GST)" else ""
            val grossAmt = if (item.grossAmount > 0.0) item.grossAmount else (item.quantity * item.unitPrice)
            val detailLeft = "   $qtyStr ${item.unit} x $cur${String.format(Locale.US, "%.2f", item.unitPrice)}$gstTag"
            val detailRight = "$cur${String.format(Locale.US, "%.2f", grossAmt)}"
            lines.add(rowLeftRight(detailLeft, detailRight))

            if (item.discountPercent > 0.0) {
                val discPctStr = if (item.discountPercent % 1.0 == 0.0) {
                    item.discountPercent.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", item.discountPercent)
                }
                lines.add(
                    rowLeftRight(
                        "   Disc ($discPctStr%): -$cur${String.format(Locale.US, "%.2f", item.discountAmount)}",
                        "After: $cur${String.format(Locale.US, "%.2f", item.afterDiscountPrice)}"
                    )
                )
            }
        }

        lines.add(divider)
        val totalQtyStr = if (invoice.totalQuantity % 1.0 == 0.0) {
            invoice.totalQuantity.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", invoice.totalQuantity)
        }
        lines.add(rowLeftRight("Total Items / Qty:", "${lineItems.size} / $totalQtyStr"))
        lines.add(rowLeftRight("Taxable Subtotal:", "$cur${String.format(Locale.US, "%.2f", invoice.subtotalAmount)}"))

        if (invoice.gstEnabled && settings.printGstBreakdown) {
            val halfRate = invoice.gstRateApplied / 2.0
            val halfRateStr = if (halfRate % 1.0 == 0.0) halfRate.toInt().toString() else String.format(Locale.US, "%.1f", halfRate)
            lines.add(rowLeftRight("CGST (@$halfRateStr%):", "$cur${String.format(Locale.US, "%.2f", invoice.cgstAmount)}"))
            lines.add(rowLeftRight("SGST (@$halfRateStr%):", "$cur${String.format(Locale.US, "%.2f", invoice.sgstAmount)}"))
        }

        if (invoice.discountAmount > 0.0) {
            lines.add(rowLeftRight("Discount:", "-$cur${String.format(Locale.US, "%.2f", invoice.discountAmount)}"))
        }

        lines.add(doubleDivider)
        lines.add(rowLeftRight("GRAND TOTAL:", "$cur${String.format(Locale.US, "%.2f", invoice.grandTotalAmount)}"))
        lines.add(doubleDivider)

        if (settings.printCashAndChange) {
            lines.add(rowLeftRight("Cash Received:", "$cur${String.format(Locale.US, "%.2f", invoice.cashReceived)}"))
            val change = invoice.balanceOrChange
            if (change >= 0) {
                lines.add(rowLeftRight("Change Returned:", "$cur${String.format(Locale.US, "%.2f", change)}"))
            } else {
                lines.add(rowLeftRight("Balance Due:", "$cur${String.format(Locale.US, "%.2f", -change)}"))
            }
            lines.add(divider)
        }

        if (settings.printBarcodeFooter) {
            lines.add(center("* ${invoice.billNumber} *"))
        }
        if (settings.customFooterMessage.isNotBlank()) {
            settings.customFooterMessage.chunked(width).forEach {
                lines.add(center(it.trim()))
            }
        }

        return lines
    }

    /**
     * Sends ESC/POS formatted byte stream over Bluetooth RFCOMM SPP socket to the selected thermal printer.
     */
    @SuppressLint("MissingPermission")
    suspend fun printOverBluetooth(
        context: Context,
        printerAddress: String,
        receiptLines: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission(context)) {
            return@withContext Result.failure(IllegalStateException("Bluetooth permission not granted."))
        }
        if (printerAddress.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Bluetooth thermal printer selected in Settings."))
        }
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth is not supported on this device."))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Please turn on Bluetooth to connect to the thermal printer."))
        }

        return@withContext try {
            val device = adapter.getRemoteDevice(printerAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket.connect()
            val outputStream: OutputStream = socket.outputStream

            // ESC @ (Initialize printer)
            outputStream.write(byteArrayOf(0x1B, 0x40))
            // ESC a 0 (Left align)
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))

            val charset = Charset.forName("UTF-8")
            for (line in receiptLines) {
                // Replace ₹ with Rs. for standard ASCII thermal codepages if needed
                val safeLine = line.replace("₹", "Rs.") + "\n"
                outputStream.write(safeLine.toByteArray(charset))
            }

            // Feed 4 lines & Full Cut (GS V 66 0)
            outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
            outputStream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
            outputStream.flush()
            socket.close()
            Result.success("Receipt printed successfully on ${device.name ?: printerAddress}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
