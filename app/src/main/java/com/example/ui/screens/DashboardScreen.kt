package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BusinessAndAppSettings
import com.example.ui.AppScreen
import com.example.ui.DashboardMetrics
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.CategoryStockPoint
import com.example.util.DailySalesPoint
import com.example.util.MonthlySalesPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    settings: BusinessAndAppSettings,
    recentInvoices: List<SalesInvoiceEntity>,
    onNavigate: (AppScreen) -> Unit,
    onOpenScannerForSale: () -> Unit,
    onOpenQuickAccessMenu: () -> Unit,
    onInvoiceClick: (SalesInvoiceEntity) -> Unit,
    onUpdateOpeningCash: (Double) -> Unit
) {
    var showEditOpeningCashDialog by remember { mutableStateOf(false) }
    var selectedChartMode by remember { mutableIntStateOf(0) } // 0 = Daily (7D Area/Line), 1 = Monthly (6M Grouped Bar)
    val cur = settings.currencySymbol

    if (showEditOpeningCashDialog) {
        var cashInput by remember { mutableStateOf(settings.openingCashInHand.toString()) }
        AlertDialog(
            onDismissRequest = { showEditOpeningCashDialog = false },
            title = { Text("Adjust Opening Cash in Hand") },
            text = {
                Column {
                    Text(
                        text = "Total Cash in Hand = Opening Cash + Total Cash Received from Sales ($cur${String.format(Locale.US, "%.2f", metrics.totalCashReceivedFromSales)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { cashInput = it },
                        label = { Text("Opening Cash Balance ($cur)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("opening_cash_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cashInput.toDoubleOrNull()?.let { onUpdateOpeningCash(it) }
                        showEditOpeningCashDialog = false
                    },
                    modifier = Modifier.testTag("save_opening_cash_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditOpeningCashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Shop Banner & Business Profile Hero Card
        item {
            BusinessHeroBannerCard(
                settings = settings,
                onEditProfileClick = { onNavigate(AppScreen.SETTINGS) },
                onQuickMenuClick = onOpenQuickAccessMenu
            )
        }

        // 2. Quick Action Strip
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onNavigate(AppScreen.BILLING) },
                    label = { Text("New Sale Invoice") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("dashboard_new_sale_chip")
                )

                AssistChip(
                    onClick = onOpenScannerForSale,
                    label = { Text("Scan Barcode / QR") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("dashboard_scan_qr_chip")
                )

                AssistChip(
                    onClick = { onNavigate(AppScreen.INVENTORY) },
                    label = { Text("Add / Manage Stock") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("dashboard_inventory_chip")
                )

                AssistChip(
                    onClick = { onNavigate(AppScreen.REPORTS) },
                    label = { Text("Sales & Item Reports") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("dashboard_reports_chip")
                )
            }
        }

        // 3. Analytics Dashboard Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analytics Dashboard",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Aggregated KPIs: Stock Value, Cash in Hand & Sales Velocity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (settings.gstEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (settings.gstEnabled) "GST ON (${settings.defaultGstSlab}%)" else "GST OFF",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (settings.gstEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // 4. Primary Hero KPI Row: Today's Sales + Overall Monthly Sales
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsKpiCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_todays_sales"),
                    title = "Today's Sales",
                    value = "$cur${String.format(Locale.US, "%,.2f", metrics.todaysSales)}",
                    subtitle = "${metrics.todaysBillsCount} bills • GST: $cur${String.format(Locale.US, "%.0f", metrics.todaysGstCollected)}",
                    icon = Icons.Default.PointOfSale,
                    accentColor = ProfitGreen,
                    onClick = { onNavigate(AppScreen.REPORTS) }
                )

                AnalyticsKpiCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_monthly_sales"),
                    title = "Monthly Sales",
                    value = "$cur${String.format(Locale.US, "%,.2f", metrics.monthlySales)}",
                    subtitle = "${metrics.monthlyBillsCount} bills • Annual: $cur${String.format(Locale.US, "%,.0f", metrics.annualSales)}",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(AppScreen.REPORTS) }
                )
            }
        }

        // 5. Secondary KPI Row: Total Current Quantity + Total Stock Value
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsKpiCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_total_quantity"),
                    title = "Total Current Qty",
                    value = String.format(Locale.US, "%,.0f Units", metrics.totalCurrentQuantity),
                    subtitle = "${metrics.lowStockItems.size} low-stock alerts",
                    icon = Icons.Default.Inventory2,
                    accentColor = WarningAmber,
                    onClick = { onNavigate(AppScreen.INVENTORY) }
                )

                AnalyticsKpiCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_stock_value"),
                    title = "Total Stock Value",
                    value = "$cur${String.format(Locale.US, "%,.2f", metrics.totalStockValuePurchase)}",
                    subtitle = "Retail Value: $cur${String.format(Locale.US, "%,.0f", metrics.totalStockValueRetail)}",
                    icon = Icons.Default.TrendingUp,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { onNavigate(AppScreen.INVENTORY) }
                )
            }
        }

        // 6. Cash in Hand Wide Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kpi_cash_in_hand"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Cash in Hand",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Total Cash in Hand",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "$cur${String.format(Locale.US, "%,.2f", metrics.cashInHand)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Opening: $cur${String.format(Locale.US, "%,.0f", settings.openingCashInHand)} + Sales Cash: $cur${String.format(Locale.US, "%,.2f", metrics.totalCashReceivedFromSales)} (Today: $cur${String.format(Locale.US, "%,.0f", metrics.todaysCashReceived)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showEditOpeningCashDialog = true },
                        modifier = Modifier.testTag("edit_opening_cash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit opening cash balance",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 7. Interactive Daily & Monthly Sales Chart Card (Recharts-Style Area & Grouped Bar Charts)
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_sales_chart_card"),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedChartMode == 0) {
                                    "Daily Sales Trend (Last 7 Days)"
                                } else {
                                    "Monthly Sales & Cash Trend (6 Months)"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Tap any data point or bar to inspect exact revenue, cash & GST",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedChartMode == 0,
                            onClick = { selectedChartMode = 0 },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Daily Sales (7D)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chart_toggle_daily")
                        )
                        FilterChip(
                            selected = selectedChartMode == 1,
                            onClick = { selectedChartMode = 1 },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Monthly Sales (6M)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chart_toggle_monthly")
                        )
                    }

                    if (selectedChartMode == 0) {
                        DailySalesInteractiveAreaChart(
                            series = metrics.dailySalesSeries,
                            currencySymbol = cur
                        )
                    } else {
                        MonthlySalesInteractiveBarChart(
                            series = metrics.monthlySalesSeries,
                            currencySymbol = cur
                        )
                    }
                }
            }
        }

        // 8. Category Stock Value & Quantity Breakdown Card
        if (metrics.categoryStockBreakdown.isNotEmpty()) {
            item {
                CategoryStockValuationCard(
                    categories = metrics.categoryStockBreakdown,
                    totalStockValuePurchase = metrics.totalStockValuePurchase,
                    totalStockValueRetail = metrics.totalStockValueRetail,
                    potentialMargin = metrics.potentialStockMargin,
                    currencySymbol = cur
                )
            }
        }

        // 9. Low Stock Alert Banner (if any)
        if (metrics.lowStockItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppScreen.INVENTORY) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DangerRed.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Low Stock Warning",
                            tint = DangerRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Stock Alert (${metrics.lowStockItems.size} Items)",
                                style = MaterialTheme.typography.titleSmall,
                                color = DangerRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = metrics.lowStockItems.joinToString(", ") {
                                    "${it.name} (${it.stockQuantity.toInt()} ${it.unit})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 10. Recent Sales Invoices Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Sales Invoices",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = { onNavigate(AppScreen.REPORTS) }) {
                    Text("View All (${recentInvoices.size})")
                }
            }
        }

        // 11. Recent Invoices List
        items(recentInvoices.take(6), key = { it.id }) { invoice ->
            RecentInvoiceCard(
                invoice = invoice,
                currencySymbol = cur,
                onClick = { onInvoiceClick(invoice) }
            )
        }
    }
}

