package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.LedgerDatabase
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Supported GST tax rate slabs (5%, 12%, 18%, 28%) split equally into CGST and SGST.
 */
val SUPPORTED_GST_SLABS = listOf(5, 12, 18, 28)

enum class GstTaxMode(val label: String) {
    UNIFORM_SLAB("Invoice Slab"),
    ITEM_WISE_SLAB("Per-Item Slab")
}

data class GstSlabBreakup(
    val gstRate: Int,
    val cgstRate: Double,
    val sgstRate: Double,
    val taxableValue: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val totalTaxAmount: Double
)

data class InvoiceUiState(
    val billNumber: String = "INV-2026-1003",
    val customerName: String = "",
    val customerMobile: String = "",
    val dateDisplay: String = "",
    val timeDisplay: String = "",
    val cartItems: List<CartLineItem> = emptyList(),
    val gstEnabled: Boolean = true,
    val gstTaxMode: GstTaxMode = GstTaxMode.UNIFORM_SLAB,
    val selectedGstSlab: Int = 18, // 5, 12, 18, 28
    val discountInput: String = "",
    val cashReceivedInput: String = "",
    val paymentMode: String = "Cash",
    val validationError: String? = null
) {
    val totalItemsCount: Int
        get() = cartItems.size

    val totalQuantity: Double
        get() = roundTwoDecimals(cartItems.sumOf { it.quantity })

    /** Sum of item-wise gross amounts before any item or bill discount */
    val grossItemAmountTotal: Double
        get() = roundTwoDecimals(cartItems.sumOf { it.grossAmount })

    /** Sum of all item-wise discount amounts */
    val itemWiseDiscountTotal: Double
        get() = roundTwoDecimals(cartItems.sumOf { it.itemDiscountAmount })

    /** Sum of item-wise after-discount prices */
    val subtotalAmount: Double
        get() = roundTwoDecimals(cartItems.sumOf { it.afterDiscountPrice })

    /** Optional extra bill-level flat discount */
    val extraBillDiscountAmount: Double
        get() = (discountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotalAmount)

    /** Combined total discount (Item-wise discounts + Extra bill discount) */
    val discountAmount: Double
        get() = roundTwoDecimals(itemWiseDiscountTotal + extraBillDiscountAmount)

    /** Net taxable amount after all discounts */
    val taxableAmount: Double
        get() = roundTwoDecimals((subtotalAmount - extraBillDiscountAmount).coerceAtLeast(0.0))

    /**
     * Computes slab-wise CGST (50%) and SGST (50%) breakdown across active items.
     */
    val slabBreakups: List<GstSlabBreakup>
        get() {
            if (!gstEnabled || cartItems.isEmpty() || taxableAmount <= 0.0) return emptyList()
            val extraDiscountRatio = if (subtotalAmount > 0.0) {
                (subtotalAmount - extraBillDiscountAmount) / subtotalAmount
            } else {
                1.0
            }

            return if (gstTaxMode == GstTaxMode.UNIFORM_SLAB) {
                val slab = selectedGstSlab
                val halfRate = slab / 2.0
                val cgst = roundTwoDecimals(taxableAmount * (halfRate / 100.0))
                val sgst = roundTwoDecimals(taxableAmount * (halfRate / 100.0))
                listOf(
                    GstSlabBreakup(
                        gstRate = slab,
                        cgstRate = halfRate,
                        sgstRate = halfRate,
                        taxableValue = taxableAmount,
                        cgstAmount = cgst,
                        sgstAmount = sgst,
                        totalTaxAmount = roundTwoDecimals(cgst + sgst)
                    )
                )
            } else {
                cartItems
                    .groupBy { it.gstRate }
                    .map { (slab, lines) ->
                        val groupAfterDisc = lines.sumOf { it.afterDiscountPrice }
                        val discountedTaxable = roundTwoDecimals(groupAfterDisc * extraDiscountRatio)
                        val halfRate = slab / 2.0
                        val cgst = roundTwoDecimals(discountedTaxable * (halfRate / 100.0))
                        val sgst = roundTwoDecimals(discountedTaxable * (halfRate / 100.0))
                        GstSlabBreakup(
                            gstRate = slab,
                            cgstRate = halfRate,
                            sgstRate = halfRate,
                            taxableValue = discountedTaxable,
                            cgstAmount = cgst,
                            sgstAmount = sgst,
                            totalTaxAmount = roundTwoDecimals(cgst + sgst)
                        )
                    }
                    .sortedBy { it.gstRate }
            }
        }

    val cgstAmount: Double
        get() = roundTwoDecimals(slabBreakups.sumOf { it.cgstAmount })

    val sgstAmount: Double
        get() = roundTwoDecimals(slabBreakups.sumOf { it.sgstAmount })

    val totalGstAmount: Double
        get() = roundTwoDecimals(cgstAmount + sgstAmount)

    val grandTotalAmount: Double
        get() = roundTwoDecimals(taxableAmount + totalGstAmount)

    val cashReceivedAmount: Double
        get() = cashReceivedInput.toDoubleOrNull() ?: grandTotalAmount

    val balanceOrChange: Double
        get() = roundTwoDecimals(cashReceivedAmount - grandTotalAmount)

    companion object {
        fun roundTwoDecimals(value: Double): Double {
            return (value * 100.0).roundToInt() / 100.0
        }
    }
}

