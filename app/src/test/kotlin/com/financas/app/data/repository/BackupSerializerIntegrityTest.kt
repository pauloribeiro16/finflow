package com.financas.app.data.repository

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class BackupSerializerIntegrityTest {

    private val serializer = BackupSerializer()

    private val categories = listOf(
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
            id = "exp_food_supermarket",
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

    private fun tx(
        id: Long,
        type: String,
        amountCents: Long,
        yearMonth: Int,
        epochDay: Long,
        categoryId: String,
        contextTag: String,
        merchant: String,
        paymentMethod: String,
        isFixed: Boolean
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = epochDay,
        categoryId = categoryId,
        contextTag = contextTag,
        merchant = merchant,
        paymentMethod = paymentMethod,
        isFixed = isFixed
    )

    private val transactions = listOf(
        tx(
            id = 1L,
            type = "EXPENSE",
            amountCents = 123456789L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 25).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "A;B\"C\nD\tE\\F",
            paymentMethod = "CARD",
            isFixed = false
        ),
        tx(
            id = 2L,
            type = "INCOME",
            amountCents = 250000L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            categoryId = "inc_fixed",
            contextTag = "TRABALHO",
            merchant = "Empresa \"X\"; Lda",
            paymentMethod = "TRANSFER",
            isFixed = true
        )
    )

    private val budgets = listOf(
        BudgetEntity(
            id = "budget_exp_food",
            categoryId = "exp_food",
            limitCents = 40000L,
            isActive = true
        ),
        BudgetEntity(
            id = "budget_exp_housing",
            categoryId = "exp_housing",
            limitCents = 120000L,
            isActive = false
        )
    )

    private val goals = listOf(
        GoalEntity(
            id = "goal_fundo_202601",
            name = "Fundo de Emergência",
            targetCents = 300000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612,
            archived = false
        ),
        GoalEntity(
            id = "goal_viagem_202603",
            name = "Viagem \"Japão\"; 2026",
            targetCents = 150000L,
            startYearMonth = 202603,
            deadlineYearMonth = null,
            archived = true
        )
    )

    @Test
    fun test1_roundTripJson_preservesEveryFieldOnCategoriesAndTransactions() {
        val json = serializer.exportToJson(categories, transactions)
        val restored = serializer.importFromJson(json)

        assertEquals(BackupSerializer.CURRENT_SCHEMA_VERSION, restored.version)
        assertEquals(categories, restored.categories)
        assertEquals(transactions, restored.transactions)

        val escapedMerchant = restored.transactions[0].merchant
        assertEquals("A;B\"C\nD\tE\\F", escapedMerchant)

        val emptyRoundTrip = serializer.importFromJson(serializer.exportToJson(emptyList(), emptyList()))
        assertEquals(BackupSerializer.CURRENT_SCHEMA_VERSION, emptyRoundTrip.version)
        assertTrue(emptyRoundTrip.categories.isEmpty())
        assertTrue(emptyRoundTrip.transactions.isEmpty())
    }

    @Test
    fun test2_csv_exactHeaderEscapingSimNaoIsoDateMonthAndPtPtAmount() {
        val categoryMap = categories.associateBy { it.id }

        val csvEscaped = serializer.exportToCsv(transactions, categoryMap)
        assertTrue(csvEscaped.startsWith(BackupSerializer.CSV_HEADER + "\n"))
        val headerColumns = BackupSerializer.CSV_HEADER.split(";")
        assertEquals(
            listOf("Data", "Mês", "Tipo", "Valor €", "Categoria", "Subcategoria", "Onde (Contexto)", "Estabelecimento/Nota", "Como (Método)", "Fixo"),
            headerColumns
        )

        assertTrue(
            csvEscaped.contains("2026-10-25;2026-10;EXPENSE;1234567,89;Alimentação;Supermercado;CASA;\"A;B\"\"C\nD\tE\\F\";CARD;Não")
        )
        assertTrue(
            csvEscaped.contains("2026-10-01;2026-10;INCOME;2500,00;Rendimento Fixo;;TRABALHO;\"Empresa \"\"X\"\"; Lda\";TRANSFER;Sim")
        )

        val unknownCategoryTx = tx(
            id = 3L,
            type = "EXPENSE",
            amountCents = 500L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 30).toEpochDay(),
            categoryId = "cat_removed",
            contextTag = "ONLINE",
            merchant = "Loja",
            paymentMethod = "MBWAY",
            isFixed = false
        )
        val csvUnknown = serializer.exportToCsv(listOf(unknownCategoryTx), emptyMap())
        val rows = csvUnknown.trim().split("\n")
        assertEquals(2, rows.size)
        val columns = rows[1].split(";")
        assertEquals(10, columns.size)
        assertEquals("2026-10-30", columns[0])
        assertEquals("2026-10", columns[1])
        assertEquals("EXPENSE", columns[2])
        assertEquals("5,00", columns[3])
        assertEquals("", columns[4])
        assertEquals("", columns[5])
        assertEquals("ONLINE", columns[6])
        assertEquals("Loja", columns[7])
        assertEquals("MBWAY", columns[8])
        assertEquals("Não", columns[9])
    }

    @Test
    fun test3_importFromJson_malformedInputThrowsIllegalArgumentException() {
        val malformedInputs = listOf("{not json", "", "[1,2", "{\"a\": }", "garbage")

        malformedInputs.forEach { input ->
            try {
                serializer.importFromJson(input)
                fail("Era esperado IllegalArgumentException para: $input")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message!!.isNotEmpty())
            }
        }
    }

    @Test
    fun test4_importFromJson_missingFieldsFallBackToSafeDefaults() {
        val minimal = serializer.importFromJson("{}")
        assertEquals(1, minimal.version)
        assertTrue(minimal.categories.isEmpty())
        assertTrue(minimal.transactions.isEmpty())

        val partial = serializer.importFromJson(
            """
            {
              "categories": [
                {"name": "Sem Id"},
                {"id": "c1"}
              ],
              "transactions": [{}]
            }
            """.trimIndent()
        )

        assertEquals(1, partial.categories.size)
        val category = partial.categories[0]
        assertEquals("c1", category.id)
        assertEquals("", category.name)
        assertEquals("category", category.icon)
        assertEquals(0xFF607D8B, category.colorHex)
        assertEquals("EXPENSE", category.type)
        assertFalse(category.isDefaultFixed)
        assertEquals(0, category.sortOrder)

        assertEquals(1, partial.transactions.size)
        val transaction = partial.transactions[0]
        assertEquals(0L, transaction.id)
        assertEquals("EXPENSE", transaction.type)
        assertEquals(0L, transaction.amountCents)
        assertEquals(0, transaction.yearMonth)
        assertEquals(0L, transaction.epochDay)
        assertEquals("", transaction.categoryId)
        assertEquals("", transaction.contextTag)
        assertEquals("", transaction.merchant)
        assertEquals("", transaction.paymentMethod)
        assertFalse(transaction.isFixed)
    }

    @Test
    fun test5_mixedRestore_partialBaseExactCountsAndFullIdempotency() = runTest {
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()

        val baseTx1 = transactions[0].copy(id = 1L)
        val baseTx3 = tx(
            id = 3L,
            type = "EXPENSE",
            amountCents = 999L,
            yearMonth = 202611,
            epochDay = LocalDate.of(2026, 11, 5).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Pingo Doce",
            paymentMethod = "CARD",
            isFixed = false
        )

        transactionDao.insertAll(listOf(baseTx1, baseTx3))
        assertEquals(2, transactionDao.count())

        val backupData = BackupData(
            version = 1,
            categories = categories,
            transactions = transactions +
                    tx(
                        id = 3L,
                        type = "EXPENSE",
                        amountCents = 999L,
                        yearMonth = 202611,
                        epochDay = LocalDate.of(2026, 11, 5).toEpochDay(),
                        categoryId = "exp_food_supermarket",
                        contextTag = "CASA",
                        merchant = "Pingo Doce",
                        paymentMethod = "CARD",
                        isFixed = false
                    ) +
                    tx(
                        id = 4L,
                        type = "EXPENSE",
                        amountCents = 777L,
                        yearMonth = 202611,
                        epochDay = LocalDate.of(2026, 11, 6).toEpochDay(),
                        categoryId = "exp_food_supermarket",
                        contextTag = "CASA",
                        merchant = "Novo Mercado",
                        paymentMethod = "CASH",
                        isFixed = false
                    )
        )

        val firstRestore = serializer.restoreBackup(backupData, categoryDao, transactionDao)
        assertEquals(3, firstRestore.categoriesImported)
        assertEquals(2, firstRestore.transactionsImported)
        assertEquals(2, firstRestore.transactionsSkipped)
        assertEquals(4, transactionDao.count())

        val secondRestore = serializer.restoreBackup(backupData, categoryDao, transactionDao)
        assertEquals(3, secondRestore.categoriesImported)
        assertEquals(0, secondRestore.transactionsImported)
        assertEquals(4, secondRestore.transactionsSkipped)
        assertEquals(4, transactionDao.count())
    }

    @Test
    fun test6_isDuplicateTransaction_matchesByIdAndByFullFieldSet() {
        val sameIdDifferentFields = transactions[0] to transactions[0].copy(amountCents = 999L)
        assertTrue(serializer.isDuplicateTransaction(sameIdDifferentFields.first, sameIdDifferentFields.second))

        val differentIdsSameFields = transactions[0] to transactions[0].copy(id = 999L)
        assertTrue(serializer.isDuplicateTransaction(differentIdsSameFields.first, differentIdsSameFields.second))

        val bothZeroIdsSameFields = transactions[0].copy(id = 0L) to transactions[0].copy(id = 0L)
        assertTrue(serializer.isDuplicateTransaction(bothZeroIdsSameFields.first, bothZeroIdsSameFields.second))
    }

    @Test
    fun test7_isDuplicateTransaction_singleFieldDifferenceMeansNotDuplicate() {
        val base = transactions[0]
        val otherId = 500L

        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, amountCents = base.amountCents + 1)))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, merchant = "Outro")))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, contextTag = "SAIDA")))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, paymentMethod = "CASH")))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, isFixed = true)))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, type = "INCOME")))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, categoryId = "exp_food")))
        assertFalse(serializer.isDuplicateTransaction(base, base.copy(id = otherId, epochDay = base.epochDay + 1)))

        assertTrue(
            "yearMonth não faz parte da chave de duplicação",
            serializer.isDuplicateTransaction(base, base.copy(yearMonth = 202511))
        )
    }

    @Test
    fun test8_roundTripJsonV2_preservesEveryFieldOnBudgetsAndGoals() {
        val json = serializer.exportToJson(categories, transactions, budgets, goals)

        assertTrue(json.startsWith("{\n  \"version\": 3,\n"))
        assertTrue(json.contains("\"budgets\": ["))
        assertTrue(json.contains("\"goals\": ["))

        val restored = serializer.importFromJson(json)

        assertEquals(3, restored.version)
        assertEquals(budgets, restored.budgets)
        assertEquals(goals, restored.goals)
        assertEquals(categories, restored.categories)
        assertEquals(transactions, restored.transactions)

        val restoredGoal = restored.goals.first { it.name == "Fundo de Emergência" }
        assertEquals(202612, restoredGoal.deadlineYearMonth)
        assertFalse(restoredGoal.archived)

        val restoredArchivedGoal = restored.goals.first { it.name == "Viagem \"Japão\"; 2026" }
        assertNull(restoredArchivedGoal.deadlineYearMonth)
        assertTrue(restoredArchivedGoal.archived)
    }

    @Test
    fun test9_importFromJson_v1PayloadWithoutBudgetsAndGoalsImportsEmptyWithoutException() {
        val v1Json = """
            {
              "version": 1,
              "categories": [
                {"id": "exp_food", "name": "Alimentação"}
              ],
              "transactions": []
            }
        """.trimIndent()

        val restored = serializer.importFromJson(v1Json)

        assertEquals(1, restored.version)
        assertEquals(1, restored.categories.size)
        assertTrue(restored.budgets.isEmpty())
        assertTrue(restored.goals.isEmpty())
    }

    @Test
    fun test10_importFromJson_missingBudgetsAndGoalsKeysYieldsEmptyLists() {
        val withoutKeys = serializer.importFromJson("{}")
        assertEquals(1, withoutKeys.version)
        assertTrue(withoutKeys.budgets.isEmpty())
        assertTrue(withoutKeys.goals.isEmpty())

        val withUnknownKeys = serializer.importFromJson(
            """
            {
              "version": 3,
              "categories": [],
              "transactions": [],
              "budgets": [],
              "goals": [],
              "outroCampo": {"a": 1}
            }
            """.trimIndent()
        )
        assertEquals(3, withUnknownKeys.version)
        assertTrue(withUnknownKeys.budgets.isEmpty())
        assertTrue(withUnknownKeys.goals.isEmpty())
    }

    @Test
    fun test11_restoreBackup_importsBudgetsAndGoalsAndIsIdempotent() = runTest {
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val goalDao = FakeGoalDao()

        val backupData = serializer.importFromJson(serializer.exportToJson(categories, transactions, budgets, goals))

        val firstRestore = serializer.restoreBackup(backupData, categoryDao, transactionDao, budgetDao, goalDao)

        assertEquals(3, firstRestore.categoriesImported)
        assertEquals(2, firstRestore.transactionsImported)
        assertEquals(0, firstRestore.transactionsSkipped)
        assertEquals(2, firstRestore.budgetsImported)
        assertEquals(2, firstRestore.goalsImported)

        assertEquals(2, budgetDao.count())
        assertEquals(2, goalDao.count())
        assertEquals(40000L, budgetDao.getById("budget_exp_food")?.limitCents)
        assertEquals(false, budgetDao.getById("budget_exp_housing")?.isActive)
        assertEquals(300000L, goalDao.getById("goal_fundo_202601")?.targetCents)

        val secondRestore = serializer.restoreBackup(backupData, categoryDao, transactionDao, budgetDao, goalDao)

        assertEquals(2, secondRestore.budgetsImported)
        assertEquals(2, secondRestore.goalsImported)
        assertEquals(0, secondRestore.transactionsImported)
        assertEquals(2, secondRestore.transactionsSkipped)

        assertEquals(2, budgetDao.count())
        assertEquals(2, goalDao.count())
    }

    @Test
    fun test12_restoreBackup_mergesGoalWithSameNameAndStartEvenWhenIdDiffers() = runTest {
        val goalDao = FakeGoalDao(
            listOf(
                GoalEntity(
                    id = "goal_fundo_de_emergência_202601",
                    name = "Fundo de Emergência",
                    targetCents = 100000L,
                    startYearMonth = 202601
                )
            )
        )

        val incoming = GoalEntity(
            id = "goal_9f8b2c1a-uuid-antigo",
            name = "Fundo de Emergência",
            targetCents = 250000L,
            startYearMonth = 202601
        )

        val backupData = BackupData(
            version = 2,
            categories = emptyList(),
            transactions = emptyList(),
            goals = listOf(incoming)
        )

        val result = serializer.restoreBackup(
            backupData,
            FakeCategoryDao(),
            FakeTransactionDao(),
            FakeBudgetDao(),
            goalDao
        )

        assertEquals(1, result.goalsImported)
        assertEquals(1, goalDao.count())

        val stored = goalDao.getAllSync().single()
        assertEquals("goal_fundo_de_emergência_202601", stored.id)
        assertEquals(250000L, stored.targetCents)
    }

    @Test
    fun test13_restoreBackup_withoutBudgetAndGoalDaosDoesNotFailAndReportsZeroCounts() = runTest {
        val backupData = serializer.importFromJson(serializer.exportToJson(categories, transactions, budgets, goals))

        val result = serializer.restoreBackup(backupData, FakeCategoryDao(), FakeTransactionDao())

        assertEquals(0, result.budgetsImported)
        assertEquals(0, result.goalsImported)
        assertEquals(3, result.categoriesImported)
        assertEquals(2, result.transactionsImported)
    }
}
