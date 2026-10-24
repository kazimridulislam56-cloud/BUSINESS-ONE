package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.InventoryItemEntity
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BusinessAndAppSettings
import com.example.data.preferences.CustomReminderRecord
import com.example.data.preferences.PartyRecord
import com.example.data.preferences.StaffUserRecord
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ProfitGreen
import com.example.util.PairedPrinterDevice
import com.example.util.ThermalPrinterManager
import java.util.Locale

enum class SettingsSubScreen(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isNew: Boolean = false,
    val testTag: String
) {
    GENERAL("General", "Business profile, theme, currency & backups", Icons.Default.SettingsApplications, true, "settings_nav_general"),
    TRANSACTION("Transaction", "Invoice prefix, default payment & discounts", Icons.Default.CurrencyRupee, true, "settings_nav_transaction"),
    INVOICE_PRINT("Invoice Print", "58mm/80mm Bluetooth thermal printer & fields", Icons.Default.Print, false, "settings_nav_invoice_print"),
    TAXES_AND_GST("Taxes & GST", "CGST/SGST slabs (5%, 12%, 18%, 28%) & HSN", Icons.Default.Percent, false, "settings_nav_taxes_gst"),
    USER_MANAGEMENT("User Management", "Staff operators, roles & permissions", Icons.Default.Group, false, "settings_nav_user_management"),
    TRANSACTION_SMS("Transaction SMS", "Auto bill SMS template & WhatsApp sharing", Icons.Default.Sms, false, "settings_nav_transaction_sms"),
    REMINDERS("Reminders", "Payment due follow-ups & low-stock alerts", Icons.Default.NotificationsActive, false, "settings_nav_reminders"),
    PARTY("Party", "Customer & supplier directory, GSTIN & balances", Icons.Default.People, false, "settings_nav_party"),
    ITEM("Item", "Default units, low-stock threshold & custom fields", Icons.Default.Category, false, "settings_nav_item")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndProfileScreen(
    settings: BusinessAndAppSettings,
    invoices: List<SalesInvoiceEntity> = emptyList(),
    inventoryItems: List<InventoryItemEntity> = emptyList(),
    initialSubScreen: SettingsSubScreen? = null,
    onClearInitialSubScreen: () -> Unit = {},
    onSetThemeMode: (String) -> Unit,
    onSetGstEnabled: (Boolean) -> Unit,
    onSetDefaultGstSlab: (Int) -> Unit,
    onSaveBusinessProfile: (String, String, String, String, String, String, String) -> Unit,
    onSaveThermalPrinterSettings: (String, String, String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, String) -> Unit,
    onSaveTransactionSettings: (String, String, Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onSaveTaxExtraSettings: (String, Boolean) -> Unit = { _, _ -> },
    onSaveSmsSettings: (Boolean, Boolean, String) -> Unit = { _, _, _ -> },
    onSaveReminderSettings: (Boolean, Boolean, String) -> Unit = { _, _, _ -> },
    onSaveItemSettings: (String, Double, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
    onSaveStaffUsers: (List<StaffUserRecord>) -> Unit = {},
    onSaveParties: (List<PartyRecord>) -> Unit = {},
    onSaveCustomReminders: (List<CustomReminderRecord>) -> Unit = {},
    onTriggerPdfBackup: () -> Unit,
    onTriggerExcelBackup: () -> Unit,
    onTriggerJsonExport: () -> Unit,
    onTriggerJsonImport: () -> Unit,
    onPreviewSampleReceipt: () -> Unit
) {
    val context = LocalContext.current
    var activeSubScreen by remember { mutableStateOf<SettingsSubScreen?>(initialSubScreen) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(initialSubScreen) {
        if (initialSubScreen != null) {
            activeSubScreen = initialSubScreen
            onClearInitialSubScreen()
        }
    }

    if (activeSubScreen != null) {
        BackHandler {
            activeSubScreen = null
        }
    }

    // Business Profile Form State
    var businessName by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var businessCategory by remember(settings.businessCategory) { mutableStateOf(settings.businessCategory) }
    var gstinNumber by remember(settings.gstinNumber) { mutableStateOf(settings.gstinNumber) }
    var businessPhone by remember(settings.businessPhone) { mutableStateOf(settings.businessPhone) }
    var businessAddress by remember(settings.businessAddress) { mutableStateOf(settings.businessAddress) }
    var logoUri by remember(settings.logoUri) { mutableStateOf(settings.logoUri) }
    var bannerUri by remember(settings.bannerUri) { mutableStateOf(settings.bannerUri) }

    // Thermal Printer Form State
    var paperWidth by remember(settings.printerPaperWidth) { mutableStateOf(settings.printerPaperWidth) }
    var printerName by remember(settings.selectedPrinterName) { mutableStateOf(settings.selectedPrinterName) }
    var printerAddress by remember(settings.selectedPrinterAddress) { mutableStateOf(settings.selectedPrinterAddress) }
    var printHeader by remember(settings.printBusinessHeader) { mutableStateOf(settings.printBusinessHeader) }
    var printCustomer by remember(settings.printCustomerDetails) { mutableStateOf(settings.printCustomerDetails) }
    var printHsn by remember(settings.printHsnColumn) { mutableStateOf(settings.printHsnColumn) }
    var printGst by remember(settings.printGstBreakdown) { mutableStateOf(settings.printGstBreakdown) }
    var printCash by remember(settings.printCashAndChange) { mutableStateOf(settings.printCashAndChange) }
    var printBarcode by remember(settings.printBarcodeFooter) { mutableStateOf(settings.printBarcodeFooter) }
    var footerMessage by remember(settings.customFooterMessage) { mutableStateOf(settings.customFooterMessage) }

    var pairedDevices by remember {
        mutableStateOf(ThermalPrinterManager.getPairedPrinters(context))
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pairedDevices = ThermalPrinterManager.getPairedPrinters(context)
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            logoUri = uri.toString()
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            bannerUri = uri.toString()
        }
    }

    // If a sub-screen is selected from the 9-row Settings menu, show its dedicated view
    activeSubScreen?.let { subScreen ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_subscreen_${subScreen.name}")
        ) {
            // Sub-screen Top Bar
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeSubScreen = null },
                        modifier = Modifier.testTag("settings_subscreen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings Menu"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = subScreen.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subScreen.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (subScreen) {
                    SettingsSubScreen.GENERAL -> {
                        item {
                            AppearanceAndGstCard(
                                settings = settings,
                                onSetThemeMode = onSetThemeMode,
                                onSetGstEnabled = onSetGstEnabled,
                                onSetDefaultGstSlab = onSetDefaultGstSlab
                            )
                        }
                        item {
                            BusinessProfileCard(
                                businessName = businessName,
                                onBusinessNameChange = { businessName = it },
                                businessCategory = businessCategory,
                                onBusinessCategoryChange = { businessCategory = it },
                                gstinNumber = gstinNumber,
                                onGstinChange = { gstinNumber = it },
                                businessPhone = businessPhone,
                                onPhoneChange = { businessPhone = it },
                                businessAddress = businessAddress,
                                onAddressChange = { businessAddress = it },
                                logoUri = logoUri,
                                bannerUri = bannerUri,
                                onPickLogo = {
                                    logoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPickBanner = {
                                    bannerPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onSave = {
                                    onSaveBusinessProfile(
                                        businessName,
                                        businessCategory,
                                        gstinNumber,
                                        businessPhone,
                                        businessAddress,
                                        logoUri,
                                        bannerUri
                                    )
                                }
                            )
                        }
                        item {
                            DataBackupAndRestoreCard(
                                onTriggerPdfBackup = onTriggerPdfBackup,
                                onTriggerExcelBackup = onTriggerExcelBackup,
                                onTriggerJsonExport = onTriggerJsonExport,
                                onTriggerJsonImport = onTriggerJsonImport
                            )
                        }
                    }

                    SettingsSubScreen.TRANSACTION -> {
                        item {
                            TransactionSettingsSubView(
                                settings = settings,
                                onSave = onSaveTransactionSettings
                            )
                        }
                    }

                    SettingsSubScreen.INVOICE_PRINT -> {
                        item {
                            ThermalPrintingCard(
                                paperWidth = paperWidth,
                                onPaperWidthChange = { paperWidth = it },
                                printerName = printerName,
                                printerAddress = printerAddress,
                                pairedDevices = pairedDevices,
                                onScanBluetooth = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                        !ThermalPrinterManager.hasBluetoothPermission(context)
                                    ) {
                                        btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    } else {
                                        pairedDevices = ThermalPrinterManager.getPairedPrinters(context)
                                    }
                                },
                                onSelectDevice = { dev ->
                                    printerName = dev.name
                                    printerAddress = dev.address
                                },
                                printHeader = printHeader,
                                onPrintHeaderChange = { printHeader = it },
                                printCustomer = printCustomer,
                                onPrintCustomerChange = { printCustomer = it },
                                printHsn = printHsn,
                                onPrintHsnChange = { printHsn = it },
                                printGst = printGst,
                                onPrintGstChange = { printGst = it },
                                printCash = printCash,
                                onPrintCashChange = { printCash = it },
                                printBarcode = printBarcode,
                                onPrintBarcodeChange = { printBarcode = it },
                                footerMessage = footerMessage,
                                onFooterMessageChange = { footerMessage = it },
                                onPreviewSampleReceipt = onPreviewSampleReceipt,
                                onSave = {
                                    onSaveThermalPrinterSettings(
                                        paperWidth,
                                        printerName,
                                        printerAddress,
                                        printHeader,
                                        printCustomer,
                                        printHsn,
                                        printGst,
                                        printCash,
                                        printBarcode,
                                        footerMessage
                                    )
                                }
                            )
                        }
                    }

                    SettingsSubScreen.TAXES_AND_GST -> {
                        item {
                            TaxesAndGstSubView(
                                settings = settings,
                                onSetGstEnabled = onSetGstEnabled,
                                onSetDefaultGstSlab = onSetDefaultGstSlab,
                                onSaveTaxExtraSettings = onSaveTaxExtraSettings
                            )
                        }
                    }

                    SettingsSubScreen.USER_MANAGEMENT -> {
                        item {
                            UserManagementSubView(
                                users = settings.staffUsers,
                                onSaveUsers = onSaveStaffUsers
                            )
                        }
                    }

                    SettingsSubScreen.TRANSACTION_SMS -> {
                        item {
                            TransactionSmsSubView(
                                settings = settings,
                                latestInvoice = invoices.firstOrNull(),
                                onSaveSmsSettings = onSaveSmsSettings
                            )
                        }
                    }

                    SettingsSubScreen.REMINDERS -> {
                        item {
                            RemindersSubView(
                                settings = settings,
                                invoices = invoices,
                                inventoryItems = inventoryItems,
                                onSaveReminderSettings = onSaveReminderSettings,
                                onSaveCustomReminders = onSaveCustomReminders
                            )
                        }
                    }

                    SettingsSubScreen.PARTY -> {
                        item {
                            PartyDirectorySubView(
                                settings = settings,
                                invoices = invoices,
                                onSaveParties = onSaveParties
                            )
                        }
                    }

                    SettingsSubScreen.ITEM -> {
                        item {
                            ItemSettingsSubView(
                                settings = settings,
                                onSaveItemSettings = onSaveItemSettings
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val filteredMenuItems = remember(searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.US)
        if (q.isEmpty()) SettingsSubScreen.entries
        else SettingsSubScreen.entries.filter {
            it.title.lowercase(Locale.US).contains(q) ||
                it.subtitle.lowercase(Locale.US).contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Top Header Bar matching Screenshots 1 & 4 ("Settings" + Search icon)
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.testTag("settings_search_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Settings"
                            )
                        }
                    }

                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search General, Transaction, GST, Party, Item…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("settings_search_input")
                        )
                    }
                }
            }
        }

        // 9 Menu Rows matching Screenshots 1 & 4:
        // General (NEW), Transaction (NEW), Invoice Print, Taxes & GST,
        // User Management, Transaction SMS, Reminders, Party, Item
        items(filteredMenuItems.size) { index ->
            val menu = filteredMenuItems[index]
            SettingsMenuRowItem(
                item = menu,
                onClick = { activeSubScreen = menu }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        }

        // Below the 9-row Settings Hub, keep the direct cards accessible on scroll
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                AppearanceAndGstCard(
                    settings = settings,
                    onSetThemeMode = onSetThemeMode,
                    onSetGstEnabled = onSetGstEnabled,
                    onSetDefaultGstSlab = onSetDefaultGstSlab
                )

                BusinessProfileCard(
                    businessName = businessName,
                    onBusinessNameChange = { businessName = it },
                    businessCategory = businessCategory,
                    onBusinessCategoryChange = { businessCategory = it },
                    gstinNumber = gstinNumber,
                    onGstinChange = { gstinNumber = it },
                    businessPhone = businessPhone,
                    onPhoneChange = { businessPhone = it },
                    businessAddress = businessAddress,
                    onAddressChange = { businessAddress = it },
                    logoUri = logoUri,
                    bannerUri = bannerUri,
                    onPickLogo = {
                        logoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickBanner = {
                        bannerPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSave = {
                        onSaveBusinessProfile(
                            businessName,
                            businessCategory,
                            gstinNumber,
                            businessPhone,
                            businessAddress,
                            logoUri,
                            bannerUri
                        )
                    }
                )

                ThermalPrintingCard(
                    paperWidth = paperWidth,
                    onPaperWidthChange = { paperWidth = it },
                    printerName = printerName,
                    printerAddress = printerAddress,
                    pairedDevices = pairedDevices,
                    onScanBluetooth = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            !ThermalPrinterManager.hasBluetoothPermission(context)
                        ) {
                            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            pairedDevices = ThermalPrinterManager.getPairedPrinters(context)
                        }
                    },
                    onSelectDevice = { dev ->
                        printerName = dev.name
                        printerAddress = dev.address
                    },
                    printHeader = printHeader,
                    onPrintHeaderChange = { printHeader = it },
                    printCustomer = printCustomer,
                    onPrintCustomerChange = { printCustomer = it },
                    printHsn = printHsn,
                    onPrintHsnChange = { printHsn = it },
                    printGst = printGst,
                    onPrintGstChange = { printGst = it },
                    printCash = printCash,
                    onPrintCashChange = { printCash = it },
                    printBarcode = printBarcode,
                    onPrintBarcodeChange = { printBarcode = it },
                    footerMessage = footerMessage,
                    onFooterMessageChange = { footerMessage = it },
                    onPreviewSampleReceipt = onPreviewSampleReceipt,
                    onSave = {
                        onSaveThermalPrinterSettings(
                            paperWidth,
                            printerName,
                            printerAddress,
                            printHeader,
                            printCustomer,
                            printHsn,
                            printGst,
                            printCash,
                            printBarcode,
                            footerMessage
                        )
                    }
                )

                DataBackupAndRestoreCard(
                    onTriggerPdfBackup = onTriggerPdfBackup,
                    onTriggerExcelBackup = onTriggerExcelBackup,
                    onTriggerJsonExport = onTriggerJsonExport,
                    onTriggerJsonImport = onTriggerJsonImport
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuRowItem(
    item: SettingsSubScreen,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag(item.testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.isNew) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF002E)
                ) {
                    Text(
                        text = "NEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open ${item.title}",
            tint = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun TransactionSettingsSubView(
    settings: BusinessAndAppSettings,
    onSave: (String, String, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var prefix by remember(settings.invoicePrefix) { mutableStateOf(settings.invoicePrefix) }
    var paymentMode by remember(settings.defaultPaymentMode) { mutableStateOf(settings.defaultPaymentMode) }
    var itemDiscount by remember(settings.enableItemWiseDiscount) { mutableStateOf(settings.enableItemWiseDiscount) }
    var extraDiscount by remember(settings.enableExtraBillDiscount) { mutableStateOf(settings.enableExtraBillDiscount) }
    var autoRoundOff by remember(settings.autoRoundOffTotal) { mutableStateOf(settings.autoRoundOffTotal) }
    var autoPreviewReceipt by remember(settings.autoPreviewReceiptOnSave) { mutableStateOf(settings.autoPreviewReceiptOnSave) }

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
                text = "Transaction & Billing Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it },
                label = { Text("Invoice Number Prefix") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Default Payment Mode", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cash", "UPI / QR", "Card", "Part-Cash").forEach { mode ->
                    FilterChip(
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode },
                        label = { Text(mode) }
                    )
                }
            }

            ReceiptFieldToggleRow("Enable Item-Wise Discount (%) & After-Discount Price", itemDiscount) {
                itemDiscount = it
            }
            ReceiptFieldToggleRow("Enable Additional Bill-Level Flat Discount", extraDiscount) {
                extraDiscount = it
            }
            ReceiptFieldToggleRow("Auto Round-Off Invoice Grand Total to Nearest Rupee", autoRoundOff) {
                autoRoundOff = it
            }
            ReceiptFieldToggleRow("Auto-Open Thermal Receipt Preview on Bill Save", autoPreviewReceipt) {
                autoPreviewReceipt = it
            }

            Button(
                onClick = {
                    onSave(prefix, paymentMode, itemDiscount, extraDiscount, autoRoundOff, autoPreviewReceipt)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Transaction Settings")
            }
        }
    }
}

@Composable
private fun TaxesAndGstSubView(
    settings: BusinessAndAppSettings,
    onSetGstEnabled: (Boolean) -> Unit,
    onSetDefaultGstSlab: (Int) -> Unit,
    onSaveTaxExtraSettings: (String, Boolean) -> Unit
) {
    var taxMode by remember(settings.defaultGstTaxMode) { mutableStateOf(settings.defaultGstTaxMode) }
    var enforceHsn by remember(settings.enforceHsnCode) { mutableStateOf(settings.enforceHsnCode) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable GST Tax (CGST + SGST)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Splits tax 50% CGST and 50% SGST across invoices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.gstEnabled,
                    onCheckedChange = onSetGstEnabled
                )
            }

            Text("Default GST Rate Slab", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 12, 18, 28).forEach { slab ->
                    FilterChip(
                        selected = settings.defaultGstSlab == slab,
                        onClick = { onSetDefaultGstSlab(slab) },
                        label = { Text("$slab% (${slab / 2.0}%+${slab / 2.0}%)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Default GST Calculation Mode", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("UNIFORM_SLAB" to "Uniform Invoice Slab", "ITEM_WISE_SLAB" to "Per-Item HSN Slab").forEach { (key, label) ->
                    FilterChip(
                        selected = taxMode == key,
                        onClick = { taxMode = key },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ReceiptFieldToggleRow("Display & Enforce HSN/SAC Code on Items & Bills", enforceHsn) {
                enforceHsn = it
            }

            Button(
                onClick = { onSaveTaxExtraSettings(taxMode, enforceHsn) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Taxes & GST Settings")
            }
        }
    }
}

@Composable
private fun UserManagementSubView(
    users: List<StaffUserRecord>,
    onSaveUsers: (List<StaffUserRecord>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Cashier / Billing") }

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
                text = "Staff & Operator Management (${users.size} Users)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            users.forEach { user ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${user.role} • ${user.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (users.size > 1) {
                            IconButton(
                                onClick = {
                                    onSaveUsers(users.filterNot { it.id == user.id })
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove user", tint = DangerRed)
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("Add Staff User / Role", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile No.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Admin / Owner", "Cashier / Billing", "Inventory Manager", "Accountant").forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick = { role = r },
                        label = { Text(r.substringBefore(" /")) }
                    )
                }
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val added = StaffUserRecord(
                            id = "U_${System.currentTimeMillis()}",
                            name = name.trim(),
                            role = role,
                            phone = phone.trim().ifBlank { "+91 98000 00000" },
                            isActive = true
                        )
                        onSaveUsers(users + added)
                        name = ""
                        phone = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Staff User")
            }
        }
    }
}

@Composable
private fun TransactionSmsSubView(
    settings: BusinessAndAppSettings,
    latestInvoice: SalesInvoiceEntity?,
    onSaveSmsSettings: (Boolean, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var autoSend by remember(settings.autoSendTransactionSms) { mutableStateOf(settings.autoSendTransactionSms) }
    var includeGst by remember(settings.includeGstInSms) { mutableStateOf(settings.includeGstInSms) }
    var template by remember(settings.smsTemplate) { mutableStateOf(settings.smsTemplate) }

    val sampleCustomer = latestInvoice?.customerName ?: "Aarav Sharma"
    val sampleBill = latestInvoice?.billNumber ?: "INV-2026-1003"
    val sampleAmt = String.format(Locale.US, "%.2f", latestInvoice?.grandTotalAmount ?: 1450.0)

    val formattedPreview = template
        .replace("{business}", settings.businessName)
        .replace("{customer}", sampleCustomer)
        .replace("{bill_no}", sampleBill)
        .replace("{amount}", sampleAmt)

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
                text = "Transaction SMS & Digital Receipt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ReceiptFieldToggleRow("Auto-Send Transaction SMS on Invoice Save", autoSend) {
                autoSend = it
            }
            ReceiptFieldToggleRow("Include CGST/SGST Breakdown in SMS", includeGst) {
                includeGst = it
            }

            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                label = { Text("SMS Template ({business}, {customer}, {bill_no}, {amount})") },
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Live SMS Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formattedPreview, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, formattedPreview)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send Transaction SMS"))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test SMS")
                }

                Button(
                    onClick = { onSaveSmsSettings(autoSend, includeGst, template) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save SMS Config")
                }
            }
        }
    }
}

@Composable
private fun RemindersSubView(
    settings: BusinessAndAppSettings,
    invoices: List<SalesInvoiceEntity>,
    inventoryItems: List<InventoryItemEntity>,
    onSaveReminderSettings: (Boolean, Boolean, String) -> Unit,
    onSaveCustomReminders: (List<CustomReminderRecord>) -> Unit
) {
    var enablePayment by remember(settings.enablePaymentReminders) { mutableStateOf(settings.enablePaymentReminders) }
    var enableLowStock by remember(settings.enableLowStockReminders) { mutableStateOf(settings.enableLowStockReminders) }
    var frequency by remember(settings.reminderFrequency) { mutableStateOf(settings.reminderFrequency) }

    var newTitle by remember { mutableStateOf("") }
    var newParty by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }

    val pendingBills = remember(invoices) {
        invoices.filter { it.grandTotalAmount - it.cashReceived > 0.5 }
    }
    val lowStockItems = remember(inventoryItems) {
        inventoryItems.filter { it.isLowStock }
    }

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
                text = "Payment & Inventory Reminders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ReceiptFieldToggleRow("Enable Customer Payment Due Reminders", enablePayment) {
                enablePayment = it
            }
            ReceiptFieldToggleRow("Enable Low-Stock Reorder Reminders", enableLowStock) {
                enableLowStock = it
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Daily", "Every 3 Days", "Weekly").forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = {
                            frequency = freq
                            onSaveReminderSettings(enablePayment, enableLowStock, freq)
                        },
                        label = { Text(freq) }
                    )
                }
            }

            if (pendingBills.isNotEmpty()) {
                Text("Automatic Bill Balance Reminders (${pendingBills.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                pendingBills.forEach { inv ->
                    val due = inv.grandTotalAmount - inv.cashReceived
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRed.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${inv.customerName} (${inv.billNumber})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Due: ${settings.currencySymbol}${String.format(Locale.US, "%.2f", due)}", color = DangerRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (lowStockItems.isNotEmpty()) {
                Text("Low-Stock Reorder Alerts (${lowStockItems.size} Items)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                lowStockItems.take(4).forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text("Only ${item.stockQuantity.toInt()} ${item.unit} left", color = DangerRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Text("Scheduled Custom Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            settings.customReminders.forEach { rem ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rem.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${rem.partyOrItemName} • ${rem.amountOrQty} • Due: ${rem.dueDate}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = {
                                onSaveCustomReminders(settings.customReminders.filterNot { it.id == rem.id })
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete reminder", tint = DangerRed)
                        }
                    }
                }
            }

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Reminder Note") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = newParty,
                    onValueChange = { newParty = it },
                    label = { Text("Party / Item") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        val added = CustomReminderRecord(
                            id = "R_${System.currentTimeMillis()}",
                            title = newTitle.trim(),
                            partyOrItemName = newParty.trim().ifBlank { "General" },
                            amountOrQty = newAmount.trim().ifBlank { "-" },
                            dueDate = "Upcoming"
                        )
                        onSaveCustomReminders(settings.customReminders + added)
                        newTitle = ""
                        newParty = ""
                        newAmount = ""
                    }
                    onSaveReminderSettings(enablePayment, enableLowStock, frequency)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add & Save Reminder")
            }
        }
    }
}