/**
 * Dedicated ViewModel for Sales Invoice Generation, dynamic item additions,
 * item-wise discount percentage & after-discount price calculation,
 * CGST/SGST tax calculation across 5%/12%/18%/28% slabs, and total sum computation.
 */
class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LedgerDatabase.getInstance(application)
    private val repository = LedgerRepository(database.ledgerDao())
    private val prefsRepo = AppPreferencesRepository(application)

    private val _uiState = MutableStateFlow(createInitialInvoiceState(1003, true, 18))
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshNextSequenceNumber()
        }
        viewModelScope.launch {
            prefsRepo.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    if (state.cartItems.isEmpty()) {
                        state.copy(
                            gstEnabled = settings.gstEnabled,
                            selectedGstSlab = settings.defaultGstSlab
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun createInitialInvoiceState(
        nextSeq: Int,
        gstEnabled: Boolean,
        gstSlab: Int
    ): InvoiceUiState {
        val now = Date()
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(now)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(now)
        val yearStr = SimpleDateFormat("yyyy", Locale.US).format(now)
        return InvoiceUiState(
            billNumber = "INV-$yearStr-$nextSeq",
            dateDisplay = dateStr,
            timeDisplay = timeStr,
            gstEnabled = gstEnabled,
            selectedGstSlab = if (gstSlab in SUPPORTED_GST_SLABS) gstSlab else 18
        )
    }

    suspend fun refreshNextSequenceNumber() {
        val count = repository.getAllInvoicesOnce().size
        val nextSeq = 1001 + count
        _uiState.update { current ->
            if (current.cartItems.isEmpty() && current.customerName.isBlank()) {
                createInitialInvoiceState(nextSeq, current.gstEnabled, current.selectedGstSlab)
            } else {
                current
            }
        }
    }

    fun updateCustomerHeader(
        customerName: String = _uiState.value.customerName,
        customerMobile: String = _uiState.value.customerMobile,
        billNumber: String = _uiState.value.billNumber,
        dateDisplay: String = _uiState.value.dateDisplay,
        timeDisplay: String = _uiState.value.timeDisplay
    ) {
        _uiState.update {
            it.copy(
                customerName = customerName,
                customerMobile = customerMobile,
                billNumber = billNumber,
                dateDisplay = dateDisplay,
                timeDisplay = timeDisplay,
                validationError = null
            )
        }
    }

    fun setGstEnabled(enabled: Boolean) {
        _uiState.update { it.copy(gstEnabled = enabled) }
    }

    fun setGstTaxMode(mode: GstTaxMode) {
        _uiState.update { it.copy(gstTaxMode = mode) }
    }

    fun setSelectedGstSlab(slab: Int) {
        if (slab !in SUPPORTED_GST_SLABS) return
        _uiState.update { it.copy(selectedGstSlab = slab) }
    }

    fun updateDiscountAndPayment(
        discountInput: String = _uiState.value.discountInput,
        cashReceivedInput: String = _uiState.value.cashReceivedInput,
        paymentMode: String = _uiState.value.paymentMode
    ) {
        _uiState.update {
            it.copy(
                discountInput = discountInput,
                cashReceivedInput = cashReceivedInput,
                paymentMode = paymentMode
            )
        }
    }

    fun setQuickCashTender(amount: Double) {
        _uiState.update {
            it.copy(cashReceivedInput = String.format(Locale.US, "%.2f", amount))
        }
    }

    fun addItemFromInventory(
        item: InventoryItemEntity,
        quantityToAdd: Double = 1.0,
        discountPercent: Double = 0.0
    ) {
        if (quantityToAdd <= 0.0) return
        _uiState.update { state ->
            val existingIdx = state.cartItems.indexOfFirst { it.itemId == item.id && it.itemId != 0L }
            val updatedItems = if (existingIdx >= 0) {
                state.cartItems.toMutableList().apply {
                    val existing = this[existingIdx]
                    this[existingIdx] = existing.copy(
                        quantity = InvoiceUiState.roundTwoDecimals(existing.quantity + quantityToAdd)
                    )
                }
            } else {
                state.cartItems + CartLineItem(
                    itemId = item.id,
                    itemName = item.name,
                    barcode = item.barcode,
                    category = item.category,
                    unit = item.unit,
                    hsnCode = item.hsnCode,
                    purchaseRate = item.purchaseRate,
                    unitPrice = item.salesRate,
                    quantity = quantityToAdd,
                    discountPercent = discountPercent.coerceIn(0.0, 100.0),
                    gstRate = if (item.gstRate in SUPPORTED_GST_SLABS) item.gstRate else state.selectedGstSlab,
                    availableStock = item.stockQuantity
                )
            }
            state.copy(cartItems = updatedItems, validationError = null)
        }
    }

    fun addCustomLineItem(
        name: String,
        unit: String,
        hsnCode: String,
        unitPrice: Double,
        quantity: Double,
        discountPercent: Double = 0.0,
        gstRate: Int = _uiState.value.selectedGstSlab
    ) {
        if (name.isBlank() || unitPrice <= 0.0 || quantity <= 0.0) return
        _uiState.update { state ->
            val customLine = CartLineItem(
                itemId = 0L,
                itemName = name.trim(),
                barcode = "CUSTOM",
                category = "General",
                unit = unit.trim().uppercase(Locale.US).ifBlank { "PCS" },
                hsnCode = hsnCode.trim(),
                purchaseRate = InvoiceUiState.roundTwoDecimals(unitPrice * 0.75),
                unitPrice = unitPrice,
                quantity = quantity,
                discountPercent = discountPercent.coerceIn(0.0, 100.0),
                gstRate = if (gstRate in SUPPORTED_GST_SLABS) gstRate else state.selectedGstSlab,
                availableStock = 999.0
            )
            state.copy(cartItems = state.cartItems + customLine, validationError = null)
        }
    }

    fun updateLineQuantity(index: Int, newQuantity: Double) {
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val mutable = state.cartItems.toMutableList()
            if (newQuantity <= 0.0) {
                mutable.removeAt(index)
            } else {
                mutable[index] = mutable[index].copy(
                    quantity = InvoiceUiState.roundTwoDecimals(newQuantity)
                )
            }
            state.copy(cartItems = mutable)
        }
    }

    fun updateLineUnitPrice(index: Int, newPrice: Double) {
        if (newPrice < 0.0) return
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val mutable = state.cartItems.toMutableList()
            mutable[index] = mutable[index].copy(
                unitPrice = InvoiceUiState.roundTwoDecimals(newPrice)
            )
            state.copy(cartItems = mutable)
        }
    }

    fun updateLineDiscountPercent(index: Int, newDiscountPercent: Double) {
        val clamped = newDiscountPercent.coerceIn(0.0, 100.0)
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val mutable = state.cartItems.toMutableList()
            mutable[index] = mutable[index].copy(
                discountPercent = InvoiceUiState.roundTwoDecimals(clamped)
            )
            state.copy(cartItems = mutable)
        }
    }

    fun updateLineGstSlab(index: Int, gstSlab: Int) {
        if (gstSlab !in SUPPORTED_GST_SLABS) return
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val mutable = state.cartItems.toMutableList()
            mutable[index] = mutable[index].copy(gstRate = gstSlab)
            state.copy(cartItems = mutable)
        }
    }

    fun removeLineItem(index: Int) {
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val mutable = state.cartItems.toMutableList()
            mutable.removeAt(index)
            state.copy(cartItems = mutable)
        }
    }

    fun clearInvoiceDraft() {
        viewModelScope.launch {
            val count = repository.getAllInvoicesOnce().size
            val state = _uiState.value
            _uiState.value = createInitialInvoiceState(
                nextSeq = 1001 + count,
                gstEnabled = state.gstEnabled,
                gstSlab = state.selectedGstSlab
            )
        }
    }

    fun addScannedBarcodeToInvoice(
        barcode: String,
        fallbackInventory: List<InventoryItemEntity>,
        onResultMessage: (String) -> Unit
    ) {
        viewModelScope.launch {
            val raw = barcode.trim()
            val cleanCode = if (raw.contains("|")) {
                raw.substringBefore("|").trim()
            } else {
                raw
            }
            if (cleanCode.isEmpty()) return@launch

            val matched = repository.findItemByBarcode(cleanCode)
                ?: fallbackInventory.firstOrNull {
                    it.barcode.equals(cleanCode, ignoreCase = true)
                }
                ?: fallbackInventory.firstOrNull {
                    it.barcode.contains(cleanCode, ignoreCase = true) ||
                        it.name.contains(cleanCode, ignoreCase = true)
                }

            if (matched != null) {
                addItemFromInventory(matched, 1.0)
                val updatedLine = _uiState.value.cartItems.firstOrNull { it.itemId == matched.id }
                val qtyInfo = updatedLine?.quantity?.toInt() ?: 1
                onResultMessage("QR Scanned: Added '${matched.name}' (${matched.barcode}) • Bill Qty: $qtyInfo")
            } else {
                onResultMessage("No item found for QR/Code '$cleanCode'. Use 'QR Entry' in Inventory to register it.")
            }
        }
    }

    fun finalizeAndSaveInvoice(
        onComplete: (SalesInvoiceEntity, List<InvoiceLineItemEntity>) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            val msg = "Please add at least one item to generate an invoice."
            _uiState.update { it.copy(validationError = msg) }
            onError(msg)
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dateObj = Date(now)
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateObj)
            val monthString = SimpleDateFormat("yyyy-MM", Locale.US).format(dateObj)
            val yearString = SimpleDateFormat("yyyy", Locale.US).format(dateObj)

            val extraDiscountRatio = if (state.subtotalAmount > 0.0) {
                (state.subtotalAmount - state.extraBillDiscountAmount) / state.subtotalAmount
            } else {
                1.0
            }

            val effectiveHeadlineGstRate = if (!state.gstEnabled) {
                0
            } else if (state.gstTaxMode == GstTaxMode.UNIFORM_SLAB) {
                state.selectedGstSlab
            } else {
                state.cartItems.firstOrNull()?.gstRate ?: state.selectedGstSlab
            }

            val invoiceEntity = SalesInvoiceEntity(
                billNumber = state.billNumber.trim().ifBlank { "INV-$now" },
                customerName = state.customerName.trim().ifBlank { "Walk-in Customer" },
                customerMobile = state.customerMobile.trim(),
                timestamp = now,
                dateString = dateString,
                monthString = monthString,
                yearString = yearString,
                totalQuantity = state.totalQuantity,
                subtotalAmount = state.taxableAmount,
                gstEnabled = state.gstEnabled,
                gstRateApplied = effectiveHeadlineGstRate,
                cgstAmount = state.cgstAmount,
                sgstAmount = state.sgstAmount,
                discountAmount = state.discountAmount,
                grandTotalAmount = state.grandTotalAmount,
                cashReceived = state.cashReceivedAmount,
                paymentMode = state.paymentMode
            )

            val lineEntities = state.cartItems.map { cartItem ->
                val itemSlab = if (!state.gstEnabled) {
                    0
                } else if (state.gstTaxMode == GstTaxMode.UNIFORM_SLAB) {
                    state.selectedGstSlab
                } else {
                    cartItem.gstRate
                }
                val halfRateFraction = (itemSlab / 200.0)
                val afterDisc = cartItem.afterDiscountPrice
                val taxableLineSub = InvoiceUiState.roundTwoDecimals(afterDisc * extraDiscountRatio)
                val lineCgst = InvoiceUiState.roundTwoDecimals(taxableLineSub * halfRateFraction)
                val lineSgst = InvoiceUiState.roundTwoDecimals(taxableLineSub * halfRateFraction)

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
                    grossAmount = cartItem.grossAmount,
                    discountPercent = cartItem.discountPercent,
                    discountAmount = cartItem.itemDiscountAmount,
                    afterDiscountPrice = afterDisc,
                    gstRate = itemSlab,
                    cgstAmount = lineCgst,
                    sgstAmount = lineSgst,
                    lineSubtotal = taxableLineSub,
                    lineTotal = InvoiceUiState.roundTwoDecimals(taxableLineSub + lineCgst + lineSgst),
                    timestamp = now
                )
            }

            val savedId = repository.createInvoice(invoiceEntity, lineEntities)
            val savedInvoice = invoiceEntity.copy(id = savedId)
            val savedLines = lineEntities.map { it.copy(invoiceId = savedId) }

            val nextCount = repository.getAllInvoicesOnce().size
            _uiState.value = createInitialInvoiceState(
                nextSeq = 1001 + nextCount,
                gstEnabled = state.gstEnabled,
                gstSlab = state.selectedGstSlab
            )
            onComplete(savedInvoice, savedLines)
        }
    }
}
