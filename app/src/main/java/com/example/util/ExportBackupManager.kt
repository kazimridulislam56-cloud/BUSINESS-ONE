package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BusinessAndAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ItemPerformanceStat(
    val itemId: Long,
    val itemName: String,
    val barcode: String,
    val category: String,
    val hsnCode: String,
    val unit: String,
    val unitsSold: Double,
    val grossRevenue: Double,
    val gstCollected: Double,
    val estimatedCost: Double,
    val netProfit: Double,
    val currentStock: Double
)

object ExportBackupManager {

    /**
     * Generates a professional PDF Report containing Business Header, KPI Summary,
     * Sales Invoices Table, and Item Performance Breakdown, written to the target Uri.
     */
    suspend fun writePdfReportToUri(
        context: Context,
        uri: Uri,
        reportTitle: String,
        settings: BusinessAndAppSettings,
        invoices: List<SalesInvoiceEntity>,
        itemStats: List<ItemPerformanceStat>,
        inventoryItems: List<InventoryItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 61, 62)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val subHeaderPaint = Paint().apply {
                color = Color.rgb(64, 81, 82)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val sectionPaint = Paint().apply {
                color = Color.rgb(15, 61, 62)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val tableHeaderPaint = Paint().apply {
                color = Color.rgb(255, 255, 255)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(20, 30, 30)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                isAntiAlias = true
            }
            val headerBgPaint = Paint().apply {
                color = Color.rgb(15, 61, 62)
                style = Paint.Style.FILL
            }
            val rowAltPaint = Paint().apply {
                color = Color.rgb(242, 247, 246)
                style = Paint.Style.FILL
            }

            var y = 45f
            val margin = 36f
            val cur = settings.currencySymbol

            fun startNewPageIfNeeded(requiredHeight: Float) {
                if (y + requiredHeight > pageHeight - 45f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 45f
                }
            }

            // Header Block
            canvas.drawText(settings.businessName, margin, y, titlePaint)
            y += 16f
            canvas.drawText("${settings.businessCategory} | GSTIN: ${settings.gstinNumber}", margin, y, subHeaderPaint)
            y += 14f
            canvas.drawText("${settings.businessAddress} | Tel: ${settings.businessPhone}", margin, y, subHeaderPaint)
            y += 22f

            val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
            canvas.drawText("$reportTitle  (Generated: $nowStr)", margin, y, sectionPaint)
            y += 18f

            // KPI Summary
            val totalSales = invoices.sumOf { it.grandTotalAmount }
            val totalTax = invoices.sumOf { it.totalTaxAmount }
            val totalCashReceived = invoices.sumOf { it.cashReceived }
            val totalStockValue = inventoryItems.sumOf { it.stockValueAtPurchase }

            canvas.drawRect(margin, y, pageWidth - margin, y + 48f, rowAltPaint)
            canvas.drawText(
                "Bills: ${invoices.size}   |   Gross Sales: $cur${String.format(Locale.US, "%.2f", totalSales)}   |   Total GST: $cur${String.format(Locale.US, "%.2f", totalTax)}",
                margin + 10f,
                y + 18f,
                bodyPaint
            )
            canvas.drawText(
                "Cash Received: $cur${String.format(Locale.US, "%.2f", totalCashReceived)}   |   Inventory Stock Value: $cur${String.format(Locale.US, "%.2f", totalStockValue)}",
                margin + 10f,
                y + 36f,
                bodyPaint
            )
            y += 68f

            // Section 1: Sales Invoices
            canvas.drawText("1. Sales Invoices Ledger (${invoices.size} Bills)", margin, y, sectionPaint)
            y += 10f
            canvas.drawRect(margin, y, pageWidth - margin, y + 20f, headerBgPaint)
            canvas.drawText("BILL NO        DATE         CUSTOMER           QTY    GST         TOTAL       CASH RCVD", margin + 8f, y + 14f, tableHeaderPaint)
            y += 22f

            invoices.forEachIndexed { idx, inv ->
                startNewPageIfNeeded(20f)
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, y - 2f, pageWidth - margin, y + 15f, rowAltPaint)
                }
                val rowStr = String.format(
                    Locale.US,
                    "%-14s %-12s %-18s %5.1f  %9.2f  %10.2f  %10.2f",
                    inv.billNumber.take(13),
                    inv.dateString,
                    inv.customerName.ifBlank { "Walk-in" }.take(17),
                    inv.totalQuantity,
                    inv.totalTaxAmount,
                    inv.grandTotalAmount,
                    inv.cashReceived
                )
                canvas.drawText(rowStr, margin + 8f, y + 11f, bodyPaint)
                y += 17f
            }

