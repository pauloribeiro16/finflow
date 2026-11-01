package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyReportGeneratorTest {

    private val generator = MonthlyReportGenerator()

    private fun tx(
        id: Long,
        type: String,
        amountCents: Long,
        categoryId: String,
        contextTag: String,
        merchant: String
    ): TransactionEntity = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = 202610,
        epochDay = 20697L + id,
        categoryId = categoryId,
        contextTag = contextTag,
        merchant = merchant,
        paymentMethod = "CARD",
        isFixed = false
    )

    private val octoberTransactions = listOf(
        tx(1, "INCOME", 200000L, "inc_base_salary", "TRABALHO", "Empresa"),
        tx(2, "EXPENSE", 50000L, "exp_food_supermarket", "CASA", "Continente"),
        tx(3, "EXPENSE", 30000L, "exp_housing_rent", "CASA", "Senhorio")
    )

    @Test
    fun generatesExactReportTextInPtPt() {
        val report = generator.generate(
            yearMonth = 202610,
            transactions = octoberTransactions,
            categories = DefaultSeed.getDefaultCategories()
        )

        val expected = listOf(
            "Relatório Financeiro — Outubro 2026",
            "Total recebido: 2.000,00 €",
            "Total gasto: 800,00 €",
            "Poupança líquida: 1.200,00 €",
            "Taxa de poupança: 60,0%",
            "% do gasto sobre o rendimento: 40,0%",
            "",
            "Top categorias:",
            "1. Alimentação — 500,00 € (62,5% dos gastos)",
            "2. Habitação & Contas — 300,00 € (37,5% dos gastos)",
            "",
            "Top contextos:",
            "1. Casa — 800,00 € (100,0% dos gastos)",
            "",
            "Top estabelecimentos:",
            "1. Continente — 500,00 € (1 movimento)",
            "2. Senhorio — 300,00 € (1 movimento)"
        ).joinToString("\n")

        assertEquals(expected, report)
    }

    @Test
    fun omitsBudgetsAndGoalsBlocksWhenAbsent() {
        val report = generator.generate(
            yearMonth = 202610,
            transactions = octoberTransactions,
            categories = DefaultSeed.getDefaultCategories()
        )

        assertFalse(report.contains("Orçamentos:"))
        assertFalse(report.contains("Metas:"))
        assertTrue(report.contains("Total recebido"))
        assertTrue(report.contains("Taxa de poupança"))
    }

    @Test
    fun includesBudgetsAndGoalsBlocksWhenProvided() {
        val budgetReport = BudgetProgressReport(
            items = listOf(
                BudgetProgress(
                    budgetId = "budget_exp_food",
                    categoryId = "exp_food",
                    categoryName = "Alimentação",
                    colorHex = 0xFFFF9800,
                    icon = "restaurant",
                    spentCents = 50000L,
                    limitCents = 40000L,
                    remainingCents = -10000L,
                    percentUsed = 125.0,
                    status = BudgetStatus.EXCEEDED
                )
            ),
            totalLimitCents = 40000L,
            totalSpentCents = 50000L,
            totalRemainingCents = -10000L
        )

        val goals = listOf(
            GoalProgress(
                goalId = "goal_reserva",
                name = "Reserva de Emergência",
                targetCents = 500000L,
                startYearMonth = 202601,
                deadlineYearMonth = 202612,
                accumulatedSavingsCents = 120000L,
                percentComplete = 24.0,
                monthsRemaining = 2,
                averageMonthlySavingsCents = 10000L,
                projectedMonthsToComplete = 38L,
                onTrack = false
            )
        )

        val report = generator.generate(
            yearMonth = 202610,
            transactions = octoberTransactions,
            categories = DefaultSeed.getDefaultCategories(),
            budgetProgress = budgetReport,
            goalProgress = goals
        )

        assertTrue(report.contains("Orçamentos:"))
        assertTrue(report.contains("- Alimentação: 500,00 € de 400,00 € (125,0%) — Excedido"))
        assertTrue(report.contains("Metas:"))
        assertTrue(report.contains("- Reserva de Emergência: 1.200,00 € de 5.000,00 € (24,0%)"))
    }

    @Test
    fun outputIsDeterministicRegardlessOfTransactionOrder() {
        val shuffled = listOf(octoberTransactions[2], octoberTransactions[0], octoberTransactions[1])

        val first = generator.generate(202610, octoberTransactions, DefaultSeed.getDefaultCategories())
        val second = generator.generate(202610, shuffled, DefaultSeed.getDefaultCategories())

        assertEquals(first, second)
    }

    @Test
    fun tiesAreBrokenAlphabeticallyByName() {
        val tied = listOf(
            tx(1, "EXPENSE", 10000L, "exp_housing_rent", "CASA", "B"),
            tx(2, "EXPENSE", 10000L, "exp_food_supermarket", "CASA", "A")
        )

        val report = generator.generate(202610, tied, DefaultSeed.getDefaultCategories())

        val foodLine = report.lines().first { it.startsWith("1.") }
        val housingLine = report.lines().first { it.startsWith("2.") }
        assertEquals("1. Alimentação — 100,00 € (50,0% dos gastos)", foodLine)
        assertEquals("2. Habitação & Contas — 100,00 € (50,0% dos gastos)", housingLine)
    }

    @Test
    fun emptyMonthProducesZeroedReportWithoutInvalidValues() {
        val report = generator.generate(202610, emptyList(), DefaultSeed.getDefaultCategories())

        assertTrue(report.contains("Total recebido: 0,00 €"))
        assertTrue(report.contains("Total gasto: 0,00 €"))
        assertTrue(report.contains("Poupança líquida: 0,00 €"))
        assertTrue(report.contains("Taxa de poupança: 0,0%"))
        assertTrue(report.contains("% do gasto sobre o rendimento: 0,0%"))
        assertTrue(report.contains("Top categorias:\nSem dados."))
        assertTrue(report.contains("Top contextos:\nSem dados."))
        assertTrue(report.contains("Top estabelecimentos:\nSem dados."))
        assertFalse(report.contains("NaN"))
        assertFalse(report.contains("Infinity"))
    }

    @Test
    fun negativeSavingsAreReportedWithSignal() {
        val transactions = listOf(
            tx(1, "INCOME", 10000L, "inc_base_salary", "TRABALHO", "Empresa"),
            tx(2, "EXPENSE", 50000L, "exp_food_supermarket", "CASA", "Continente")
        )

        val report = generator.generate(202610, transactions, DefaultSeed.getDefaultCategories())

        assertTrue(report.contains("Poupança líquida: -400,00 €"))
        assertTrue(report.contains("Taxa de poupança: -400,0%"))
        assertTrue(report.contains("% do gasto sobre o rendimento: 500,0%"))
    }

    @Test
    fun topListsAreTruncatedToThreeCategoriesThreeContextsAndFiveMerchants() {
        val transactions = listOf(
            // 5 categories, 4 contexts and 7 merchants
            tx(1, "EXPENSE", 50000L, "exp_food_supermarket", "CASA", "Mercado A"),
            tx(2, "EXPENSE", 40000L, "exp_housing_rent", "CASA", "Imobiliária B"),
            tx(3, "EXPENSE", 30000L, "exp_transport_fuel", "COMERCIO", "Gasolina C"),
            tx(4, "EXPENSE", 20000L, "exp_health_pharmacy", "SAIDA", "Farmácia D"),
            tx(5, "EXPENSE", 10000L, "exp_leisure_bars", "TRABALHO", "Bar E"),
            tx(6, "EXPENSE", 5000L, "exp_shopping_tech", "ONLINE", "Loja F"),
            tx(7, "EXPENSE", 1000L, "exp_travel_lodging", "FERIAS", "Hotel G")
        )

        val report = generator.generate(202610, transactions, DefaultSeed.getDefaultCategories())

        val categories = sectionEntries(report, "Top categorias:")
        assertEquals(3, categories.size)
        assertTrue(categories[0].startsWith("1. Alimentação"))
        assertTrue(categories[1].startsWith("2. Habitação & Contas"))
        assertTrue(categories[2].startsWith("3. Transportes"))
        assertTrue(categories.none { it.contains("Saúde") })

        val contexts = sectionEntries(report, "Top contextos:")
        assertEquals(3, contexts.size)
        assertTrue(contexts[0].startsWith("1. Casa"))
        assertTrue(contexts[1].startsWith("2. Comércio / Rua"))
        assertTrue(contexts[2].startsWith("3. Saída / Social"))

        val merchants = sectionEntries(report, "Top estabelecimentos:")
        assertEquals(5, merchants.size)
        assertTrue(merchants[0].startsWith("1. Mercado A"))
        assertTrue(merchants[1].startsWith("2. Imobiliária B"))
        assertTrue(merchants[2].startsWith("3. Gasolina C"))
        assertTrue(merchants[3].startsWith("4. Farmácia D"))
        assertTrue(merchants[4].startsWith("5. Bar E"))
        assertTrue(merchants.none { it.contains("Loja F") })
        assertTrue(merchants.none { it.contains("Hotel G") })
    }

    private fun sectionEntries(report: String, header: String): List<String> {
        val section = report.split("\n\n").first { it.startsWith(header) }
        return section.lines().drop(1)
    }

    @Test
    fun criterion10_topListsRespectCustomLimits() {
        val transactions = listOf(
            tx(1, "EXPENSE", 50000L, "exp_food_supermarket", "CASA", "Mercado A"),
            tx(2, "EXPENSE", 40000L, "exp_housing_rent", "CASA", "Imobiliária B"),
            tx(3, "EXPENSE", 30000L, "exp_transport_fuel", "COMERCIO", "Gasolina C"),
            tx(4, "EXPENSE", 20000L, "exp_health_pharmacy", "SAIDA", "Farmácia D"),
            tx(5, "EXPENSE", 10000L, "exp_leisure_bars", "TRABALHO", "Bar E")
        )

        val report = generator.generate(
            yearMonth = 202610,
            transactions = transactions,
            categories = DefaultSeed.getDefaultCategories(),
            topCategoriesLimit = 1,
            topContextsLimit = 2,
            topMerchantsLimit = 2
        )

        assertEquals(1, sectionEntries(report, "Top categorias:").size)
        assertEquals(2, sectionEntries(report, "Top contextos:").size)
        assertEquals(2, sectionEntries(report, "Top estabelecimentos:").size)
        assertTrue(sectionEntries(report, "Top categorias:").first().startsWith("1. Alimentação"))
    }

    @Test
    fun criterion10_merchantBlockCountsMovementsInSingularAndPlural() {
        val transactions = listOf(
            tx(1, "EXPENSE", 10000L, "exp_food_supermarket", "CASA", "Continente"),
            tx(2, "EXPENSE", 20000L, "exp_food_supermarket", "CASA", "Continente"),
            tx(3, "EXPENSE", 5000L, "exp_housing_rent", "CASA", "Senhorio")
        )

        val report = generator.generate(202610, transactions, DefaultSeed.getDefaultCategories())
        val merchants = sectionEntries(report, "Top estabelecimentos:")

        assertEquals("1. Continente — 300,00 € (2 movimentos)", merchants[0])
        assertEquals("2. Senhorio — 50,00 € (1 movimento)", merchants[1])
    }

    @Test
    fun criterion10_emptyBudgetReportDoesNotEmitTheBudgetsBlock() {
        val report = generator.generate(
            yearMonth = 202610,
            transactions = octoberTransactions,
            categories = DefaultSeed.getDefaultCategories(),
            budgetProgress = BudgetProgressReport()
        )

        assertFalse(report.contains("Orçamentos:"))
        assertTrue(report.contains("Top categorias:"))
    }

    @Test
    fun criterion10_multipleGoalsAreListedInTheGoalsBlock() {
        val goals = listOf(
            GoalProgress(
                goalId = "goal_reserva",
                name = "Reserva de Emergência",
                targetCents = 500000L,
                startYearMonth = 202601,
                deadlineYearMonth = 202612,
                accumulatedSavingsCents = 120000L,
                percentComplete = 24.0,
                monthsRemaining = 2,
                averageMonthlySavingsCents = 10000L,
                projectedMonthsToComplete = 38L,
                onTrack = false
            ),
            GoalProgress(
                goalId = "goal_viagem",
                name = "Viagem ao Japão",
                targetCents = 300000L,
                startYearMonth = 202603,
                deadlineYearMonth = null,
                accumulatedSavingsCents = 30000L,
                percentComplete = 10.0,
                monthsRemaining = null,
                averageMonthlySavingsCents = 10000L,
                projectedMonthsToComplete = 27L,
                onTrack = null
            )
        )

        val report = generator.generate(
            yearMonth = 202610,
            transactions = octoberTransactions,
            categories = DefaultSeed.getDefaultCategories(),
            goalProgress = goals
        )

        assertTrue(report.contains("Metas:"))
        assertTrue(report.contains("- Reserva de Emergência: 1.200,00 € de 5.000,00 € (24,0%)"))
        assertTrue(report.contains("- Viagem ao Japão: 300,00 € de 3.000,00 € (10,0%)"))
    }

    @Test
    fun criterion10_reportIsDeterministicWithALargerShuffledSet() {
        val transactions = listOf(
            tx(1, "INCOME", 200000L, "inc_base_salary", "TRABALHO", "Empresa"),
            tx(2, "EXPENSE", 50000L, "exp_food_supermarket", "CASA", "Continente"),
            tx(3, "EXPENSE", 30000L, "exp_housing_rent", "CASA", "Senhorio"),
            tx(4, "EXPENSE", 20000L, "exp_transport_fuel", "COMERCIO", "Galp"),
            tx(5, "EXPENSE", 10000L, "exp_health_pharmacy", "SAIDA", "Farmácia"),
            tx(6, "EXPENSE", 5000L, "exp_leisure_bars", "TRABALHO", "Bar"),
            tx(7, "INCOME", 10000L, "inc_refunds", "TRABALHO", "Reembolso")
        )

        val shuffled = listOf(transactions[5], transactions[0], transactions[3], transactions[6], transactions[2], transactions[4], transactions[1])

        val first = generator.generate(202610, transactions, DefaultSeed.getDefaultCategories())
        val second = generator.generate(202610, shuffled, DefaultSeed.getDefaultCategories())

        assertEquals(first, second)
    }

    @Test
    fun criterion10_expensesWithoutIncome_ShowZeroRatesWithoutNaN() {
        val transactions = listOf(
            tx(1, "EXPENSE", 50000L, "exp_food_supermarket", "CASA", "Continente")
        )

        val report = generator.generate(202610, transactions, DefaultSeed.getDefaultCategories())

        assertTrue(report.contains("Total recebido: 0,00 €"))
        assertTrue(report.contains("Total gasto: 500,00 €"))
        assertTrue(report.contains("Poupança líquida: -500,00 €"))
        assertTrue(report.contains("Taxa de poupança: 0,0%"))
        assertTrue(report.contains("% do gasto sobre o rendimento: 0,0%"))
        assertTrue(report.contains("1. Alimentação — 500,00 € (100,0% dos gastos)"))
        assertFalse(report.contains("NaN"))
        assertFalse(report.contains("Infinity"))
    }

    @Test
    fun criterion10_budgetsBlockUsesTheStatusLabelOfEachBudget() {
        val budgetReport = BudgetProgressReport(
            items = listOf(
                BudgetProgress(
                    budgetId = "budget_exp_food",
                    categoryId = "exp_food",
                    categoryName = "Alimentação",
                    colorHex = 0xFFFF9800,
                    icon = "restaurant",
                    spentCents = 5000L,
                    limitCents = 40000L,
                    remainingCents = 35000L,
                    percentUsed = 12.5,
                    status = BudgetStatus.OK
                ),
                BudgetProgress(
                    budgetId = "budget_exp_housing",
                    categoryId = "exp_housing",
                    categoryName = "Habitação & Contas",
                    colorHex = 0xFF1976D2,
                    icon = "home",
                    spentCents = 40000L,
                    limitCents = 40000L,
                    remainingCents = 0L,
                    percentUsed = 100.0,
                    status = BudgetStatus.WARNING
                )
            )
        )

        val report = generator.generate(
            yearMonth = 202610,
            transactions = octoberTransactions,
            categories = DefaultSeed.getDefaultCategories(),
            budgetProgress = budgetReport
        )

        assertTrue(report.contains("- Alimentação: 50,00 € de 400,00 € (12,5%) — OK"))
        assertTrue(report.contains("- Habitação & Contas: 400,00 € de 400,00 € (100,0%) — Atenção"))
    }
}
