package com.example.data.repository

import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceLineItemEntity
import com.example.data.local.LedgerDao
import com.example.data.local.SalesInvoiceEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class LedgerRepository(private val dao: LedgerDao) {

    val allInventoryItems: Flow<List<InventoryItemEntity>> = dao.observeAllInventoryItems()
    val allInvoices: Flow<List<SalesInvoiceEntity>> = dao.observeAllInvoices()
    val allLineItems: Flow<List<InvoiceLineItemEntity>> = dao.observeAllLineItems()

    suspend fun getAllInventoryItemsOnce(): List<InventoryItemEntity> = dao.getAllInventoryItemsOnce()
    suspend fun getAllInvoicesOnce(): List<SalesInvoiceEntity> = dao.getAllInvoicesOnce()
    suspend fun getAllLineItemsOnce(): List<InvoiceLineItemEntity> = dao.getAllLineItemsOnce()

    suspend fun findItemByBarcode(barcode: String): InventoryItemEntity? =
        dao.getItemByBarcode(barcode.trim())

    suspend fun saveInventoryItem(item: InventoryItemEntity): Long {
        return if (item.id == 0L) {
            dao.insertInventoryItem(item)
        } else {
            dao.updateInventoryItem(item)
            item.id
        }
    }

    suspend fun deleteInventoryItem(item: InventoryItemEntity) {
        dao.deleteInventoryItem(item)
    }

    suspend fun adjustStock(itemId: Long, delta: Double) {
        if (delta >= 0) {
            dao.incrementItemStock(itemId, delta)
        } else {
            dao.decrementItemStock(itemId, -delta)
        }
    }

    suspend fun createInvoice(
        invoice: SalesInvoiceEntity,
        lineItems: List<InvoiceLineItemEntity>
    ): Long {
        return dao.createInvoiceAndDeductStock(invoice, lineItems)
    }

    suspend fun deleteInvoice(invoice: SalesInvoiceEntity) {
        dao.deleteInvoiceAndRestoreStock(invoice)
    }

    suspend fun getLineItemsForInvoice(invoiceId: Long): List<InvoiceLineItemEntity> {
        return dao.getLineItemsForInvoice(invoiceId)
    }

    suspend fun exportDatabaseToJson(): String {
        val items = dao.getAllInventoryItemsOnce()
        val invoices = dao.getAllInvoicesOnce()
        val lineItems = dao.getAllLineItemsOnce()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val itemsArray = JSONArray()
        items.forEach { item ->
            itemsArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("barcode", item.barcode)
                    put("category", item.category)
                    put("unit", item.unit)
                    put("hsnCode", item.hsnCode)
                    put("purchaseRate", item.purchaseRate)
                    put("salesRate", item.salesRate)
                    put("stockQuantity", item.stockQuantity)
                    put("minStockAlert", item.minStockAlert)
                    put("gstRate", item.gstRate)
                    put("updatedAt", item.updatedAt)
                }
            )
        }
        root.put("inventoryItems", itemsArray)

        val invoicesArray = JSONArray()
        invoices.forEach { inv ->
            invoicesArray.put(
                JSONObject().apply {
                    put("id", inv.id)
                    put("billNumber", inv.billNumber)
                    put("customerName", inv.customerName)
                    put("customerMobile", inv.customerMobile)
                    put("timestamp", inv.timestamp)
                    put("dateString", inv.dateString)
                    put("monthString", inv.monthString)
                    put("yearString", inv.yearString)
                    put("totalQuantity", inv.totalQuantity)
                    put("subtotalAmount", inv.subtotalAmount)
                    put("gstEnabled", inv.gstEnabled)
                    put("gstRateApplied", inv.gstRateApplied)
                    put("cgstAmount", inv.cgstAmount)
                    put("sgstAmount", inv.sgstAmount)
                    put("discountAmount", inv.discountAmount)
                    put("grandTotalAmount", inv.grandTotalAmount)
                    put("cashReceived", inv.cashReceived)
                    put("paymentMode", inv.paymentMode)
                }
            )
        }
        root.put("salesInvoices", invoicesArray)

        val linesArray = JSONArray()
        lineItems.forEach { line ->
            linesArray.put(
                JSONObject().apply {
                    put("id", line.id)
                    put("invoiceId", line.invoiceId)
                    put("billNumber", line.billNumber)
                    put("itemId", line.itemId)
                    put("itemName", line.itemName)
                    put("barcode", line.barcode)
                    put("category", line.category)
                    put("unit", line.unit)
                    put("hsnCode", line.hsnCode)
                    put("quantity", line.quantity)
                    put("purchaseRate", line.purchaseRate)
                    put("unitPrice", line.unitPrice)
                    put("gstRate", line.gstRate)
                    put("cgstAmount", line.cgstAmount)
                    put("sgstAmount", line.sgstAmount)
                    put("lineSubtotal", line.lineSubtotal)
                    put("lineTotal", line.lineTotal)
                    put("timestamp", line.timestamp)
                }
            )
        }
        root.put("invoiceLineItems", linesArray)

        return root.toString(2)
    }

    suspend fun importDatabaseFromJson(jsonString: String): Pair<Int, Int> {
        val root = JSONObject(jsonString)
        val itemsArray = root.optJSONArray("inventoryItems") ?: JSONArray()
        val invoicesArray = root.optJSONArray("salesInvoices") ?: JSONArray()
        val linesArray = root.optJSONArray("invoiceLineItems") ?: JSONArray()

        val importedItems = mutableListOf<InventoryItemEntity>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            importedItems.add(
                InventoryItemEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", "Unnamed Item"),
                    barcode = obj.optString("barcode", ""),
                    category = obj.optString("category", "General"),
                    unit = obj.optString("unit", "PCS"),
                    hsnCode = obj.optString("hsnCode", ""),
                    purchaseRate = obj.optDouble("purchaseRate", 0.0),
                    salesRate = obj.optDouble("salesRate", 0.0),
                    stockQuantity = obj.optDouble("stockQuantity", 0.0),
                    minStockAlert = obj.optDouble("minStockAlert", 5.0),
                    gstRate = obj.optInt("gstRate", 18),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        if (importedItems.isNotEmpty()) {
            dao.insertInventoryItems(importedItems)
        }

        val importedInvoices = mutableListOf<SalesInvoiceEntity>()
        for (i in 0 until invoicesArray.length()) {
            val obj = invoicesArray.getJSONObject(i)
            importedInvoices.add(
                SalesInvoiceEntity(
                    id = obj.optLong("id", 0L),
                    billNumber = obj.optString("billNumber", "INV-IMP-$i"),
                    customerName = obj.optString("customerName", "Walk-in Customer"),
                    customerMobile = obj.optString("customerMobile", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    dateString = obj.optString("dateString", "2026-10-23"),
                    monthString = obj.optString("monthString", "2026-10"),
                    yearString = obj.optString("yearString", "2026"),
                    totalQuantity = obj.optDouble("totalQuantity", 1.0),
                    subtotalAmount = obj.optDouble("subtotalAmount", 0.0),
                    gstEnabled = obj.optBoolean("gstEnabled", true),
                    gstRateApplied = obj.optInt("gstRateApplied", 18),
                    cgstAmount = obj.optDouble("cgstAmount", 0.0),
                    sgstAmount = obj.optDouble("sgstAmount", 0.0),
                    discountAmount = obj.optDouble("discountAmount", 0.0),
                    grandTotalAmount = obj.optDouble("grandTotalAmount", 0.0),
                    cashReceived = obj.optDouble("cashReceived", 0.0),
                    paymentMode = obj.optString("paymentMode", "Cash")
                )
            )
        }
        if (importedInvoices.isNotEmpty()) {
            dao.insertInvoices(importedInvoices)
        }

        val importedLines = mutableListOf<InvoiceLineItemEntity>()
        for (i in 0 until linesArray.length()) {
            val obj = linesArray.getJSONObject(i)
            importedLines.add(
                InvoiceLineItemEntity(
                    id = obj.optLong("id", 0L),
                    invoiceId = obj.optLong("invoiceId", 0L),
                    billNumber = obj.optString("billNumber", ""),
                    itemId = obj.optLong("itemId", 0L),
                    itemName = obj.optString("itemName", ""),
                    barcode = obj.optString("barcode", ""),
                    category = obj.optString("category", ""),
                    unit = obj.optString("unit", "PCS"),
                    hsnCode = obj.optString("hsnCode", ""),
                    quantity = obj.optDouble("quantity", 1.0),
                    purchaseRate = obj.optDouble("purchaseRate", 0.0),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    gstRate = obj.optInt("gstRate", 18),
                    cgstAmount = obj.optDouble("cgstAmount", 0.0),
                    sgstAmount = obj.optDouble("sgstAmount", 0.0),
                    lineSubtotal = obj.optDouble("lineSubtotal", 0.0),
                    lineTotal = obj.optDouble("lineTotal", 0.0),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
        if (importedLines.isNotEmpty()) {
            dao.insertLineItems(importedLines)
        }

        return Pair(importedItems.size, importedInvoices.size)
    }
}