/**
 * Recharts-inspired interactive Area + Cubic Line Chart for Daily Sales & Cash Received.
 */
@Composable
private fun DailySalesInteractiveAreaChart(
    series: List<DailySalesPoint>,
    currencySymbol: String
) {
    if (series.isEmpty()) return
    var selectedIndex by remember(series) { mutableIntStateOf(series.lastIndex) }
    val selectedPoint = series.getOrElse(selectedIndex) { series.last() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val maxSales = (series.maxOfOrNull { maxOf(it.salesAmount, it.cashReceived) } ?: 1.0).coerceAtLeast(100.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Active Tooltip Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_chart_tooltip"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${selectedPoint.fullDateLabel} (${selectedPoint.invoiceCount} Bills)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Qty Sold: ${String.format(Locale.US, "%.1f", selectedPoint.totalQuantitySold)} • GST: $currencySymbol${String.format(Locale.US, "%.2f", selectedPoint.gstAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sales: $currencySymbol${String.format(Locale.US, "%,.2f", selectedPoint.salesAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = "Cash: $currencySymbol${String.format(Locale.US, "%,.2f", selectedPoint.cashReceived)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor
                    )
                }
            }
        }

        // Canvas Area & Line Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .pointerInput(series) {
                    detectTapGestures { offset ->
                        val count = series.size
                        if (count > 0 && size.width > 0) {
                            val step = size.width.toFloat() / count
                            val idx = (offset.x / step).toInt().coerceIn(0, count - 1)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val topPad = 14.dp.toPx()
            val bottomPad = 14.dp.toPx()
            val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val count = series.size
            val slotW = w / count

            // Horizontal CartesianGrid lines
            for (g in 0..3) {
                val gy = topPad + (chartH * g / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val salesOffsets = series.mapIndexed { idx, pt ->
                val x = idx * slotW + slotW / 2f
                val norm = (pt.salesAmount / maxSales).toFloat().coerceIn(0f, 1f)
                val y = topPad + chartH * (1f - norm)
                Offset(x, y)
            }

            // Area fill path under sales line
            if (salesOffsets.size >= 2) {
                val areaPath = Path().apply {
                    moveTo(salesOffsets.first().x, topPad + chartH)
                    salesOffsets.forEach { lineTo(it.x, it.y) }
                    lineTo(salesOffsets.last().x, topPad + chartH)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = topPad,
                        endY = topPad + chartH
                    )
                )

                val strokePath = Path().apply {
                    moveTo(salesOffsets.first().x, salesOffsets.first().y)
                    for (i in 1 until salesOffsets.size) {
                        lineTo(salesOffsets[i].x, salesOffsets[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Selected vertical cursor line & data dots
            salesOffsets.forEachIndexed { idx, ptOffset ->
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.55f),
                        start = Offset(ptOffset.x, topPad),
                        end = Offset(ptOffset.x, topPad + chartH),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                drawCircle(
                    color = if (isSelected) secondaryColor else primaryColor,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = ptOffset
                )
            }
        }

        // X-Axis Day Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            series.forEachIndexed { idx, pt ->
                Text(
                    text = pt.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == selectedIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.clickable { selectedIndex = idx }
                )
            }
        }
    }
}

/**
 * Recharts-inspired interactive Grouped Bar Chart for 6-Month Sales vs Cash Received & GST.
 */
@Composable
private fun MonthlySalesInteractiveBarChart(
    series: List<MonthlySalesPoint>,
    currencySymbol: String
) {
    if (series.isEmpty()) return
    var selectedIndex by remember(series) { mutableIntStateOf(series.lastIndex) }
    val selectedMonth = series.getOrElse(selectedIndex) { series.last() }

    val salesBarColor = MaterialTheme.colorScheme.primary
    val cashBarColor = ProfitGreen
    val gstBarColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val maxVal = (series.maxOfOrNull { maxOf(it.salesAmount, it.cashReceived) } ?: 1.0).coerceAtLeast(100.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Tooltip Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_chart_tooltip"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${selectedMonth.monthLabel} (${selectedMonth.invoiceCount} Invoices)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Units Sold: ${String.format(Locale.US, "%.1f", selectedMonth.totalQuantitySold)} • GST: $currencySymbol${String.format(Locale.US, "%,.2f", selectedMonth.gstAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sales: $currencySymbol${String.format(Locale.US, "%,.2f", selectedMonth.salesAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = salesBarColor
                    )
                    Text(
                        text = "Cash: $currencySymbol${String.format(Locale.US, "%,.2f", selectedMonth.cashReceived)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cashBarColor
                    )
                }
            }
        }

        // Grouped Bar Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .pointerInput(series) {
                    detectTapGestures { offset ->
                        val count = series.size
                        if (count > 0 && size.width > 0) {
                            val step = size.width.toFloat() / count
                            val idx = (offset.x / step).toInt().coerceIn(0, count - 1)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val topPad = 12.dp.toPx()
            val bottomPad = 10.dp.toPx()
            val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val count = series.size
            val slotW = w / count
            val singleBarW = (slotW * 0.28f).coerceAtMost(22.dp.toPx())

            for (g in 0..3) {
                val gy = topPad + (chartH * g / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            series.forEachIndexed { idx, pt ->
                val centerX = idx * slotW + slotW / 2f
                val salesRatio = (pt.salesAmount / maxVal).toFloat().coerceIn(0.04f, 1f)
                val cashRatio = (pt.cashReceived / maxVal).toFloat().coerceIn(0.04f, 1f)

                val salesH = chartH * salesRatio
                val cashH = chartH * cashRatio

                // Highlight selected slot background
                if (idx == selectedIndex) {
                    drawRoundRect(
                        color = salesBarColor.copy(alpha = 0.08f),
                        topLeft = Offset(idx * slotW + 4.dp.toPx(), topPad),
                        size = Size(slotW - 8.dp.toPx(), chartH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }

                // Bar 1: Gross Sales
                drawRoundRect(
                    color = salesBarColor,
                    topLeft = Offset(centerX - singleBarW - 2.dp.toPx(), topPad + chartH - salesH),
                    size = Size(singleBarW, salesH),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Bar 2: Cash Received
                drawRoundRect(
                    color = cashBarColor,
                    topLeft = Offset(centerX + 2.dp.toPx(), topPad + chartH - cashH),
                    size = Size(singleBarW, cashH),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // GST Cap Overlay on Sales Bar
                if (pt.gstAmount > 0.0) {
                    val gstRatio = (pt.gstAmount / maxVal).toFloat().coerceIn(0.02f, salesRatio)
                    val gstH = chartH * gstRatio
                    drawRoundRect(
                        color = gstBarColor,
                        topLeft = Offset(centerX - singleBarW - 2.dp.toPx(), topPad + chartH - gstH),
                        size = Size(singleBarW, gstH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        // X-Axis Month Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            series.forEachIndexed { idx, pt ->
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == selectedIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.clickable { selectedIndex = idx }
                )
            }
        }
    }
}

@Composable
private fun CategoryStockValuationCard(
    categories: List<CategoryStockPoint>,
    totalStockValuePurchase: Double,
    totalStockValueRetail: Double,
    potentialMargin: Double,
    currencySymbol: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_stock_valuation_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Stock Valuation by Category",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Purchase Valuation: $currencySymbol${String.format(Locale.US, "%,.0f", totalStockValuePurchase)} • Est. Margin: +$currencySymbol${String.format(Locale.US, "%,.0f", potentialMargin)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfitGreen
                    )
                }
            }

            HorizontalDivider()

            categories.take(5).forEach { cat ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${cat.category} (${cat.totalQuantity.toInt()} units)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", cat.stockValuePurchase)} (${(cat.shareFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { cat.shareFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessHeroBannerCard(
    settings: BusinessAndAppSettings,
    onEditProfileClick: () -> Unit,
    onQuickMenuClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            if (settings.bannerUri.isNotBlank()) {
                AsyncImage(
                    model = Uri.parse(settings.bannerUri),
                    contentDescription = "Shop Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_shop_banner),
                    contentDescription = "Shop Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6071F22),
                                Color(0xB30F3D3E),
                                Color(0x4D0F3D3E)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onEditProfileClick() },
                            color = Color.White
                        ) {
                            if (settings.logoUri.isNotBlank()) {
                                AsyncImage(
                                    model = Uri.parse(settings.logoUri),
                                    contentDescription = "Business Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_business_logo),
                                    contentDescription = "Business Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = settings.businessName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = settings.businessCategory,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (settings.gstinNumber.isNotBlank()) {
                                Text(
                                    text = "GSTIN: ${settings.gstinNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.16f), CircleShape)
                            .testTag("edit_business_profile_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = "Edit Business Profile",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.42f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thermal: ${settings.printerPaperWidth} • ${settings.selectedPrinterName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = onQuickMenuClick,
                        modifier = Modifier.testTag("hero_quick_menu_button")
                    ) {
                        Text(
                            text = "Quick Menu →",
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentInvoiceCard(
    invoice: SalesInvoiceEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val timeStr = remember(invoice.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(invoice.timestamp))
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_invoice_${invoice.billNumber}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${invoice.billNumber} • ${invoice.customerName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$timeStr • Qty: ${invoice.totalQuantity.toInt()} • ${invoice.paymentMode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", invoice.grandTotalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Print / View →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
