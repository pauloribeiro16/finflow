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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupRoundTripFidelityTest {

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

    private val transactions = listOf(
        TransactionEntity(
            id = 1L,
            type = "EXPENSE",
            amountCents = 2450L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 10).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente",
            paymentMethod = "MBWAY",
            isFixed = false
        ),
        TransactionEntity(
            id = 2L,
            type = "INCOME",
            amountCents = 250000L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            categoryId = "inc_fixed",
            contextTag = "TRABALHO",
            merchant = "Empresa",
            paymentMethod = "TRANSFER",
            isFixed = true
        ),
        TransactionEntity(
            id = 3L,
            type = "EXPENSE",
            amountCents = 55000L,
            yearMonth = 202611,
            epochDay = LocalDate.of(2026, 11, 1).toEpochDay(),
            categoryId = "exp_food",
            contextTag = "COMERCIO",
            merchant = "Renda",
            paymentMethod = "DIRECT_DEBIT",
            isFixed = true
        )
    )

    private val budgets = listOf(
        BudgetEntity(id = "budget_exp_food", categoryId = "exp_food", limitCents = 40000L, isActive = true),
        BudgetEntity(id = "budget_exp_food_supermarket", categoryId = "exp_food_supermarket", limitCents = 10000L, isActive = false)
    )

    private val goals = listOf(
        GoalEntity(
            id = GoalEntity.deterministicId("Fundo de Emergência", 202601),
            name = "Fundo de Emergência",
            targetCents = 300000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612,
            archived = false
        ),
        GoalEntity(
            id = GoalEntity.deterministicId("Viagem ao Japão", 202603),
            name = "Viagem ao Japão",
            targetCents = 150000L,
            startYearMonth = 202603,
            deadlineYearMonth = null,
            archived = true
        )
    )

    @Test
    fun test1_roundTripJsonV2_preservesEveryFieldOfEveryEntity() {
        val json = serializer.exportToJson(categories, transactions, budgets, goals)
        val restored = serializer.importFromJson(json)

        assertEquals(BackupSerializer.CURRENT_SCHEMA_VERSION, restored.version)
        assertEquals(categories, restored.categories)
        assertEquals(transactions, restored.transactions)
        assertEquals(budgets, restored.budgets)
        assertEquals(goals, restored.goals)

        val restoredCategory = restored.categories.first { it.id == "exp_food_supermarket" }
        assertEquals("exp_food", restoredCategory.parentId)
        assertEquals("Supermercado", restoredCategory.name)
        assertEquals(0xFFFF9800L, restoredCategory.colorHex)
        assertEquals("EXPENSE", restoredCategory.type)
        assertEquals(2, restoredCategory.sortOrder)

        val restoredBudget = restored.budgets.first { it.id == "budget_exp_food" }
        assertEquals("exp_food", restoredBudget.categoryId)
        assertEquals(40000L, restoredBudget.limitCents)
        assertTrue(restoredBudget.isActive)

        val restoredInactiveBudget = restored.budgets.first { it.id == "budget_exp_food_supermarket" }
        assertFalse(restoredInactiveBudget.isActive)

        val restoredGoal = restored.goals.first { it.name == "Fundo de Emergência" }
        assertEquals(300000L, restoredGoal.targetCents)
        assertEquals(202601, restoredGoal.startYearMonth)
        assertEquals(202612, restoredGoal.deadlineYearMonth)
        assertFalse(restoredGoal.archived)
    }

    @Test
    fun test2_restoreBackupIntoEmptyDaos_RebuildsTheSameContentFieldByField() = runTest {
        val json = serializer.exportToJson(categories, transactions, budgets, goals)
        val backupData = serializer.importFromJson(json)

        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val goalDao = FakeGoalDao()

        val result = serializer.restoreBackup(backupData, categoryDao, transactionDao, budgetDao, goalDao)

        assertEquals(categories.size, result.categoriesImported)
        assertEquals(transactions.size, result.transactionsImported)
        assertEquals(0, result.transactionsSkipped)
        assertEquals(budgets.size, result.budgetsImported)
        assertEquals(goals.size, result.goalsImported)

        assertEquals(categories.sortedBy { it.id }, categoryDao.getAllSync().sortedBy { it.id })
        assertEquals(transactions.sortedBy { it.id }, transactionDao.getAllSync().sortedBy { it.id })
        assertEquals(budgets.sortedBy { it.id }, budgetDao.getAllSync().sortedBy { it.id })
        assertEquals(goals.sortedBy { it.id }, goalDao.getAllSync().sortedBy { it.id })

        val restoredBudget = budgetDao.getById("budget_exp_food_supermarket")!!
        assertEquals(10000L, restoredBudget.limitCents)
        assertFalse(restoredBudget.isActive)

        val restoredGoal = goalDao.getById(GoalEntity.deterministicId("Viagem ao Japão", 202603))!!
        assertEquals(null, restoredGoal.deadlineYearMonth)
        assertTrue(restoredGoal.archived)
    }

    @Test
    fun test3_restoreBackupTwice_IsIdempotentAndKeepsBudgetsAndGoals() = runTest {
        val backupData = serializer.importFromJson(
            serializer.exportToJson(categories, transactions, budgets, goals)
        )

        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val goalDao = FakeGoalDao()

        serializer.restoreBackup(backupData, categoryDao, transactionDao, budgetDao, goalDao)
        val second = serializer.restoreBackup(backupData, categoryDao, transactionDao, budgetDao, goalDao)

        assertEquals(0, second.transactionsImported)
        assertEquals(transactions.size, second.transactionsSkipped)
        assertEquals(categories.size, categoryDao.count())
        assertEquals(transactions.size, transactionDao.count())
        assertEquals(budgets.size, budgetDao.count())
        assertEquals(goals.size, goalDao.count())

        assertEquals(40000L, budgetDao.getById("budget_exp_food")?.limitCents)
        assertEquals(300000L, goalDao.getById(goals.first().id)?.targetCents)
    }

    @Test
    fun test4_csvExportContainsOnlyTransactionsWithExactHeader() {
        val csv = serializer.exportToCsv(transactions, categories.associateBy { it.id })

        assertTrue(csv.startsWith(BackupSerializer.CSV_HEADER + "\n"))
        assertEquals(
            "Data;Mês;Tipo;Valor €;Categoria;Subcategoria;Onde (Contexto);Estabelecimento/Nota;Como (Método);Fixo",
            csv.trim().lines().first()
        )

        val rows = csv.trim().lines().drop(1)
        assertEquals(transactions.size, rows.size)
        assertEquals(10, rows.first().split(";").size)

        assertFalse("CSV não pode conter orçamentos", csv.contains("budget_"))
        assertFalse("CSV não pode conter metas", csv.contains("goal_"))
        assertFalse("CSV não pode conter o limite de orçamento", csv.contains("40000"))
        assertFalse("CSV não pode conter o alvo da meta", csv.contains("300000"))
        assertFalse("CSV não pode conter nomes de meta", csv.contains("Fundo de Emergência"))
    }

    @Test
    fun test5_csvRowsCarryTheExactValuesOfEachTransaction() {
        val csv = serializer.exportToCsv(transactions, categories.associateBy { it.id })
        val rows = csv.trim().lines().drop(1)

        assertEquals(
            "2026-10-10;2026-10;EXPENSE;24,50;Alimentação;Supermercado;CASA;Continente;MBWAY;Não",
            rows.first()
        )
        assertEquals(
            "2026-10-01;2026-10;INCOME;2500,00;Rendimento Fixo;;TRABALHO;Empresa;TRANSFER;Sim",
            rows[1]
        )
        assertEquals(
            "2026-11-01;2026-11;EXPENSE;550,00;Alimentação;;COMERCIO;Renda;DIRECT_DEBIT;Sim",
            rows[2]
        )
    }

    @Test
    fun test6_specialCharactersSurviveJsonRoundTripInMerchantAndGoalName() = runTest {
        val trickyMerchant = "Padaria; \"Alfa\" \n SECOND\n € 5,00 — Ação"
        val trickyGoalName = "Meta; \"Europa\" \n € 1000 — Ação"

        val sourceTransactions = listOf(
            transactions[0].copy(id = 11L, merchant = trickyMerchant)
        )
        val sourceGoals = listOf(
            goals[0].copy(
                id = GoalEntity.deterministicId(trickyGoalName, 202601),
                name = trickyGoalName
            )
        )

        val json = serializer.exportToJson(categories, sourceTransactions, budgets, sourceGoals)
        val restored = serializer.importFromJson(json)

        assertEquals(trickyMerchant, restored.transactions.first().merchant)
        assertEquals(trickyGoalName, restored.goals.first().name)

        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val goalDao = FakeGoalDao()

        serializer.restoreBackup(restored, categoryDao, transactionDao, budgetDao, goalDao)

        assertEquals(trickyMerchant, transactionDao.getAllSync().first().merchant)
        assertEquals(trickyGoalName, goalDao.getAllSync().first().name)
    }

    @Test
    fun test7_specialCharactersAreEscapedAndUnescapedInCsv() {
        val trickyMerchant = "Padaria; \"Alfa\" € 5,00"
        val sourceTransactions = listOf(
            transactions[0].copy(id = 21L, merchant = trickyMerchant)
        )

        val csv = serializer.exportToCsv(sourceTransactions, categories.associateBy { it.id })
        val row = csv.trim().lines().drop(1).first()

        assertTrue("Aspas e ponto-e-vírgula têm de ser escapados", row.contains("\"Padaria; \"\"Alfa\"\" € 5,00\""))
        assertTrue("O valor em euros não pode ser partido pelo escape", row.contains("24,50"))
    }

    @Test
    fun test8_newLinesInsideCsvFieldStayInsideTheQuotedField() {
        val sourceTransactions = listOf(
            transactions[0].copy(id = 22L, merchant = "Linha1\nLinha2")
        )

        val csv = serializer.exportToCsv(sourceTransactions, categories.associateBy { it.id })

        assertTrue("A quebra de linha tem de ficar dentro do campo entre aspas", csv.contains("\"Linha1\nLinha2\""))
        assertEquals(3, csv.trim().lines().size)
    }

    @Test
    fun test9_accentedAndEuroCharactersSurviveJsonRoundTrip() {
        val sourceTransactions = listOf(
            transactions[0].copy(id = 31L, merchant = "Pastelaria Açúcar & Café € 3,50")
        )
        val sourceGoals = listOf(
            goals[0].copy(id = "goal_accentos_202601", name = "Viagem àCzechia")
        )

        val restored = serializer.importFromJson(
            serializer.exportToJson(categories, sourceTransactions, budgets, sourceGoals)
        )

        assertEquals("Pastelaria Açúcar & Café € 3,50", restored.transactions.first().merchant)
        assertEquals("Viagem àCzechia", restored.goals.first().name)
    }
}
