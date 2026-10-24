package com.example

import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.SalesInvoiceEntity
import com.example.data.preferences.BusinessAndAppSettings
import com.example.ui.CartLineItem
import com.example.ui.GstTaxMode
import com.example.ui.InvoiceUiState
import com.example.util.ThermalPrinterManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun cartLineItem_computesItemWiseAmount_discountPercent_andAfterDiscountPrice() {
        val item = CartLineItem(
            itemId = 1L,
            itemName = "Wireless Scanner",
            barcode = "8901030845210",
            category = "Electronics",
            unit = "PCS",
            hsnCode = "8471",
            purchaseRate = 1400.0,
            unitPrice = 2000.0,
            quantity = 2.0,
            discountPercent = 15.0, // 15% discount
            gstRate = 18,
            availableStock = 20.0
        )

        // Item-wise gross amount = 2 * 2000 = 4000.0
        assertEquals(4000.0, item.grossAmount, 0.001)
        // Item-wise discount amount = 15% of 4000 = 600.0
        assertEquals(600.0, item.itemDiscountAmount, 0.001)
        // Per-unit price after discount = 2000 * 0.85 = 1700.0
        assertEquals(1700.0, item.unitPriceAfterDiscount, 0.001)
        // Item-wise after-discount price = 4000 - 600 = 3400.0
        assertEquals(3400.0, item.afterDiscountPrice, 0.001)
    }

    @Test
    fun invoiceUiState_appliesItemWiseDiscountBeforeGstCalculation() {
        val state = InvoiceUiState(
            cartItems = listOf(
                CartLineItem(
                    itemId = 1L,
                    itemName = "Scanner",
                    barcode = "8901",
                    category = "Electronics",
                    unit = "PCS",
                    hsnCode = "8471",
                    purchaseRate = 1000.0,
                    unitPrice = 1000.0,
                    quantity = 2.0,
                    discountPercent = 10.0, // Gross 2000, Disc 200, After-Disc 1800
                    gstRate = 18,
                    availableStock = 10.0
                )
            ),
            gstEnabled = true,
            gstTaxMode = GstTaxMode.UNIFORM_SLAB,
            selectedGstSlab = 18,
            cashReceivedInput = "2200"
        )

        assertEquals(2000.0, state.grossItemAmountTotal, 0.001)
        assertEquals(200.0, state.itemWiseDiscountTotal, 0.001)
        assertEquals(1800.0, state.subtotalAmount, 0.001) // After-discount item price
        assertEquals(162.0, state.cgstAmount, 0.001)      // 9% of 1800
        assertEquals(162.0, state.sgstAmount, 0.001)      // 9% of 1800
        assertEquals(2124.0, state.grandTotalAmount, 0.001)
        assertEquals(76.0, state.balanceOrChange, 0.001)
    }

    @Test
    fun thermalReceiptFormatter_respects58mmAnd80mmWidths_andPrintsItemDiscounts() {
        val invoice = SalesInvoiceEntity(
            id = 1L,
            billNumber = "INV-2026-1001",
            customerName = "Test Customer",
            customerMobile = "9876543210",
            timestamp = 1760000000000L,
            dateString = "2026-10-23",
            monthString = "2026-10",
            yearString = "2026",
            totalQuantity = 1.0,
            subtotalAmount = 450.0,
            gstEnabled = true,
            gstRateApplied = 12,
            cgstAmount = 27.0,
            sgstAmount = 27.0,
            discountAmount = 50.0,
            grandTotalAmount = 504.0,
            cashReceived = 600.0,
            paymentMode = "Cash"
        )
        val lines = listOf(
            InvoiceLineItemEntity(
                invoiceId = 1L,
                billNumber = "INV-2026-1001",
                itemId = 1L,
                itemName = "Thermal Roll Pack",
                barcode = "890123",
                category = "Stationery",
                unit = "PACK",
                hsnCode = "4811",
                quantity = 1.0,
                purchaseRate = 300.0,
                unitPrice = 500.0,
                grossAmount = 500.0,
                discountPercent = 10.0,
                discountAmount = 50.0,
                afterDiscountPrice = 450.0,
                gstRate = 12,
                cgstAmount = 27.0,
                sgstAmount = 27.0,
                lineSubtotal = 450.0,
                lineTotal = 504.0,
                timestamp = 1760000000000L
            )
        )

        val settings58 = BusinessAndAppSettings(printerPaperWidth = "58mm")
        val receipt58 = ThermalPrinterManager.formatThermalReceiptText(invoice, lines, settings58)
        assertTrue(receipt58.all { it.length <= 32 })
        assertTrue(receipt58.any { it.contains("Disc (10%)") })
        assertTrue(receipt58.any { it.contains("After:") })

        val settings80 = BusinessAndAppSettings(printerPaperWidth = "80mm")
        val receipt80 = ThermalPrinterManager.formatThermalReceiptText(invoice, lines, settings80)
        assertTrue(receipt80.all { it.length <= 48 })
    }
}
