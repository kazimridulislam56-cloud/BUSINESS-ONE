package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ledgerflow_settings")

data class StaffUserRecord(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val isActive: Boolean = true
)

data class PartyRecord(
    val id: String,
    val name: String,
    val phone: String,
    val gstin: String,
    val partyType: String, // "Customer" or "Supplier"
    val openingBalance: Double
)

data class BankAccountRecord(
    val id: String,
    val accountName: String,
    val bankName: String,
    val accountNumberOrUpi: String,
    val openingBalance: Double
)

data class CustomReminderRecord(
    val id: String,
    val title: String,
    val partyOrItemName: String,
    val amountOrQty: String,
    val dueDate: String
)

data class BusinessAndAppSettings(
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val gstEnabled: Boolean = true,
    val defaultGstSlab: Int = 18, // 5, 12, 18, 28
    val currencySymbol: String = "₹",
    val openingCashInHand: Double = 15000.0,
    val businessName: String = "Apex Mercantile & POS",
    val businessCategory: String = "General Retail & Electronics",
    val gstinNumber: String = "27AABCA8921K1Z5",
    val businessPhone: String = "+91 98201 44500",
    val businessAddress: String = "42 Trade Center Plaza, MG Road, Bengaluru",
    val logoUri: String = "",
    val bannerUri: String = "",
    // Thermal Printer Configuration
    val printerPaperWidth: String = "58mm", // "58mm" (2-inch) or "80mm" (3-inch)
    val selectedPrinterName: String = "Not Connected",
    val selectedPrinterAddress: String = "",
    val printBusinessHeader: Boolean = true,
    val printCustomerDetails: Boolean = true,
    val printHsnColumn: Boolean = true,
    val printGstBreakdown: Boolean = true,
    val printCashAndChange: Boolean = true,
    val printBarcodeFooter: Boolean = true,
    val customFooterMessage: String = "Thank you for your business! Goods once sold are exchangeable within 7 days.",
    // Transaction Settings
    val invoicePrefix: String = "INV-2026-",
    val defaultPaymentMode: String = "Cash",
    val enableItemWiseDiscount: Boolean = true,
    val enableExtraBillDiscount: Boolean = true,
    val autoRoundOffTotal: Boolean = false,
    val autoPreviewReceiptOnSave: Boolean = true,
    // Taxes & GST Extra Settings
    val defaultGstTaxMode: String = "UNIFORM_SLAB", // UNIFORM_SLAB or ITEM_WISE_SLAB
    val enforceHsnCode: Boolean = true,
    // Transaction SMS Settings
    val autoSendTransactionSms: Boolean = true,
    val includeGstInSms: Boolean = true,
    val smsTemplate: String = "Thank you for shopping at {business}! Your Bill {bill_no} for Rs.{amount} is confirmed.",
    // Reminders Settings
    val enablePaymentReminders: Boolean = true,
    val enableLowStockReminders: Boolean = true,
    val reminderFrequency: String = "Every 3 Days",
    // Item & Additional Fields Settings
    val defaultItemUnit: String = "PCS",
    val defaultLowStockAlert: Double = 5.0,
    val enableFieldMrp: Boolean = true,
    val enableFieldBatchNo: Boolean = true,
    val enableFieldExpiryDate: Boolean = false,
    val enableFieldWholesalePrice: Boolean = true,
    val enableFieldRackLocation: Boolean = true,
    // Encoded Lists for Users, Parties, Bank Accounts, Custom Reminders
    val staffUsersRaw: String = "",
    val partiesRaw: String = "",
    val bankAccountsRaw: String = "",
    val customRemindersRaw: String = ""
) {
    val staffUsers: List<StaffUserRecord>
        get() = if (staffUsersRaw.isBlank()) {
            listOf(
                StaffUserRecord("1", "Rajesh Sharma", "Admin / Owner", "+91 98201 44500", true),
                StaffUserRecord("2", "Priya Nair", "Cashier / Billing", "+91 98450 11223", true),
                StaffUserRecord("3", "Vikram Desai", "Inventory Manager", "+91 97312 88901", true)
            )
        } else {
            staffUsersRaw.split(";;").mapNotNull { row ->
                val p = row.split("|")
                if (p.size >= 4) StaffUserRecord(p[0], p[1], p[2], p[3], p.getOrNull(4)?.toBoolean() ?: true)
                else null
            }
        }

    val savedParties: List<PartyRecord>
        get() = if (partiesRaw.isBlank()) {
            listOf(
                PartyRecord("P1", "Metro Retail Mart", "9820199200", "27AAACM1234F1Z2", "Customer", 2400.0),
                PartyRecord("P2", "Sunrise Electronics Distributors", "9845077110", "29AABCS9981K1Z9", "Supplier", 8500.0),
                PartyRecord("P3", "Aarav Tech Solutions", "9820112233", "27AABCA4412D1Z1", "Customer", 0.0)
            )
        } else {
            partiesRaw.split(";;").mapNotNull { row ->
                val p = row.split("|")
                if (p.size >= 6) PartyRecord(p[0], p[1], p[2], p[3], p[4], p[5].toDoubleOrNull() ?: 0.0)
                else null
            }
        }

    val bankAccounts: List<BankAccountRecord>
        get() = if (bankAccountsRaw.isBlank()) {
            listOf(
                BankAccountRecord("B1", "HDFC Current Account", "HDFC Bank", "A/C •••• 4821", 42500.0),
                BankAccountRecord("B2", "Shop Merchant QR / UPI", "SBI Business UPI", "apexpos@sbi", 18200.0)
            )
        } else {
            bankAccountsRaw.split(";;").mapNotNull { row ->
                val p = row.split("|")
                if (p.size >= 5) BankAccountRecord(p[0], p[1], p[2], p[3], p[4].toDoubleOrNull() ?: 0.0)
                else null
            }
        }

    val customReminders: List<CustomReminderRecord>
        get() = if (customRemindersRaw.isBlank()) {
            listOf(
                CustomReminderRecord("R1", "Follow up pending invoice payment", "Metro Retail Mart", "₹2,400", "Tomorrow"),
                CustomReminderRecord("R2", "Reorder Thermal Paper Rolls (58mm)", "Sunrise Distributors", "20 Rolls", "This Week")
            )
        } else {
            customRemindersRaw.split(";;").mapNotNull { row ->
                val p = row.split("|")
                if (p.size >= 5) CustomReminderRecord(p[0], p[1], p[2], p[3], p[4])
                else null
            }
        }
}

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GST_ENABLED = booleanPreferencesKey("gst_enabled")
        val DEFAULT_GST_SLAB = intPreferencesKey("default_gst_slab")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val OPENING_CASH = doublePreferencesKey("opening_cash_in_hand")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val BUSINESS_CATEGORY = stringPreferencesKey("business_category")
        val GSTIN_NUMBER = stringPreferencesKey("gstin_number")
        val BUSINESS_PHONE = stringPreferencesKey("business_phone")
        val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
        val LOGO_URI = stringPreferencesKey("logo_uri")
        val BANNER_URI = stringPreferencesKey("banner_uri")
        val PRINTER_PAPER_WIDTH = stringPreferencesKey("printer_paper_width")
        val SELECTED_PRINTER_NAME = stringPreferencesKey("selected_printer_name")
        val SELECTED_PRINTER_ADDRESS = stringPreferencesKey("selected_printer_address")
        val PRINT_BUSINESS_HEADER = booleanPreferencesKey("print_business_header")
        val PRINT_CUSTOMER_DETAILS = booleanPreferencesKey("print_customer_details")
        val PRINT_HSN_COLUMN = booleanPreferencesKey("print_hsn_column")
        val PRINT_GST_BREAKDOWN = booleanPreferencesKey("print_gst_breakdown")
        val PRINT_CASH_AND_CHANGE = booleanPreferencesKey("print_cash_and_change")
        val PRINT_BARCODE_FOOTER = booleanPreferencesKey("print_barcode_footer")
        val CUSTOM_FOOTER_MESSAGE = stringPreferencesKey("custom_footer_message")

        val INVOICE_PREFIX = stringPreferencesKey("invoice_prefix")
        val DEFAULT_PAYMENT_MODE = stringPreferencesKey("default_payment_mode")
        val ENABLE_ITEM_DISC = booleanPreferencesKey("enable_item_wise_discount")
        val ENABLE_EXTRA_DISC = booleanPreferencesKey("enable_extra_bill_discount")
        val AUTO_ROUND_OFF = booleanPreferencesKey("auto_round_off_total")
        val AUTO_PREVIEW_RECEIPT = booleanPreferencesKey("auto_preview_receipt_on_save")
        val DEFAULT_GST_TAX_MODE = stringPreferencesKey("default_gst_tax_mode")
        val ENFORCE_HSN_CODE = booleanPreferencesKey("enforce_hsn_code")
        val AUTO_SEND_SMS = booleanPreferencesKey("auto_send_transaction_sms")
        val INCLUDE_GST_SMS = booleanPreferencesKey("include_gst_in_sms")
        val SMS_TEMPLATE = stringPreferencesKey("sms_template")
        val ENABLE_PAYMENT_REMINDERS = booleanPreferencesKey("enable_payment_reminders")
        val ENABLE_LOW_STOCK_REMINDERS = booleanPreferencesKey("enable_low_stock_reminders")
        val REMINDER_FREQUENCY = stringPreferencesKey("reminder_frequency")
        val DEFAULT_ITEM_UNIT = stringPreferencesKey("default_item_unit")
        val DEFAULT_LOW_STOCK_ALERT = doublePreferencesKey("default_low_stock_alert")
        val ENABLE_FIELD_MRP = booleanPreferencesKey("enable_field_mrp")
        val ENABLE_FIELD_BATCH = booleanPreferencesKey("enable_field_batch")
        val ENABLE_FIELD_EXPIRY = booleanPreferencesKey("enable_field_expiry")
        val ENABLE_FIELD_WHOLESALE = booleanPreferencesKey("enable_field_wholesale")
        val ENABLE_FIELD_RACK = booleanPreferencesKey("enable_field_rack")

        val STAFF_USERS_RAW = stringPreferencesKey("staff_users_raw")
        val PARTIES_RAW = stringPreferencesKey("parties_raw")
        val BANK_ACCOUNTS_RAW = stringPreferencesKey("bank_accounts_raw")
        val CUSTOM_REMINDERS_RAW = stringPreferencesKey("custom_reminders_raw")
    }

    val settingsFlow: Flow<BusinessAndAppSettings> = context.dataStore.data.map { prefs ->
        BusinessAndAppSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            gstEnabled = prefs[Keys.GST_ENABLED] ?: true,
            defaultGstSlab = prefs[Keys.DEFAULT_GST_SLAB] ?: 18,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "₹",
            openingCashInHand = prefs[Keys.OPENING_CASH] ?: 15000.0,
            businessName = prefs[Keys.BUSINESS_NAME] ?: "Apex Mercantile & POS",
            businessCategory = prefs[Keys.BUSINESS_CATEGORY] ?: "General Retail & Electronics",
            gstinNumber = prefs[Keys.GSTIN_NUMBER] ?: "27AABCA8921K1Z5",
            businessPhone = prefs[Keys.BUSINESS_PHONE] ?: "+91 98201 44500",
            businessAddress = prefs[Keys.BUSINESS_ADDRESS] ?: "42 Trade Center Plaza, MG Road, Bengaluru",
            logoUri = prefs[Keys.LOGO_URI] ?: "",
            bannerUri = prefs[Keys.BANNER_URI] ?: "",
            printerPaperWidth = prefs[Keys.PRINTER_PAPER_WIDTH] ?: "58mm",
            selectedPrinterName = prefs[Keys.SELECTED_PRINTER_NAME] ?: "Not Connected",
            selectedPrinterAddress = prefs[Keys.SELECTED_PRINTER_ADDRESS] ?: "",
            printBusinessHeader = prefs[Keys.PRINT_BUSINESS_HEADER] ?: true,
            printCustomerDetails = prefs[Keys.PRINT_CUSTOMER_DETAILS] ?: true,
            printHsnColumn = prefs[Keys.PRINT_HSN_COLUMN] ?: true,
            printGstBreakdown = prefs[Keys.PRINT_GST_BREAKDOWN] ?: true,
            printCashAndChange = prefs[Keys.PRINT_CASH_AND_CHANGE] ?: true,
            printBarcodeFooter = prefs[Keys.PRINT_BARCODE_FOOTER] ?: true,
            customFooterMessage = prefs[Keys.CUSTOM_FOOTER_MESSAGE]
                ?: "Thank you for your business! Goods once sold are exchangeable within 7 days.",
            invoicePrefix = prefs[Keys.INVOICE_PREFIX] ?: "INV-2026-",
            defaultPaymentMode = prefs[Keys.DEFAULT_PAYMENT_MODE] ?: "Cash",
            enableItemWiseDiscount = prefs[Keys.ENABLE_ITEM_DISC] ?: true,
            enableExtraBillDiscount = prefs[Keys.ENABLE_EXTRA_DISC] ?: true,
            autoRoundOffTotal = prefs[Keys.AUTO_ROUND_OFF] ?: false,
            autoPreviewReceiptOnSave = prefs[Keys.AUTO_PREVIEW_RECEIPT] ?: true,
            defaultGstTaxMode = prefs[Keys.DEFAULT_GST_TAX_MODE] ?: "UNIFORM_SLAB",
            enforceHsnCode = prefs[Keys.ENFORCE_HSN_CODE] ?: true,
            autoSendTransactionSms = prefs[Keys.AUTO_SEND_SMS] ?: true,
            includeGstInSms = prefs[Keys.INCLUDE_GST_SMS] ?: true,
            smsTemplate = prefs[Keys.SMS_TEMPLATE]
                ?: "Thank you for shopping at {business}! Your Bill {bill_no} for Rs.{amount} is confirmed.",
            enablePaymentReminders = prefs[Keys.ENABLE_PAYMENT_REMINDERS] ?: true,
            enableLowStockReminders = prefs[Keys.ENABLE_LOW_STOCK_REMINDERS] ?: true,
            reminderFrequency = prefs[Keys.REMINDER_FREQUENCY] ?: "Every 3 Days",
            defaultItemUnit = prefs[Keys.DEFAULT_ITEM_UNIT] ?: "PCS",
            defaultLowStockAlert = prefs[Keys.DEFAULT_LOW_STOCK_ALERT] ?: 5.0,
            enableFieldMrp = prefs[Keys.ENABLE_FIELD_MRP] ?: true,
            enableFieldBatchNo = prefs[Keys.ENABLE_FIELD_BATCH] ?: true,
            enableFieldExpiryDate = prefs[Keys.ENABLE_FIELD_EXPIRY] ?: false,
            enableFieldWholesalePrice = prefs[Keys.ENABLE_FIELD_WHOLESALE] ?: true,
            enableFieldRackLocation = prefs[Keys.ENABLE_FIELD_RACK] ?: true,
            staffUsersRaw = prefs[Keys.STAFF_USERS_RAW] ?: "",
            partiesRaw = prefs[Keys.PARTIES_RAW] ?: "",
            bankAccountsRaw = prefs[Keys.BANK_ACCOUNTS_RAW] ?: "",
            customRemindersRaw = prefs[Keys.CUSTOM_REMINDERS_RAW] ?: ""
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setGstEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GST_ENABLED] = enabled }
    }

    suspend fun setDefaultGstSlab(slab: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_GST_SLAB] = slab }
    }

    suspend fun setOpeningCashInHand(amount: Double) {
        context.dataStore.edit { it[Keys.OPENING_CASH] = amount }
    }

    suspend fun updateBusinessProfile(
        name: String,
        category: String,
        gstin: String,
        phone: String,
        address: String,
        logoUri: String,
        bannerUri: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = name
            prefs[Keys.BUSINESS_CATEGORY] = category
            prefs[Keys.GSTIN_NUMBER] = gstin
            prefs[Keys.BUSINESS_PHONE] = phone
            prefs[Keys.BUSINESS_ADDRESS] = address
            prefs[Keys.LOGO_URI] = logoUri
            prefs[Keys.BANNER_URI] = bannerUri
        }
    }

    suspend fun updateThermalPrinterSettings(
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
        context.dataStore.edit { prefs ->
            prefs[Keys.PRINTER_PAPER_WIDTH] = paperWidth
            prefs[Keys.SELECTED_PRINTER_NAME] = printerName
            prefs[Keys.SELECTED_PRINTER_ADDRESS] = printerAddress
            prefs[Keys.PRINT_BUSINESS_HEADER] = printHeader
            prefs[Keys.PRINT_CUSTOMER_DETAILS] = printCustomer
            prefs[Keys.PRINT_HSN_COLUMN] = printHsn
            prefs[Keys.PRINT_GST_BREAKDOWN] = printGst
            prefs[Keys.PRINT_CASH_AND_CHANGE] = printCash
            prefs[Keys.PRINT_BARCODE_FOOTER] = printBarcode
            prefs[Keys.CUSTOM_FOOTER_MESSAGE] = footerMessage
        }
    }

    suspend fun updateTransactionSettings(
        invoicePrefix: String,
        defaultPaymentMode: String,
        enableItemDiscount: Boolean,
        enableExtraDiscount: Boolean,
        autoRoundOff: Boolean,
        autoPreviewReceipt: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INVOICE_PREFIX] = invoicePrefix
            prefs[Keys.DEFAULT_PAYMENT_MODE] = defaultPaymentMode
            prefs[Keys.ENABLE_ITEM_DISC] = enableItemDiscount
            prefs[Keys.ENABLE_EXTRA_DISC] = enableExtraDiscount
            prefs[Keys.AUTO_ROUND_OFF] = autoRoundOff
            prefs[Keys.AUTO_PREVIEW_RECEIPT] = autoPreviewReceipt
        }
    }

    suspend fun updateTaxExtraSettings(
        defaultGstTaxMode: String,
        enforceHsnCode: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_GST_TAX_MODE] = defaultGstTaxMode
            prefs[Keys.ENFORCE_HSN_CODE] = enforceHsnCode
        }
    }

    suspend fun updateSmsSettings(
        autoSendSms: Boolean,
        includeGst: Boolean,
        template: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SEND_SMS] = autoSendSms
            prefs[Keys.INCLUDE_GST_SMS] = includeGst
            prefs[Keys.SMS_TEMPLATE] = template
        }
    }

    suspend fun updateReminderSettings(
        enablePayment: Boolean,
        enableLowStock: Boolean,
        frequency: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_PAYMENT_REMINDERS] = enablePayment
            prefs[Keys.ENABLE_LOW_STOCK_REMINDERS] = enableLowStock
            prefs[Keys.REMINDER_FREQUENCY] = frequency
        }
    }

    suspend fun updateItemSettings(
        defaultUnit: String,
        defaultLowStock: Double,
        mrp: Boolean,
        batch: Boolean,
        expiry: Boolean,
        wholesale: Boolean,
        rack: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_ITEM_UNIT] = defaultUnit
            prefs[Keys.DEFAULT_LOW_STOCK_ALERT] = defaultLowStock
            prefs[Keys.ENABLE_FIELD_MRP] = mrp
            prefs[Keys.ENABLE_FIELD_BATCH] = batch
            prefs[Keys.ENABLE_FIELD_EXPIRY] = expiry
            prefs[Keys.ENABLE_FIELD_WHOLESALE] = wholesale
            prefs[Keys.ENABLE_FIELD_RACK] = rack
        }
    }

    suspend fun saveStaffUsers(users: List<StaffUserRecord>) {
        val raw = users.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.role.replace("|", "")}|${it.phone.replace("|", "")}|${it.isActive}"
        }
        context.dataStore.edit { it[Keys.STAFF_USERS_RAW] = raw }
    }

    suspend fun saveParties(parties: List<PartyRecord>) {
        val raw = parties.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.phone.replace("|", "")}|${it.gstin.replace("|", "")}|${it.partyType}|${it.openingBalance}"
        }
        context.dataStore.edit { it[Keys.PARTIES_RAW] = raw }
    }

    suspend fun saveBankAccounts(accounts: List<BankAccountRecord>) {
        val raw = accounts.joinToString(";;") {
            "${it.id}|${it.accountName.replace("|", "")}|${it.bankName.replace("|", "")}|${it.accountNumberOrUpi.replace("|", "")}|${it.openingBalance}"
        }
        context.dataStore.edit { it[Keys.BANK_ACCOUNTS_RAW] = raw }
    }

    suspend fun saveCustomReminders(reminders: List<CustomReminderRecord>) {
        val raw = reminders.joinToString(";;") {
            "${it.id}|${it.title.replace("|", "")}|${it.partyOrItemName.replace("|", "")}|${it.amountOrQty.replace("|", "")}|${it.dueDate.replace("|", "")}"
        }
        context.dataStore.edit { it[Keys.CUSTOM_REMINDERS_RAW] = raw }
    }
}
