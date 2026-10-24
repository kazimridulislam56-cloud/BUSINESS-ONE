package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.data.local.SalesInvoiceEntity
import com.example.ui.ReportTab
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ProfitGreen
import com.example.util.ItemPerformanceStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    invoices: List<SalesInvoiceEntity>,
    itemPerformanceStats: List<ItemPerformanceStat>,
    currencySymbol: String,
    onOpenInvoiceReceipt: (SalesInvoiceEntity) -> Unit,
    onDeleteInvoice: (SalesInvoiceEntity) -> Unit,
    onExportPdfReport: (String, List<SalesInvoiceEntity>) -> Unit,
    onExportExcelCsv: (List<SalesInvoiceEntity>) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val activeTab = ReportTab.entries[selectedTabIndex]

    val availableDates = remember(invoices) {
        val list = invoices.map { it.dateString }.distinct().sortedDescending()
        if (list.isEmpty()) listOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) else list
    }
    val availableMonths = remember(invoices) {
        val list = invoices.map { it.monthString }.distinct().sortedDescending()
        if (list.isEmpty()) listOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())) else list
    }
    val availableYears = remember(invoices) {
        val list = invoices.map { it.yearString }.distinct().sortedDescending()
        if (list.isEmpty()) listOf(SimpleDateFormat("yyyy", Locale.US).format(Date())) else list
    }

    var selectedDate by remember(availableDates) { mutableStateOf("All Days") }
    var selectedMonth by remember(availableMonths) { mutableStateOf(availableMonths.first()) }
    var selectedYear by remember(availableYears) { mutableStateOf(availableYears.first()) }

    val filteredInvoices = remember(invoices, activeTab, selectedDate, selectedMonth, selectedYear) {
        when (activeTab) {
            ReportTab.DAILY -> {
                if (selectedDate == "All Days") invoices
                else invoices.filter { it.dateString == selectedDate }
            }
            ReportTab.MONTHLY -> invoices.filter { it.monthString == selectedMonth }
            ReportTab.ANNUAL -> invoices.filter { it.yearString == selectedYear }
            ReportTab.ITEM_PERFORMANCE -> invoices
        }
    }

    val totalRevenue = remember(filteredInvoices) { filteredInvoices.sumOf { it.grandTotalAmount } }
    val totalTaxCollected = remember(filteredInvoices) { filteredInvoices.sumOf { it.totalTaxAmount } }
    val totalCashCollected = remember(filteredInvoices) { filteredInvoices.sumOf { it.cashReceived } }
    val totalUnitsSold = remember(filteredInvoices) { filteredInvoices.sumOf { it.totalQuantity } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp
        ) {
            ReportTab.entries.forEachIndexed { idx, tab ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTabIndex == idx) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("report_tab_${tab.name}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Export Toolbar (PDF & Excel CSV)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onExportPdfReport("${activeTab.label} Sales & Inventory Report", filteredInvoices)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF")
                    }

                    OutlinedButton(
                        onClick = { onExportExcelCsv(filteredInvoices) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_excel_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Excel (CSV)")
                    }
                }
            }

            // 2. Period Selector Chips for Daily / Monthly / Annual
            if (activeTab != ReportTab.ITEM_PERFORMANCE) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (activeTab) {
                            ReportTab.DAILY -> {
                                (listOf("All Days") + availableDates).forEach { d ->
                                    FilterChip(
                                        selected = selectedDate == d,
                                        onClick = { selectedDate = d },
                                        label = { Text(d) }
                                    )
                                }
                            }
                            ReportTab.MONTHLY -> {
                                availableMonths.forEach { m ->
                                    FilterChip(
                                        selected = selectedMonth == m,
                                        onClick = { selectedMonth = m },
                                        label = { Text(m) }
                                    )
                                }
                            }
                            ReportTab.ANNUAL -> {
                                availableYears.forEach { y ->
                                    FilterChip(
                                        selected = selectedYear == y,
                                        onClick = { selectedYear = y },
                                        label = { Text("Year $y") }
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // 3. Summary Metric Cards + Visual Bar Chart
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${activeTab.label} Sales Overview (${filteredInvoices.size} Invoices)",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Gross Sales",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalRevenue)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Cash Received",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalCashCollected)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "GST Collected (CGST+SGST): $currencySymbol${String.format(Locale.US, "%,.2f", totalTaxCollected)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Units Sold: ${String.format(Locale.US, "%,.1f", totalUnitsSold)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (filteredInvoices.isNotEmpty()) {
                                SalesVelocityBarChart(
                                    invoices = filteredInvoices.take(7).reversed(),
                                    currencySymbol = currencySymbol
                                )
                            }
                        }
                    }
                }

                // 4. Filtered Invoices List
                items(filteredInvoices, key = { it.id }) { invoice ->
                    ReportInvoiceRowCard(
                        invoice = invoice,
                        currencySymbol = currencySymbol,
                        onPrintClick = { onOpenInvoiceReceipt(invoice) },
                        onDeleteClick = { onDeleteInvoice(invoice) }
                    )
                }
            } else {
                // ITEM PERFORMANCE REPORT TAB
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val totalProfit = itemPerformanceStats.sumOf { it.netProfit }
                            val totalItemRevenue = itemPerformanceStats.sumOf { it.grossRevenue }
                            Text(
                                text = "Specific Item Performance & Margin Analysis",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Taxable Item Revenue: $currencySymbol${String.format(Locale.US, "%,.2f", totalItemRevenue)} • Estimated Net Profit: $currencySymbol${String.format(Locale.US, "%,.2f", totalProfit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfitGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(itemPerformanceStats, key = { "${it.itemId}_${it.itemName}" }) { stat ->
                    ItemPerformanceRowCard(
                        stat = stat,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesVelocityBarChart(
    invoices: List<SalesInvoiceEntity>,
    currencySymbol: String
) {
    val barColor = MaterialTheme.colorScheme.primary
    val taxColor = MaterialTheme.colorScheme.secondary
    val maxVal = (invoices.maxOfOrNull { it.grandTotalAmount } ?: 1.0).coerceAtLeast(1.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        Text(
            text = "Recent Invoice Revenue & Tax Breakdown ($currencySymbol)",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val count = invoices.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.52f).coerceAtMost(46.dp.toPx())

            invoices.forEachIndexed { i, inv ->
                val ratio = (inv.grandTotalAmount / maxVal).toFloat().coerceIn(0.08f, 1f)
                val barH = size.height * ratio
                val x = i * slotW + (slotW - barW) / 2f
                val y = size.height - barH

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                if (inv.totalTaxAmount > 0.0) {
                    val taxRatio = (inv.totalTaxAmount / maxVal).toFloat().coerceIn(0.02f, ratio)
                    val taxH = size.height * taxRatio
                    drawRoundRect(
                        color = taxColor,
                        topLeft = Offset(x, size.height - taxH),
                        size = Size(barW, taxH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            invoices.forEach { inv ->
                Text(
                    text = inv.billNumber.takeLast(4),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReportInvoiceRowCard(
    invoice: SalesInvoiceEntity,
    currencySymbol: String,
    onPrintClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPrintClick() }
            .testTag("report_invoice_card_${invoice.billNumber}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${invoice.billNumber} • ${invoice.customerName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Date: ${invoice.dateString} • Mobile: ${invoice.customerMobile.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", invoice.grandTotalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (invoice.gstEnabled) {
                        "Qty: ${invoice.totalQuantity.toInt()} • CGST: $currencySymbol${String.format(Locale.US, "%.2f", invoice.cgstAmount)} • SGST: $currencySymbol${String.format(Locale.US, "%.2f", invoice.sgstAmount)} • Cash: $currencySymbol${String.format(Locale.US, "%.0f", invoice.cashReceived)}"
                    } else {
                        "Qty: ${invoice.totalQuantity.toInt()} • GST: Off • Cash Received: $currencySymbol${String.format(Locale.US, "%.2f", invoice.cashReceived)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onPrintClick) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Thermal Receipt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Invoice",
                            tint = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPerformanceRowCard(
    stat: ItemPerformanceStat,
    currencySymbol: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_perf_card_${stat.itemId}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stat.category} • HSN: ${stat.hsnCode.ifBlank { "N/A" }} • Barcode: ${stat.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", stat.unitsSold)} ${stat.unit} Sold",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Revenue: $currencySymbol${String.format(Locale.US, "%,.2f", stat.grossRevenue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "GST Collected: $currencySymbol${String.format(Locale.US, "%,.2f", stat.gstCollected)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Profit: $currencySymbol${String.format(Locale.US, "%,.2f", stat.netProfit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                    Text(
                        text = "Current Stock: ${String.format(Locale.US, "%.0f", stat.currentStock)} ${stat.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
