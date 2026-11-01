package com.financas.app.data.repository

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupSerializerTest {

    private val serializer = BackupSerializer()

    private val sampleCategories = listOf(
        CategoryEntity(
            id = "exp_food",
            parentId = null,
            name = "Alimentação",
            icon = "restaurant",
            colorHex = 0xFFFF9800,
            type = "EXPENSE",
            isDefaultFixed = false,
            sortOrder = 1
        ),
        CategoryEntity(
            id = "exp_food_super",
            parentId = "exp_food",
            name = "Supermercado",
            icon = "shopping_cart",
            colorHex = 0xFFFF9800,
            type = "EXPENSE",
            isDefaultFixed = false,
            sortOrder = 2
        ),
        CategoryEntity(
            id = "inc_fixed",
            parentId = null,
            name = "Rendimento Fixo",
            icon = "payments",
            colorHex = 0xFF2E7D32,
            type = "INCOME",
            isDefaultFixed = true,
            sortOrder = 0
        )
    )

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = 101L,
            type = "EXPENSE",
            amountCents = 1250L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 25).toEpochDay(),
            categoryId = "exp_food_super",
            contextTag = "CASA",
            merchant = "Continente & Filhos; Lda",
            paymentMethod = "CARD",
            isFixed = false
        ),
        TransactionEntity(
            id = 102L,
            type = "INCOME",
            amountCents = 250000L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            categoryId = "inc_fixed",
            contextTag = "TRABALHO",
            merchant = "Empresa XPTO",
            paymentMethod = "TRANSFER",
            isFixed = true
        )
    )

    @Test
    fun testRoundTripJsonPreservesAllFields() {
        val json = serializer.exportToJson(sampleCategories, sampleTransactions)
        val backupData = serializer.importFromJson(json)

        assertEquals(BackupSerializer.CURRENT_SCHEMA_VERSION, backupData.version)
        assertEquals(sampleCategories.size, backupData.categories.size)
        assertEquals(sampleTransactions.size, backupData.transactions.size)

        // Validate categories
        val cat0 = backupData.categories[0]
        assertEquals("exp_food", cat0.id)
        assertNull(cat0.parentId)
        assertEquals("Alimentação", cat0.name)
        assertEquals("restaurant", cat0.icon)
        assertEquals(0xFFFF9800, cat0.colorHex)
        assertEquals("EXPENSE", cat0.type)
        assertFalse(cat0.isDefaultFixed)
        assertEquals(1, cat0.sortOrder)

        val cat1 = backupData.categories[1]
        assertEquals("exp_food_super", cat1.id)
        assertEquals("exp_food", cat1.parentId)
        assertEquals("Supermercado", cat1.name)

        // Validate transactions
        val tx0 = backupData.transactions[0]
        assertEquals(101L, tx0.id)
        assertEquals("EXPENSE", tx0.type)
        assertEquals(1250L, tx0.amountCents)
        assertEquals(202610, tx0.yearMonth)
        assertEquals(LocalDate.of(2026, 10, 25).toEpochDay(), tx0.epochDay)
        assertEquals("exp_food_super", tx0.categoryId)
        assertEquals("CASA", tx0.contextTag)
        assertEquals("Continente & Filhos; Lda", tx0.merchant)
        assertEquals("CARD", tx0.paymentMethod)
        assertFalse(tx0.isFixed)

        val tx1 = backupData.transactions[1]
        assertEquals(102L, tx1.id)
        assertEquals("INCOME", tx1.type)
        assertEquals(250000L, tx1.amountCents)
        assertTrue(tx1.isFixed)
    }

    @Test
    fun testExportToCsvFormatsPtPtAndAllDimensions() {
        val categoryMap = sampleCategories.associateBy { it.id }
        val csv = serializer.exportToCsv(sampleTransactions, categoryMap)

        val lines = csv.trim().split("\n")
        assertEquals(3, lines.size)
        assertEquals(BackupSerializer.CSV_HEADER, lines[0])

        // First row: expense with subcategory and escaped semicolon
        // Expected columns: Data;Mês;Tipo;Valor €;Categoria;Subcategoria;Onde (Contexto);Estabelecimento/Nota;Como (Método);Fixo
        val row1 = lines[1]
        assertTrue(row1.contains("2026-10-25;2026-10;EXPENSE;12,50;Alimentação;Supermercado;CASA;\"Continente & Filhos; Lda\";CARD;Não"))

        // Second row: income directly under root category
        val row2 = lines[2]
        assertTrue(row2.contains("2026-10-01;2026-10;INCOME;2500,00;Rendimento Fixo;;TRABALHO;Empresa XPTO;TRANSFER;Sim"))
    }

    @Test
    fun testRestoreBackupIsIdempotent() = runTest {
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()

        val json = serializer.exportToJson(sampleCategories, sampleTransactions)
        val backupData = serializer.importFromJson(json)

        // First restore
        val firstResult = serializer.restoreBackup(backupData, categoryDao, transactionDao)
        assertEquals(3, firstResult.categoriesImported)
        assertEquals(2, firstResult.transactionsImported)
        assertEquals(0, firstResult.transactionsSkipped)

        assertEquals(3, categoryDao.count())
        assertEquals(2, transactionDao.count())

        // Second restore of identical data
        val secondResult = serializer.restoreBackup(backupData, categoryDao, transactionDao)
        assertEquals(3, secondResult.categoriesImported)
        assertEquals(0, secondResult.transactionsImported)
        assertEquals(2, secondResult.transactionsSkipped)

        // Database remains unchanged (no duplicates)
        assertEquals(3, categoryDao.count())
        assertEquals(2, transactionDao.count())
    }

    @Test
    fun testExportToCsv_escapesQuotesAndNewlinesInMerchant() {
        val tricky = sampleTransactions[0].copy(merchant = "Loja \"Central\"\nSegunda linha")
        val csv = serializer.exportToCsv(listOf(tricky), sampleCategories.associateBy { it.id })

        assertTrue(csv.startsWith(BackupSerializer.CSV_HEADER + "\n"))
        assertTrue(csv.contains("\"Loja \"\"Central\"\"\nSegunda linha\""))
        assertTrue(csv.contains(";CARD;Não"))
    }

    @Test
    fun testExportJsonV2_includesBudgetsAndGoals() {
        val budgets = listOf(
            BudgetEntity(
                id = "budget_exp_food_super",
                categoryId = "exp_food_super",
                limitCents = 25000L,
                isActive = true
            )
        )
        val goals = listOf(
            GoalEntity(
                id = "goal_fundo_202610",
                name = "Fundo de Emergência",
                targetCents = 500000L,
                startYearMonth = 202610,
                deadlineYearMonth = 202712
            )
        )

        val json = serializer.exportToJson(sampleCategories, sampleTransactions, budgets, goals)

        assertTrue(json.startsWith("{\n  \"version\": 2,\n"))
        assertTrue(json.contains("\"budgets\": ["))
        assertTrue(json.contains("\"id\": \"budget_exp_food_super\""))
        assertTrue(json.contains("\"limitCents\": 25000"))
        assertTrue(json.contains("\"isActive\": true"))
        assertTrue(json.contains("\"goals\": ["))
        assertTrue(json.contains("\"id\": \"goal_fundo_202610\""))
        assertTrue(json.contains("\"name\": \"Fundo de Emergência\""))
        assertTrue(json.contains("\"targetCents\": 500000"))
        assertTrue(json.contains("\"startYearMonth\": 202610"))
        assertTrue(json.contains("\"deadlineYearMonth\": 202712"))
        assertTrue(json.contains("\"archived\": false"))

        val restored = serializer.importFromJson(json)
        assertEquals(budgets, restored.budgets)
        assertEquals(goals, restored.goals)
    }

    @Test
    fun exportAndImport_withLinkedTransactionId_preservesFidelityAndHandlesNulls() {
        val refundTx = TransactionEntity(
            id = 103L,
            type = "INCOME",
            amountCents = 1500L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 26).toEpochDay(),
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo MB WAY",
            paymentMethod = "MBWAY",
            isFixed = false,
            linkedTransactionId = 101L
        )

        val txs = sampleTransactions + refundTx
        val json = serializer.exportToJson(sampleCategories, txs)

        assertTrue(json.contains("\"linkedTransactionId\": 101"))

        val restored = serializer.importFromJson(json)
        val restoredRefund = restored.transactions.find { it.id == 103L }
        assertEquals(101L, restoredRefund?.linkedTransactionId)

        val restoredRegular = restored.transactions.find { it.id == 101L }
        assertNull(restoredRegular?.linkedTransactionId)

        // Test explicit null literal in JSON
        val jsonWithNullLiteral = """
            {
              "version": 2,
              "categories": [],
              "transactions": [
                {
                  "id": 999,
                  "type": "EXPENSE",
                  "amountCents": 1000,
                  "yearMonth": 202610,
                  "epochDay": 20000,
                  "categoryId": "exp_food",
                  "contextTag": "CASA",
                  "merchant": "Super",
                  "paymentMethod": "CARD",
                  "isFixed": false,
                  "linkedTransactionId": null
                }
              ]
            }
        """.trimIndent()

        val parsedWithNull = serializer.importFromJson(jsonWithNullLiteral)
        assertNull(parsedWithNull.transactions.first().linkedTransactionId)
    }

    @Test
    fun testRoundTripJsonPreservesInvestments() = runTest {
        val investments = listOf(
            InvestmentAssetEntity(
                id = "inv_1",
                name = "PPR Stoik",
                type = "PPR",
                institution = "Banco BiG",
                investedCents = 150000L,
                currentValueCents = 175000L,
                monthlyContributionCents = 10000L,
                notes = "Reforço mensal automático",
                lastUpdatedEpochDay = 20000L
            )
        )

        val json = serializer.exportToJson(
            categories = sampleCategories,
            transactions = sampleTransactions,
            investments = investments
        )

        val restored = serializer.importFromJson(json)
        assertEquals(1, restored.investments.size)
        val asset = restored.investments[0]
        assertEquals("inv_1", asset.id)
        assertEquals("PPR Stoik", asset.name)
        assertEquals("PPR", asset.type)
        assertEquals("Banco BiG", asset.institution)
        assertEquals(150000L, asset.investedCents)
        assertEquals(175000L, asset.currentValueCents)
        assertEquals(10000L, asset.monthlyContributionCents)
        assertEquals("Reforço mensal automático", asset.notes)

        // Test restoreBackup
        val catDao = FakeCategoryDao()
        val txDao = FakeTransactionDao()
        val invDao = FakeInvestmentAssetDao()

        val result = serializer.restoreBackup(
            backupData = restored,
            categoryDao = catDao,
            transactionDao = txDao,
            investmentAssetDao = invDao
        )

        assertEquals(1, result.investmentsImported)
        assertEquals(1, invDao.count())
    }
}
