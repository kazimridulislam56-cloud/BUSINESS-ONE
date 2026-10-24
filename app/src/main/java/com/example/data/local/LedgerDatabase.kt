package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        InventoryItemEntity::class,
        SalesInvoiceEntity::class,
        InvoiceLineItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getInstance(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledgerflow_pos.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedStarterCatalogIfEmpty(dao: LedgerDao) {
            if (dao.getInventoryCount() > 0) return

            val now = System.currentTimeMillis()
            val starterItems = listOf(
                InventoryItemEntity(
                    name = "Wireless Barcode Scanner 2D",
                    barcode = "8901030845210",
                    category = "Electronics",
                    unit = "PCS",
                    hsnCode = "8471",
                    purchaseRate = 1450.0,
                    salesRate = 2199.0,
                    stockQuantity = 24.0,
                    minStockAlert = 5.0,
                    gstRate = 18,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "58mm Thermal Paper Roll (Pack of 10)",
                    barcode = "8901030845227",
                    category = "Stationery",
                    unit = "PACK",
                    hsnCode = "4811",
                    purchaseRate = 180.0,
                    salesRate = 290.0,
                    stockQuantity = 65.0,
                    minStockAlert = 15.0,
                    gstRate = 12,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "Arabica Roasted Coffee Beans 1kg",
                    barcode = "8901030845234",
                    category = "Groceries",
                    unit = "KG",
                    hsnCode = "0901",
                    purchaseRate = 620.0,
                    salesRate = 890.0,
                    stockQuantity = 18.0,
                    minStockAlert = 6.0,
                    gstRate = 5,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "Organic Cold-Pressed Olive Oil 1L",
                    barcode = "8901030845241",
                    category = "Groceries",
                    unit = "LTR",
                    hsnCode = "1509",
                    purchaseRate = 540.0,
                    salesRate = 780.0,
                    stockQuantity = 14.0,
                    minStockAlert = 5.0,
                    gstRate = 5,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "USB-C FastGaN Power Adapter 65W",
                    barcode = "8901030845258",
                    category = "Electronics",
                    unit = "PCS",
                    hsnCode = "8504",
                    purchaseRate = 790.0,
                    salesRate = 1299.0,
                    stockQuantity = 4.0, // Low stock highlight
                    minStockAlert = 6.0,
                    gstRate = 18,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "Stainless Steel Insulated Flask 750ml",
                    barcode = "8901030845265",
                    category = "Home & Kitchen",
                    unit = "PCS",
                    hsnCode = "7323",
                    purchaseRate = 340.0,
                    salesRate = 550.0,
                    stockQuantity = 32.0,
                    minStockAlert = 8.0,
                    gstRate = 12,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "Heavy-Duty Cash Drawer 5-Bill",
                    barcode = "8901030845272",
                    category = "Hardware",
                    unit = "BOX",
                    hsnCode = "8473",
                    purchaseRate = 2100.0,
                    salesRate = 3250.0,
                    stockQuantity = 9.0,
                    minStockAlert = 3.0,
                    gstRate = 18,
                    updatedAt = now
                ),
                InventoryItemEntity(
                    name = "Luxury Sparkling Beverage Case",
                    barcode = "8901030845289",
                    category = "Beverages",
                    unit = "BOX",
                    hsnCode = "2202",
                    purchaseRate = 480.0,
                    salesRate = 720.0,
                    stockQuantity = 3.0, // Low stock highlight
                    minStockAlert = 5.0,
                    gstRate = 28,
                    updatedAt = now
                )
            )

            val insertedIds = starterItems.map { dao.insertInventoryItem(it) }
            val itemsWithIds = starterItems.mapIndexed { idx, item -> item.copy(id = insertedIds[idx]) }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val yearFormat = SimpleDateFormat("yyyy", Locale.US)

            // Seed 3 realistic invoices (2 from today, 1 from earlier this month)
            val todayDate = Date(now)
            val todayStr = dateFormat.format(todayDate)
            val monthStr = monthFormat.format(todayDate)
            val yearStr = yearFormat.format(todayDate)

            // Invoice 1: Today Morning
            val item0 = itemsWithIds[0]
            val item1 = itemsWithIds[1]
            val sub1 = (item0.salesRate * 1) + (item1.salesRate * 2) // 2199 + 580 = 2779
            val cgst1 = sub1 * 0.09
            val sgst1 = sub1 * 0.09
            val grand1 = Math.round((sub1 + cgst1 + sgst1) * 100.0) / 100.0

            dao.createInvoiceAndDeductStock(
                invoice = SalesInvoiceEntity(
                    billNumber = "INV-2026-1001",
                    customerName = "Aarav Mehta",
                    customerMobile = "9820411290",
                    timestamp = now - 3_600_000L * 2,
                    dateString = todayStr,
                    monthString = monthStr,
                    yearString = yearStr,
                    totalQuantity = 3.0,
                    subtotalAmount = sub1,
                    gstEnabled = true,
                    gstRateApplied = 18,
                    cgstAmount = cgst1,
                    sgstAmount = sgst1,
                    discountAmount = 0.0,
                    grandTotalAmount = grand1,
                    cashReceived = 3300.0,
                    paymentMode = "Cash"
                ),
                lineItems = listOf(
                    InvoiceLineItemEntity(
                        invoiceId = 0L,
                        billNumber = "INV-2026-1001",
                        itemId = item0.id,
                        itemName = item0.name,
                        barcode = item0.barcode,
                        category = item0.category,
                        unit = item0.unit,
                        hsnCode = item0.hsnCode,
                        quantity = 1.0,
                        purchaseRate = item0.purchaseRate,
                        unitPrice = item0.salesRate,
                        gstRate = 18,
                        cgstAmount = item0.salesRate * 0.09,
                        sgstAmount = item0.salesRate * 0.09,
                        lineSubtotal = item0.salesRate,
                        lineTotal = item0.salesRate * 1.18,
                        timestamp = now - 3_600_000L * 2
                    ),
                    InvoiceLineItemEntity(
                        invoiceId = 0L,
                        billNumber = "INV-2026-1001",
                        itemId = item1.id,
                        itemName = item1.name,
                        barcode = item1.barcode,
                        category = item1.category,
                        unit = item1.unit,
                        hsnCode = item1.hsnCode,
                        quantity = 2.0,
                        purchaseRate = item1.purchaseRate,
                        unitPrice = item1.salesRate,
                        gstRate = 18,
                        cgstAmount = (item1.salesRate * 2) * 0.09,
                        sgstAmount = (item1.salesRate * 2) * 0.09,
                        lineSubtotal = item1.salesRate * 2,
                        lineTotal = (item1.salesRate * 2) * 1.18,
                        timestamp = now - 3_600_000L * 2
                    )
                )
            )

            // Invoice 2: Today Afternoon
            val item2 = itemsWithIds[2]
            val item5 = itemsWithIds[5]
            val sub2 = (item2.salesRate * 2) + (item5.salesRate * 1) // 1780 + 550 = 2330
            val cgst2 = sub2 * 0.06
            val sgst2 = sub2 * 0.06
            val grand2 = Math.round((sub2 + cgst2 + sgst2) * 100.0) / 100.0

            dao.createInvoiceAndDeductStock(
                invoice = SalesInvoiceEntity(
                    billNumber = "INV-2026-1002",
                    customerName = "Priya Nair",
                    customerMobile = "9845092318",
                    timestamp = now - 1_800_000L,
                    dateString = todayStr,
                    monthString = monthStr,
                    yearString = yearStr,
                    totalQuantity = 3.0,
                    subtotalAmount = sub2,
                    gstEnabled = true,
                    gstRateApplied = 12,
                    cgstAmount = cgst2,
                    sgstAmount = sgst2,
                    discountAmount = 0.0,
                    grandTotalAmount = grand2,
                    cashReceived = grand2,
                    paymentMode = "Cash"
                ),
                lineItems = listOf(
                    InvoiceLineItemEntity(
                        invoiceId = 0L,
                        billNumber = "INV-2026-1002",
                        itemId = item2.id,
                        itemName = item2.name,
                        barcode = item2.barcode,
                        category = item2.category,
                        unit = item2.unit,
                        hsnCode = item2.hsnCode,
                        quantity = 2.0,
                        purchaseRate = item2.purchaseRate,
                        unitPrice = item2.salesRate,
                        gstRate = 12,
                        cgstAmount = (item2.salesRate * 2) * 0.06,
                        sgstAmount = (item2.salesRate * 2) * 0.06,
                        lineSubtotal = item2.salesRate * 2,
                        lineTotal = (item2.salesRate * 2) * 1.12,
                        timestamp = now - 1_800_000L
                    ),
                    InvoiceLineItemEntity(
                        invoiceId = 0L,
                        billNumber = "INV-2026-1002",
                        itemId = item5.id,
                        itemName = item5.name,
                        barcode = item5.barcode,
                        category = item5.category,
                        unit = item5.unit,
                        hsnCode = item5.hsnCode,
                        quantity = 1.0,
                        purchaseRate = item5.purchaseRate,
                        unitPrice = item5.salesRate,
                        gstRate = 12,
                        cgstAmount = item5.salesRate * 0.06,
                        sgstAmount = item5.salesRate * 0.06,
                        lineSubtotal = item5.salesRate,
                        lineTotal = item5.salesRate * 1.12,
                        timestamp = now - 1_800_000L
                    )
                )
            )

            // Invoice 3: Earlier this month
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, -3)
            }
            val prevDate = cal.time
            val item6 = itemsWithIds[6]
            val sub3 = item6.salesRate * 1.0
            val cgst3 = sub3 * 0.09
            val sgst3 = sub3 * 0.09
            val grand3 = Math.round((sub3 + cgst3 + sgst3) * 100.0) / 100.0

            dao.createInvoiceAndDeductStock(
                invoice = SalesInvoiceEntity(
                    billNumber = "INV-2026-1000",
                    customerName = "Rohan Deshmukh",
                    customerMobile = "9765110944",
                    timestamp = prevDate.time,
                    dateString = dateFormat.format(prevDate),
                    monthString = monthFormat.format(prevDate),
                    yearString = yearFormat.format(prevDate),
                    totalQuantity = 1.0,
                    subtotalAmount = sub3,
                    gstEnabled = true,
                    gstRateApplied = 18,
                    cgstAmount = cgst3,
                    sgstAmount = sgst3,
                    discountAmount = 0.0,
                    grandTotalAmount = grand3,
                    cashReceived = 4000.0,
                    paymentMode = "Cash"
                ),
                lineItems = listOf(
                    InvoiceLineItemEntity(
                        invoiceId = 0L,
                        billNumber = "INV-2026-1000",
                        itemId = item6.id,
                        itemName = item6.name,
                        barcode = item6.barcode,
                        category = item6.category,
                        unit = item6.unit,
                        hsnCode = item6.hsnCode,
                        quantity = 1.0,
                        purchaseRate = item6.purchaseRate,
                        unitPrice = item6.salesRate,
                        gstRate = 18,
                        cgstAmount = cgst3,
                        sgstAmount = sgst3,
                        lineSubtotal = sub3,
                        lineTotal = grand3,
                        timestamp = prevDate.time
                    )
                )
            )
        }
    }
}
