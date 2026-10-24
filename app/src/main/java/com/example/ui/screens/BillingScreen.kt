package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.InventoryItemEntity
import com.example.ui.CartLineItem
import com.example.ui.GstTaxMode
import com.example.ui.InvoiceUiState
import com.example.ui.SUPPORTED_GST_SLABS
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ProfitGreen
import com.example.util.QrCodeCanvas
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillingScreen(
    invoiceState: InvoiceUiState,
    inventoryItems: List<InventoryItemEntity>,
    currencySymbol: String,
    onUpdateCustomerHeader: (String, String, String, String, String) -> Unit,
    onSetGstEnabled: (Boolean) -> Unit,
    onSetGstTaxMode: (GstTaxMode) -> Unit,
    onSetSelectedGstSlab: (Int) -> Unit,
    onUpdateDiscountAndPayment: (String, String, String) -> Unit,
    onQuickCashTender: (Double) -> Unit,
    onAddItemToCart: (InventoryItemEntity, Double) -> Unit,
    onAddCustomItem: (String, String, String, Double, Double, Double, Int) -> Unit,
    onUpdateCartQuantity: (Int, Double) -> Unit,
    onUpdateCartUnitPrice: (Int, Double) -> Unit,
    onUpdateCartDiscountPercent: (Int, Double) -> Unit,
    onUpdateCartGstSlab: (Int, Int) -> Unit,
    onRemoveCartItem: (Int) -> Unit,
    onClearDraft: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onScanOrEnterQrCode: (String) -> Unit = {},
    onFinalizeInvoice: () -> Unit
) {
    var showInventoryPickerDialog by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var inlineQrInput by remember { mutableStateOf("") }

    if (showInventoryPickerDialog) {
        InventoryCatalogPickerModal(
            inventoryItems = inventoryItems,
            currencySymbol = currencySymbol,
            onSelectItem = { item ->
                onAddItemToCart(item, 1.0)
                showInventoryPickerDialog = false
            },
            onOpenQrScanner = {
                showInventoryPickerDialog = false
                onOpenBarcodeScanner()
            },
            onDismiss = { showInventoryPickerDialog = false }
        )
    }

    if (showCustomItemDialog) {
        CustomLineItemModal(
            currencySymbol = currencySymbol,
            defaultGstSlab = invoiceState.selectedGstSlab,
            onAdd = { name, unit, hsn, rate, qty, discPct, slab ->
                onAddCustomItem(name, unit, hsn, rate, qty, discPct, slab)
                showCustomItemDialog = false
            },
            onDismiss = { showCustomItemDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("billing_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header with Bill Number, Date, Time & Customer Details
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sales Invoice Generation",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Scan QR codes from inventory, set item discount % & calculate GST",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onClearDraft,
                            modifier = Modifier.testTag("reset_invoice_draft_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Reset invoice draft"
                            )
                        }
                    }

                    // Bill No, Date, Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = invoiceState.billNumber,
                            onValueChange = {
                                onUpdateCustomerHeader(
                                    invoiceState.customerName,
                                    invoiceState.customerMobile,
                                    it,
                                    invoiceState.dateDisplay,
                                    invoiceState.timeDisplay
                                )
                            },
                            label = { Text("Bill Number") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("bill_number_input")
                        )

                        OutlinedTextField(
                            value = invoiceState.dateDisplay,
                            onValueChange = {
                                onUpdateCustomerHeader(
                                    invoiceState.customerName,
                                    invoiceState.customerMobile,
                                    invoiceState.billNumber,
                                    it,
                                    invoiceState.timeDisplay
                                )
                            },
                            label = { Text("Date") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_date_input")
                        )

                        OutlinedTextField(
                            value = invoiceState.timeDisplay,
                            onValueChange = {
                                onUpdateCustomerHeader(
                                    invoiceState.customerName,
                                    invoiceState.customerMobile,
                                    invoiceState.billNumber,
                                    invoiceState.dateDisplay,
                                    it
                                )
                            },
                            label = { Text("Time") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("bill_time_input")
                        )
                    }

                    // Customer Name & Mobile Number Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = invoiceState.customerName,
                            onValueChange = {
                                onUpdateCustomerHeader(
                                    it,
                                    invoiceState.customerMobile,
                                    invoiceState.billNumber,
                                    invoiceState.dateDisplay,
                                    invoiceState.timeDisplay
                                )
                            },
                            label = { Text("Customer Name") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_name_input")
                        )

                        OutlinedTextField(
                            value = invoiceState.customerMobile,
                            onValueChange = {
                                onUpdateCustomerHeader(
                                    invoiceState.customerName,
                                    it,
                                    invoiceState.billNumber,
                                    invoiceState.dateDisplay,
                                    invoiceState.timeDisplay
                                )
                            },
                            label = { Text("Mobile Number") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_mobile_input")
                        )
                    }
                }
            }
        }

        // 2. Selling-Time QR Code Scanner & Inventory Item Addition Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenBarcodeScanner,
                            modifier = Modifier
                                .weight(1.15f)
                                .height(52.dp)
                                .testTag("billing_scan_barcode_button")
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR to Add Item")
                        }

                        OutlinedButton(
                            onClick = { showInventoryPickerDialog = true },
                            modifier = Modifier
                                .weight(0.85f)
                                .height(52.dp)
                                .testTag("billing_pick_inventory_button")
                        ) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Stock")
                        }
                    }

                    // Inline Selling-Time QR Code / Barcode Lookup Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inlineQrInput,
                            onValueChange = { inlineQrInput = it },
                            label = { Text("Scan or Enter Item QR Code / Barcode") },
                            placeholder = { Text("e.g. 8901030845210 or QR-SKU-1001") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = onOpenBarcodeScanner,
                                    modifier = Modifier.testTag("billing_inline_qr_camera_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Open camera QR scanner",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (inlineQrInput.isNotBlank()) {
                                        onScanOrEnterQrCode(inlineQrInput.trim())
                                        inlineQrInput = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("billing_inline_qr_input")
                        )

                        FilledTonalButton(
                            onClick = {
                                if (inlineQrInput.isNotBlank()) {
                                    onScanOrEnterQrCode(inlineQrInput.trim())
                                    inlineQrInput = ""
                                }
                            },
                            enabled = inlineQrInput.isNotBlank(),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("billing_inline_qr_submit_button")
                        ) {
                            Text("Add QR")
                        }
                    }

                    // Quick-Scan Inventory QR Code Strip for Selling Time
                    if (inventoryItems.isNotEmpty()) {
                        Text(
                            text = "Tap an Inventory QR Code to Add Item to Bill:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            inventoryItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onAddItemToCart(item, 1.0) }
                                        .testTag("selling_qr_card_${item.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        QrCodeCanvas(
                                            code = item.barcode,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Column {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${item.barcode} • $currencySymbol${item.salesRate.toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Dynamic Item List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bill Items (${invoiceState.totalItemsCount} Items • Total Qty: ${String.format(Locale.US, "%.1f", invoiceState.totalQuantity)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showCustomItemDialog = true },
                    modifier = Modifier.testTag("add_custom_bill_item_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Item")
                }
            }
        }

        // 4. Empty State or Dynamic Cart Line Items with Item-Wise Amount, Discount %, and After-Discount Price
        if (invoiceState.cartItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No items added to this invoice yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Use 'Scan QR to Add Item', enter a QR code above, or tap a quick item chip:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            inventoryItems.take(5).forEach { item ->
                                AssistChip(
                                    onClick = { onAddItemToCart(item, 1.0) },
                                    label = {
                                        Text("${item.name} ($currencySymbol${item.salesRate.toInt()})")
                                    },
                                    modifier = Modifier.testTag("quick_add_item_${item.id}")
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(invoiceState.cartItems.size) { index ->
                val line = invoiceState.cartItems[index]
                CartLineItemRowCard(
                    index = index,
                    item = line,
                    showItemGstSelector = invoiceState.gstEnabled && invoiceState.gstTaxMode == GstTaxMode.ITEM_WISE_SLAB,
                    currencySymbol = currencySymbol,
                    onQuantityChange = { newQty -> onUpdateCartQuantity(index, newQty) },
                    onUnitPriceChange = { newPrice -> onUpdateCartUnitPrice(index, newPrice) },
                    onDiscountPercentChange = { newDiscPct -> onUpdateCartDiscountPercent(index, newDiscPct) },
                    onGstSlabChange = { newSlab -> onUpdateCartGstSlab(index, newSlab) },
                    onRemove = { onRemoveCartItem(index) }
                )
            }
        }

        // 5. GST Management & Tax Breakdown Card (On/Off + Uniform or Item-Wise 5%, 12%, 18%, 28% CGST/SGST)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GST Tax Management (CGST + SGST)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (invoiceState.gstEnabled) {
                                    "Applied on after-discount item prices • 5%, 12%, 18%, 28% slabs"
                                } else {
                                    "GST is turned OFF for this invoice"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = invoiceState.gstEnabled,
                            onCheckedChange = onSetGstEnabled,
                            modifier = Modifier.testTag("billing_gst_switch")
                        )
                    }

                    if (invoiceState.gstEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GstTaxMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = invoiceState.gstTaxMode == mode,
                                    onClick = { onSetGstTaxMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("gst_mode_${mode.name}")
                                )
                            }
                        }

                        if (invoiceState.gstTaxMode == GstTaxMode.UNIFORM_SLAB) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SUPPORTED_GST_SLABS.forEach { slab ->
                                    FilterChip(
                                        selected = invoiceState.selectedGstSlab == slab,
                                        onClick = { onSetSelectedGstSlab(slab) },
                                        label = { Text("$slab% GST") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("gst_slab_${slab}_chip")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Totals, Item-Wise Discount & After-Discount Breakdown, CGST/SGST, and Cash Collection Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Bill Summary & Cash Collection",
                        style = MaterialTheme.typography.titleLarge
                    )

                    SummaryLine(
                        "Total Quantity (${invoiceState.totalItemsCount} Items)",
                        "${String.format(Locale.US, "%.2f", invoiceState.totalQuantity)} Units"
                    )
                    SummaryLine(
                        "Gross Item-Wise Amount",
                        "$currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.grossItemAmountTotal)}"
                    )

                    if (invoiceState.itemWiseDiscountTotal > 0.0) {
                        SummaryLine(
                            "Item-Wise Discounts Saved",
                            "- $currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.itemWiseDiscountTotal)}"
                        )
                    }

                    SummaryLine(
                        "After-Discount Item Price (Subtotal)",
                        "$currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.subtotalAmount)}"
                    )

                    if (invoiceState.extraBillDiscountAmount > 0.0) {
                        SummaryLine(
                            "Additional Bill Discount",
                            "- $currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.extraBillDiscountAmount)}"
                        )
                        SummaryLine(
                            "Net Taxable Amount",
                            "$currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.taxableAmount)}"
                        )
                    }

                    if (invoiceState.gstEnabled && invoiceState.slabBreakups.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        invoiceState.slabBreakups.forEach { breakup ->
                            SummaryLine(
                                "CGST (@${breakup.cgstRate}% on $currencySymbol${String.format(Locale.US, "%.2f", breakup.taxableValue)})",
                                "+ $currencySymbol${String.format(Locale.US, "%,.2f", breakup.cgstAmount)}"
                            )
                            SummaryLine(
                                "SGST (@${breakup.sgstRate}% on $currencySymbol${String.format(Locale.US, "%.2f", breakup.taxableValue)})",
                                "+ $currencySymbol${String.format(Locale.US, "%,.2f", breakup.sgstAmount)}"
                            )
                        }
                        SummaryLine(
                            "Total GST (CGST + SGST)",
                            "$currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.totalGstAmount)}"
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAND TOTAL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", invoiceState.grandTotalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("invoice_grand_total_text")
                        )
                    }

                    // Payment Mode Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "UPI / QR", "Card", "Part-Cash").forEach { mode ->
                            FilterChip(
                                selected = invoiceState.paymentMode == mode,
                                onClick = {
                                    onUpdateDiscountAndPayment(
                                        invoiceState.discountInput,
                                        invoiceState.cashReceivedInput,
                                        mode
                                    )
                                },
                                label = { Text(mode) }
                            )
                        }
                    }

                    // Extra Bill Discount & Total Cash Received Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = invoiceState.discountInput,
                            onValueChange = {
                                onUpdateDiscountAndPayment(
                                    it,
                                    invoiceState.cashReceivedInput,
                                    invoiceState.paymentMode
                                )
                            },
                            label = { Text("Extra Discount ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("discount_input")
                        )

                        OutlinedTextField(
                            value = invoiceState.cashReceivedInput,
                            onValueChange = {
                                onUpdateDiscountAndPayment(
                                    invoiceState.discountInput,
                                    it,
                                    invoiceState.paymentMode
                                )
                            },
                            placeholder = {
                                Text(String.format(Locale.US, "%.2f", invoiceState.grandTotalAmount))
                            },
                            label = { Text("Total Cash Received ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("cash_received_input")
                        )
                    }

                    // Quick Cash Tender Denomination Chips
                    if (invoiceState.grandTotalAmount > 0.0) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssistChip(
                                onClick = { onQuickCashTender(invoiceState.grandTotalAmount) },
                                label = { Text("Exact ($currencySymbol${String.format(Locale.US, "%.0f", invoiceState.grandTotalAmount)})") }
                            )
                            val rounded500 = kotlin.math.ceil(invoiceState.grandTotalAmount / 500.0) * 500.0
                            if (rounded500 > invoiceState.grandTotalAmount) {
                                AssistChip(
                                    onClick = { onQuickCashTender(rounded500) },
                                    label = { Text("$currencySymbol${rounded500.toInt()}") }
                                )
                            }
                            val rounded2000 = kotlin.math.ceil(invoiceState.grandTotalAmount / 2000.0) * 2000.0
                            if (rounded2000 > rounded500) {
                                AssistChip(
                                    onClick = { onQuickCashTender(rounded2000) },
                                    label = { Text("$currencySymbol${rounded2000.toInt()}") }
                                )
                            }
                        }
                    }

                    // Change / Balance Indicator
                    val change = invoiceState.balanceOrChange
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (change >= 0.0) "Change to Return:" else "Balance Due from Customer:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", kotlin.math.abs(change))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (change >= 0.0) ProfitGreen else DangerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onFinalizeInvoice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("generate_invoice_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Bill & Open Thermal Receipt",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.Print, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Item card displaying:
 * - Item-wise Gross Amount (Qty x Unit Rate)
 * - Editable Discount Percentage (%) + quick preset chips (0%, 5%, 10%, 15%, 20%)
 * - Discount Amount saved & Per-unit After-Discount Price
 * - Final Item-Wise After-Discount Price
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartLineItemRowCard(
    index: Int,
    item: CartLineItem,
    showItemGstSelector: Boolean,
    currencySymbol: String,
    onQuantityChange: (Double) -> Unit,
    onUnitPriceChange: (Double) -> Unit,
    onDiscountPercentChange: (Double) -> Unit,
    onGstSlabChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var discountText by remember(item.itemId, index) {
        mutableStateOf(
            if (item.discountPercent == 0.0) "" else {
                if (item.discountPercent % 1.0 == 0.0) item.discountPercent.toInt().toString()
                else item.discountPercent.toString()
            }
        )
    }
    var rateText by remember(item.itemId, index, item.unitPrice) {
        mutableStateOf(
            if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString()
            else String.format(Locale.US, "%.2f", item.unitPrice)
        )
    }

    LaunchedEffect(item.discountPercent) {
        val currentParsed = discountText.toDoubleOrNull() ?: 0.0
        if (currentParsed != item.discountPercent) {
            discountText = if (item.discountPercent == 0.0) "" else {
                if (item.discountPercent % 1.0 == 0.0) item.discountPercent.toInt().toString()
                else item.discountPercent.toString()
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_row_$index"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Item Name, QR/Barcode, HSN, Quantity Stepper & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrCodeCanvas(
                        code = item.barcode,
                        modifier = Modifier.size(34.dp)
                    )
                    Column {
                        Text(
                            text = item.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "QR: ${item.barcode} • HSN: ${item.hsnCode.ifBlank { "N/A" }} • Rate: $currencySymbol${String.format(Locale.US, "%,.2f", item.unitPrice)}/${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onQuantityChange(item.quantity - 1.0) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("decrease_qty_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease quantity",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (item.quantity % 1.0 == 0.0) {
                            item.quantity.toInt().toString()
                        } else {
                            String.format(Locale.US, "%.1f", item.quantity)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    IconButton(
                        onClick = { onQuantityChange(item.quantity + 1.0) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("increase_qty_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase quantity",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.testTag("remove_cart_item_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove item",
                            tint = DangerRed
                        )
                    }
                }
            }

            // Middle Row: Editable Unit Rate & Editable Item Discount % Input + Quick Discount % Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        it.toDoubleOrNull()?.let { validRate -> onUnitPriceChange(validRate) }
                    },
                    label = { Text("Rate ($currencySymbol/${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_rate_input_$index")
                )

                OutlinedTextField(
                    value = discountText,
                    onValueChange = {
                        discountText = it
                        val pct = it.toDoubleOrNull() ?: 0.0
                        onDiscountPercentChange(pct.coerceIn(0.0, 100.0))
                    },
                    label = { Text("Discount %") },
                    placeholder = { Text("0%") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_discount_percent_input_$index")
                )
            }

            // Quick Discount Percentage Preset Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0, 5, 10, 15, 20).forEach { pct ->
                    FilterChip(
                        selected = item.discountPercent.toInt() == pct && (item.discountPercent % 1.0 == 0.0),
                        onClick = {
                            discountText = if (pct == 0) "" else pct.toString()
                            onDiscountPercentChange(pct.toDouble())
                        },
                        label = { Text(if (pct == 0) "No Disc" else "$pct% Off") },
                        modifier = Modifier.testTag("item_${index}_disc_chip_$pct")
                    )
                }
            }

            // Item-Wise Breakdown Box: Item-Wise Amount | Discount (% & Value) | After-Discount Price
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: Item-Wise Gross Amount
                    Column {
                        Text(
                            text = "Item Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.grossAmount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (item.discountPercent > 0.0) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                            modifier = Modifier.testTag("item_gross_amount_$index")
                        )
                    }

                    // Column 2: Discount Percentage & Discount Amount
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Discount (${String.format(Locale.US, "%.1f", item.discountPercent)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (item.itemDiscountAmount > 0.0) {
                                "- $currencySymbol${String.format(Locale.US, "%,.2f", item.itemDiscountAmount)}"
                            } else {
                                "$currencySymbol" + "0.00"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.itemDiscountAmount > 0.0) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("item_discount_amount_$index")
                        )
                    }

                    // Column 3: After-Discount Price (Net Item Total & Unit Rate After Discount)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "After Discount Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.afterDiscountPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("item_after_discount_price_$index")
                        )
                        if (item.discountPercent > 0.0) {
                            Text(
                                text = "@$currencySymbol${String.format(Locale.US, "%.2f", item.unitPriceAfterDiscount)}/${item.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }

            if (showItemGstSelector) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Item GST:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SUPPORTED_GST_SLABS.forEach { slab ->
                        FilterChip(
                            selected = item.gstRate == slab,
                            onClick = { onGstSlabChange(slab) },
                            label = { Text("$slab%") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCatalogPickerModal(
    inventoryItems: List<InventoryItemEntity>,
    currencySymbol: String,
    onSelectItem: (InventoryItemEntity) -> Unit,
    onOpenQrScanner: () -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(inventoryItems, search) {
        val q = search.trim().lowercase(Locale.US)
        if (q.isEmpty()) inventoryItems else inventoryItems.filter {
            it.name.lowercase(Locale.US).contains(q) ||
                it.barcode.lowercase(Locale.US).contains(q) ||
                it.category.lowercase(Locale.US).contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select or Scan from Inventory",
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onOpenQrScanner,
                    modifier = Modifier.testTag("picker_scan_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search name, QR code, barcode, category…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectItem(item) },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QrCodeCanvas(
                                        code = item.barcode,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "QR: ${item.barcode} • Stock: ${item.stockQuantity.toInt()} ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%.2f", item.salesRate)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CustomLineItemModal(
    currencySymbol: String,
    defaultGstSlab: Int,
    onAdd: (String, String, String, Double, Double, Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("PCS") }
    var hsn by remember { mutableStateOf("9983") }
    var rate by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var discountPercent by remember { mutableStateOf("0") }
    var gstSlab by remember { mutableIntStateOf(defaultGstSlab) }

    val parsedRate = rate.toDoubleOrNull() ?: 0.0
    val parsedQty = qty.toDoubleOrNull() ?: 1.0
    val parsedDisc = (discountPercent.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val grossAmt = parsedRate * parsedQty
    val afterDiscAmt = grossAmt * (1.0 - parsedDisc / 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Bill Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hsn,
                        onValueChange = { hsn = it },
                        label = { Text("HSN Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Sales Rate ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = discountPercent,
                        onValueChange = { discountPercent = it },
                        label = { Text("Disc %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                if (grossAmt > 0.0) {
                    Text(
                        text = "Item Amount: $currencySymbol${String.format(Locale.US, "%.2f", grossAmt)} → After Discount: $currencySymbol${String.format(Locale.US, "%.2f", afterDiscAmt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SUPPORTED_GST_SLABS.forEach { slab ->
                        FilterChip(
                            selected = gstSlab == slab,
                            onClick = { gstSlab = slab },
                            label = { Text("$slab%") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && parsedRate > 0.0) {
                        onAdd(name, unit, hsn, parsedRate, parsedQty, parsedDisc, gstSlab)
                    }
                }
            ) {
                Text("Add to Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
