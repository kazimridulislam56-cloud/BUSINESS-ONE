package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    // --- Inventory Queries ---
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun observeAllInventoryItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    suspend fun getAllInventoryItemsOnce(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateInventoryItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET stockQuantity = MAX(0.0, stockQuantity - :soldQty), updatedAt = :timestamp WHERE id = :itemId")
    suspend fun decrementItemStock(itemId: Long, soldQty: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_items SET stockQuantity = stockQuantity + :addedQty, updatedAt = :timestamp WHERE id = :itemId")
    suspend fun incrementItemStock(itemId: Long, addedQty: Double, timestamp: Long = System.currentTimeMillis())

    // --- Sales Invoices & Line Items ---
    @Query("SELECT * FROM sales_invoices ORDER BY timestamp DESC")
    fun observeAllInvoices(): Flow<List<SalesInvoiceEntity>>

    @Query("SELECT * FROM sales_invoices ORDER BY timestamp DESC")
    suspend fun getAllInvoicesOnce(): List<SalesInvoiceEntity>

    @Query("SELECT * FROM invoice_line_items ORDER BY timestamp DESC")
    fun observeAllLineItems(): Flow<List<InvoiceLineItemEntity>>

    @Query("SELECT * FROM invoice_line_items ORDER BY timestamp DESC")
    suspend fun getAllLineItemsOnce(): List<InvoiceLineItemEntity>

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun getLineItemsForInvoice(invoiceId: Long): List<InvoiceLineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SalesInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<SalesInvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(lineItems: List<InvoiceLineItemEntity>)

    @Delete
    suspend fun deleteInvoice(invoice: SalesInvoiceEntity)

    @Query("DELETE FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteLineItemsByInvoiceId(invoiceId: Long)

    @Transaction
    suspend fun createInvoiceAndDeductStock(
        invoice: SalesInvoiceEntity,
        lineItems: List<InvoiceLineItemEntity>
    ): Long {
        val invoiceId = insertInvoice(invoice)
        val linkedItems = lineItems.map { it.copy(invoiceId = invoiceId, billNumber = invoice.billNumber) }
        insertLineItems(linkedItems)
        val now = System.currentTimeMillis()
        linkedItems.forEach { line ->
            if (line.itemId > 0L) {
                decrementItemStock(line.itemId, line.quantity, now)
            }
        }
        return invoiceId
    }

    @Transaction
    suspend fun deleteInvoiceAndRestoreStock(invoice: SalesInvoiceEntity) {
        val lines = getLineItemsForInvoice(invoice.id)
        val now = System.currentTimeMillis()
        lines.forEach { line ->
            if (line.itemId > 0L) {
                incrementItemStock(line.itemId, line.quantity, now)
            }
        }
        deleteLineItemsByInvoiceId(invoice.id)
        deleteInvoice(invoice)
    }

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun getInventoryCount(): Int
}
