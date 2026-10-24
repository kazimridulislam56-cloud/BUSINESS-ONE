package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BankAccountRecord
import com.example.data.preferences.BusinessAndAppSettings
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ProfitGreen
import com.example.util.ItemPerformanceStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BusinessMoreOption {
    BANK_ACCOUNTS,
    DAY_BOOK,
    ALL_TXNS_REPORT,
    PROFIT_AND_LOSS,
    BALANCE_SHEET,
    BILLWISE_PNL,
    PRINT_SETTINGS,
    TXN_SMS_SETTINGS,
    RECYCLE_BIN
}

enum class InventoryMoreOption {
    IMPORT_ITEMS,
    EXPORT_ITEMS,
    ITEM_WISE_PNL,
    ADDITIONAL_FIELDS,
    ITEM_DETAILS,
    LOW_STOCK_SUMMARY
}

/**
 * Reusable icon tile styled after the "More Options" screenshots:
 * A crisp line icon resting on a soft sky-blue rounded base pedestal with label underneath.
 */
@Composable
private fun MoreOptionGridTile(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft sky-blue pedestal base matching screenshot aesthetic
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(54.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE0F2FE))
            )
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF334155),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.TopCenter)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Screenshot 2: "More Options" 3x3 Accounting, Reports & Settings Sheet
 * (Bank Accounts, Day Book, All Txns Report, Profit & Loss, Balance Sheet,
 * Billwise PnL, Print Settings, Txn SMS Settings, Recycle Bin)
 */
