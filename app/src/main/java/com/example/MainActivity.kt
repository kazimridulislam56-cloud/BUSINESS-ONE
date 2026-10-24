package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SalesInvoiceEntity
import com.example.ui.AppScreen
import com.example.ui.InvoiceViewModel
import com.example.ui.PosViewModel
import com.example.ui.components.BusinessMoreOption
import com.example.ui.components.BusinessMoreOptionFeatureModal
import com.example.ui.components.BusinessMoreOptionsDialog
import com.example.ui.components.InventoryMoreOption
import com.example.ui.components.InventoryMoreOptionFeatureModal
import com.example.ui.components.InventoryMoreOptionsDialog
import com.example.ui.components.ThermalReceiptModal
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsAndProfileScreen
import com.example.ui.screens.SettingsSubScreen
import com.example.ui.theme.LedgerFlowTheme
import com.example.util.BarcodeScannerDialog
import com.example.util.ScannerMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val posViewModel: PosViewModel = viewModel()
            val invoiceViewModel: InvoiceViewModel = viewModel()
            val settings by posViewModel.settingsState.collectAsStateWithLifecycle()

            LedgerFlowTheme(themeMode = settings.themeMode) {
                LedgerFlowPosApp(
                    viewModel = posViewModel,
                    invoiceViewModel = invoiceViewModel
                )
            }
        }
    }
}

