package com.financas.app.data.repository

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.model.AppSettingsBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSanitizerTest {

    private fun tx(
        amountCents: Long = 1250L,
        yearMonth: Int = 202611,
        epochDay: Long = 20_768L,
        type: String = "EXPENSE",
        merchant: String = "Mercearia"
    ) = TransactionEntity(
        id = 1,
        type = type,
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = epochDay,
        categoryId = "sub_groceries",
        contextTag = "COMERCIO",
        merchant = merchant,
        paymentMethod = "MB WAY"
    )

    private fun category(id: String = "sub_groceries", type: String = "EXPENSE") = CategoryEntity(
        id = id,
        parentId = null,
        name = "Mercearia",
        icon = "category",
        colorHex = 0xFF607D8B,
        type = type,
        isDefaultFixed = false,
        sortOrder = 0
    )

    private fun backup(
        transactions: List<TransactionEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
        budgets: List<BudgetEntity> = emptyList(),
        settings: AppSettingsBackup? = null
    ) = BackupData(version = 3, categories = categories, transactions = transactions, budgets = budgets, settings = settings)

    @Test
    fun validBackup_passesUnchanged() {
        val data = backup(transactions = listOf(tx()), categories = listOf(category()))
        val result = BackupSanitizer.sanitize(data)
        assertEquals(data, result.data)
        assertEquals(0, result.droppedRecords)
    }

    @Test
    fun overflowingAndMalformedTransactions_areDropped() {
        val data = backup(
            transactions = listOf(
                tx(),
                tx(amountCents = Long.MAX_VALUE),
                tx(amountCents = Long.MIN_VALUE),
                tx(yearMonth = 202613),
                tx(yearMonth = 0),
                tx(epochDay = Long.MAX_VALUE),
                tx(type = "DROP TABLE")
            )
        )
        val result = BackupSanitizer.sanitize(data)
        assertEquals(listOf(tx()), result.data.transactions)
        assertEquals(6, result.droppedRecords)
    }

    @Test
    fun oversizedText_isTruncated() {
        val result = BackupSanitizer.sanitize(backup(transactions = listOf(tx(merchant = "x".repeat(50_000)))))
        assertEquals(BackupSanitizer.MAX_LABEL_CHARS, result.data.transactions.single().merchant.length)
    }

    @Test
    fun invalidCategoriesAndBudgets_areDropped() {
        val data = backup(
            categories = listOf(category(), category(id = " "), category(type = "expense; --")),
            budgets = listOf(
                BudgetEntity(id = "b1", categoryId = "sub_groceries", limitCents = 50_000L),
                BudgetEntity(id = "b2", categoryId = "sub_groceries", limitCents = -1L)
            )
        )
        val result = BackupSanitizer.sanitize(data)
        assertEquals(1, result.data.categories.size)
        assertEquals(listOf("b1"), result.data.budgets.map { it.id })
        assertEquals(3, result.droppedRecords)
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooManyRecords_areRejected() {
        BackupSanitizer.sanitize(backup(categories = List(BackupSanitizer.MAX_CATEGORIES + 1) { category(id = "c$it") }))
    }

    @Test
    fun settings_areClampedToSupportedValues() {
        val settings = BackupSanitizer.sanitizeSettings(
            AppSettingsBackup(
                userName = "n".repeat(500),
                monthlyCycleDay = 99,
                themeMode = "NEON",
                appLanguage = "xx",
                customContextTags = listOf(" GINASIO ", "", "t".repeat(500), "GINASIO")
            )
        )
        assertEquals(80, settings.userName.length)
        assertEquals(28, settings.monthlyCycleDay)
        assertEquals("DARK", settings.themeMode)
        assertEquals("EN", settings.appLanguage)
        assertEquals(listOf("GINASIO"), settings.customContextTags)
        assertTrue(BackupSanitizer.sanitizeSettings(AppSettingsBackup(monthlyCycleDay = -3)).monthlyCycleDay == 1)
    }
}