@Composable
fun BusinessMoreOptionsDialog(
    onSelectOption: (BusinessMoreOption) -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onExportJsonBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("business_more_options_sheet"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            ) {
                // Header: "More Options" + Close X
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More Options",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_more_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close More Options"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Row 1: Bank Accounts | Day Book | All Txns Report
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoreOptionGridTile(
                        icon = Icons.Default.AccountBalance,
                        label = "Bank Accounts",
                        testTag = "more_opt_bank_accounts",
                        onClick = { onSelectOption(BusinessMoreOption.BANK_ACCOUNTS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.MenuBook,
                        label = "Day Book",
                        testTag = "more_opt_day_book",
                        onClick = { onSelectOption(BusinessMoreOption.DAY_BOOK) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = "All Txns Report",
                        testTag = "more_opt_all_txns",
                        onClick = { onSelectOption(BusinessMoreOption.ALL_TXNS_REPORT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Profit & Loss | Balance Sheet | Billwise PnL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoreOptionGridTile(
                        icon = Icons.Default.SwapVert,
                        label = "Profit & Loss",
                        testTag = "more_opt_profit_loss",
                        onClick = { onSelectOption(BusinessMoreOption.PROFIT_AND_LOSS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Balance Sheet",
                        testTag = "more_opt_balance_sheet",
                        onClick = { onSelectOption(BusinessMoreOption.BALANCE_SHEET) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.TrendingUp,
                        label = "Billwise PnL",
                        testTag = "more_opt_billwise_pnl",
                        onClick = { onSelectOption(BusinessMoreOption.BILLWISE_PNL) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Print Settings | Txn SMS Settings | Recycle Bin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoreOptionGridTile(
                        icon = Icons.Default.Print,
                        label = "Print Settings",
                        testTag = "more_opt_print_settings",
                        onClick = { onSelectOption(BusinessMoreOption.PRINT_SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.Sms,
                        label = "Txn SMS Settings",
                        testTag = "more_opt_sms_settings",
                        onClick = { onSelectOption(BusinessMoreOption.TXN_SMS_SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.RestoreFromTrash,
                        label = "Recycle Bin",
                        testTag = "more_opt_recycle_bin",
                        onClick = { onSelectOption(BusinessMoreOption.RECYCLE_BIN) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Export Strip (PDF, Excel, JSON)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onExportExcel,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onExportJsonBackup,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Screenshot 3: "More Options" 3x2 Inventory & Items Sheet
 * (Import Items, Export Items, Item wise PnL, Additional Fields, Item Details, Low Stock Summary)
 */
@Composable
fun InventoryMoreOptionsDialog(
    onSelectOption: (InventoryMoreOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("inventory_more_options_sheet"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            ) {
                // Header: "More Options" + Close X
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More Options",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_inventory_more_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Inventory More Options"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Row 1: Import Items | Export Items | Item wise PnL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoreOptionGridTile(
                        icon = Icons.Default.FileDownload,
                        label = "Import Items",
                        testTag = "inv_opt_import_items",
                        onClick = { onSelectOption(InventoryMoreOption.IMPORT_ITEMS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.FileUpload,
                        label = "Export Items",
                        testTag = "inv_opt_export_items",
                        onClick = { onSelectOption(InventoryMoreOption.EXPORT_ITEMS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.SwapVert,
                        label = "Item wise PnL",
                        testTag = "inv_opt_item_wise_pnl",
                        onClick = { onSelectOption(InventoryMoreOption.ITEM_WISE_PNL) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Additional Fields | Item Details | Low Stock Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoreOptionGridTile(
                        icon = Icons.Default.AddCircleOutline,
                        label = "Additional Fields",
                        testTag = "inv_opt_additional_fields",
                        onClick = { onSelectOption(InventoryMoreOption.ADDITIONAL_FIELDS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        label = "Item Details",
                        testTag = "inv_opt_item_details",
                        onClick = { onSelectOption(InventoryMoreOption.ITEM_DETAILS) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreOptionGridTile(
                        icon = Icons.Default.TrendingDown,
                        label = "Low Stock Summary",
                        testTag = "inv_opt_low_stock_summary",
                        onClick = { onSelectOption(InventoryMoreOption.LOW_STOCK_SUMMARY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Full-featured modal dispatcher for all Business More Options (Bank Accounts, Day Book,
 * All Txns Report, Profit & Loss, Balance Sheet, Billwise PnL, Recycle Bin).
 */
@Composable
fun BusinessMoreOptionFeatureModal(
    option: BusinessMoreOption,
    settings: BusinessAndAppSettings,
    invoices: List<SalesInvoiceEntity>,
    lineItems: List<InvoiceLineItemEntity>,
    inventoryItems: List<InventoryItemEntity>,
    deletedInvoices: List<Pair<SalesInvoiceEntity, List<InvoiceLineItemEntity>>>,
    deletedInventoryItems: List<InventoryItemEntity>,
    onSaveBankAccounts: (List<BankAccountRecord>) -> Unit,
    onOpenInvoiceReceipt: (SalesInvoiceEntity) -> Unit,
    onExportPdf: (String, List<SalesInvoiceEntity>) -> Unit,
    onExportExcel: (List<SalesInvoiceEntity>) -> Unit,
    onRestoreInvoice: (SalesInvoiceEntity, List<InvoiceLineItemEntity>) -> Unit,
    onRestoreItem: (InventoryItemEntity) -> Unit,
    onClearRecycleBin: () -> Unit,
    onDismiss: () -> Unit
) {
    val currency = settings.currencySymbol
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                when (option) {
                    BusinessMoreOption.BANK_ACCOUNTS -> {
                        BankAccountsContent(
                            accounts = settings.bankAccounts,
                            invoices = invoices,
                            currency = currency,
                            onSaveBankAccounts = onSaveBankAccounts,
                            onDismiss = onDismiss
                        )
                    }
                    BusinessMoreOption.DAY_BOOK -> {
                        DayBookContent(
                            invoices = invoices,
                            currency = currency,
                            onOpenInvoiceReceipt = onOpenInvoiceReceipt,
                            onDismiss = onDismiss
                        )
                    }
                    BusinessMoreOption.ALL_TXNS_REPORT -> {
                        AllTransactionsContent(
                            invoices = invoices,
                            currency = currency,
                            onOpenInvoiceReceipt = onOpenInvoiceReceipt,
                            onExportPdf = { filtered -> onExportPdf("All Transactions Report", filtered) },
                            onExportExcel = onExportExcel,
                            onDismiss = onDismiss
                        )
                    }
                    BusinessMoreOption.PROFIT_AND_LOSS -> {
                        ProfitAndLossContent(
                            invoices = invoices,
                            lineItems = lineItems,
                            currency = currency,
                            onDismiss = onDismiss
                        )
                    }
                    BusinessMoreOption.BALANCE_SHEET -> {
                        BalanceSheetContent(
                            settings = settings,
                            invoices = invoices,
                            lineItems = lineItems,
                            inventoryItems = inventoryItems,
                            currency = currency,
                            onDismiss = onDismiss
                        )
                    }
                    BusinessMoreOption.BILLWISE_PNL -> {
                        BillwisePnLContent(
                            invoices = invoices,
                            lineItems = lineItems,
                            currency = currency,
                            onOpenInvoiceReceipt = onOpenInvoiceReceipt,
                            onDismiss = onDismiss
                        )
                    }
                    BusinessMoreOption.RECYCLE_BIN -> {
                        RecycleBinContent(
                            deletedInvoices = deletedInvoices,
                            deletedItems = deletedInventoryItems,
                            currency = currency,
                            onRestoreInvoice = onRestoreInvoice,
                            onRestoreItem = onRestoreItem,
                            onClearRecycleBin = onClearRecycleBin,
                            onDismiss = onDismiss
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ModalHeaderBar(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun BankAccountsContent(
    accounts: List<BankAccountRecord>,
    invoices: List<SalesInvoiceEntity>,
    currency: String,
    onSaveBankAccounts: (List<BankAccountRecord>) -> Unit,
    onDismiss: () -> Unit
) {
    var accName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accNoOrUpi by remember { mutableStateOf("") }
    var openingBal by remember { mutableStateOf("") }

    val digitalInflow = remember(invoices) {
        invoices.filter { it.paymentMode != "Cash" }.sumOf { it.cashReceived }
    }
    val totalOpeningBank = remember(accounts) { accounts.sumOf { it.openingBalance } }
    val totalCurrentBank = totalOpeningBank + digitalInflow

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "Bank & UPI Accounts",
            subtitle = "Track bank balances and UPI / Card digital sales inflows",
            onDismiss = onDismiss
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Bank & UPI Balance", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "$currency${String.format(Locale.US, "%,.2f", totalCurrentBank)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UPI / Card Inflow", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "+$currency${String.format(Locale.US, "%,.2f", digitalInflow)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
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
                            Text(acc.accountName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${acc.bankName} • ${acc.accountNumberOrUpi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$currency${String.format(Locale.US, "%,.2f", acc.openingBalance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        HorizontalDivider()
        Text("Add Bank / UPI Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = accName,
                onValueChange = { accName = it },
                label = { Text("Account Label") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = accNoOrUpi,
                onValueChange = { accNoOrUpi = it },
                label = { Text("A/C No. or UPI ID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = openingBal,
                onValueChange = { openingBal = it },
                label = { Text("Opening ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(0.9f)
            )
        }
        Button(
            onClick = {
                if (accName.isNotBlank()) {
                    val newAcc = BankAccountRecord(
                        id = "B_${System.currentTimeMillis()}",
                        accountName = accName.trim(),
                        bankName = bankName.trim().ifBlank { "Business Bank" },
                        accountNumberOrUpi = accNoOrUpi.trim().ifBlank { "UPI/Current" },
                        openingBalance = openingBal.toDoubleOrNull() ?: 0.0
                    )
                    onSaveBankAccounts(accounts + newAcc)
                    accName = ""
                    bankName = ""
                    accNoOrUpi = ""
                    openingBal = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Bank / UPI Account")
        }
    }
}

@Composable
private fun DayBookContent(
    invoices: List<SalesInvoiceEntity>,
    currency: String,
    onOpenInvoiceReceipt: (SalesInvoiceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val dates = remember(invoices) {
        val d = invoices.map { it.dateString }.distinct().sortedDescending()
        if (d.isEmpty()) listOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) else d
    }
    var selectedDate by remember(dates) { mutableStateOf(dates.first()) }
    val dayInvoices = remember(invoices, selectedDate) {
        invoices.filter { it.dateString == selectedDate }
    }
    val dayTotalSales = dayInvoices.sumOf { it.grandTotalAmount }
    val dayCashIn = dayInvoices.filter { it.paymentMode == "Cash" || it.paymentMode == "Part-Cash" }.sumOf { it.cashReceived }
    val dayBankIn = dayInvoices.filter { it.paymentMode != "Cash" && it.paymentMode != "Part-Cash" }.sumOf { it.cashReceived }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "Day Book",
            subtitle = "Daily chronological register of sales, cash & digital receipts",
            onDismiss = onDismiss
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dates.forEach { d ->
                FilterChip(
                    selected = selectedDate == d,
                    onClick = { selectedDate = d },
                    label = { Text(d) }
                )
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Day Sales ($selectedDate)", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$currency${String.format(Locale.US, "%,.2f", dayTotalSales)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Cash In", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$currency${String.format(Locale.US, "%,.0f", dayCashIn)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UPI / Card In", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$currency${String.format(Locale.US, "%,.0f", dayBankIn)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dayInvoices, key = { it.id }) { inv ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInvoiceReceipt(inv) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${inv.billNumber} • ${inv.customerName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Mode: ${inv.paymentMode} • Qty: ${inv.totalQuantity.toInt()} • Received: $currency${String.format(Locale.US, "%.0f", inv.cashReceived)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "$currency${String.format(Locale.US, "%,.2f", inv.grandTotalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllTransactionsContent(
    invoices: List<SalesInvoiceEntity>,
    currency: String,
    onOpenInvoiceReceipt: (SalesInvoiceEntity) -> Unit,
    onExportPdf: (List<SalesInvoiceEntity>) -> Unit,
    onExportExcel: (List<SalesInvoiceEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    var modeFilter by remember { mutableStateOf("All") }
    val filtered = remember(invoices, modeFilter) {
        if (modeFilter == "All") invoices else invoices.filter { it.paymentMode == modeFilter }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "All Transactions Report",
            subtitle = "${filtered.size} total transactions • Tap any bill to view thermal receipt",
            onDismiss = onDismiss
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Cash", "UPI / QR", "Card", "Part-Cash").forEach { mode ->
                FilterChip(
                    selected = modeFilter == mode,
                    onClick = { modeFilter = mode },
                    label = { Text(mode) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onExportPdf(filtered) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF")
            }
            OutlinedButton(onClick = { onExportExcel(filtered) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Excel")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { inv ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInvoiceReceipt(inv) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${inv.billNumber} • ${inv.customerName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${inv.dateString} • ${inv.paymentMode} • GST: $currency${String.format(Locale.US, "%.2f", inv.totalTaxAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "$currency${String.format(Locale.US, "%,.2f", inv.grandTotalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfitAndLossContent(
    invoices: List<SalesInvoiceEntity>,
    lineItems: List<InvoiceLineItemEntity>,
    currency: String,
    onDismiss: () -> Unit
) {
    val grossItemSales = lineItems.sumOf { it.grossAmount.takeIf { g -> g > 0.0 } ?: (it.unitPrice * it.quantity) }
    val totalDiscountsGiven = invoices.sumOf { it.discountAmount }
    val netTaxableSales = invoices.sumOf { it.subtotalAmount }
    val totalCogs = lineItems.sumOf { it.purchaseRate * it.quantity }
    val grossProfit = netTaxableSales - totalCogs
    val totalGstCollected = invoices.sumOf { it.totalTaxAmount }
    val marginPct = if (netTaxableSales > 0.0) (grossProfit / netTaxableSales) * 100.0 else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "Profit & Loss Statement",
            subtitle = "Real-time revenue, cost of goods sold (COGS) & net profit analysis",
            onDismiss = onDismiss
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PnLRow("Gross Item Sales Value", "$currency${String.format(Locale.US, "%,.2f", grossItemSales)}")
                PnLRow("Less: Item & Bill Discounts", "- $currency${String.format(Locale.US, "%,.2f", totalDiscountsGiven)}")
                HorizontalDivider()
                PnLRow("Net Taxable Sales Revenue", "$currency${String.format(Locale.US, "%,.2f", netTaxableSales)}", bold = true)
                PnLRow("Less: Cost of Goods Sold (Purchase Cost)", "- $currency${String.format(Locale.US, "%,.2f", totalCogs)}")
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NET OPERATING PROFIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$currency${String.format(Locale.US, "%,.2f", grossProfit)} (${String.format(Locale.US, "%.1f", marginPct)}%)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (grossProfit >= 0.0) ProfitGreen else DangerRed
                    )
                }
                HorizontalDivider()
                PnLRow("GST Tax Collected (CGST + SGST)", "$currency${String.format(Locale.US, "%,.2f", totalGstCollected)}")
                PnLRow("Gross Invoice Billed (Incl. GST)", "$currency${String.format(Locale.US, "%,.2f", invoices.sumOf { it.grandTotalAmount })}")
            }
        }
    }
}

@Composable
private fun BalanceSheetContent(
    settings: BusinessAndAppSettings,
    invoices: List<SalesInvoiceEntity>,
    lineItems: List<InvoiceLineItemEntity>,
    inventoryItems: List<InventoryItemEntity>,
    currency: String,
    onDismiss: () -> Unit
) {
    val cashInHand = settings.openingCashInHand + invoices.filter { it.paymentMode == "Cash" || it.paymentMode == "Part-Cash" }.sumOf { it.cashReceived }
    val bankBalance = settings.bankAccounts.sumOf { it.openingBalance } + invoices.filter { it.paymentMode != "Cash" && it.paymentMode != "Part-Cash" }.sumOf { it.cashReceived }
    val closingInventoryVal = inventoryItems.sumOf { it.stockValueAtPurchase }
    val customerReceivables = invoices.sumOf { (it.grandTotalAmount - it.cashReceived).coerceAtLeast(0.0) } +
        settings.savedParties.filter { it.partyType == "Customer" }.sumOf { it.openingBalance }
    val totalAssets = cashInHand + bankBalance + closingInventoryVal + customerReceivables

    val netProfit = invoices.sumOf { it.subtotalAmount } - lineItems.sumOf { it.purchaseRate * it.quantity }
    val gstPayable = invoices.sumOf { it.totalTaxAmount }
    val supplierPayables = settings.savedParties.filter { it.partyType == "Supplier" }.sumOf { it.openingBalance }
    val openingCapital = (totalAssets - netProfit - gstPayable - supplierPayables).coerceAtLeast(0.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "Balance Sheet",
            subtitle = "Assets vs Capital & Liabilities financial position",
            onDismiss = onDismiss
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("ASSETS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                PnLRow("Cash in Hand", "$currency${String.format(Locale.US, "%,.2f", cashInHand)}")
                PnLRow("Bank & UPI Accounts Balance", "$currency${String.format(Locale.US, "%,.2f", bankBalance)}")
                PnLRow("Closing Inventory Valuation (At Cost)", "$currency${String.format(Locale.US, "%,.2f", closingInventoryVal)}")
                PnLRow("Sundry Debtors (Customer Receivables)", "$currency${String.format(Locale.US, "%,.2f", customerReceivables)}")
                HorizontalDivider()
                PnLRow("Total Assets", "$currency${String.format(Locale.US, "%,.2f", totalAssets)}", bold = true)
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("CAPITAL & LIABILITIES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                PnLRow("Owner's Capital & Opening Reserves", "$currency${String.format(Locale.US, "%,.2f", openingCapital)}")
                PnLRow("Retained Net Operating Profit", "$currency${String.format(Locale.US, "%,.2f", netProfit)}")
                PnLRow("GST Tax Output Liability (CGST + SGST)", "$currency${String.format(Locale.US, "%,.2f", gstPayable)}")
                PnLRow("Sundry Creditors (Supplier Payables)", "$currency${String.format(Locale.US, "%,.2f", supplierPayables)}")
                HorizontalDivider()
                PnLRow("Total Capital & Liabilities", "$currency${String.format(Locale.US, "%,.2f", totalAssets)}", bold = true)
            }
        }
    }
}

@Composable
private fun BillwisePnLContent(
    invoices: List<SalesInvoiceEntity>,
    lineItems: List<InvoiceLineItemEntity>,
    currency: String,
    onOpenInvoiceReceipt: (SalesInvoiceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val linesByInvoice = remember(lineItems) { lineItems.groupBy { it.invoiceId } }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "Billwise Profit & Loss",
            subtitle = "Exact sale value, purchase cost & net profit earned on each invoice",
            onDismiss = onDismiss
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(invoices, key = { it.id }) { inv ->
                val invLines = linesByInvoice[inv.id].orEmpty()
                val billCost = invLines.sumOf { it.purchaseRate * it.quantity }
                val billProfit = inv.subtotalAmount - billCost
                val billMargin = if (inv.subtotalAmount > 0.0) (billProfit / inv.subtotalAmount) * 100.0 else 0.0

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInvoiceReceipt(inv) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${inv.billNumber} • ${inv.customerName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Profit: +$currency${String.format(Locale.US, "%.2f", billProfit)} (${String.format(Locale.US, "%.0f", billMargin)}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (billProfit >= 0.0) ProfitGreen else DangerRed
                            )
                        }
                        Text(
                            text = "Date: ${inv.dateString} • Taxable Sale: $currency${String.format(Locale.US, "%.2f", inv.subtotalAmount)} • Purchase Cost: $currency${String.format(Locale.US, "%.2f", billCost)} • Disc: $currency${String.format(Locale.US, "%.2f", inv.discountAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecycleBinContent(
    deletedInvoices: List<Pair<SalesInvoiceEntity, List<InvoiceLineItemEntity>>>,
    deletedItems: List<InventoryItemEntity>,
    currency: String,
    onRestoreInvoice: (SalesInvoiceEntity, List<InvoiceLineItemEntity>) -> Unit,
    onRestoreItem: (InventoryItemEntity) -> Unit,
    onClearRecycleBin: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalHeaderBar(
            title = "Recycle Bin",
            subtitle = "Restore recently deleted invoices and inventory items",
            onDismiss = onDismiss
        )

        if (deletedInvoices.isEmpty() && deletedItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.RestoreFromTrash,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recycle Bin is Empty", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Any invoice or inventory item you delete will appear here for safe recovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deletedInvoices, key = { "inv_${it.first.billNumber}_${it.first.timestamp}" }) { (inv, lines) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                Text("Deleted Bill: ${inv.billNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${inv.customerName} • $currency${String.format(Locale.US, "%.2f", inv.grandTotalAmount)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            FilledTonalButton(onClick = { onRestoreInvoice(inv, lines) }) {
                                Text("Restore")
                            }
                        }
                    }
                }

                items(deletedItems, key = { "item_${it.id}_${it.barcode}" }) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                Text("Deleted Item: ${item.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "QR: ${item.barcode} • Rate: $currency${item.salesRate}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            FilledTonalButton(onClick = { onRestoreItem(item) }) {
                                Text("Restore")
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onClearRecycleBin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Empty Recycle Bin Permanently")
            }
        }
    }
}

/**
 * Full-featured modal dispatcher for all Inventory More Options (Import Items, Export Items,
 * Item wise PnL, Additional Fields, Item Details, Low Stock Summary).
 */
@Composable
fun InventoryMoreOptionFeatureModal(
    option: InventoryMoreOption,
    settings: BusinessAndAppSettings,
    inventoryItems: List<InventoryItemEntity>,
    itemPerformanceStats: List<ItemPerformanceStat>,
    onImportJsonFile: () -> Unit,
    onImportSampleCatalogPack: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onSaveItemSettings: (String, Double, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onRestockItem: (InventoryItemEntity, Double) -> Unit,
    onRestockAllLow: () -> Unit,
    onDismiss: () -> Unit
) {
    val currency = settings.currencySymbol
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (option) {
                    InventoryMoreOption.IMPORT_ITEMS -> {
                        ModalHeaderBar(
                            title = "Import Items",
                            subtitle = "Import inventory SKUs from JSON backup or bulk retail pack",
                            onDismiss = onDismiss
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                onImportJsonFile()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select JSON Backup File to Import")
                        }
                        FilledTonalButton(
                            onClick = {
                                onImportSampleCatalogPack()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("One-Tap Import Retail SKU Pack (3 Items)")
                        }
                    }

                    InventoryMoreOption.EXPORT_ITEMS -> {
                        ModalHeaderBar(
                            title = "Export Items",
                            subtitle = "Export your complete inventory master (${inventoryItems.size} SKUs)",
                            onDismiss = onDismiss
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                onExportExcel()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Items to Excel (CSV)")
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onExportPdf()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Inventory Valuation PDF")
                        }
                    }

                    InventoryMoreOption.ITEM_WISE_PNL -> {
                        ModalHeaderBar(
                            title = "Item-Wise Profit & Loss",
                            subtitle = "Profitability & sales margin across ${itemPerformanceStats.size} items",
                            onDismiss = onDismiss
                        )
                        val totalItemProfit = itemPerformanceStats.sumOf { it.netProfit }
                        Text(
                            text = "Total Item Net Profit: $currency${String.format(Locale.US, "%,.2f", totalItemProfit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(itemPerformanceStats, key = { "${it.itemId}_${it.itemName}" }) { stat ->
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
                                            Text(stat.itemName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Sold: ${stat.unitsSold.toInt()} ${stat.unit} • Rev: $currency${String.format(Locale.US, "%.0f", stat.grossRevenue)} • Cost: $currency${String.format(Locale.US, "%.0f", stat.estimatedCost)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "+$currency${String.format(Locale.US, "%,.2f", stat.netProfit)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    InventoryMoreOption.ADDITIONAL_FIELDS -> {
                        var mrp by remember { mutableStateOf(settings.enableFieldMrp) }
                        var batch by remember { mutableStateOf(settings.enableFieldBatchNo) }
                        var expiry by remember { mutableStateOf(settings.enableFieldExpiryDate) }
                        var wholesale by remember { mutableStateOf(settings.enableFieldWholesalePrice) }
                        var rack by remember { mutableStateOf(settings.enableFieldRackLocation) }

                        ModalHeaderBar(
                            title = "Additional Item Fields",
                            subtitle = "Enable custom metadata fields for inventory items",
                            onDismiss = onDismiss
                        )
                        ToggleSettingRow("MRP (Maximum Retail Price)", mrp) { mrp = it }
                        ToggleSettingRow("Batch / Lot Number", batch) { batch = it }
                        ToggleSettingRow("Manufacturing / Expiry Date", expiry) { expiry = it }
                        ToggleSettingRow("Wholesale / Distributor Rate", wholesale) { wholesale = it }
                        ToggleSettingRow("Rack / Shelf Storage Location", rack) { rack = it }

                        Button(
                            onClick = {
                                onSaveItemSettings(
                                    settings.defaultItemUnit,
                                    settings.defaultLowStockAlert,
                                    mrp,
                                    batch,
                                    expiry,
                                    wholesale,
                                    rack
                                )
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Additional Fields")
                        }
                    }

                    InventoryMoreOption.ITEM_DETAILS -> {
                        ModalHeaderBar(
                            title = "Item Details & Valuation",
                            subtitle = "Stock count, purchase cost & retail valuation across all SKUs",
                            onDismiss = onDismiss
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(inventoryItems, key = { it.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${item.stockQuantity.toInt()} ${item.unit}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.isLowStock) DangerRed else ProfitGreen
                                            )
                                        }
                                        Text(
                                            "QR: ${item.barcode} • HSN: ${item.hsnCode} • Purchase: $currency${item.purchaseRate} • Sale: $currency${item.salesRate} • Stock Val: $currency${String.format(Locale.US, "%,.0f", item.stockValueAtPurchase)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    InventoryMoreOption.LOW_STOCK_SUMMARY -> {
                        val lowItems = remember(inventoryItems) { inventoryItems.filter { it.isLowStock } }
                        ModalHeaderBar(
                            title = "Low Stock Summary",
                            subtitle = "${lowItems.size} items at or below minimum stock alert threshold",
                            onDismiss = onDismiss
                        )
                        if (lowItems.isEmpty()) {
                            Text(
                                "All inventory items are well stocked above their minimum alert thresholds!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfitGreen
                            )
                        } else {
                            Button(
                                onClick = {
                                    onRestockAllLow()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Restock All Low-Stock Items (+10 Units Each)")
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(lowItems, key = { it.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DangerRed.copy(alpha = 0.1f),
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
                                                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Current: ${item.stockQuantity.toInt()} ${item.unit} (Min Alert: ${item.minStockAlert.toInt()} ${item.unit})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DangerRed
                                                )
                                            }
                                            FilledTonalButton(onClick = { onRestockItem(item, 10.0) }) {
                                                Text("+10 Stock")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PnLRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun ToggleSettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
