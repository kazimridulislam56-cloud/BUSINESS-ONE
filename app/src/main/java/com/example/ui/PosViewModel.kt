package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.LedgerDatabase
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.BusinessAndAppSettings
import com.example.data.repository.LedgerRepository
import com.example.util.ExportBackupManager
import com.example.util.ItemPerformanceStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class AppScreen(val route: String) {
    DASHBOARD("dashboard"),
    BILLING("billing"),
    INVENTORY("inventory"),
    REPORTS("reports"),
    SETTINGS("settings")
}

enum class InventorySortOption(val label: String) {
    NAME_ASC("Name (A–Z)"),
    CATEGORY("Category"),
    STOCK_LOW_HIGH("Stock: Low → High"),
    STOCK_HIGH_LOW("Stock: High → Low"),
    PRICE_HIGH_LOW("Sales Rate: High → Low")
}

enum class ReportTab(val label: String) {
    DAILY("Daily"),
    MONTHLY("Monthly"),
    ANNUAL("Annual"),
    ITEM_PERFORMANCE("Item Performance")
}

data class CartLineItem(
    val itemId: Long,
    val itemName: String,
    val barcode: String,
    val category: String,
    val unit: String,
    val hsnCode: String,
    val purchaseRate: Double,
    val unitPrice: Double,
    val quantity: Double,
    val discountPercent: Double = 0.0,
    val gstRate: Int,
    val availableStock: Double
) {
    /** Item-wise gross amount before discount (Qty * Unit Price) */
    val grossAmount: Double
        get() = BillingDraftState.roundCurrency(quantity * unitPrice)

    /** Item-wise discount amount calculated from discountPercent */
    val itemDiscountAmount: Double
        get() = BillingDraftState.roundCurrency(grossAmount * (discountPercent.coerceIn(0.0, 100.0) / 100.0))

    /** Per-unit price after applying item discount % */
    val unitPriceAfterDiscount: Double
        get() = BillingDraftState.roundCurrency(unitPrice * (1.0 - discountPercent.coerceIn(0.0, 100.0) / 100.0))

    /** Item-wise net price after discount */
    val afterDiscountPrice: Double
        get() = BillingDraftState.roundCurrency((grossAmount - itemDiscountAmount).coerceAtLeast(0.0))

    /** Taxable line subtotal (after item-wise discount) */
    val lineSubtotal: Double
        get() = afterDiscountPrice
}

data class BillingDraftState(
    val billNumber: String = "INV-2026-1003",
    val customerName: String = "",
    val customerMobile: String = "",
    val dateDisplay: String = "",
    val timeDisplay: String = "",
    val cartItems: List<CartLineItem> = emptyList(),
    val gstEnabled: Boolean = true,
    val selectedGstSlab: Int = 18, // 5, 12, 18, 28
    val discountInput: String = "",
    val cashReceivedInput: String = "",
    val paymentMode: String = "Cash"
) {
    val totalQuantity: Double
        get() = cartItems.sumOf { it.quantity }

    val subtotalAmount: Double
        get() = cartItems.sumOf { it.lineSubtotal }

    val discountAmount: Double
        get() = discountInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val taxableAmount: Double
        get() = (subtotalAmount - discountAmount).coerceAtLeast(0.0)

    val totalGstAmount: Double
        get() = if (!gstEnabled) 0.0 else (taxableAmount * (selectedGstSlab / 100.0))

    val cgstAmount: Double
        get() = roundCurrency(totalGstAmount / 2.0)

    val sgstAmount: Double
        get() = roundCurrency(totalGstAmount / 2.0)

    val grandTotalAmount: Double
        get() = roundCurrency(taxableAmount + cgstAmount + sgstAmount)

    val cashReceivedAmount: Double
        get() = cashReceivedInput.toDoubleOrNull() ?: grandTotalAmount

    val balanceOrChange: Double
        get() = roundCurrency(cashReceivedAmount - grandTotalAmount)

    companion object {
        fun roundCurrency(value: Double): Double {
            return (value * 100.0).roundToInt() / 100.0
        }
    }
}

