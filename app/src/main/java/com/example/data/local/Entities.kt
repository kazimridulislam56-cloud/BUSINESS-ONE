package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [Index(value = ["barcode"]), Index(value = ["category"])]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val barcode: String,
    val category: String,
    val unit: String, // PCS, KG, BOX, LTR, PACK, MTR
    val hsnCode: String,
    val purchaseRate: Double,
    val salesRate: Double,
    val stockQuantity: Double,
    val minStockAlert: Double = 5.0,
    val gstRate: Int = 18, // 0, 5, 12, 18, 28
    val updatedAt: Long = System.currentTimeMillis()
) {
    val stockValueAtPurchase: Double
        get() = stockQuantity * purchaseRate

    val stockValueAtSales: Double
        get() = stockQuantity * salesRate

    val unitMargin: Double
        get() = salesRate - purchaseRate

    val isLowStock: Boolean
        get() = stockQuantity <= minStockAlert
}

@Entity(
    tableName = "sales_invoices",
    indices = [
        Index(value = ["billNumber"], unique = true),
        Index(value = ["dateString"]),
        Index(value = ["monthString"]),
        Index(value = ["yearString"])
    ]
)
data class SalesInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val billNumber: String,
    val customerName: String,
    val customerMobile: String,
    val timestamp: Long,
    val dateString: String, // yyyy-MM-dd
    val monthString: String, // yyyy-MM
    val yearString: String, // yyyy
    val totalQuantity: Double,
    val subtotalAmount: Double,
    val gstEnabled: Boolean,
    val gstRateApplied: Int, // 0, 5, 12, 18, 28
    val cgstAmount: Double,
    val sgstAmount: Double,
    val discountAmount: Double = 0.0,
    val grandTotalAmount: Double,
    val cashReceived: Double,
    val paymentMode: String = "Cash" // Cash, UPI / QR, Card, Part-Cash
) {
    val balanceOrChange: Double
        get() = cashReceived - grandTotalAmount

    val totalTaxAmount: Double
        get() = cgstAmount + sgstAmount
}

@Entity(
    tableName = "invoice_line_items",
    indices = [Index(value = ["invoiceId"]), Index(value = ["itemId"])]
)
data class InvoiceLineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceId: Long,
    val billNumber: String,
    val itemId: Long,
    val itemName: String,
    val barcode: String,
    val category: String,
    val unit: String,
    val hsnCode: String,
    val quantity: Double,
    val purchaseRate: Double,
    val unitPrice: Double,
    val grossAmount: Double = quantity * unitPrice,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val afterDiscountPrice: Double = quantity * unitPrice,
    val gstRate: Int,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val lineSubtotal: Double, // Equal to afterDiscountPrice (taxable line value)
    val lineTotal: Double,
    val timestamp: Long
) {
    val lineProfit: Double
        get() = afterDiscountPrice - (purchaseRate * quantity)
}