private data class NavDestination(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(AppScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    NavDestination(AppScreen.BILLING, "Billing", Icons.Default.PointOfSale, "nav_billing"),
    NavDestination(AppScreen.INVENTORY, "Inventory", Icons.Default.Inventory2, "nav_inventory"),
    NavDestination(AppScreen.REPORTS, "Reports", Icons.Default.Analytics, "nav_reports"),
    NavDestination(AppScreen.SETTINGS, "Settings", Icons.Default.Settings, "nav_settings")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerFlowPosApp(
    viewModel: PosViewModel,
    invoiceViewModel: InvoiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val filteredInventory by viewModel.filteredInventoryItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inventorySearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.inventorySortOption.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allLineItems by viewModel.allLineItems.collectAsStateWithLifecycle()
    val itemPerfStats by viewModel.itemPerformanceStats.collectAsStateWithLifecycle()
    val invoiceState by invoiceViewModel.uiState.collectAsStateWithLifecycle()
    val activeReceiptPreview by viewModel.activeReceiptPreview.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val deletedInvoices by viewModel.deletedInvoices.collectAsStateWithLifecycle()
    val deletedInventoryItems by viewModel.deletedInventoryItems.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSaleBarcodeScanner by remember { mutableStateOf(false) }
    var showBusinessMoreOptions by remember { mutableStateOf(false) }
    var activeBusinessFeatureModal by remember { mutableStateOf<BusinessMoreOption?>(null) }
    var showInventoryMoreOptions by remember { mutableStateOf(false) }
    var activeInventoryFeatureModal by remember { mutableStateOf<InventoryMoreOption?>(null) }
    var settingsInitialSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }

    var pendingPdfReportTitle by remember { mutableStateOf("Comprehensive POS Sales & Inventory Report") }
    var pendingPdfInvoices by remember { mutableStateOf<List<SalesInvoiceEntity>>(emptyList()) }
    var pendingCsvInvoices by remember { mutableStateOf<List<SalesInvoiceEntity>>(emptyList()) }

    // BackHandler on secondary screens returns to Dashboard
    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(AppScreen.DASHBOARD)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // SAF Launchers for PDF, Excel CSV, and JSON Backup Export/Import
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val targetList = pendingPdfInvoices.ifEmpty { allInvoices }
            viewModel.exportPdfReport(context, uri, pendingPdfReportTitle, targetList)
        }
    }

    val createExcelCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val targetList = pendingCsvInvoices.ifEmpty { allInvoices }
            viewModel.exportExcelCsv(context, uri, targetList)
        }
    }

    val createJsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportJsonDatabaseBackup(context, uri)
        }
    }

    val openJsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importJsonDatabaseBackup(context, uri)
        }
    }

    // QR / Barcode Scanner Modal for instant Sale item addition from inventory
    if (showSaleBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Item QR Code to Add to Bill",
            subtitle = "Scan or tap an inventory QR code to add item to the current sale",
            mode = ScannerMode.SELLING_BILL,
            inventoryItems = inventoryItems,
            onBarcodeScanned = { scannedBarcode ->
                showSaleBarcodeScanner = false
                invoiceViewModel.addScannedBarcodeToInvoice(
                    barcode = scannedBarcode,
                    fallbackInventory = inventoryItems,
                    onResultMessage = { msg -> viewModel.postStatusMessage(msg) }
                )
                viewModel.navigateTo(AppScreen.BILLING)
            },
            onContinuousBarcodeScanned = { scannedBarcode ->
                invoiceViewModel.addScannedBarcodeToInvoice(
                    barcode = scannedBarcode,
                    fallbackInventory = inventoryItems,
                    onResultMessage = { msg -> viewModel.postStatusMessage(msg) }
                )
                viewModel.navigateTo(AppScreen.BILLING)
            },
            onDismiss = { showSaleBarcodeScanner = false }
        )
    }

    // Thermal Receipt Preview & Bluetooth Print Modal
    activeReceiptPreview?.let { (invoice, lines) ->
        ThermalReceiptModal(
            invoice = invoice,
            lineItems = lines,
            settings = settings,
            onPaperWidthChange = { newWidth ->
                viewModel.saveThermalPrinterSettings(
                    paperWidth = newWidth,
                    printerName = settings.selectedPrinterName,
                    printerAddress = settings.selectedPrinterAddress,
                    printHeader = settings.printBusinessHeader,
                    printCustomer = settings.printCustomerDetails,
                    printHsn = settings.printHsnColumn,
                    printGst = settings.printGstBreakdown,
                    printCash = settings.printCashAndChange,
                    printBarcode = settings.printBarcodeFooter,
                    footerMessage = settings.customFooterMessage
                )
            },
            onExportPdfClick = {
                pendingPdfReportTitle = "Tax Invoice ${invoice.billNumber}"
                pendingPdfInvoices = listOf(invoice)
                createPdfLauncher.launch("${invoice.billNumber}.pdf")
            },
            onStatusMessage = { msg -> viewModel.postStatusMessage(msg) },
            onDismiss = { viewModel.closeReceiptPreview() }
        )
    }

    // 1. Business & Accounting 3x3 "More Options" Sheet (Screenshot 2)
    if (showBusinessMoreOptions) {
        BusinessMoreOptionsDialog(
            onSelectOption = { option ->
                showBusinessMoreOptions = false
                when (option) {
                    BusinessMoreOption.PRINT_SETTINGS -> {
                        settingsInitialSubScreen = SettingsSubScreen.INVOICE_PRINT
                        viewModel.navigateTo(AppScreen.SETTINGS)
                    }
                    BusinessMoreOption.TXN_SMS_SETTINGS -> {
                        settingsInitialSubScreen = SettingsSubScreen.TRANSACTION_SMS
                        viewModel.navigateTo(AppScreen.SETTINGS)
                    }
                    else -> {
                        activeBusinessFeatureModal = option
                    }
                }
            },
            onExportPdf = {
                showBusinessMoreOptions = false
                pendingPdfReportTitle = "Full Sales & Inventory Report"
                pendingPdfInvoices = allInvoices
                createPdfLauncher.launch("LedgerFlow_Report.pdf")
            },
            onExportExcel = {
                showBusinessMoreOptions = false
                pendingCsvInvoices = allInvoices
                createExcelCsvLauncher.launch("LedgerFlow_Export.csv")
            },
            onExportJsonBackup = {
                showBusinessMoreOptions = false
                createJsonBackupLauncher.launch("LedgerFlow_Backup.json")
            },
            onDismiss = { showBusinessMoreOptions = false }
        )
    }

    // Active Business Feature Detail Modal
    activeBusinessFeatureModal?.let { option ->
        BusinessMoreOptionFeatureModal(
            option = option,
            settings = settings,
            invoices = allInvoices,
            lineItems = allLineItems,
            inventoryItems = inventoryItems,
            deletedInvoices = deletedInvoices,
            deletedInventoryItems = deletedInventoryItems,
            onSaveBankAccounts = { viewModel.saveBankAccounts(it) },
            onOpenInvoiceReceipt = { invoice ->
                activeBusinessFeatureModal = null
                viewModel.openInvoiceReceiptPreview(invoice)
            },
            onExportPdf = { title, filtered ->
                pendingPdfReportTitle = title
                pendingPdfInvoices = filtered
                createPdfLauncher.launch("LedgerFlow_${title.replace(" ", "_")}.pdf")
            },
            onExportExcel = { filtered ->
                pendingCsvInvoices = filtered
                createExcelCsvLauncher.launch("LedgerFlow_Accounting_Export.csv")
            },
            onRestoreInvoice = { inv, lines -> viewModel.restoreDeletedInvoice(inv, lines) },
            onRestoreItem = { item -> viewModel.restoreDeletedInventoryItem(item) },
            onClearRecycleBin = { viewModel.clearRecycleBin() },
            onDismiss = { activeBusinessFeatureModal = null }
        )
    }

    // 2. Inventory & Items 3x2 "More Options" Sheet (Screenshot 3)
    if (showInventoryMoreOptions) {
        InventoryMoreOptionsDialog(
            onSelectOption = { option ->
                showInventoryMoreOptions = false
                activeInventoryFeatureModal = option
            },
            onDismiss = { showInventoryMoreOptions = false }
        )
    }

    // Active Inventory Feature Detail Modal
    activeInventoryFeatureModal?.let { option ->
        InventoryMoreOptionFeatureModal(
            option = option,
            settings = settings,
            inventoryItems = inventoryItems,
            itemPerformanceStats = itemPerfStats,
            onImportJsonFile = {
                openJsonBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onImportSampleCatalogPack = { viewModel.importSampleBulkCatalogPack() },
            onExportPdf = {
                pendingPdfReportTitle = "Inventory Master & Valuation Catalog"
                pendingPdfInvoices = allInvoices
                createPdfLauncher.launch("LedgerFlow_Inventory_Catalog.pdf")
            },
            onExportExcel = {
                pendingCsvInvoices = allInvoices
                createExcelCsvLauncher.launch("LedgerFlow_Inventory_Export.csv")
            },
            onSaveItemSettings = { unit, lowAlert, mrp, batch, expiry, wholesale, rack ->
                viewModel.saveItemSettings(
                    defaultUnit = unit,
                    defaultLowStock = lowAlert,
                    mrp = mrp,
                    batch = batch,
                    expiry = expiry,
                    wholesale = wholesale,
                    rack = rack
                )
            },
            onRestockItem = { item, qty -> viewModel.adjustItemStock(item, qty) },
            onRestockAllLow = { viewModel.restockAllLowStockItems() },
            onDismiss = { activeInventoryFeatureModal = null }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = settings.businessName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LedgerFlow POS • ${currentScreen.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Instant Camera Barcode Scanner Shortcut
                        IconButton(
                            onClick = { showSaleBarcodeScanner = true },
                            modifier = Modifier.testTag("topbar_barcode_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode or QR Code"
                            )
                        }

                        // Dark / Light Theme Quick Toggle
                        IconButton(
                            onClick = {
                                val next = if (settings.themeMode == "DARK") "LIGHT" else "DARK"
                                viewModel.setThemeMode(next)
                            },
                            modifier = Modifier.testTag("topbar_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (settings.themeMode == "DARK") Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark or Light Mode"
                            )
                        }

                        // More Options Button (opens Inventory 3x2 sheet on Inventory screen, Business 3x3 sheet elsewhere)
                        IconButton(
                            onClick = {
                                if (currentScreen == AppScreen.INVENTORY) {
                                    showInventoryMoreOptions = true
                                } else {
                                    showBusinessMoreOptions = true
                                }
                            },
                            modifier = Modifier.testTag("topbar_quick_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Open More Options Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        NAV_DESTINATIONS.forEach { dest ->
                            NavigationBarItem(
                                selected = currentScreen == dest.screen,
                                onClick = { viewModel.navigateTo(dest.screen) },
                                icon = {
                                    Icon(imageVector = dest.icon, contentDescription = dest.label)
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NAV_DESTINATIONS.forEach { dest ->
                            NavigationRailItem(
                                selected = currentScreen == dest.screen,
                                onClick = { viewModel.navigateTo(dest.screen) },
                                icon = {
                                    Icon(imageVector = dest.icon, contentDescription = dest.label)
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                metrics = metrics,
                                settings = settings,
                                recentInvoices = allInvoices,
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenScannerForSale = { showSaleBarcodeScanner = true },
                                onOpenQuickAccessMenu = { showBusinessMoreOptions = true },
                                onInvoiceClick = { viewModel.openInvoiceReceiptPreview(it) },
                                onUpdateOpeningCash = { viewModel.setOpeningCashInHand(it) }
                            )
                        }

                        AppScreen.BILLING -> {
                            BillingScreen(
                                invoiceState = invoiceState,
                                inventoryItems = inventoryItems,
                                currencySymbol = settings.currencySymbol,
                                onUpdateCustomerHeader = { name, mobile, billNo, date, time ->
                                    invoiceViewModel.updateCustomerHeader(name, mobile, billNo, date, time)
                                },
                                onSetGstEnabled = { invoiceViewModel.setGstEnabled(it) },
                                onSetGstTaxMode = { invoiceViewModel.setGstTaxMode(it) },
                                onSetSelectedGstSlab = { invoiceViewModel.setSelectedGstSlab(it) },
                                onUpdateDiscountAndPayment = { discount, cash, mode ->
                                    invoiceViewModel.updateDiscountAndPayment(discount, cash, mode)
                                },
                                onQuickCashTender = { amount ->
                                    invoiceViewModel.setQuickCashTender(amount)
                                },
                                onAddItemToCart = { item, qty ->
                                    invoiceViewModel.addItemFromInventory(item, qty)
                                    viewModel.postStatusMessage("Added '${item.name}' to current sale.")
                                },
                                onAddCustomItem = { name, unit, hsn, rate, qty, discPct, slab ->
                                    invoiceViewModel.addCustomLineItem(name, unit, hsn, rate, qty, discPct, slab)
                                },
                                onUpdateCartQuantity = { idx, qty ->
                                    invoiceViewModel.updateLineQuantity(idx, qty)
                                },
                                onUpdateCartUnitPrice = { idx, price ->
                                    invoiceViewModel.updateLineUnitPrice(idx, price)
                                },
                                onUpdateCartDiscountPercent = { idx, discPct ->
                                    invoiceViewModel.updateLineDiscountPercent(idx, discPct)
                                },
                                onUpdateCartGstSlab = { idx, slab ->
                                    invoiceViewModel.updateLineGstSlab(idx, slab)
                                },
                                onRemoveCartItem = { idx -> invoiceViewModel.removeLineItem(idx) },
                                onClearDraft = { invoiceViewModel.clearInvoiceDraft() },
                                onOpenBarcodeScanner = { showSaleBarcodeScanner = true },
                                onScanOrEnterQrCode = { qrCode ->
                                    invoiceViewModel.addScannedBarcodeToInvoice(
                                        barcode = qrCode,
                                        fallbackInventory = inventoryItems,
                                        onResultMessage = { msg -> viewModel.postStatusMessage(msg) }
                                    )
                                },
                                onFinalizeInvoice = {
                                    invoiceViewModel.finalizeAndSaveInvoice(
                                        onComplete = { savedInvoice, _ ->
                                            viewModel.openInvoiceReceiptPreview(savedInvoice)
                                            viewModel.postStatusMessage("Invoice ${savedInvoice.billNumber} saved & stock updated!")
                                        },
                                        onError = { err -> viewModel.postStatusMessage(err) }
                                    )
                                }
                            )
                        }

                        AppScreen.INVENTORY -> {
                            InventoryScreen(
                                allItems = inventoryItems,
                                filteredItems = filteredInventory,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                sortOption = sortOption,
                                currencySymbol = settings.currencySymbol,
                                onSearchQueryChange = { viewModel.setInventorySearchQuery(it) },
                                onCategoryChange = { viewModel.setSelectedCategory(it) },
                                onSortOptionChange = { viewModel.setInventorySortOption(it) },
                                onSaveItem = { id, name, barcode, category, unit, hsn, pRate, sRate, stock, minAlert, gst ->
                                    viewModel.saveInventoryItem(
                                        id = id,
                                        name = name,
                                        barcode = barcode,
                                        category = category,
                                        unit = unit,
                                        hsnCode = hsn,
                                        purchaseRate = pRate,
                                        salesRate = sRate,
                                        stockQuantity = stock,
                                        minStockAlert = minAlert,
                                        gstRate = gst
                                    )
                                },
                                onAdjustStock = { item, delta -> viewModel.adjustItemStock(item, delta) },
                                onDeleteItem = { item -> viewModel.deleteInventoryItem(item) },
                                onSellItemNow = { item ->
                                    invoiceViewModel.addItemFromInventory(item, 1.0)
                                    viewModel.postStatusMessage("Added '${item.name}' to current sale.")
                                    viewModel.navigateTo(AppScreen.BILLING)
                                },
                                onOpenMoreOptions = { showInventoryMoreOptions = true }
                            )
                        }

                        AppScreen.REPORTS -> {
                            ReportsScreen(
                                invoices = allInvoices,
                                itemPerformanceStats = itemPerfStats,
                                currencySymbol = settings.currencySymbol,
                                onOpenInvoiceReceipt = { viewModel.openInvoiceReceiptPreview(it) },
                                onDeleteInvoice = { viewModel.deleteInvoice(it) },
                                onExportPdfReport = { title, filtered ->
                                    pendingPdfReportTitle = title
                                    pendingPdfInvoices = filtered
                                    createPdfLauncher.launch("LedgerFlow_${title.replace(" ", "_")}.pdf")
                                },
                                onExportExcelCsv = { filtered ->
                                    pendingCsvInvoices = filtered
                                    createExcelCsvLauncher.launch("LedgerFlow_Sales_Export.csv")
                                }
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsAndProfileScreen(
                                settings = settings,
                                invoices = allInvoices,
                                inventoryItems = inventoryItems,
                                initialSubScreen = settingsInitialSubScreen,
                                onClearInitialSubScreen = { settingsInitialSubScreen = null },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetGstEnabled = { viewModel.setGstEnabled(it) },
                                onSetDefaultGstSlab = { viewModel.setDefaultGstSlab(it) },
                                onSaveBusinessProfile = { name, cat, gstin, phone, addr, logo, banner ->
                                    viewModel.saveBusinessProfile(name, cat, gstin, phone, addr, logo, banner)
                                },
                                onSaveThermalPrinterSettings = { width, pName, pAddr, pHeader, pCust, pHsn, pGst, pCash, pBarcode, footer ->
                                    viewModel.saveThermalPrinterSettings(
                                        width,
                                        pName,
                                        pAddr,
                                        pHeader,
                                        pCust,
                                        pHsn,
                                        pGst,
                                        pCash,
                                        pBarcode,
                                        footer
                                    )
                                },
                                onSaveTransactionSettings = { prefix, payMode, itemDisc, roundOff, autoShare, negativeStock ->
                                    viewModel.saveTransactionSettings(
                                        prefix,
                                        payMode,
                                        itemDisc,
                                        roundOff,
                                        autoShare,
                                        negativeStock
                                    )
                                },
                                onSaveTaxExtraSettings = { taxMode, stateOfSupply ->
                                    viewModel.saveTaxExtraSettings(taxMode, stateOfSupply)
                                },
                                onSaveSmsSettings = { smsOn, whatsappOn, template ->
                                    viewModel.saveSmsSettings(smsOn, whatsappOn, template)
                                },
                                onSaveReminderSettings = { paymentRem, lowStockRem, days ->
                                    viewModel.saveReminderSettings(paymentRem, lowStockRem, days)
                                },
                                onSaveItemSettings = { unit, lowAlert, mrp, batch, expiry, wholesale, rack ->
                                    viewModel.saveItemSettings(
                                        unit,
                                        lowAlert,
                                        mrp,
                                        batch,
                                        expiry,
                                        wholesale,
                                        rack
                                    )
                                },
                                onSaveStaffUsers = { viewModel.saveStaffUsers(it) },
                                onSaveParties = { viewModel.saveParties(it) },
                                onSaveCustomReminders = { viewModel.saveCustomReminders(it) },
                                onTriggerPdfBackup = {
                                    pendingPdfReportTitle = "Full POS Backup Report"
                                    pendingPdfInvoices = allInvoices
                                    createPdfLauncher.launch("LedgerFlow_Full_Report.pdf")
                                },
                                onTriggerExcelBackup = {
                                    pendingCsvInvoices = allInvoices
                                    createExcelCsvLauncher.launch("LedgerFlow_Full_Backup.csv")
                                },
                                onTriggerJsonExport = {
                                    createJsonBackupLauncher.launch("LedgerFlow_Database_Backup.json")
                                },
                                onTriggerJsonImport = {
                                    openJsonBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                onPreviewSampleReceipt = {
                                    allInvoices.firstOrNull()?.let { viewModel.openInvoiceReceiptPreview(it) }
                                        ?: viewModel.postStatusMessage("Create an invoice first to preview receipt.")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
