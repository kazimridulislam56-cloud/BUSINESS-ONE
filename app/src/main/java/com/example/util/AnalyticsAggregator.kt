package com.example.util

import com.example.data.local.InventoryItemEntity
import com.example.data.local.SalesInvoiceEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DailySalesPoint(
    val dateKey: String,      // yyyy-MM-dd
    val dayLabel: String,     // e.g. "Mon", "Tue" or "23 Oct"
    val fullDateLabel: String,// e.g. "23 Oct 2026"
    val salesAmount: Double,
    val cashReceived: Double,
    val gstAmount: Double,
    val invoiceCount: Int,
    val totalQuantitySold: Double
)

data class MonthlySalesPoint(
    val monthKey: String,     // yyyy-MM
    val monthLabel: String,   // e.g. "Oct '26"
    val salesAmount: Double,
    val cashReceived: Double,
    val gstAmount: Double,
    val invoiceCount: Int,
    val totalQuantitySold: Double
)

data class CategoryStockPoint(
    val category: String,
    val totalQuantity: Double,
    val stockValuePurchase: Double,
    val stockValueRetail: Double,
    val shareFraction: Float
)

/**
 * Pure domain aggregation logic for the Analytics Dashboard KPIs and Daily/Monthly chart series.
 */
object AnalyticsAggregator {

    fun aggregateDailySalesSeries(
        invoices: List<SalesInvoiceEntity>,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        daysCount: Int = 7
    ): List<DailySalesPoint> {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortFmt = SimpleDateFormat("dd MMM", Locale.US)
        val fullFmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val invoicesByDate = invoices.groupBy { it.dateString }

        val result = mutableListOf<DailySalesPoint>()
        for (offset in (daysCount - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = referenceTimeMillis
                add(Calendar.DAY_OF_MONTH, -offset)
            }
            val date = cal.time
            val key = keyFmt.format(date)
            val dayInvoices = invoicesByDate[key].orEmpty()

            result.add(
                DailySalesPoint(
                    dateKey = key,
                    dayLabel = shortFmt.format(date),
                    fullDateLabel = fullFmt.format(date),
                    salesAmount = roundTwoDecimals(dayInvoices.sumOf { it.grandTotalAmount }),
                    cashReceived = roundTwoDecimals(dayInvoices.sumOf { it.cashReceived }),
                    gstAmount = roundTwoDecimals(dayInvoices.sumOf { it.totalTaxAmount }),
                    invoiceCount = dayInvoices.size,
                    totalQuantitySold = roundTwoDecimals(dayInvoices.sumOf { it.totalQuantity })
                )
            )
        }
        return result
    }

    fun aggregateMonthlySalesSeries(
        invoices: List<SalesInvoiceEntity>,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        monthsCount: Int = 6
    ): List<MonthlySalesPoint> {
        val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val labelFmt = SimpleDateFormat("MMM ''yy", Locale.US)
        val invoicesByMonth = invoices.groupBy { it.monthString }

        val result = mutableListOf<MonthlySalesPoint>()
        for (offset in (monthsCount - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = referenceTimeMillis
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -offset)
            }
            val date = cal.time
            val key = keyFmt.format(date)
            val monthInvoices = invoicesByMonth[key].orEmpty()

            result.add(
                MonthlySalesPoint(
                    monthKey = key,
                    monthLabel = labelFmt.format(date),
                    salesAmount = roundTwoDecimals(monthInvoices.sumOf { it.grandTotalAmount }),
                    cashReceived = roundTwoDecimals(monthInvoices.sumOf { it.cashReceived }),
                    gstAmount = roundTwoDecimals(monthInvoices.sumOf { it.totalTaxAmount }),
                    invoiceCount = monthInvoices.size,
                    totalQuantitySold = roundTwoDecimals(monthInvoices.sumOf { it.totalQuantity })
                )
            )
        }
        return result
    }

    fun aggregateCategoryStockBreakdown(
        inventoryItems: List<InventoryItemEntity>
    ): List<CategoryStockPoint> {
        val totalPurchaseValue = inventoryItems.sumOf { it.stockValueAtPurchase }.coerceAtLeast(1.0)
        return inventoryItems
            .groupBy { it.category.ifBlank { "General" } }
            .map { (category, items) ->
                val catPurchaseVal = roundTwoDecimals(items.sumOf { it.stockValueAtPurchase })
                val catRetailVal = roundTwoDecimals(items.sumOf { it.stockValueAtSales })
                val catQty = roundTwoDecimals(items.sumOf { it.stockQuantity })
                CategoryStockPoint(
                    category = category,
                    totalQuantity = catQty,
                    stockValuePurchase = catPurchaseVal,
                    stockValueRetail = catRetailVal,
                    shareFraction = (catPurchaseVal / totalPurchaseValue).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.stockValuePurchase }
    }

    fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