            y += 20f
            startNewPageIfNeeded(60f)

            // Section 2: Item Performance Report
            canvas.drawText("2. Item Performance & Profitability Report", margin, y, sectionPaint)
            y += 10f
            canvas.drawRect(margin, y, pageWidth - margin, y + 20f, headerBgPaint)
            canvas.drawText("ITEM NAME              HSN     SOLD QTY   REVENUE      PROFIT       STOCK LEFT", margin + 8f, y + 14f, tableHeaderPaint)
            y += 22f

            itemStats.forEachIndexed { idx, stat ->
                startNewPageIfNeeded(20f)
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, y - 2f, pageWidth - margin, y + 15f, rowAltPaint)
                }
                val rowStr = String.format(
                    Locale.US,
                    "%-22s %-7s %8.1f  %10.2f  %10.2f  %10.1f %s",
                    stat.itemName.take(21),
                    stat.hsnCode.take(6),
                    stat.unitsSold,
                    stat.grossRevenue,
                    stat.netProfit,
                    stat.currentStock,
                    stat.unit
                )
                canvas.drawText(rowStr, margin + 8f, y + 11f, bodyPaint)
                y += 17f
            }

            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            Result.success("PDF report saved successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates an Excel-compatible UTF-8 CSV workbook containing Sales Invoices,
     * Item Performance, and Full Inventory Stock Status.
     */
    suspend fun writeExcelCsvToUri(
        context: Context,
        uri: Uri,
        invoices: List<SalesInvoiceEntity>,
        itemStats: List<ItemPerformanceStat>,
        inventoryItems: List<InventoryItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val sb = StringBuilder()
            // UTF-8 BOM so Microsoft Excel opens currency symbols & columns cleanly
            sb.append("\uFEFF")

            fun esc(value: String): String {
                val cleaned = value.replace("\"", "\"\"")
                return "\"$cleaned\""
            }

            sb.appendLine("SECTION 1: SALES INVOICES LEDGER")
            sb.appendLine("Bill Number,Date,Month,Customer Name,Mobile Number,Total Qty,Taxable Subtotal,GST Enabled,GST Rate (%),CGST,SGST,Discount,Grand Total,Cash Received,Balance/Change,Payment Mode")
            invoices.forEach { inv ->
                sb.appendLine(
                    listOf(
                        esc(inv.billNumber),
                        esc(inv.dateString),
                        esc(inv.monthString),
                        esc(inv.customerName),
                        esc(inv.customerMobile),
                        inv.totalQuantity,
                        inv.subtotalAmount,
                        inv.gstEnabled,
                        inv.gstRateApplied,
                        inv.cgstAmount,
                        inv.sgstAmount,
                        inv.discountAmount,
                        inv.grandTotalAmount,
                        inv.cashReceived,
                        inv.balanceOrChange,
                        esc(inv.paymentMode)
                    ).joinToString(",")
                )
            }

            sb.appendLine()
            sb.appendLine("SECTION 2: ITEM PERFORMANCE REPORT")
            sb.appendLine("Item Name,Barcode,Category,HSN Code,Unit,Units Sold,Gross Revenue,GST Collected,Estimated Cost,Net Profit,Current Stock")
            itemStats.forEach { st ->
                sb.appendLine(
                    listOf(
                        esc(st.itemName),
                        esc(st.barcode),
                        esc(st.category),
                        esc(st.hsnCode),
                        esc(st.unit),
                        st.unitsSold,
                        st.grossRevenue,
                        st.gstCollected,
                        st.estimatedCost,
                        st.netProfit,
                        st.currentStock
                    ).joinToString(",")
                )
            }

            sb.appendLine()
            sb.appendLine("SECTION 3: CURRENT INVENTORY MASTER")
            sb.appendLine("ID,Item Name,Barcode/QR,Category,Unit,HSN Code,Purchase Rate,Sales Rate,GST Slab (%),Stock Quantity,Stock Value (Purchase),Stock Value (Sales)")
            inventoryItems.forEach { item ->
                sb.appendLine(
                    listOf(
                        item.id,
                        esc(item.name),
                        esc(item.barcode),
                        esc(item.category),
                        esc(item.unit),
                        esc(item.hsnCode),
                        item.purchaseRate,
                        item.salesRate,
                        item.gstRate,
                        item.stockQuantity,
                        item.stockValueAtPurchase,
                        item.stockValueAtSales
                    ).joinToString(",")
                )
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            Result.success("Excel (CSV) data exported successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