@Composable
private fun PartyDirectorySubView(
    settings: BusinessAndAppSettings,
    invoices: List<SalesInvoiceEntity>,
    onSaveParties: (List<PartyRecord>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf("Customer") }
    var openingBal by remember { mutableStateOf("") }

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
                text = "Party Directory (Customers & Suppliers)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            settings.savedParties.forEach { party ->
                val partySales = invoices.filter { it.customerName.equals(party.name, ignoreCase = true) }.sumOf { it.grandTotalAmount }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${party.name} (${party.partyType})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Phone: ${party.phone} • GSTIN: ${party.gstin.ifBlank { "Unregistered" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (partySales > 0.0) {
                                Text(
                                    "Total Billed Sales: ${settings.currencySymbol}${String.format(Locale.US, "%,.2f", partySales)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProfitGreen
                                )
                            }
                        }
                        Text(
                            text = "${settings.currencySymbol}${String.format(Locale.US, "%,.0f", party.openingBalance)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (party.partyType == "Customer") ProfitGreen else DangerRed
                        )
                    }
                }
            }

            HorizontalDivider()
            Text("Add New Party (Customer / Supplier)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Customer", "Supplier").forEach { t ->
                    FilterChip(
                        selected = partyType == t,
                        onClick = { partyType = t },
                        label = { Text(t) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party Name *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile No.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("Party GSTIN") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = openingBal,
                    onValueChange = { openingBal = it },
                    label = { Text("Opening Bal (${settings.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newParty = PartyRecord(
                            id = "P_${System.currentTimeMillis()}",
                            name = name.trim(),
                            phone = phone.trim(),
                            gstin = gstin.trim().uppercase(Locale.US),
                            partyType = partyType,
                            openingBalance = openingBal.toDoubleOrNull() ?: 0.0
                        )
                        onSaveParties(settings.savedParties + newParty)
                        name = ""
                        phone = ""
                        gstin = ""
                        openingBal = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Party")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemSettingsSubView(
    settings: BusinessAndAppSettings,
    onSaveItemSettings: (String, Double, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var defaultUnit by remember(settings.defaultItemUnit) { mutableStateOf(settings.defaultItemUnit) }
    var lowStockAlert by remember(settings.defaultLowStockAlert) { mutableStateOf(settings.defaultLowStockAlert.toInt().toString()) }
    var mrp by remember(settings.enableFieldMrp) { mutableStateOf(settings.enableFieldMrp) }
    var batch by remember(settings.enableFieldBatchNo) { mutableStateOf(settings.enableFieldBatchNo) }
    var expiry by remember(settings.enableFieldExpiryDate) { mutableStateOf(settings.enableFieldExpiryDate) }
    var wholesale by remember(settings.enableFieldWholesalePrice) { mutableStateOf(settings.enableFieldWholesalePrice) }
    var rack by remember(settings.enableFieldRackLocation) { mutableStateOf(settings.enableFieldRackLocation) }

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
                text = "Item & Inventory Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text("Default Quantity Unit", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PCS", "KG", "BOX", "LTR", "PACK", "MTR").forEach { u ->
                    FilterChip(
                        selected = defaultUnit == u,
                        onClick = { defaultUnit = u },
                        label = { Text(u) }
                    )
                }
            }

            OutlinedTextField(
                value = lowStockAlert,
                onValueChange = { lowStockAlert = it },
                label = { Text("Default Low-Stock Alert Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text("Additional Item Fields", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ReceiptFieldToggleRow("MRP (Maximum Retail Price)", mrp) { mrp = it }
            ReceiptFieldToggleRow("Batch / Lot Number", batch) { batch = it }
            ReceiptFieldToggleRow("Expiry / Warranty Date", expiry) { expiry = it }
            ReceiptFieldToggleRow("Wholesale Price Tier", wholesale) { wholesale = it }
            ReceiptFieldToggleRow("Rack / Shelf Location", rack) { rack = it }

            Button(
                onClick = {
                    onSaveItemSettings(
                        defaultUnit,
                        lowStockAlert.toDoubleOrNull() ?: 5.0,
                        mrp,
                        batch,
                        expiry,
                        wholesale,
                        rack
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Item Settings")
            }
        }
    }
}

@Composable
private fun AppearanceAndGstCard(
    settings: BusinessAndAppSettings,
    onSetThemeMode: (String) -> Unit,
    onSetGstEnabled: (Boolean) -> Unit,
    onSetDefaultGstSlab: (Int) -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Appearance & GST Tax Defaults",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SYSTEM" to "System", "LIGHT" to "Light Mode", "DARK" to "Dark Mode").forEach { (key, label) ->
                    FilterChip(
                        selected = settings.themeMode == key,
                        onClick = { onSetThemeMode(key) },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_$key")
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GST Tax Calculation (CGST + SGST)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Enable or disable GST breakdown across invoices and receipts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.gstEnabled,
                    onCheckedChange = onSetGstEnabled,
                    modifier = Modifier.testTag("settings_gst_master_switch")
                )
            }

            if (settings.gstEnabled) {
                Text(
                    text = "Default Configurable GST Rate (50% CGST + 50% SGST)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 12, 18, 28).forEach { slab ->
                        FilterChip(
                            selected = settings.defaultGstSlab == slab,
                            onClick = { onSetDefaultGstSlab(slab) },
                            label = { Text("$slab%") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_gst_slab_$slab")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessProfileCard(
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    businessCategory: String,
    onBusinessCategoryChange: (String) -> Unit,
    gstinNumber: String,
    onGstinChange: (String) -> Unit,
    businessPhone: String,
    onPhoneChange: (String) -> Unit,
    businessAddress: String,
    onAddressChange: (String) -> Unit,
    logoUri: String,
    bannerUri: String,
    onPickLogo: () -> Unit,
    onPickBanner: () -> Unit,
    onSave: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Business Profile & Branding",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPickLogo() },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (logoUri.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(logoUri),
                                contentDescription = "Selected Business Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_business_logo),
                                contentDescription = "Default Business Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap Logo", style = MaterialTheme.typography.labelSmall)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPickBanner() }
                    ) {
                        if (bannerUri.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(bannerUri),
                                contentDescription = "Selected Shop Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_shop_banner),
                                contentDescription = "Default Shop Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap Shop Banner to Choose Photo", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickLogo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_logo_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Logo")
                }
                OutlinedButton(
                    onClick = onPickBanner,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_banner_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shop Banner")
                }
            }

            OutlinedTextField(
                value = businessName,
                onValueChange = onBusinessNameChange,
                label = { Text("Business Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_business_name_input")
            )

            OutlinedTextField(
                value = businessCategory,
                onValueChange = onBusinessCategoryChange,
                label = { Text("Business Category") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_business_category_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gstinNumber,
                    onValueChange = onGstinChange,
                    label = { Text("GSTIN Number") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = businessPhone,
                    onValueChange = onPhoneChange,
                    label = { Text("Contact Phone") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = businessAddress,
                onValueChange = onAddressChange,
                label = { Text("Shop Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_business_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Business Profile")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThermalPrintingCard(
    paperWidth: String,
    onPaperWidthChange: (String) -> Unit,
    printerName: String,
    printerAddress: String,
    pairedDevices: List<PairedPrinterDevice>,
    onScanBluetooth: () -> Unit,
    onSelectDevice: (PairedPrinterDevice) -> Unit,
    printHeader: Boolean,
    onPrintHeaderChange: (Boolean) -> Unit,
    printCustomer: Boolean,
    onPrintCustomerChange: (Boolean) -> Unit,
    printHsn: Boolean,
    onPrintHsnChange: (Boolean) -> Unit,
    printGst: Boolean,
    onPrintGstChange: (Boolean) -> Unit,
    printCash: Boolean,
    onPrintCashChange: (Boolean) -> Unit,
    printBarcode: Boolean,
    onPrintBarcodeChange: (Boolean) -> Unit,
    footerMessage: String,
    onFooterMessageChange: (String) -> Unit,
    onPreviewSampleReceipt: () -> Unit,
    onSave: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bluetooth Thermal Printing",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(
                text = "Select Thermal Roll Width",
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paperWidth == "58mm",
                    onClick = { onPaperWidthChange("58mm") },
                    label = { Text("58mm / 2-Inch (32 chars)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_paper_58mm")
                )
                FilterChip(
                    selected = paperWidth == "80mm",
                    onClick = { onPaperWidthChange("80mm") },
                    label = { Text("80mm / 3-Inch (48 chars)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_paper_80mm")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selected Printer: $printerName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = printerAddress.ifBlank { "No MAC address selected" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onScanBluetooth,
                    modifier = Modifier.testTag("scan_bluetooth_printers_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Paired")
                }
            }

            if (pairedDevices.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pairedDevices.forEach { dev: PairedPrinterDevice ->
                        FilterChip(
                            selected = printerAddress == dev.address,
                            onClick = { onSelectDevice(dev) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("${dev.name} (${dev.address})") }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Customizable Thermal Receipt Fields",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            ReceiptFieldToggleRow("Print Business Header (Name, Category, GSTIN)", printHeader, onPrintHeaderChange)
            ReceiptFieldToggleRow("Print Customer Name & Mobile Number", printCustomer, onPrintCustomerChange)
            ReceiptFieldToggleRow("Print Item HSN Code Tag", printHsn, onPrintHsnChange)
            ReceiptFieldToggleRow("Print CGST & SGST Tax Breakdown", printGst, onPrintGstChange)
            ReceiptFieldToggleRow("Print Cash Received & Change Returned", printCash, onPrintCashChange)
            ReceiptFieldToggleRow("Print Invoice Barcode & Footer", printBarcode, onPrintBarcodeChange)

            OutlinedTextField(
                value = footerMessage,
                onValueChange = onFooterMessageChange,
                label = { Text("Custom Receipt Footer Message") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPreviewSampleReceipt,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_thermal_receipt_button")
                ) {
                    Text("Preview Receipt")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_thermal_printer_button")
                ) {
                    Text("Save Printer Config")
                }
            }
        }
    }
}

@Composable
private fun DataBackupAndRestoreCard(
    onTriggerPdfBackup: () -> Unit,
    onTriggerExcelBackup: () -> Unit,
    onTriggerJsonExport: () -> Unit,
    onTriggerJsonImport: () -> Unit
) {
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
                text = "Data Backup, Export & Restore",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Export sales & inventory ledgers in PDF or Excel (CSV), or back up and restore your complete POS database in JSON.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onTriggerPdfBackup,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_backup_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup PDF")
                }

                Button(
                    onClick = onTriggerExcelBackup,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_backup_excel_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup Excel")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onTriggerJsonExport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_export_json_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Data (JSON)")
                }

                OutlinedButton(
                    onClick = onTriggerJsonImport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_import_json_button")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Data (JSON)")
                }
            }
        }
    }
}

@Composable
private fun ReceiptFieldToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
