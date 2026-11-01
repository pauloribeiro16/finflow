package com.financas.app.domain

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class MonthlyAnalyticsCalculatorTest {

    private lateinit var calculator: MonthlyAnalyticsCalculator
    private val categories = DefaultSeed.getDefaultCategories()

    @Before
    fun setUp() {
        calculator = MonthlyAnalyticsCalculator()
    }

    @Test
    fun testStandardMonthWithSalaryAndExpenses() {
        val baseEpoch = LocalDate.of(2026, 10, 1).toEpochDay()

        val transactions = listOf(
            // Salário Base (1.500,00 €)
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 150000L,
                yearMonth = 202610,
                epochDay = baseEpoch,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa X",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            // Renda (500,00 € - Fixo)
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 50000L,
                yearMonth = 202610,
                epochDay = baseEpoch,
                categoryId = "exp_housing_rent",
                contextTag = "CASA",
                merchant = "Senhorio",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            // Supermercado (250,00 € - Variável)
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 25000L,
                yearMonth = 202610,
                epochDay = baseEpoch + 5,
                categoryId = "exp_food_supermarket",
                contextTag = "COMERCIO",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            ),
            // Almoço de Trabalho (50,00 € - Variável)
            TransactionEntity(
                id = 4,
                type = "EXPENSE",
                amountCents = 5000L,
                yearMonth = 202610,
                epochDay = baseEpoch + 6,
                categoryId = "exp_food_lunch",
                contextTag = "TRABALHO",
                merchant = "Café Central",
                paymentMethod = "MBWAY",
                isFixed = false
            )
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = transactions,
            categories = categories
        )

        // Verificações de Totais
        assertEquals(150000L, summary.totalIncomeCents)
        assertEquals(150000L, summary.baseIncomeCents)
        assertEquals(0L, summary.extraIncomeCents)

        assertEquals(80000L, summary.totalExpenseCents) // 500 + 250 + 50 = 800 €
        assertEquals(50000L, summary.fixedExpenseCents)
        assertEquals(30000L, summary.variableExpenseCents)

        assertEquals(70000L, summary.netSavingsCents) // 1500 - 800 = 700 €

        // Taxa de Poupança: 700 / 1500 = 46.666...%
        assertEquals(46.66, summary.savingsRatePercent, 0.01)

        // Rácio Gasto/Rendimento: 800 / 1500 = 53.333...%
        assertEquals(53.33, summary.expenseIncomeRatioPercent, 0.01)

        // Rácio Fixo/Rendimento: 500 / 1500 = 33.333...%
        assertEquals(33.33, summary.fixedExpenseIncomeRatioPercent, 0.01)

        // Rácio Variável/Rendimento: 300 / 1500 = 20.0%
        assertEquals(20.0, summary.variableExpenseIncomeRatioPercent, 0.01)
    }

    @Test
    fun testZeroIncomeGuardsAgainstDivisionByZero() {
        val baseEpoch = LocalDate.of(2026, 10, 1).toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 25000L,
                yearMonth = 202610,
                epochDay = baseEpoch,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            )
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = transactions,
            categories = categories
        )

        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(25000L, summary.totalExpenseCents)
        assertEquals(-25000L, summary.netSavingsCents)

        // Nenhuma divisão por zero deve produzir NaN ou Infinity
        assertFalse(summary.savingsRatePercent.isNaN())
        assertFalse(summary.savingsRatePercent.isInfinite())
        assertEquals(0.0, summary.savingsRatePercent, 0.0)

        assertFalse(summary.expenseIncomeRatioPercent.isNaN())
        assertFalse(summary.expenseIncomeRatioPercent.isInfinite())
        assertEquals(0.0, summary.expenseIncomeRatioPercent, 0.0)

        val catShare = summary.categoryShares.first()
        assertEquals(0.0, catShare.percentageOfIncome, 0.0)
        assertEquals(100.0, catShare.percentageOfExpense, 0.01)
    }

    @Test
    fun testZeroExpenseGuardsAgainstDivisionByZero() {
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 150000L,
                yearMonth = 202610,
                epochDay = 1000L,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa",
                paymentMethod = "TRANSFER",
                isFixed = true
            )
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = transactions,
            categories = categories
        )

        assertEquals(150000L, summary.totalIncomeCents)
        assertEquals(0L, summary.totalExpenseCents)
        assertEquals(150000L, summary.netSavingsCents)
        assertEquals(100.0, summary.savingsRatePercent, 0.01)
        assertEquals(0.0, summary.expenseIncomeRatioPercent, 0.0)
        assertTrue(summary.categoryShares.isEmpty())
    }

    @Test
    fun testRollupSubcategoriesToParentCategory() {
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 100000L, // 1.000 €
                yearMonth = 202610,
                epochDay = 1000L,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            // Alimentação > Supermercado (120 €)
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 12000L,
                yearMonth = 202610,
                epochDay = 1001L,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Pingo Doce",
                paymentMethod = "CARD",
                isFixed = false
            ),
            // Alimentação > Almoços / Trabalho (80 €)
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 8000L,
                yearMonth = 202610,
                epochDay = 1002L,
                categoryId = "exp_food_lunch",
                contextTag = "TRABALHO",
                merchant = "Cantina",
                paymentMethod = "MBWAY",
                isFixed = false
            ),
            // Habitação > Renda (400 €)
            TransactionEntity(
                id = 4,
                type = "EXPENSE",
                amountCents = 40000L,
                yearMonth = 202610,
                epochDay = 1003L,
                categoryId = "exp_housing_rent",
                contextTag = "CASA",
                merchant = "Senhorio",
                paymentMethod = "TRANSFER",
                isFixed = true
            )
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = transactions,
            categories = categories
        )

        // Total Expense = 600 € (60000 cents)
        assertEquals(60000L, summary.totalExpenseCents)
        assertEquals(2, summary.categoryShares.size)

        // Top category must be Habitação (400 €)
        val topCategory = summary.categoryShares[0]
        assertEquals("exp_housing", topCategory.categoryId)
        assertEquals(40000L, topCategory.amountCents)
        assertEquals(40.0, topCategory.percentageOfIncome, 0.01) // 400 / 1000 = 40%
        assertEquals(66.66, topCategory.percentageOfExpense, 0.01) // 400 / 600 = 66.66%

        // Second category is Alimentação (200 € = 120 + 80)
        val foodCategory = summary.categoryShares[1]
        assertEquals("exp_food", foodCategory.categoryId)
        assertEquals(20000L, foodCategory.amountCents)
        assertEquals(20.0, foodCategory.percentageOfIncome, 0.01) // 200 / 1000 = 20%
        assertEquals(33.33, foodCategory.percentageOfExpense, 0.01) // 200 / 600 = 33.33%

        // Subcategories of Alimentação
        assertEquals(2, foodCategory.subcategories.size)
        val sub1 = foodCategory.subcategories[0]
        assertEquals("exp_food_supermarket", sub1.categoryId)
        assertEquals(12000L, sub1.amountCents)
        assertEquals(12.0, sub1.percentageOfIncome, 0.01) // 120 / 1000 = 12%

        val sub2 = foodCategory.subcategories[1]
        assertEquals("exp_food_lunch", sub2.categoryId)
        assertEquals(8000L, sub2.amountCents)
        assertEquals(8.0, sub2.percentageOfIncome, 0.01) // 80 / 1000 = 8%
    }

    @Test
    fun testHistoricalAverageAndDeviation() {
        val historicalMonths = listOf(100000L, 100000L) // Média histórica = 1.000 €

        // Caso 1: Gasto atual = 920 € (92000 cents) -> Desvio = -8.0%
        val deviationBelow = MonthlyAnalyticsCalculator.calculateDeviationVsAverage(92000L, 100000L)
        assertNotNull(deviationBelow)
        assertEquals(-8.0, deviationBelow!!, 0.01)

        // Caso 2: Gasto atual = 1100 € (110000 cents) -> Desvio = +10.0%
        val deviationAbove = MonthlyAnalyticsCalculator.calculateDeviationVsAverage(110000L, 100000L)
        assertNotNull(deviationAbove)
        assertEquals(10.0, deviationAbove!!, 0.01)

        // Caso 3: Média histórica 0L -> Desvio é null
        val deviationZero = MonthlyAnalyticsCalculator.calculateDeviationVsAverage(92000L, 0L)
        assertNull(deviationZero)

        // Teste integrado no MonthlySummary
        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(
                TransactionEntity(
                    id = 1,
                    type = "EXPENSE",
                    amountCents = 92000L,
                    yearMonth = 202610,
                    epochDay = 1000L,
                    categoryId = "exp_housing_rent",
                    contextTag = "CASA",
                    merchant = "Senhorio",
                    paymentMethod = "TRANSFER",
                    isFixed = true
                )
            ),
            categories = categories,
            historicalMonthlyExpenses = historicalMonths
        )

        assertNotNull(summary.deviationVsAveragePercent)
        assertEquals(-8.0, summary.deviationVsAveragePercent!!, 0.01)
    }

    @Test
    fun testFourDimensionsBreakdown() {
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 100000L,
                yearMonth = 202610,
                epochDay = 1000L,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 1500L,
                yearMonth = 202610,
                epochDay = 1001L,
                categoryId = "exp_food_lunch",
                contextTag = "TRABALHO",
                merchant = "Café Central",
                paymentMethod = "MBWAY",
                isFixed = false
            ),
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 2500L,
                yearMonth = 202610,
                epochDay = 1002L,
                categoryId = "exp_food_lunch",
                contextTag = "TRABALHO",
                merchant = "Café Central",
                paymentMethod = "MBWAY",
                isFixed = false
            ),
            TransactionEntity(
                id = 4,
                type = "EXPENSE",
                amountCents = 6000L,
                yearMonth = 202610,
                epochDay = 1003L,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            )
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = transactions,
            categories = categories
        )

        // Context breakdown
        assertEquals(2, summary.contextShares.size)
        val trabalhoContext = summary.contextShares.find { it.contextTag == "TRABALHO" }
        assertNotNull(trabalhoContext)
        assertEquals(4000L, trabalhoContext!!.amountCents)
        assertEquals(2, trabalhoContext.transactionCount)

        // Merchant breakdown
        assertEquals(2, summary.topMerchants.size)
        val continete = summary.topMerchants.find { it.merchant == "Continente" }
        assertNotNull(continete)
        assertEquals(6000L, continete!!.amountCents)

        val cafeCentral = summary.topMerchants.find { it.merchant == "Café Central" }
        assertNotNull(cafeCentral)
        assertEquals(4000L, cafeCentral!!.amountCents)
        assertEquals(2, cafeCentral.transactionCount)
        assertEquals(2000L, cafeCentral.averagePerVisitCents) // 4000 / 2 = 2000

        // Method breakdown
        assertEquals(2, summary.methodShares.size)
        val mbway = summary.methodShares.find { it.paymentMethod == "MBWAY" }
        assertNotNull(mbway)
        assertEquals(4000L, mbway!!.amountCents)

        val card = summary.methodShares.find { it.paymentMethod == "CARD" }
        assertNotNull(card)
        assertEquals(6000L, card!!.amountCents)
    }

    @Test
    fun testBaseVsExtraIncomeClassification_byFixedFlagAndKnownCategoryIds() {
        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 150000L, yearMonth = 202610, epochDay = 1000L,
                categoryId = "inc_base_salary", contextTag = "TRABALHO", merchant = "Empresa",
                paymentMethod = "TRANSFER", isFixed = false
            ),
            TransactionEntity(
                id = 2, type = "INCOME", amountCents = 8000L, yearMonth = 202610, epochDay = 1001L,
                categoryId = "inc_meal_allowance", contextTag = "TRABALHO", merchant = "Subsídio",
                paymentMethod = "TRANSFER", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "INCOME", amountCents = 5000L, yearMonth = 202610, epochDay = 1002L,
                categoryId = "inc_bonus", contextTag = "TRABALHO", merchant = "Bónus",
                paymentMethod = "TRANSFER", isFixed = false
            ),
            TransactionEntity(
                id = 4, type = "INCOME", amountCents = 2000L, yearMonth = 202610, epochDay = 1003L,
                categoryId = "inc_other", contextTag = "TRABALHO", merchant = "Extra",
                paymentMethod = "MBWAY", isFixed = true
            )
        )

        val summary = calculator.calculateMonthlySummary(202610, transactions, categories)

        assertEquals(165000L, summary.totalIncomeCents)
        assertEquals(160000L, summary.baseIncomeCents)
        assertEquals(5000L, summary.extraIncomeCents)
    }

    @Test
    fun testZeroIncomeGuards_allDimensionPercentagesAreExactlyZero() {
        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "EXPENSE", amountCents = 40000L, yearMonth = 202610, epochDay = 1000L,
                categoryId = "exp_housing_rent", contextTag = "CASA", merchant = "Senhorio",
                paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 3000L, yearMonth = 202610, epochDay = 1001L,
                categoryId = "exp_food_lunch", contextTag = "SAIDA", merchant = "Café Central",
                paymentMethod = "MBWAY", isFixed = false
            )
        )

        val summary = calculator.calculateMonthlySummary(202610, transactions, categories)

        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(0.0, summary.savingsRatePercent, 0.0)
        assertEquals(0.0, summary.expenseIncomeRatioPercent, 0.0)
        assertEquals(0.0, summary.fixedExpenseIncomeRatioPercent, 0.0)
        assertEquals(0.0, summary.variableExpenseIncomeRatioPercent, 0.0)

        summary.contextShares.forEach { assertEquals(0.0, it.percentageOfIncome, 0.0) }
        summary.topMerchants.forEach { assertEquals(0.0, it.percentageOfIncome, 0.0) }
        summary.methodShares.forEach { assertEquals(0.0, it.percentageOfIncome, 0.0) }

        val senhorio = summary.topMerchants.first { it.merchant == "Senhorio" }
        assertEquals(40000L / 43000L.toDouble() * 100.0, senhorio.percentageOfExpense, 0.001)
    }

    @Test
    fun testContextMerchantMethodShares_exactValues_trimAndBlankMerchantExcluded() {
        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 50000L, yearMonth = 202610, epochDay = 1000L,
                categoryId = "inc_base_salary", contextTag = "TRABALHO", merchant = "Empresa",
                paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 10000L, yearMonth = 202610, epochDay = 1001L,
                categoryId = "exp_food_supermarket", contextTag = "CASA", merchant = "Continente ",
                paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 5000L, yearMonth = 202610, epochDay = 1002L,
                categoryId = "exp_food_supermarket", contextTag = "CASA", merchant = "Continente",
                paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 4, type = "EXPENSE", amountCents = 2000L, yearMonth = 202610, epochDay = 1003L,
                categoryId = "exp_food_lunch", contextTag = "SAIDA", merchant = "   ",
                paymentMethod = "MBWAY", isFixed = false
            ),
            TransactionEntity(
                id = 5, type = "EXPENSE", amountCents = 8000L, yearMonth = 202610, epochDay = 1004L,
                categoryId = "exp_food_supermarket", contextTag = "CASA", merchant = "Pingo Doce",
                paymentMethod = "MBWAY", isFixed = false
            )
        )

        val summary = calculator.calculateMonthlySummary(202610, transactions, categories)

        assertEquals(25000L, summary.totalExpenseCents)

        assertEquals(2, summary.contextShares.size)
        val casa = summary.contextShares[0]
        assertEquals("CASA", casa.contextTag)
        assertEquals("Casa", casa.contextLabel)
        assertEquals(23000L, casa.amountCents)
        assertEquals(3, casa.transactionCount)
        assertEquals(46.0, casa.percentageOfIncome, 0.001)
        assertEquals(92.0, casa.percentageOfExpense, 0.001)
        assertEquals("SAIDA", summary.contextShares[1].contextTag)
        assertEquals(2000L, summary.contextShares[1].amountCents)

        assertEquals(2, summary.topMerchants.size)
        val continente = summary.topMerchants[0]
        assertEquals("Continente", continente.merchant)
        assertEquals(15000L, continente.amountCents)
        assertEquals(2, continente.transactionCount)
        assertEquals(7500L, continente.averagePerVisitCents)
        assertEquals(30.0, continente.percentageOfIncome, 0.001)
        assertEquals(60.0, continente.percentageOfExpense, 0.001)
        assertEquals("Pingo Doce", summary.topMerchants[1].merchant)

        assertEquals(2, summary.methodShares.size)
        assertEquals("CARD", summary.methodShares[0].paymentMethod)
        assertEquals("Cartão", summary.methodShares[0].methodLabel)
        assertEquals(15000L, summary.methodShares[0].amountCents)
        assertEquals("MBWAY", summary.methodShares[1].paymentMethod)
        assertEquals("MB Way", summary.methodShares[1].methodLabel)
        assertEquals(10000L, summary.methodShares[1].amountCents)
    }

    @Test
    fun testHistoricalAverageAndDeviationGuards_emptyZeroAndPositiveOnly() {
        assertEquals(0L, MonthlyAnalyticsCalculator.calculateHistoricalAverage(emptyList()))
        assertEquals(0L, MonthlyAnalyticsCalculator.calculateHistoricalAverage(listOf(0L, 0L, 0L)))
        assertEquals(150000L, MonthlyAnalyticsCalculator.calculateHistoricalAverage(listOf(0L, 100000L, 200000L)))
        assertEquals(60000L, MonthlyAnalyticsCalculator.calculateHistoricalAverage(listOf(-5000L, 90000L, 30000L)))

        assertEquals(-100.0, MonthlyAnalyticsCalculator.calculateDeviationVsAverage(0L, 100000L)!!, 0.001)
        assertNull(MonthlyAnalyticsCalculator.calculateDeviationVsAverage(50000L, 0L))
        assertNull(MonthlyAnalyticsCalculator.calculateDeviationVsAverage(50000L, -100L))

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 3000L, yearMonth = 202610, epochDay = 1000L,
                    categoryId = "exp_food_lunch", contextTag = "TRABALHO", merchant = "Café Central",
                    paymentMethod = "MBWAY", isFixed = false
                )
            ),
            categories = categories,
            historicalMonthlyExpenses = emptyList()
        )
        assertNull(summary.deviationVsAveragePercent)
    }
}