data class DashboardMetrics(
    val totalCurrentQuantity: Double = 0.0,
    val totalStockValuePurchase: Double = 0.0,
    val totalStockValueRetail: Double = 0.0,
    val potentialStockMargin: Double = 0.0,
    val cashInHand: Double = 0.0,
    val totalCashReceivedFromSales: Double = 0.0,
    val todaysCashReceived: Double = 0.0,
    val todaysSales: Double = 0.0,
    val todaysBillsCount: Int = 0,
    val todaysGstCollected: Double = 0.0,
    val monthlySales: Double = 0.0,
    val monthlyBillsCount: Int = 0,
    val monthlyGstCollected: Double = 0.0,
    val annualSales: Double = 0.0,
    val lowStockItems: List<InventoryItemEntity> = emptyList(),
    val dailySalesSeries: List<com.example.util.DailySalesPoint> = emptyList(),
    val monthlySalesSeries: List<com.example.util.MonthlySalesPoint> = emptyList(),
    val categoryStockBreakdown: List<com.example.util.CategoryStockPoint> = emptyList()
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LedgerDatabase.getInstance(application)
    private val repository = LedgerRepository(database.ledgerDao())
    private val prefsRepo = AppPreferencesRepository(application)

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val settingsState: StateFlow<BusinessAndAppSettings> = prefsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BusinessAndAppSettings()
    )

    val inventoryItems: StateFlow<List<InventoryItemEntity>> = repository.allInventoryItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allInvoices: StateFlow<List<SalesInvoiceEntity>> = repository.allInvoices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLineItems: StateFlow<List<InvoiceLineItemEntity>> = repository.allLineItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Inventory Search, Category Filter & Sort
    private val _inventorySearchQuery = MutableStateFlow("")
    val inventorySearchQuery: StateFlow<String> = _inventorySearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _inventorySortOption = MutableStateFlow(InventorySortOption.NAME_ASC)
    val inventorySortOption: StateFlow<InventorySortOption> = _inventorySortOption.asStateFlow()

    val filteredInventoryItems: StateFlow<List<InventoryItemEntity>> = combine(
        inventoryItems,
        _inventorySearchQuery,
        _selectedCategory,
        _inventorySortOption
    ) { items, query, category, sort ->
        val q = query.trim().lowercase(Locale.US)
        val filtered = items.filter { item ->
            val matchesCat = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase(Locale.US).contains(q) ||
                item.barcode.lowercase(Locale.US).contains(q) ||
                item.hsnCode.lowercase(Locale.US).contains(q) ||
                item.category.lowercase(Locale.US).contains(q)
            matchesCat && matchesQuery
        }
        when (sort) {
            InventorySortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase(Locale.US) }
            InventorySortOption.CATEGORY -> filtered.sortedWith(compareBy({ it.category }, { it.name }))
            InventorySortOption.STOCK_LOW_HIGH -> filtered.sortedBy { it.stockQuantity }
            InventorySortOption.STOCK_HIGH_LOW -> filtered.sortedByDescending { it.stockQuantity }
            InventorySortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.salesRate }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Analytics Dashboard Computed Metrics & Time-Series Aggregations
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        inventoryItems,
        allInvoices,
        settingsState
    ) { items, invoices, settings ->
        val nowMillis = System.currentTimeMillis()
        val now = Date(nowMillis)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(now)
        val yearStr = SimpleDateFormat("yyyy", Locale.US).format(now)

        val totalQty = items.sumOf { it.stockQuantity }
        val stockValPurchase = items.sumOf { it.stockValueAtPurchase }
        val stockValRetail = items.sumOf { it.stockValueAtSales }
        val totalCashFromSales = invoices.sumOf { it.cashReceived }
        val cashInHand = settings.openingCashInHand + totalCashFromSales

        val todaysInvoices = invoices.filter { it.dateString == todayStr }
        val monthlyInvoices = invoices.filter { it.monthString == monthStr }
        val annualInvoices = invoices.filter { it.yearString == yearStr }

        DashboardMetrics(
            totalCurrentQuantity = totalQty,
            totalStockValuePurchase = stockValPurchase,
            totalStockValueRetail = stockValRetail,
            potentialStockMargin = (stockValRetail - stockValPurchase).coerceAtLeast(0.0),
            cashInHand = cashInHand,
            totalCashReceivedFromSales = totalCashFromSales,
            todaysCashReceived = todaysInvoices.sumOf { it.cashReceived },
            todaysSales = todaysInvoices.sumOf { it.grandTotalAmount },
            todaysBillsCount = todaysInvoices.size,
            todaysGstCollected = todaysInvoices.sumOf { it.totalTaxAmount },
            monthlySales = monthlyInvoices.sumOf { it.grandTotalAmount },
            monthlyBillsCount = monthlyInvoices.size,
            monthlyGstCollected = monthlyInvoices.sumOf { it.totalTaxAmount },
            annualSales = annualInvoices.sumOf { it.grandTotalAmount },
            lowStockItems = items.filter { it.isLowStock },
            dailySalesSeries = com.example.util.AnalyticsAggregator.aggregateDailySalesSeries(invoices, nowMillis, 7),
            monthlySalesSeries = com.example.util.AnalyticsAggregator.aggregateMonthlySalesSeries(invoices, nowMillis, 6),
            categoryStockBreakdown = com.example.util.AnalyticsAggregator.aggregateCategoryStockBreakdown(items)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMetrics()
    )

    // Item Performance Stats for Comprehensive Reports
    val itemPerformanceStats: StateFlow<List<ItemPerformanceStat>> = combine(
        inventoryItems,
        allLineItems
    ) { items, lines ->
        val groupedByItem = lines.groupBy { it.itemId to it.itemName }
        val statsFromSales = groupedByItem.map { (_, itemLines) ->
            val first = itemLines.first()
            val invItem = items.find { it.id == first.itemId }
            val soldQty = itemLines.sumOf { it.quantity }
            val revenue = itemLines.sumOf { it.lineSubtotal }
            val gst = itemLines.sumOf { it.cgstAmount + it.sgstAmount }
            val cost = itemLines.sumOf { it.purchaseRate * it.quantity }
            val profit = revenue - cost
            ItemPerformanceStat(
                itemId = first.itemId,
                itemName = first.itemName,
                barcode = first.barcode,
                category = first.category,
                hsnCode = first.hsnCode,
                unit = first.unit,
                unitsSold = soldQty,
                grossRevenue = revenue,
                gstCollected = gst,
                estimatedCost = cost,
                netProfit = profit,
                currentStock = invItem?.stockQuantity ?: 0.0
            )
        }
        // Also include inventory items with 0 sales so the report is complete
        val soldIds = statsFromSales.map { it.itemId }.toSet()
        val zeroSaleStats = items.filter { it.id !in soldIds }.map { item ->
            ItemPerformanceStat(
                itemId = item.id,
                itemName = item.name,
                barcode = item.barcode,
                category = item.category,
                hsnCode = item.hsnCode,
                unit = item.unit,
                unitsSold = 0.0,
                grossRevenue = 0.0,
                gstCollected = 0.0,
                estimatedCost = 0.0,
                netProfit = 0.0,
                currentStock = item.stockQuantity
            )
        }
        (statsFromSales + zeroSaleStats).sortedByDescending { it.grossRevenue }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Billing Draft State
    private val _billingDraft = MutableStateFlow(createFreshBillingDraft(1003, true, 18))
    val billingDraft: StateFlow<BillingDraftState> = _billingDraft.asStateFlow()

    // Active Invoice Receipt Preview Modal State
    private val _activeReceiptPreview = MutableStateFlow<Pair<SalesInvoiceEntity, List<InvoiceLineItemEntity>>?>(null)
    val activeReceiptPreview: StateFlow<Pair<SalesInvoiceEntity, List<InvoiceLineItemEntity>>?> =
        _activeReceiptPreview.asStateFlow()

    init {
        viewModelScope.launch {
            LedgerDatabase.seedStarterCatalogIfEmpty(database.ledgerDao())
            refreshNextBillNumber()
        }
        viewModelScope.launch {
            settingsState.collect { settings ->
                _billingDraft.update { draft ->
                    if (draft.cartItems.isEmpty()) {
                        draft.copy(
                            gstEnabled = settings.gstEnabled,
                            selectedGstSlab = settings.defaultGstSlab
                        )
                    } else {
                        draft
                    }
                }
            }
        }
    }

    private fun createFreshBillingDraft(
        nextSeq: Int,
        gstEnabled: Boolean,
        gstSlab: Int
    ): BillingDraftState {
        val now = Date()
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(now)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(now)
        val yearStr = SimpleDateFormat("yyyy", Locale.US).format(now)
        return BillingDraftState(
            billNumber = "INV-$yearStr-$nextSeq",
            dateDisplay = dateStr,
            timeDisplay = timeStr,
            gstEnabled = gstEnabled,
            selectedGstSlab = gstSlab
        )
    }

    private suspend fun refreshNextBillNumber() {
        val count = repository.getAllInvoicesOnce().size
        val nextSeq = 1001 + count
        val settings = settingsState.value
        _billingDraft.update { current ->
            if (current.cartItems.isEmpty() && current.customerName.isBlank()) {
                createFreshBillingDraft(nextSeq, settings.gstEnabled, settings.defaultGstSlab)
            } else {
                current
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    // --- Billing Operations ---
    fun updateCustomerDetails(
        customerName: String = _billingDraft.value.customerName,
        customerMobile: String = _billingDraft.value.customerMobile,
        billNumber: String = _billingDraft.value.billNumber,
        dateDisplay: String = _billingDraft.value.dateDisplay,
        timeDisplay: String = _billingDraft.value.timeDisplay
    ) {
        _billingDraft.update {
            it.copy(
                customerName = customerName,
                customerMobile = customerMobile,
                billNumber = billNumber,
                dateDisplay = dateDisplay,
                timeDisplay = timeDisplay
            )
        }
    }

    fun updateBillingTaxAndPayment(
        gstEnabled: Boolean = _billingDraft.value.gstEnabled,
        gstSlab: Int = _billingDraft.value.selectedGstSlab,
        discountInput: String = _billingDraft.value.discountInput,
        cashReceivedInput: String = _billingDraft.value.cashReceivedInput,
        paymentMode: String = _billingDraft.value.paymentMode
    ) {
        _billingDraft.update {
            it.copy(
                gstEnabled = gstEnabled,
                selectedGstSlab = gstSlab,
                discountInput = discountInput,
                cashReceivedInput = cashReceivedInput,
                paymentMode = paymentMode
            )
        }
    }

    fun addItemToCart(item: InventoryItemEntity, quantityToAdd: Double = 1.0) {
        _billingDraft.update { draft ->
            val existingIndex = draft.cartItems.indexOfFirst { it.itemId == item.id && it.itemId != 0L }
            val updatedList = if (existingIndex >= 0) {
                draft.cartItems.toMutableList().apply {
                    val existing = this[existingIndex]
                    this[existingIndex] = existing.copy(quantity = existing.quantity + quantityToAdd)
                }
            } else {
                draft.cartItems + CartLineItem(
                    itemId = item.id,
                    itemName = item.name,
                    barcode = item.barcode,
                    category = item.category,
                    unit = item.unit,
                    hsnCode = item.hsnCode,
                    purchaseRate = item.purchaseRate,
                    unitPrice = item.salesRate,
                    quantity = quantityToAdd,
                    gstRate = item.gstRate,
                    availableStock = item.stockQuantity
                )
            }
            draft.copy(cartItems = updatedList)
        }
        _statusMessage.value = "Added '${item.name}' to current sale."
    }

    fun addCustomLineItemToCart(
        name: String,
        unit: String,
        hsnCode: String,
        unitPrice: Double,
        quantity: Double
    ) {
        if (name.isBlank() || unitPrice <= 0.0 || quantity <= 0.0) return
        _billingDraft.update { draft ->
            val newItem = CartLineItem(
                itemId = 0L,
                itemName = name.trim(),
                barcode = "CUSTOM",
                category = "General",
                unit = unit.ifBlank { "PCS" },
                hsnCode = hsnCode.trim(),
                purchaseRate = unitPrice * 0.75,
                unitPrice = unitPrice,
                quantity = quantity,
                gstRate = draft.selectedGstSlab,
                availableStock = 999.0
            )
            draft.copy(cartItems = draft.cartItems + newItem)
        }
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Double) {
        _billingDraft.update { draft ->
            if (index !in draft.cartItems.indices) return@update draft
            val mutable = draft.cartItems.toMutableList()
            if (newQuantity <= 0.0) {
                mutable.removeAt(index)
            } else {
                mutable[index] = mutable[index].copy(quantity = newQuantity)
            }
            draft.copy(cartItems = mutable)
        }
    }

    fun updateCartItemRate(index: Int, newRate: Double) {
        if (newRate < 0.0) return
        _billingDraft.update { draft ->
            if (index !in draft.cartItems.indices) return@update draft
            val mutable = draft.cartItems.toMutableList()
            mutable[index] = mutable[index].copy(unitPrice = newRate)
            draft.copy(cartItems = mutable)
        }
    }

    fun removeCartItem(index: Int) {
        _billingDraft.update { draft ->
            if (index !in draft.cartItems.indices) return@update draft
            val mutable = draft.cartItems.toMutableList()
            mutable.removeAt(index)
            draft.copy(cartItems = mutable)
        }
    }

    fun clearBillingDraft() {
        viewModelScope.launch {
            val count = repository.getAllInvoicesOnce().size
            val settings = settingsState.value
            _billingDraft.value = createFreshBillingDraft(1001 + count, settings.gstEnabled, settings.defaultGstSlab)
        }
    }

    fun handleBarcodeScannedForSale(barcode: String) {
        viewModelScope.launch {
            val clean = barcode.trim()
            val matched = repository.findItemByBarcode(clean)
                ?: inventoryItems.value.firstOrNull {
                    it.barcode.contains(clean, ignoreCase = true) ||
                        it.name.contains(clean, ignoreCase = true)
                }

            if (matched != null) {
                addItemToCart(matched, 1.0)
                _currentScreen.value = AppScreen.BILLING
            } else {
                _statusMessage.value = "No item found with barcode '$clean'. Add it in Inventory first."
            }
        }
    }

    fun finalizeAndSaveInvoice(onSuccess: (SalesInvoiceEntity, List<InvoiceLineItemEntity>) -> Unit = { _, _ -> }) {
        val draft = _billingDraft.value
        if (draft.cartItems.isEmpty()) {
            _statusMessage.value = "Please add at least one item to generate an invoice."
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dateObj = Date(now)
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateObj)
            val monthString = SimpleDateFormat("yyyy-MM", Locale.US).format(dateObj)
            val yearString = SimpleDateFormat("yyyy", Locale.US).format(dateObj)

            val invoiceEntity = SalesInvoiceEntity(
                billNumber = draft.billNumber.ifBlank { "INV-$now" },
                customerName = draft.customerName.trim().ifBlank { "Walk-in Customer" },
                customerMobile = draft.customerMobile.trim(),
                timestamp = now,
                dateString = dateString,
                monthString = monthString,
                yearString = yearString,
                totalQuantity = draft.totalQuantity,
                subtotalAmount = draft.subtotalAmount,
                gstEnabled = draft.gstEnabled,
                gstRateApplied = if (draft.gstEnabled) draft.selectedGstSlab else 0,
                cgstAmount = draft.cgstAmount,
                sgstAmount = draft.sgstAmount,
                discountAmount = draft.discountAmount,
                grandTotalAmount = draft.grandTotalAmount,
                cashReceived = draft.cashReceivedAmount,
                paymentMode = draft.paymentMode
            )

            val halfRateFraction = if (draft.gstEnabled) (draft.selectedGstSlab / 200.0) else 0.0
            val lineEntities = draft.cartItems.map { cartItem ->
                val lineSub = cartItem.lineSubtotal
                val lineCgst = BillingDraftState.roundCurrency(lineSub * halfRateFraction)
                val lineSgst = BillingDraftState.roundCurrency(lineSub * halfRateFraction)
                InvoiceLineItemEntity(
                    invoiceId = 0L,
                    billNumber = invoiceEntity.billNumber,
                    itemId = cartItem.itemId,
                    itemName = cartItem.itemName,
                    barcode = cartItem.barcode,
                    category = cartItem.category,
                    unit = cartItem.unit,
                    hsnCode = cartItem.hsnCode,
                    quantity = cartItem.quantity,
                    purchaseRate = cartItem.purchaseRate,
                    unitPrice = cartItem.unitPrice,
                    gstRate = if (draft.gstEnabled) draft.selectedGstSlab else 0,
                    cgstAmount = lineCgst,
                    sgstAmount = lineSgst,
                    lineSubtotal = lineSub,
                    lineTotal = BillingDraftState.roundCurrency(lineSub + lineCgst + lineSgst),
                    timestamp = now
                )
            }

            val savedId = repository.createInvoice(invoiceEntity, lineEntities)
            val savedInvoice = invoiceEntity.copy(id = savedId)
            val savedLines = lineEntities.map { it.copy(invoiceId = savedId) }

            _activeReceiptPreview.value = Pair(savedInvoice, savedLines)
            _statusMessage.value = "Invoice ${savedInvoice.billNumber} saved & stock updated!"

            val nextCount = repository.getAllInvoicesOnce().size
            val settings = settingsState.value
            _billingDraft.value = createFreshBillingDraft(
                nextSeq = 1001 + nextCount,
                gstEnabled = settings.gstEnabled,
                gstSlab = settings.defaultGstSlab
            )
            onSuccess(savedInvoice, savedLines)
        }
    }

    fun openInvoiceReceiptPreview(invoice: SalesInvoiceEntity) {
        viewModelScope.launch {
            val lines = repository.getLineItemsForInvoice(invoice.id)
            _activeReceiptPreview.value = Pair(invoice, lines)
        }
    }

    fun closeReceiptPreview() {
        _activeReceiptPreview.value = null
    }

    // Recycle Bin State for deleted Invoices (with their line items) and Inventory Items
    private val _deletedInvoices = MutableStateFlow<List<Pair<SalesInvoiceEntity, List<InvoiceLineItemEntity>>>>(emptyList())
    val deletedInvoices: StateFlow<List<Pair<SalesInvoiceEntity, List<InvoiceLineItemEntity>>>> =
        _deletedInvoices.asStateFlow()

    private val _deletedInventoryItems = MutableStateFlow<List<InventoryItemEntity>>(emptyList())
    val deletedInventoryItems: StateFlow<List<InventoryItemEntity>> =
        _deletedInventoryItems.asStateFlow()

    fun deleteInvoice(invoice: SalesInvoiceEntity) {
        viewModelScope.launch {
            val lines = repository.getLineItemsForInvoice(invoice.id)
            _deletedInvoices.update { listOf(invoice to lines) + it }
            repository.deleteInvoice(invoice)
            _statusMessage.value = "Moved invoice ${invoice.billNumber} to Recycle Bin & restored stock."
        }
    }

    fun restoreDeletedInvoice(invoice: SalesInvoiceEntity, lines: List<InvoiceLineItemEntity>) {
        viewModelScope.launch {
            repository.createInvoice(invoice.copy(id = 0L), lines.map { it.copy(id = 0L) })
            _deletedInvoices.update { list -> list.filterNot { it.first.id == invoice.id && it.first.billNumber == invoice.billNumber } }
            _statusMessage.value = "Restored invoice ${invoice.billNumber} from Recycle Bin."
        }
    }

    fun restoreDeletedInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.saveInventoryItem(item.copy(id = 0L))
            _deletedInventoryItems.update { list -> list.filterNot { it.id == item.id && it.name == item.name } }
            _statusMessage.value = "Restored '${item.name}' from Recycle Bin."
        }
    }

    fun clearRecycleBin() {
        _deletedInvoices.value = emptyList()
        _deletedInventoryItems.value = emptyList()
        _statusMessage.value = "Recycle Bin emptied permanently."
    }

    // --- Inventory Management Operations ---
    fun setInventorySearchQuery(query: String) {
        _inventorySearchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setInventorySortOption(option: InventorySortOption) {
        _inventorySortOption.value = option
    }

    fun saveInventoryItem(
        id: Long = 0L,
        name: String,
        barcode: String,
        category: String,
        unit: String,
        hsnCode: String,
        purchaseRate: Double,
        salesRate: Double,
        stockQuantity: Double,
        minStockAlert: Double,
        gstRate: Int
    ) {
        if (name.isBlank()) {
            _statusMessage.value = "Item name is required."
            return
        }
        viewModelScope.launch {
            val finalBarcode = barcode.trim().ifBlank {
                "890" + (System.currentTimeMillis() % 10000000000L).toString().padStart(10, '0')
            }
            val entity = InventoryItemEntity(
                id = id,
                name = name.trim(),
                barcode = finalBarcode,
                category = category.trim().ifBlank { "General" },
                unit = unit.trim().uppercase(Locale.US).ifBlank { "PCS" },
                hsnCode = hsnCode.trim(),
                purchaseRate = purchaseRate.coerceAtLeast(0.0),
                salesRate = salesRate.coerceAtLeast(0.0),
                stockQuantity = stockQuantity.coerceAtLeast(0.0),
                minStockAlert = minStockAlert.coerceAtLeast(0.0),
                gstRate = gstRate,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveInventoryItem(entity)
            _statusMessage.value = if (id == 0L) "Added '${entity.name}' to inventory." else "Updated '${entity.name}'."
        }
    }

    fun adjustItemStock(item: InventoryItemEntity, delta: Double) {
        viewModelScope.launch {
            repository.adjustStock(item.id, delta)
        }
    }

    fun restockAllLowStockItems(quantityToAdd: Double = 10.0) {
        viewModelScope.launch {
            val lowItems = inventoryItems.value.filter { it.isLowStock }
            lowItems.forEach { item ->
                repository.adjustStock(item.id, quantityToAdd)
            }
            _statusMessage.value = "Restocked ${lowItems.size} low-stock items (+${quantityToAdd.toInt()} each)."
        }
    }

    fun importSampleBulkCatalogPack() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val samplePack = listOf(
                InventoryItemEntity(
                    name = "Logitech B170 Wireless Mouse",
                    barcode = "QR-LOGI-170",
                    category = "Electronics",
                    unit = "PCS",
                    hsnCode = "8471",
                    purchaseRate = 410.0,
                    salesRate = 595.0,
                    stockQuantity = 24.0,
                    minStockAlert = 6.0,
                    gstRate = 18,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "A4 Copier Paper Ream (500 Sheets)",
                    barcode = "QR-PAPER-A4",
                    category = "Stationery",
                    unit = "PACK",
                    hsnCode = "4802",
                    purchaseRate = 215.0,
                    salesRate = 290.0,
                    stockQuantity = 40.0,
                    minStockAlert = 10.0,
                    gstRate = 12,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "Arabica Roast Coffee Beans 500g",
                    barcode = "QR-COFFEE-500",
                    category = "Beverages",
                    unit = "PACK",
                    hsnCode = "0901",
                    purchaseRate = 320.0,
                    salesRate = 450.0,
                    stockQuantity = 18.0,
                    minStockAlert = 5.0,
                    gstRate = 5,
                    updatedAt = now
                )
            )
            samplePack.forEach { repository.saveInventoryItem(it) }
            _statusMessage.value = "Imported ${samplePack.size} catalog items into Inventory!"
        }
    }

    fun deleteInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            _deletedInventoryItems.update { listOf(item) + it }
            repository.deleteInventoryItem(item)
            _statusMessage.value = "Moved '${item.name}' to Recycle Bin."
        }
    }

    // --- Settings, Theme, GST, Business Profile & Thermal Printer ---
    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setGstEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setGstEnabled(enabled)
            _billingDraft.update { it.copy(gstEnabled = enabled) }
        }
    }

    fun setDefaultGstSlab(slab: Int) {
        viewModelScope.launch {
            prefsRepo.setDefaultGstSlab(slab)
            _billingDraft.update { it.copy(selectedGstSlab = slab) }
        }
    }

    fun setOpeningCashInHand(amount: Double) {
        viewModelScope.launch {
            prefsRepo.setOpeningCashInHand(amount)
            _statusMessage.value = "Updated Opening Cash balance."
        }
    }

    fun saveBusinessProfile(
        name: String,
        category: String,
        gstin: String,
        phone: String,
        address: String,
        logoUri: String,
        bannerUri: String
    ) {
        viewModelScope.launch {
            prefsRepo.updateBusinessProfile(
                name = name.trim(),
                category = category.trim(),
                gstin = gstin.trim().uppercase(Locale.US),
                phone = phone.trim(),
                address = address.trim(),
                logoUri = logoUri,
                bannerUri = bannerUri
            )
            _statusMessage.value = "Business profile saved!"
        }
    }

    fun saveThermalPrinterSettings(
        paperWidth: String,
        printerName: String,
        printerAddress: String,
        printHeader: Boolean,
        printCustomer: Boolean,
        printHsn: Boolean,
        printGst: Boolean,
        printCash: Boolean,
        printBarcode: Boolean,
        footerMessage: String
    ) {
        viewModelScope.launch {
            prefsRepo.updateThermalPrinterSettings(
                paperWidth = paperWidth,
                printerName = printerName,
                printerAddress = printerAddress,
                printHeader = printHeader,
                printCustomer = printCustomer,
                printHsn = printHsn,
                printGst = printGst,
                printCash = printCash,
                printBarcode = printBarcode,
                footerMessage = footerMessage
            )
            _statusMessage.value = "Thermal printer configuration saved!"
        }
    }

    fun saveTransactionSettings(
        invoicePrefix: String,
        defaultPaymentMode: String,
        enableItemDiscount: Boolean,
        enableExtraDiscount: Boolean,
        autoRoundOff: Boolean,
        autoPreviewReceipt: Boolean
    ) {
        viewModelScope.launch {
            prefsRepo.updateTransactionSettings(
                invoicePrefix = invoicePrefix,
                defaultPaymentMode = defaultPaymentMode,
                enableItemDiscount = enableItemDiscount,
                enableExtraDiscount = enableExtraDiscount,
                autoRoundOff = autoRoundOff,
                autoPreviewReceipt = autoPreviewReceipt
            )
            _statusMessage.value = "Transaction settings saved!"
        }
    }

    fun saveTaxExtraSettings(defaultGstTaxMode: String, enforceHsnCode: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateTaxExtraSettings(defaultGstTaxMode, enforceHsnCode)
            _statusMessage.value = "Taxes & GST configuration saved!"
        }
    }

    fun saveSmsSettings(autoSendSms: Boolean, includeGst: Boolean, template: String) {
        viewModelScope.launch {
            prefsRepo.updateSmsSettings(autoSendSms, includeGst, template)
            _statusMessage.value = "Transaction SMS settings saved!"
        }
    }

    fun saveReminderSettings(enablePayment: Boolean, enableLowStock: Boolean, frequency: String) {
        viewModelScope.launch {
            prefsRepo.updateReminderSettings(enablePayment, enableLowStock, frequency)
            _statusMessage.value = "Reminder settings saved!"
        }
    }

    fun saveItemSettings(
        defaultUnit: String,
        defaultLowStock: Double,
        mrp: Boolean,
        batch: Boolean,
        expiry: Boolean,
        wholesale: Boolean,
        rack: Boolean
    ) {
        viewModelScope.launch {
            prefsRepo.updateItemSettings(defaultUnit, defaultLowStock, mrp, batch, expiry, wholesale, rack)
            _statusMessage.value = "Item & Additional Fields settings saved!"
        }
    }

    fun saveStaffUsers(users: List<com.example.data.preferences.StaffUserRecord>) {
        viewModelScope.launch {
            prefsRepo.saveStaffUsers(users)
            _statusMessage.value = "Updated User Management list."
        }
    }

    fun saveParties(parties: List<com.example.data.preferences.PartyRecord>) {
        viewModelScope.launch {
            prefsRepo.saveParties(parties)
            _statusMessage.value = "Saved Party directory."
        }
    }

    fun saveBankAccounts(accounts: List<com.example.data.preferences.BankAccountRecord>) {
        viewModelScope.launch {
            prefsRepo.saveBankAccounts(accounts)
            _statusMessage.value = "Updated Bank Accounts."
        }
    }

    fun saveCustomReminders(reminders: List<com.example.data.preferences.CustomReminderRecord>) {
        viewModelScope.launch {
            prefsRepo.saveCustomReminders(reminders)
            _statusMessage.value = "Updated Reminders."
        }
    }

    // --- PDF, Excel (CSV), and JSON Backup Export/Import ---
    fun exportPdfReport(context: Context, uri: Uri, reportTitle: String, filteredInvoices: List<SalesInvoiceEntity>) {
        viewModelScope.launch {
            val result = ExportBackupManager.writePdfReportToUri(
                context = context,
                uri = uri,
                reportTitle = reportTitle,
                settings = settingsState.value,
                invoices = filteredInvoices,
                itemStats = itemPerformanceStats.value,
                inventoryItems = inventoryItems.value
            )
            _statusMessage.value = result.getOrElse { "PDF export failed: ${it.localizedMessage}" }
        }
    }

    fun exportExcelCsv(context: Context, uri: Uri, filteredInvoices: List<SalesInvoiceEntity>) {
        viewModelScope.launch {
            val result = ExportBackupManager.writeExcelCsvToUri(
                context = context,
                uri = uri,
                invoices = filteredInvoices,
                itemStats = itemPerformanceStats.value,
                inventoryItems = inventoryItems.value
            )
            _statusMessage.value = result.getOrElse { "Excel CSV export failed: ${it.localizedMessage}" }
        }
    }

    fun exportJsonDatabaseBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = repository.exportDatabaseToJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                _statusMessage.value = "Database backup exported to JSON successfully."
            } catch (e: Exception) {
                _statusMessage.value = "Backup export failed: ${e.localizedMessage}"
            }
        }
    }

    fun importJsonDatabaseBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json.isNullOrBlank()) {
                    _statusMessage.value = "Selected backup file is empty."
                    return@launch
                }
                val (itemsCount, invoicesCount) = repository.importDatabaseFromJson(json)
                refreshNextBillNumber()
                _statusMessage.value = "Restored $itemsCount inventory items and $invoicesCount sales invoices!"
            } catch (e: Exception) {
                _statusMessage.value = "Backup import failed: ${e.localizedMessage}"
            }
        }
    }
}
