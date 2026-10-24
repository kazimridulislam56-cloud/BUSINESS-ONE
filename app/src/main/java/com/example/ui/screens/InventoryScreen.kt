package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.InventoryItemEntity
import com.example.ui.InventorySortOption
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ProfitGreen
import com.example.util.BarcodeCanvas
import com.example.util.BarcodeScannerDialog
import com.example.util.QrCodeCanvas
import com.example.util.ScannerMode
import java.util.Locale

@Composable
fun InventoryScreen(
    allItems: List<InventoryItemEntity>,
    filteredItems: List<InventoryItemEntity>,
    searchQuery: String,
    selectedCategory: String,
    sortOption: InventorySortOption,
    currencySymbol: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSortOptionChange: (InventorySortOption) -> Unit,
    onSaveItem: (Long, String, String, String, String, String, Double, Double, Double, Double, Int) -> Unit,
    onAdjustStock: (InventoryItemEntity, Double) -> Unit,
    onDeleteItem: (InventoryItemEntity) -> Unit,
    onSellItemNow: (InventoryItemEntity) -> Unit,
    onOpenMoreOptions: () -> Unit = {}
) {
    var editingItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var prefilledBarcode by remember { mutableStateOf("") }
    var showInventoryScanner by remember { mutableStateOf(false) }
    var showDirectQrEntryScanner by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }

    val categories = remember(allItems) {
        val dynamic = allItems.map { it.category }.distinct().sorted()
        listOf("All") + (dynamic + listOf("Electronics", "Groceries", "Stationery", "Home & Kitchen", "Hardware", "Beverages")).distinct()
    }

    if (showInventoryScanner) {
        BarcodeScannerDialog(
            title = "Scan Item QR / Barcode",
            subtitle = "Find an existing item in stock or register a new QR code item",
            mode = ScannerMode.ITEM_ENTRY,
            inventoryItems = allItems,
            onBarcodeScanned = { scannedCode ->
                showInventoryScanner = false
                val existing = allItems.firstOrNull { it.barcode.equals(scannedCode, ignoreCase = true) }
                if (existing != null) {
                    onSearchQueryChange(scannedCode)
                } else {
                    editingItem = null
                    prefilledBarcode = scannedCode
                    showAddOrEditDialog = true
                }
            },
            onDismiss = { showInventoryScanner = false }
        )
    }

    if (showDirectQrEntryScanner) {
        BarcodeScannerDialog(
            title = "Scan QR Code for New Item Entry",
            subtitle = "Scan or generate a QR code to pre-fill the new item entry form",
            mode = ScannerMode.ITEM_ENTRY,
            inventoryItems = allItems,
            onBarcodeScanned = { scannedCode ->
                showDirectQrEntryScanner = false
                editingItem = null
                prefilledBarcode = scannedCode
                showAddOrEditDialog = true
            },
            onDismiss = { showDirectQrEntryScanner = false }
        )
    }

    if (showAddOrEditDialog) {
        AddOrEditInventoryModal(
            initialItem = editingItem,
            prefilledBarcode = prefilledBarcode,
            allItems = allItems,
            currencySymbol = currencySymbol,
            onSave = { id, name, barcode, category, unit, hsn, purchaseRate, salesRate, stock, minAlert, gst ->
                onSaveItem(id, name, barcode, category, unit, hsn, purchaseRate, salesRate, stock, minAlert, gst)
                showAddOrEditDialog = false
                editingItem = null
                prefilledBarcode = ""
            },
            onDismiss = {
                showAddOrEditDialog = false
                editingItem = null
                prefilledBarcode = ""
            }
        )
    }

    val totalStockCount = remember(filteredItems) { filteredItems.sumOf { it.stockQuantity } }
    val totalPurchaseValuation = remember(filteredItems) { filteredItems.sumOf { it.stockValueAtPurchase } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_screen")
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Top Inventory Valuation & Add Item / QR Entry / More Options Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inventory Master (${filteredItems.size} SKUs)",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Total Stock: ${String.format(Locale.US, "%,.0f", totalStockCount)} Units • Value: $currencySymbol${String.format(Locale.US, "%,.0f", totalPurchaseValuation)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { showDirectQrEntryScanner = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("qr_add_inventory_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("QR Entry")
                }

                Button(
                    onClick = {
                        editingItem = null
                        prefilledBarcode = ""
                        showAddOrEditDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("open_add_inventory_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }

                IconButton(
                    onClick = onOpenMoreOptions,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .testTag("inventory_more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Inventory More Options",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Search Bar + QR Scanner + Sort-By Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search name, QR code, barcode, HSN…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inventory_search_input")
            )

            IconButton(
                onClick = { showInventoryScanner = true },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("inventory_qr_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR or Barcode in Inventory",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Box {
                OutlinedButton(
                    onClick = { showSortDropdown = true },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("inventory_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sort Inventory",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort")
                }

                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false }
                ) {
                    InventorySortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                showSortDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Category Filter Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory.equals(category, ignoreCase = true),
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Inventory Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredItems, key = { it.id }) { item ->
                InventoryItemCard(
                    item = item,
                    currencySymbol = currencySymbol,
                    onAdjustStock = { delta -> onAdjustStock(item, delta) },
                    onEdit = {
                        editingItem = item
                        showAddOrEditDialog = true
                    },
                    onDelete = { onDeleteItem(item) },
                    onSellNow = { onSellItemNow(item) }
                )
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItemEntity,
    currencySymbol: String,
    onAdjustStock: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSellNow: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_card_${item.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "HSN: ${item.hsnCode.ifBlank { "N/A" }} • GST ${item.gstRate}%",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.isLowStock) {
                        DangerRed.copy(alpha = 0.14f)
                    } else {
                        ProfitGreen.copy(alpha = 0.14f)
                    }
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.0f", item.stockQuantity)} ${item.unit} in Stock",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (item.isLowStock) DangerRed else ProfitGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Purchase Rate, Sales Rate, Unit Margin & Visual QR Code + Barcode Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Purchase Rate: $currencySymbol${String.format(Locale.US, "%,.2f", item.purchaseRate)} / ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sales Rate: $currencySymbol${String.format(Locale.US, "%,.2f", item.salesRate)} / ${item.unit}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Margin: +$currencySymbol${String.format(Locale.US, "%.2f", item.unitMargin)} / ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ProfitGreen
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrCodeCanvas(
                        code = item.barcode,
                        modifier = Modifier.size(44.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        BarcodeCanvas(
                            code = item.barcode,
                            modifier = Modifier
                                .width(100.dp)
                                .height(26.dp),
                            barColor = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.barcode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Toolbar: Quick Stock +/- | Sell Item | Edit | Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onAdjustStock(-1.0) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrement stock",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Stock ±1",
                        style = MaterialTheme.typography.labelSmall
                    )
                    IconButton(
                        onClick = { onAdjustStock(1.0) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increment stock",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = onSellNow,
                        label = { Text("Add to Sale") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("sell_inventory_item_${item.id}")
                    )
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit item"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete item",
                            tint = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddOrEditInventoryModal(
    initialItem: InventoryItemEntity?,
    prefilledBarcode: String,
    allItems: List<InventoryItemEntity>,
    currencySymbol: String,
    onSave: (Long, String, String, String, String, String, Double, Double, Double, Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var barcode by remember {
        mutableStateOf(
            initialItem?.barcode ?: prefilledBarcode.ifBlank {
                "890" + (System.currentTimeMillis() % 10000000000L).toString().padStart(10, '0')
            }
        )
    }
    var category by remember { mutableStateOf(initialItem?.category ?: "Electronics") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "PCS") }
    var hsnCode by remember { mutableStateOf(initialItem?.hsnCode ?: "8471") }
    var purchaseRate by remember { mutableStateOf(initialItem?.purchaseRate?.toString() ?: "") }
    var salesRate by remember { mutableStateOf(initialItem?.salesRate?.toString() ?: "") }
    var stockQuantity by remember { mutableStateOf(initialItem?.stockQuantity?.toString() ?: "10") }
    var minStockAlert by remember { mutableStateOf(initialItem?.minStockAlert?.toString() ?: "5") }
    var gstRate by remember { mutableIntStateOf(initialItem?.gstRate ?: 18) }
    var showEntryQrScanner by remember { mutableStateOf(false) }

    if (showEntryQrScanner) {
        BarcodeScannerDialog(
            title = "Scan QR Code for Item Entry",
            subtitle = "Scan an item's QR code / barcode to assign it to this item entry",
            mode = ScannerMode.ITEM_ENTRY,
            inventoryItems = allItems,
            onBarcodeScanned = { scannedCode ->
                showEntryQrScanner = false
                // Support pipe-delimited QR payload (e.g. "CODE|NAME|RATE") or plain QR code string
                if (scannedCode.contains("|")) {
                    val parts = scannedCode.split("|").map { it.trim() }
                    if (parts.isNotEmpty() && parts[0].isNotBlank()) barcode = parts[0]
                    if (parts.size >= 2 && name.isBlank() && parts[1].isNotBlank()) name = parts[1]
                    if (parts.size >= 3 && salesRate.isBlank() && parts[2].toDoubleOrNull() != null) {
                        salesRate = parts[2]
                    }
                } else {
                    barcode = scannedCode.trim()
                }
            },
            onDismiss = { showEntryQrScanner = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialItem == null) "Add New Inventory Item" else "Edit Inventory Item")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_item_name_input")
                )

                // QR Code / Barcode Field with built-in QR Scanner Button for Item Entry
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Item QR Code / Barcode *") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { showEntryQrScanner = true },
                            modifier = Modifier.testTag("modal_scan_qr_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR Code for Item Entry",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_item_barcode_input")
                )

                // Action Row: Use QR Code Scanner to Add Code | Generate New QR Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showEntryQrScanner = true },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("modal_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan QR to Add Code")
                    }

                    OutlinedButton(
                        onClick = {
                            barcode = "QR-" + (System.currentTimeMillis() % 100000000L)
                                .toString()
                                .padStart(8, '0')
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("modal_generate_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New QR")
                    }
                }

                // Live 2D QR Code & Barcode Preview Card inside Item Entry
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QrCodeCanvas(
                                code = barcode,
                                modifier = Modifier.size(48.dp)
                            )
                            Column {
                                Text(
                                    text = "Item QR Code Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = barcode.ifBlank { "No code set" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        BarcodeCanvas(
                            code = barcode,
                            modifier = Modifier
                                .width(88.dp)
                                .height(28.dp),
                            barColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // HSN Code, Category & Quantity Unit
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("modal_item_hsn_input")
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("modal_item_category_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("modal_item_unit_input")
                    )
                }

                // Quick Unit Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("PCS", "KG", "BOX", "LTR", "PACK", "MTR").forEach { u ->
                        FilterChip(
                            selected = unit.equals(u, ignoreCase = true),
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                // Purchase Rate & Sales Rate
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchaseRate,
                        onValueChange = { purchaseRate = it },
                        label = { Text("Purchase Rate ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("modal_purchase_rate_input")
                    )
                    OutlinedTextField(
                        value = salesRate,
                        onValueChange = { salesRate = it },
                        label = { Text("Sales Rate ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("modal_sales_rate_input")
                    )
                }

                // Stock Count & Low Stock Alert
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Total Stock Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("modal_stock_count_input")
                    )
                    OutlinedTextField(
                        value = minStockAlert,
                        onValueChange = { minStockAlert = it },
                        label = { Text("Low Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Item GST Slab
                Text(
                    text = "GST Tax Slab (%)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 12, 18, 28).forEach { slab ->
                        FilterChip(
                            selected = gstRate == slab,
                            onClick = { gstRate = slab },
                            label = { Text("$slab%") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialItem?.id ?: 0L,
                        name,
                        barcode,
                        category,
                        unit,
                        hsnCode,
                        purchaseRate.toDoubleOrNull() ?: 0.0,
                        salesRate.toDoubleOrNull() ?: 0.0,
                        stockQuantity.toDoubleOrNull() ?: 0.0,
                        minStockAlert.toDoubleOrNull() ?: 5.0,
                        gstRate
                    )
                },
                modifier = Modifier.testTag("save_inventory_item_button")
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
