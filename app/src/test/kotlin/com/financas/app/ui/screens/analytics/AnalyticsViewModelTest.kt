package com.financas.app.ui.screens.analytics

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeTransactionDao
import com.financas.app.ui.fakes.FakeUserProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var analyticsCalculator: MonthlyAnalyticsCalculator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao()
        analyticsCalculator = MonthlyAnalyticsCalculator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_displayModeToggle_formatsValuesAcrossAllFourViews() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()

        // Income: 2000 € (200_000 cents)
        // Expense: 500 € (50_000 cents) -> 25,0% do recebido
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 200000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 50000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "MBWAY",
                isFixed = true
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        // 1. Initial Mode: EURO
        assertEquals(DisplayMode.EURO, viewModel.uiState.value.displayMode)

        // Vista 1: No Quê
        val parentFoodEuro = viewModel.uiState.value.parentCategories.find { it.id == "exp_food" }
        assertNotNull(parentFoodEuro)
        assertEquals("500,00 €", parentFoodEuro!!.formattedValue)

        // Vista 2: Onde
        val contextCasaEuro = viewModel.uiState.value.contexts.find { it.tag == "CASA" }
        assertNotNull(contextCasaEuro)
        assertEquals("500,00 €", contextCasaEuro!!.formattedValue)

        val merchantContinenteEuro = viewModel.uiState.value.topMerchants.find { it.merchant == "Continente" }
        assertNotNull(merchantContinenteEuro)
        assertEquals("500,00 €", merchantContinenteEuro!!.formattedValue)

        // Vista 3: Como
        assertEquals("500,00 €", viewModel.uiState.value.rigidity.formattedFixed)
        val methodMbwayEuro = viewModel.uiState.value.paymentMethods.find { it.code == "MBWAY" }
        assertNotNull(methodMbwayEuro)
        assertEquals("500,00 €", methodMbwayEuro!!.formattedValue)

        // Vista 4: Evolução
        val octEvoEuro = viewModel.uiState.value.selectedEvolutionMonthItem
        assertNotNull(octEvoEuro)
        assertEquals("500,00 €", octEvoEuro!!.formattedExpense)

        // 2. Toggle Mode to INCOME_PERCENTAGE
        viewModel.toggleDisplayMode()
        assertEquals(DisplayMode.INCOME_PERCENTAGE, viewModel.uiState.value.displayMode)

        // Vista 1: No Quê in % do recebido
        val parentFoodPct = viewModel.uiState.value.parentCategories.find { it.id == "exp_food" }
        assertNotNull(parentFoodPct)
        assertEquals("25,0% do recebido", parentFoodPct!!.formattedValue)

        // Vista 2: Onde in % do recebido
        val contextCasaPct = viewModel.uiState.value.contexts.find { it.tag == "CASA" }
        assertNotNull(contextCasaPct)
        assertEquals("25,0% do recebido", contextCasaPct!!.formattedValue)

        val merchantContinentePct = viewModel.uiState.value.topMerchants.find { it.merchant == "Continente" }
        assertNotNull(merchantContinentePct)
        assertEquals("25,0% do recebido", merchantContinentePct!!.formattedValue)

        // Vista 3: Como in % do recebido
        assertEquals("25,0% do recebido", viewModel.uiState.value.rigidity.formattedFixed)
        val methodMbwayPct = viewModel.uiState.value.paymentMethods.find { it.code == "MBWAY" }
        assertNotNull(methodMbwayPct)
        assertEquals("25,0% do recebido", methodMbwayPct!!.formattedValue)

        // Vista 4: Evolução in % do recebido
        val octEvoPct = viewModel.uiState.value.selectedEvolutionMonthItem
        assertNotNull(octEvoPct)
        assertEquals("25,0% do recebido", octEvoPct!!.formattedExpense)
    }

    @Test
    fun test2_noQue_selectingParentCategoryExpandsSubcategoriesSummingTo100Percent() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()

        // Total Parent Food = 350 € + 150 € = 500 € (50_000 cents)
        // Supermercado: 350 € -> 70,0%
        // Restaurantes: 150 € -> 30,0%
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 35000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Supermercado A",
                paymentMethod = "CARD",
                isFixed = false
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 15000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_restaurant",
                contextTag = "SAIDA",
                merchant = "Restaurante B",
                paymentMethod = "CARD",
                isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        // Before selection: expandedSubcategories should be empty
        assertNull(viewModel.uiState.value.selectedParentCategoryId)
        assertTrue(viewModel.uiState.value.expandedSubcategories.isEmpty())

        // Select "exp_food"
        viewModel.selectParentCategory("exp_food")
        assertEquals("exp_food", viewModel.uiState.value.selectedParentCategoryId)

        val expanded = viewModel.uiState.value.expandedSubcategories
        assertEquals(2, expanded.size)

        val totalChildAmount = expanded.sumOf { it.amountCents }
        assertEquals(50000L, totalChildAmount)

        val sumPercentages = expanded.sumOf { it.percentageOfParent }
        assertEquals(100.0, sumPercentages, 0.01)

        val supermarket = expanded.find { it.id == "exp_food_supermarket" }
        assertNotNull(supermarket)
        assertEquals(70.0, supermarket!!.percentageOfParent, 0.01)

        val restaurant = expanded.find { it.id == "exp_food_restaurant" }
        assertNotNull(restaurant)
        assertEquals(30.0, restaurant!!.percentageOfParent, 0.01)

        // Deselecting toggles it back to null
        viewModel.selectParentCategory("exp_food")
        assertNull(viewModel.uiState.value.selectedParentCategoryId)
        assertTrue(viewModel.uiState.value.expandedSubcategories.isEmpty())
    }

    @Test
    fun test3_onde_sortsContextsAndMerchantsDescendingWithFrequencyAndAverageTicket() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()

        // Merchant Continente: 3 transactions (100 €, 50 €, 150 €) -> Total 300 €, count = 3, avg ticket = 100 € (10_000 cents)
        // Merchant Pingo Doce: 1 transaction (80 €) -> Total 80 €, count = 1, avg ticket = 80 € (8_000 cents)
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 10000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 5000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            ),
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 15000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            ),
            TransactionEntity(
                id = 4,
                type = "EXPENSE",
                amountCents = 8000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "TRABALHO",
                merchant = "Pingo Doce",
                paymentMethod = "CARD",
                isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val topMerchants = viewModel.uiState.value.topMerchants
        assertEquals(2, topMerchants.size)

        // Check Ranking: #1 Continente, #2 Pingo Doce
        assertEquals("Continente", topMerchants[0].merchant)
        assertEquals(30000L, topMerchants[0].amountCents)
        assertEquals(3, topMerchants[0].transactionCount)
        assertEquals(10000L, topMerchants[0].averagePerVisitCents)
        assertEquals("100,00 € / ida", topMerchants[0].formattedAverageTicket)

        assertEquals("Pingo Doce", topMerchants[1].merchant)
        assertEquals(8000L, topMerchants[1].amountCents)
        assertEquals(1, topMerchants[1].transactionCount)
        assertEquals(8000L, topMerchants[1].averagePerVisitCents)
        assertEquals("80,00 € / ida", topMerchants[1].formattedAverageTicket)

        // Check Contexts: CASA (300 €) > TRABALHO (80 €)
        val contexts = viewModel.uiState.value.contexts
        assertEquals(2, contexts.size)
        assertEquals("CASA", contexts[0].tag)
        assertEquals(30000L, contexts[0].amountCents)
        assertEquals(3, contexts[0].transactionCount)

        assertEquals("TRABALHO", contexts[1].tag)
        assertEquals(8000L, contexts[1].amountCents)
        assertEquals(1, contexts[1].transactionCount)
    }

    @Test
    fun test4_evolucao_calculates12MonthsAndAverageWithoutDilutionFromZeroMonths() = runTest {
        // Year 2026: 12 months (202601 to 202612)
        // January: 1000 € (100_000 cents)
        // February: 2000 € (200_000 cents)
        // Months 3 to 12: 0 € (future empty months)
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 100000L,
                yearMonth = 202601,
                epochDay = LocalDate.of(2026, 1, 10).toEpochDay(),
                categoryId = "exp_housing_rent",
                contextTag = "CASA",
                merchant = "Senhorio",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 200000L,
                yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 10).toEpochDay(),
                categoryId = "exp_housing_rent",
                contextTag = "CASA",
                merchant = "Senhorio",
                paymentMethod = "TRANSFER",
                isFixed = true
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202602
        )

        val evolutionMonths = viewModel.uiState.value.evolutionMonths
        assertEquals(12, evolutionMonths.size)

        // Historical Monthly Average should be: (1000 € + 2000 €) / 2 = 1500 € (150_000 cents)
        // NOT diluted by the other 10 empty months (which would be 300_000 / 12 = 25_000 cents)
        val historicalAverage = viewModel.uiState.value.historicalMonthlyAverageExpenseCents
        assertEquals(150000L, historicalAverage)

        // Selected month is February 2026 (2000 €)
        // Deviation vs average: (2000 - 1500) / 1500 * 100 = +33.33%
        val febItem = evolutionMonths.find { it.yearMonth == 202602 }
        assertNotNull(febItem)
        assertNotNull(febItem!!.deviationVsAveragePercent)
        assertEquals(33.33, febItem.deviationVsAveragePercent!!, 0.05)

        // Select January 2026
        viewModel.selectEvolutionMonth(202601)
        assertEquals(202601, viewModel.uiState.value.selectedEvolutionMonth)
        val janItem = viewModel.uiState.value.selectedEvolutionMonthItem
        assertNotNull(janItem)
        assertEquals(202601, janItem!!.yearMonth)
        // Jan deviation vs average: (1000 - 1500) / 1500 * 100 = -33.33%
        assertNotNull(janItem.deviationVsAveragePercent)
        assertEquals(-33.33, janItem.deviationVsAveragePercent!!, 0.05)
    }

    @Test
    fun test5_comparar_previousMonthBaseline_buildsDeltasAndRanking() = runTest {
        val epochSep = LocalDate.of(2026, 9, 10).toEpochDay()
        val epochOct = LocalDate.of(2026, 10, 10).toEpochDay()

        // September 2026: income 1800 €, expenses 850 € (Food 600 € + Housing 250 €)
        // October 2026: income 2000 €, expenses 800 € (Food 800 € + Housing 200 €)
        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 180000L, yearMonth = 202609,
                epochDay = epochSep, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 60000L, yearMonth = 202609,
                epochDay = epochSep, categoryId = "exp_food_supermarket", contextTag = "CASA",
                merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 25000L, yearMonth = 202609,
                epochDay = epochSep, categoryId = "exp_housing_rent", contextTag = "CASA",
                merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 4, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                epochDay = epochOct, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 5, type = "EXPENSE", amountCents = 50000L, yearMonth = 202610,
                epochDay = epochOct, categoryId = "exp_food_supermarket", contextTag = "CASA",
                merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 6, type = "EXPENSE", amountCents = 30000L, yearMonth = 202610,
                epochDay = epochOct, categoryId = "exp_food_restaurant", contextTag = "SAIDA",
                merchant = "Restaurante", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 7, type = "EXPENSE", amountCents = 20000L, yearMonth = 202610,
                epochDay = epochOct, categoryId = "exp_housing_rent", contextTag = "CASA",
                merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        assertEquals(ComparisonBaseline.PREVIOUS_MONTH, viewModel.uiState.value.comparisonBaseline)
        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("Mês Anterior", comparison!!.baselineLabel)

        // Expense: 1000 € vs 850 € -> +150,00 € (+17,6%)
        assertEquals("1.000,00 €", comparison.currentExpense)
        assertEquals("850,00 €", comparison.baselineExpense)
        assertEquals("+150,00 €", comparison.expenseDelta)
        assertEquals("17,6%", comparison.expenseDeltaPercent)
        assertTrue(comparison.expenseDeltaPositive)

        // Income: 2000 € vs 1800 € -> +200,00 € (+11,1%)
        assertEquals("+200,00 €", comparison.incomeDelta)
        assertEquals("11,1%", comparison.incomeDeltaPercent)
        assertTrue(comparison.incomeDeltaPositive)

        // Savings: 1000 € vs 950 € -> +50,00 € (+5,3%)
        assertEquals("+50,00 €", comparison.savingsDelta)
        assertEquals("5,3%", comparison.savingsDeltaPercent)
        assertTrue(comparison.savingsDeltaPositive)

        // Ranking: Food rolls up subs (+200 €), Housing drops (-50 €)
        assertEquals(1, comparison.increases.size)
        val increase = comparison.increases[0]
        assertEquals("exp_food", increase.categoryId)
        assertEquals("Alimentação", increase.categoryName)
        assertEquals("800,00 €", increase.current)
        assertEquals("600,00 €", increase.baseline)
        assertEquals("+200,00 €", increase.delta)
        assertEquals("33,3%", increase.deltaPercent)

        assertEquals(1, comparison.decreases.size)
        val decrease = comparison.decreases[0]
        assertEquals("exp_housing", decrease.categoryId)
        assertEquals("200,00 €", decrease.current)
        assertEquals("250,00 €", decrease.baseline)
        assertEquals("-50,00 €", decrease.delta)
        assertEquals("-20,0%", decrease.deltaPercent)
    }

    @Test
    fun test6_comparar_switchBaselines_andNullWhenNoBaselineData() = runTest {
        val epochOct2026 = LocalDate.of(2026, 10, 10).toEpochDay()
        val epochOct2025 = LocalDate.of(2025, 10, 10).toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                epochDay = epochOct2026, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 80000L, yearMonth = 202610,
                epochDay = epochOct2026, categoryId = "exp_food_supermarket", contextTag = "CASA",
                merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "INCOME", amountCents = 180000L, yearMonth = 202510,
                epochDay = epochOct2025, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 4, type = "EXPENSE", amountCents = 60000L, yearMonth = 202510,
                epochDay = epochOct2025, categoryId = "exp_food_supermarket", contextTag = "CASA",
                merchant = "Continente", paymentMethod = "CARD", isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        // PREVIOUS_MONTH (default): September 2026 has no data -> comparison null
        assertEquals(ComparisonBaseline.PREVIOUS_MONTH, viewModel.uiState.value.comparisonBaseline)
        assertNull(viewModel.uiState.value.comparison)

        // SAME_MONTH_LAST_YEAR: October 2025 has data -> non-null with deltas
        viewModel.setComparisonBaseline(ComparisonBaseline.SAME_MONTH_LAST_YEAR)
        assertEquals(ComparisonBaseline.SAME_MONTH_LAST_YEAR, viewModel.uiState.value.comparisonBaseline)
        val yearComparison = viewModel.uiState.value.comparison
        assertNotNull(yearComparison)
        assertEquals("Ano Anterior", yearComparison!!.baselineLabel)
        assertEquals("600,00 €", yearComparison.baselineExpense)
        assertEquals("+200,00 €", yearComparison.expenseDelta)

        // MONTHLY_AVERAGE: active months exist -> non-null with averaged baseline
        viewModel.setComparisonBaseline(ComparisonBaseline.MONTHLY_AVERAGE)
        assertEquals(ComparisonBaseline.MONTHLY_AVERAGE, viewModel.uiState.value.comparisonBaseline)
        val averageComparison = viewModel.uiState.value.comparison
        assertNotNull(averageComparison)
        assertEquals("Média Mensal", averageComparison!!.baselineLabel)
        // Only October 2026 is active in the year: average expense = 800 € -> delta 0
        assertEquals("800,00 €", averageComparison.baselineExpense)
        assertEquals("0,00 €", averageComparison.expenseDelta)
        assertEquals("0,0%", averageComparison.expenseDeltaPercent)
    }

    @Test
    fun test7_evolucao_categoryTrend_zeroFilledSeriesAndChipSelection() = runTest {
        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "EXPENSE", amountCents = 100000L, yearMonth = 202601,
                epochDay = LocalDate.of(2026, 1, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 200000L, yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 50000L, yearMonth = 202603,
                epochDay = LocalDate.of(2026, 3, 10).toEpochDay(), categoryId = "exp_transport_fuel",
                contextTag = "MOBILIDADE", merchant = "Galp", paymentMethod = "CARD", isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202602
        )

        // Chips: top categories of the year, first selected by default
        val chips = viewModel.uiState.value.trendChips
        assertEquals(2, chips.size)
        assertEquals("exp_food", chips[0].categoryId)
        assertTrue(chips[0].selected)
        assertEquals("exp_transport", chips[1].categoryId)
        assertFalse(chips[1].selected)

        // Food trend: 12 zero-filled points, non-diluted average (300.000 / 2 = 150.000)
        val foodTrend = viewModel.uiState.value.categoryTrend
        assertNotNull(foodTrend)
        assertEquals("Alimentação", foodTrend!!.categoryName)
        assertEquals(12, foodTrend.points.size)
        assertEquals(100000.0, foodTrend.points[0].value, 0.01)
        assertEquals("1.000,00 €", foodTrend.points[0].formattedValue)
        assertEquals(200000.0, foodTrend.points[1].value, 0.01)
        assertEquals(0.0, foodTrend.points[11].value, 0.01)
        assertEquals(150000L, foodTrend.averageExpenseCents)
        assertEquals("1.500,00 €", foodTrend.formattedAverage)
        assertEquals("2.000,00 €", foodTrend.selectedMonthDetail)
        assertEquals(33.33, foodTrend.selectedMonthDeviation!!, 0.05)

        // Selecting the transport chip updates the series
        viewModel.selectEvolutionCategory("exp_transport")
        val updatedChips = viewModel.uiState.value.trendChips
        assertFalse(updatedChips[0].selected)
        assertTrue(updatedChips[1].selected)

        val transportTrend = viewModel.uiState.value.categoryTrend
        assertNotNull(transportTrend)
        assertEquals("Transportes", transportTrend!!.categoryName)
        assertEquals(12, transportTrend.points.size)
        assertEquals(50000.0, transportTrend.points[2].value, 0.01)
        assertEquals(0.0, transportTrend.points[0].value, 0.01)
        assertEquals(50000L, transportTrend.averageExpenseCents)
        assertEquals(50000L, transportTrend.points.sumOf { it.value.toLong() })
    }

    @Test
    fun test8_anos_yearComparisonAndTotals() = runTest {
        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 300000L, yearMonth = 202501,
                epochDay = LocalDate.of(2025, 1, 10).toEpochDay(), categoryId = "inc_base_salary",
                contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 50000L, yearMonth = 202501,
                epochDay = LocalDate.of(2025, 1, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 30000L, yearMonth = 202503,
                epochDay = LocalDate.of(2025, 3, 10).toEpochDay(), categoryId = "exp_housing_rent",
                contextTag = "CASA", merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 4, type = "INCOME", amountCents = 240000L, yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "inc_base_salary",
                contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 5, type = "EXPENSE", amountCents = 60000L, yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "exp_housing_rent",
                contextTag = "CASA", merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 6, type = "EXPENSE", amountCents = 20000L, yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 7, type = "EXPENSE", amountCents = 10000L, yearMonth = 202605,
                epochDay = LocalDate.of(2026, 5, 10).toEpochDay(), categoryId = "exp_transport_fuel",
                contextTag = "MOBILIDADE", merchant = "Galp", paymentMethod = "CARD", isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202602
        )

        val yearComparison = viewModel.uiState.value.yearComparison
        assertEquals(2, yearComparison.size)

        val firstYear = yearComparison[0]
        assertEquals(2025, firstYear.year)
        assertEquals(80000L, firstYear.totalExpenseCents)
        assertEquals(12, firstYear.monthlyExpenseCents.size)
        assertEquals(50000L, firstYear.monthlyExpenseCents[0])
        assertEquals(0L, firstYear.monthlyExpenseCents[1])
        assertEquals(30000L, firstYear.monthlyExpenseCents[2])

        val secondYear = yearComparison[1]
        assertEquals(2026, secondYear.year)
        assertEquals(90000L, secondYear.totalExpenseCents)
        assertEquals(80000L, secondYear.monthlyExpenseCents[1])
        assertEquals(10000L, secondYear.monthlyExpenseCents[4])

        val yearTotals = viewModel.uiState.value.yearTotals
        assertEquals(2, yearTotals.size)

        val totals2025 = yearTotals[0]
        assertEquals(2025, totals2025.year)
        assertEquals("3.000,00 €", totals2025.formattedIncome)
        assertEquals("800,00 €", totals2025.formattedExpense)
        assertEquals("2.200,00 €", totals2025.formattedSavings)
        assertEquals("73,3%", totals2025.formattedRate)

        val totals2026 = yearTotals[1]
        assertEquals(2026, totals2026.year)
        assertEquals("2.400,00 €", totals2026.formattedIncome)
        assertEquals("900,00 €", totals2026.formattedExpense)
        assertEquals("1.500,00 €", totals2026.formattedSavings)
        assertEquals("62,5%", totals2026.formattedRate)
    }

    @Test
    fun test9_noQue_unknownCategoryFallsBackToGrayWithIdAsName() = runTest {
        val yearMonth = 202610
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = yearMonth,
                    epochDay = LocalDate.of(2026, 10, 12).toEpochDay(), categoryId = "cat_custom_unknown",
                    contextTag = "ONLINE", merchant = "Loja Estrangeira", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "INCOME", amountCents = 200000L, yearMonth = yearMonth,
                    epochDay = LocalDate.of(2026, 10, 1).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val fallback = viewModel.uiState.value.parentCategories.find { it.id == "cat_custom_unknown" }
        assertNotNull(fallback)
        assertEquals("cat_custom_unknown", fallback!!.name)
        assertEquals(0xFF888888, fallback.colorHex)
        assertEquals(5000L, fallback.amountCents)
        assertEquals("category", fallback.icon)
        assertEquals(2.5, fallback.percentageOfIncome, 0.001)
    }

    @Test
    fun test10_comparar_monthlyAverageBaseline_exactMath() = runTest {
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 300000L, yearMonth = 202601,
                    epochDay = LocalDate.of(2026, 1, 5).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 100000L, yearMonth = 202601,
                    epochDay = LocalDate.of(2026, 1, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "INCOME", amountCents = 300000L, yearMonth = 202602,
                    epochDay = LocalDate.of(2026, 2, 5).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 200000L, yearMonth = 202602,
                    epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 5, type = "INCOME", amountCents = 300000L, yearMonth = 202603,
                    epochDay = LocalDate.of(2026, 3, 5).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 6, type = "EXPENSE", amountCents = 300000L, yearMonth = 202603,
                    epochDay = LocalDate.of(2026, 3, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202603
        )

        viewModel.setComparisonBaseline(ComparisonBaseline.MONTHLY_AVERAGE)

        assertEquals(ComparisonBaseline.MONTHLY_AVERAGE, viewModel.uiState.value.comparisonBaseline)
        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("Média Mensal", comparison!!.baselineLabel)

        assertEquals("3.000,00 €", comparison.currentExpense)
        assertEquals("2.000,00 €", comparison.baselineExpense)
        assertEquals("+1.000,00 €", comparison.expenseDelta)
        assertEquals("50,0%", comparison.expenseDeltaPercent)
        assertTrue(comparison.expenseDeltaPositive)

        assertEquals("3.000,00 €", comparison.currentIncome)
        assertEquals("3.000,00 €", comparison.baselineIncome)
        assertEquals("0,00 €", comparison.incomeDelta)
        assertEquals("0,0%", comparison.incomeDeltaPercent)

        assertEquals("0,00 €", comparison.currentSavings)
        assertEquals("1.000,00 €", comparison.baselineSavings)
        assertEquals("-1.000,00 €", comparison.savingsDelta)
        assertEquals("-100,0%", comparison.savingsDeltaPercent)

        assertEquals(1, comparison.increases.size)
        val increase = comparison.increases[0]
        assertEquals("exp_food", increase.categoryId)
        assertEquals("3.000,00 €", increase.current)
        assertEquals("2.000,00 €", increase.baseline)
        assertEquals("+1.000,00 €", increase.delta)
        assertEquals("50,0%", increase.deltaPercent)
        assertTrue(comparison.decreases.isEmpty())
    }

    @Test
    fun test11_evolucao_savingsRateTrend_twelvePointsAndAverageOverActiveMonths() = runTest {
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 300000L, yearMonth = 202601,
                    epochDay = LocalDate.of(2026, 1, 5).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 100000L, yearMonth = 202601,
                    epochDay = LocalDate.of(2026, 1, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "INCOME", amountCents = 300000L, yearMonth = 202602,
                    epochDay = LocalDate.of(2026, 2, 5).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 200000L, yearMonth = 202602,
                    epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 5, type = "INCOME", amountCents = 300000L, yearMonth = 202603,
                    epochDay = LocalDate.of(2026, 3, 5).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 6, type = "EXPENSE", amountCents = 300000L, yearMonth = 202603,
                    epochDay = LocalDate.of(2026, 3, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202603
        )

        val trend = viewModel.uiState.value.savingsRateTrend
        assertNotNull(trend)
        assertEquals(12, trend!!.points.size)
        assertEquals(66.66, trend.points[0].value, 0.01)
        assertEquals("66,7%", trend.points[0].formattedValue)
        assertEquals("33,3%", trend.points[1].formattedValue)
        assertEquals("0,0%", trend.points[2].formattedValue)
        assertEquals("0,0%", trend.points[11].formattedValue)
        assertEquals(0.0, trend.points[11].value, 0.001)
        assertEquals(33.33, trend.averageRatePercent, 0.01)
        assertEquals("33,3%", trend.formattedAverage)
    }

    @Test
    fun test12_evolucao_trendChipSelectionIsStableAcrossMonthChanges() = runTest {
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 100000L, yearMonth = 202601,
                    epochDay = LocalDate.of(2026, 1, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 20000L, yearMonth = 202601,
                    epochDay = LocalDate.of(2026, 1, 12).toEpochDay(), categoryId = "exp_transport_fuel",
                    contextTag = "COMERCIO", merchant = "Galp", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "EXPENSE", amountCents = 50000L, yearMonth = 202602,
                    epochDay = LocalDate.of(2026, 2, 10).toEpochDay(), categoryId = "exp_food_supermarket",
                    contextTag = "CASA", merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 80000L, yearMonth = 202602,
                    epochDay = LocalDate.of(2026, 2, 12).toEpochDay(), categoryId = "exp_transport_fuel",
                    contextTag = "COMERCIO", merchant = "Galp", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202601
        )

        val chips = viewModel.uiState.value.trendChips
        assertEquals(listOf("exp_food", "exp_transport"), chips.map { it.categoryId })
        assertTrue(chips[0].selected)
        assertFalse(chips[1].selected)

        viewModel.setYearMonth(202602)
        val chipsAfterMonthChange = viewModel.uiState.value.trendChips
        assertEquals(listOf("exp_food", "exp_transport"), chipsAfterMonthChange.map { it.categoryId })
        assertTrue(chipsAfterMonthChange[0].selected)
        assertFalse(chipsAfterMonthChange[1].selected)

        viewModel.selectEvolutionCategory("exp_transport")
        val chipsAfterSelection = viewModel.uiState.value.trendChips
        assertFalse(chipsAfterSelection[0].selected)
        assertTrue(chipsAfterSelection[1].selected)

        viewModel.setYearMonth(202603)
        val chipsAfterAnotherChange = viewModel.uiState.value.trendChips
        assertEquals(listOf("exp_food", "exp_transport"), chipsAfterAnotherChange.map { it.categoryId })
        assertFalse(chipsAfterAnotherChange[0].selected)
        assertTrue(chipsAfterAnotherChange[1].selected)
    }

    @Test
    fun test13_setDisplayMode_sameModeIsNoOp() = runTest {
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                    epochDay = LocalDate.of(2026, 10, 1).toEpochDay(), categoryId = "inc_base_salary",
                    contextTag = "TRABALHO", merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        assertEquals(DisplayMode.EURO, viewModel.uiState.value.displayMode)
        val stateBefore = viewModel.uiState.value

        viewModel.setDisplayMode(DisplayMode.EURO)

        assertSame("Chamada com o mesmo modo não pode emitir novo estado", stateBefore, viewModel.uiState.value)

        viewModel.setDisplayMode(DisplayMode.INCOME_PERCENTAGE)
        assertEquals(DisplayMode.INCOME_PERCENTAGE, viewModel.uiState.value.displayMode)
        assertTrue(stateBefore !== viewModel.uiState.value)
    }

    @Test
    fun test14_monthNavigation_acrossYearBoundary_resetsSelection() = runTest {
        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202601
        )

        assertEquals(202601, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Janeiro 2026", viewModel.uiState.value.monthTitle)
        assertEquals(202601, viewModel.uiState.value.selectedEvolutionMonth)

        viewModel.selectPreviousMonth()
        assertEquals(202512, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Dezembro 2025", viewModel.uiState.value.monthTitle)
        assertEquals(202512, viewModel.uiState.value.selectedEvolutionMonth)
        assertNull(viewModel.uiState.value.selectedParentCategoryId)

        viewModel.selectNextMonth()
        assertEquals(202601, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Janeiro 2026", viewModel.uiState.value.monthTitle)
        assertEquals(202601, viewModel.uiState.value.selectedEvolutionMonth)
    }

    @Test
    fun test15_calendario_calendarDaysAggregateSpendingPerDayOfSelectedMonth() = runTest {
        val yearMonth = 202610
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()
        val sep20 = LocalDate.of(2026, 9, 20).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 2500L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
                    merchant = "Cantina", paymentMethod = "MBWAY", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "INCOME", amountCents = 200000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 99000L, yearMonth = 202609,
                    epochDay = sep20, categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            todayEpochDayProvider = { oct05 },
            initialYearMonth = yearMonth
        )

        val days = viewModel.uiState.value.calendarDays
        assertEquals(31, days.size)
        assertEquals((1..31).toList(), days.map { it.dayOfMonth })

        val day5 = days[4]
        assertEquals(5, day5.dayOfMonth)
        assertEquals(oct05, day5.epochDay)
        assertEquals(7500L, day5.totalExpenseCents)
        assertEquals("75,00 €", day5.formattedTotal)
        assertEquals(2, day5.transactionCount)
        assertTrue(day5.isToday)

        val day1 = days.first()
        assertEquals(0L, day1.totalExpenseCents)
        assertEquals(0, day1.transactionCount)
        assertFalse(day1.isToday)

        // Transactions of other months are not part of the calendar
        assertEquals(7500L, days.sumOf { it.totalExpenseCents })

        // Switching month recomputes the calendar
        viewModel.setYearMonth(202609)
        val septemberDays = viewModel.uiState.value.calendarDays
        assertEquals(30, septemberDays.size)
        assertEquals(99000L, septemberDays[19].totalExpenseCents)
        assertEquals(1, septemberDays[19].transactionCount)
    }

    @Test
    fun test16_calendario_selectDayReturnsThatDaysTransactions() = runTest {
        val yearMonth = 202610
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()
        val oct06 = LocalDate.of(2026, 10, 6).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 2500L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
                    merchant = "Cantina", paymentMethod = "MBWAY", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "INCOME", amountCents = 200000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 12000L, yearMonth = yearMonth,
                    epochDay = oct06, categoryId = "exp_transport_fuel", contextTag = "MOBILIDADE",
                    merchant = "Galp", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            todayEpochDayProvider = { oct05 },
            initialYearMonth = yearMonth
        )

        assertNull(viewModel.uiState.value.selectedCalendarDay)

        viewModel.selectCalendarDay(oct05)

        val selection = viewModel.uiState.value.selectedCalendarDay
        assertNotNull(selection)
        assertEquals(oct05, selection!!.epochDay)
        assertEquals("5 de outubro", selection.label)
        assertEquals(7500L, selection.totalExpenseCents)
        assertEquals("75,00 €", selection.formattedTotal)
        assertEquals(3, selection.transactions.size)
        assertEquals(
            listOf("Empresa", "Cantina", "Continente"),
            selection.transactions.map { it.merchant }
        )

        // A day without movements selects an empty selection (empty state in the UI)
        viewModel.selectCalendarDay(LocalDate.of(2026, 10, 20).toEpochDay())
        val emptySelection = viewModel.uiState.value.selectedCalendarDay
        assertNotNull(emptySelection)
        assertTrue(emptySelection!!.transactions.isEmpty())
        assertEquals(0L, emptySelection.totalExpenseCents)
    }

    @Test
    fun test17_calendario_selectionIsClearedOnMonthChange() = runTest {
        val yearMonth = 202610
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            todayEpochDayProvider = { oct05 },
            initialYearMonth = yearMonth
        )

        viewModel.selectCalendarDay(oct05)
        assertNotNull(viewModel.uiState.value.selectedCalendarDay)

        viewModel.setYearMonth(202609)
        assertNull(viewModel.uiState.value.selectedCalendarDay)

        viewModel.setYearMonth(202610)
        assertNull(viewModel.uiState.value.selectedCalendarDay)
    }

    @Test
    fun test18_calendario_monthlyReportIsGeneratedForTheSelectedMonth() = runTest {
        val yearMonth = 202610
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 200000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 50000L, yearMonth = yearMonth,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val report = viewModel.uiState.value.monthlyReport
        assertTrue(report.contains("Relatório Financeiro — Outubro 2026"))
        assertTrue(report.contains("Total recebido: 2.000,00 €"))
        assertTrue(report.contains("Total gasto: 500,00 €"))
        assertTrue(report.contains("Poupança líquida: 1.500,00 €"))
        assertTrue(report.contains("Taxa de poupança: 75,0%"))
        assertTrue(report.contains("Top categorias:"))
        assertFalse(report.contains("Orçamentos:"))
        assertFalse(report.contains("Metas:"))
    }

    @Test
    fun test19_shareableReportIsBuiltAtClickTimeForTheCurrentlySelectedMonth() = runTest {
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()
        val nov05 = LocalDate.of(2026, 11, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 50000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        val octoberReport = viewModel.buildMonthlyReport()
        assertTrue(octoberReport.contains("Relatório Financeiro — Outubro 2026"))
        assertTrue(octoberReport.contains("Total recebido: 2.000,00 €"))
        assertTrue(octoberReport.contains("Total gasto: 500,00 €"))

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 3, type = "INCOME", amountCents = 300000L, yearMonth = 202611,
                    epochDay = nov05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 100000L, yearMonth = 202611,
                    epochDay = nov05, categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        viewModel.setYearMonth(202611)

        // Click-time build: the month label always follows the current selection
        val reportRightAfterSwitch = viewModel.buildMonthlyReport()
        assertTrue(reportRightAfterSwitch.contains("Relatório Financeiro — Novembro 2026"))
        assertFalse(reportRightAfterSwitch.contains("Outubro"))

        advanceUntilIdle()

        val novemberReport = viewModel.buildMonthlyReport()
        assertTrue(novemberReport.contains("Relatório Financeiro — Novembro 2026"))
        assertTrue(novemberReport.contains("Total recebido: 3.000,00 €"))
        assertTrue(novemberReport.contains("Total gasto: 1.000,00 €"))
        assertTrue(novemberReport.contains("Poupança líquida: 2.000,00 €"))
        assertFalse(novemberReport.contains("Outubro"))
        assertFalse(novemberReport.contains("500,00 €"))
        assertFalse(novemberReport.contains("Continente"))
    }

    @Test
    fun test20_buildMonthlyReport_alwaysMatchesTheCurrentlySelectedMonth() = runTest {
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()
        val sep05 = LocalDate.of(2026, 9, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 50000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "INCOME", amountCents = 300000L, yearMonth = 202609,
                    epochDay = sep05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        viewModel.setYearMonth(202609)
        assertTrue(viewModel.buildMonthlyReport().contains("Relatório Financeiro — Setembro 2026"))

        viewModel.setYearMonth(202610)
        val octoberReport = viewModel.buildMonthlyReport()
        assertTrue(octoberReport.contains("Relatório Financeiro — Outubro 2026"))
        assertFalse(octoberReport.contains("Setembro"))

        viewModel.setYearMonth(202609)
        val septemberReport = viewModel.buildMonthlyReport()
        assertTrue(septemberReport.contains("Relatório Financeiro — Setembro 2026"))
        assertFalse(septemberReport.contains("Outubro"))
    }

    @Test
    fun test21_calendarDays_followTheSelectedMonthAcrossSeveralChanges() = runTest {
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            todayEpochDayProvider = { oct05 },
            initialYearMonth = 202610
        )

        assertEquals(31, viewModel.uiState.value.calendarDays.size)
        assertEquals(5000L, viewModel.uiState.value.calendarDays[4].totalExpenseCents)

        viewModel.setYearMonth(202609)
        assertEquals(30, viewModel.uiState.value.calendarDays.size)
        assertEquals(0L, viewModel.uiState.value.calendarDays.sumOf { it.totalExpenseCents })

        viewModel.setYearMonth(202602)
        assertEquals(28, viewModel.uiState.value.calendarDays.size)

        viewModel.setYearMonth(202610)
        assertEquals(31, viewModel.uiState.value.calendarDays.size)
        assertEquals(5000L, viewModel.uiState.value.calendarDays[4].totalExpenseCents)
    }

    @Test
    fun test22_selectCalendarDay_withADayOfAnotherMonthIsIgnored() = runTest {
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()
        val nov05 = LocalDate.of(2026, 11, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            todayEpochDayProvider = { oct05 },
            initialYearMonth = 202610
        )

        viewModel.selectCalendarDay(nov05)

        assertNull(viewModel.uiState.value.selectedCalendarDay)

        viewModel.selectCalendarDay(oct05)
        assertNotNull(viewModel.uiState.value.selectedCalendarDay)
    }

    @Test
    fun test23_selectCalendarDay_replacesThePreviousSelection() = runTest {
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()
        val oct06 = LocalDate.of(2026, 10, 6).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 12000L, yearMonth = 202610,
                    epochDay = oct06, categoryId = "exp_transport_fuel", contextTag = "COMERCIO",
                    merchant = "Galp", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            todayEpochDayProvider = { oct05 },
            initialYearMonth = 202610
        )

        viewModel.selectCalendarDay(oct05)
        assertEquals(oct05, viewModel.uiState.value.selectedCalendarDay?.epochDay)

        viewModel.selectCalendarDay(oct06)
        val selection = viewModel.uiState.value.selectedCalendarDay
        assertNotNull(selection)
        assertEquals(oct06, selection!!.epochDay)
        assertEquals(12000L, selection.totalExpenseCents)
        assertEquals(1, selection.transactions.size)
    }

    @Test
    fun test24_monthlyReport_includesBudgetsAndGoalsBlocksWhenDaosAreProvided() = runTest {
        val oct05 = LocalDate.of(2026, 10, 5).toEpochDay()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Empresa", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 50000L, yearMonth = 202610,
                    epochDay = oct05, categoryId = "exp_food_supermarket", contextTag = "CASA",
                    merchant = "Continente", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = FakeBudgetDao(
                listOf(
                    BudgetEntity(
                        id = "budget_exp_food",
                        categoryId = "exp_food",
                        limitCents = 50000L,
                        isActive = true
                    )
                )
            ),
            goalDao = FakeGoalDao(
                listOf(
                    GoalEntity(
                        id = "goal_reserva",
                        name = "Reserva",
                        targetCents = 100000L,
                        startYearMonth = 202601
                    )
                )
            ),
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        val report = viewModel.buildMonthlyReport()

        assertTrue(report.contains("Orçamentos:"))
        assertTrue(report.contains("- Alimentação: 500,00 € de 500,00 € (100,0%) — Atenção"))
        assertTrue(report.contains("Metas:"))
        assertTrue(report.contains("- Reserva: 1.500,00 € de 1.000,00 € (150,0%)"))
    }

    @Test
    fun test25_monthlyReport_forAMonthWithoutTransactions_isZeroed() = runTest {
        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        val report = viewModel.buildMonthlyReport()

        assertTrue(report.contains("Relatório Financeiro — Outubro 2026"))
        assertTrue(report.contains("Total recebido: 0,00 €"))
        assertTrue(report.contains("Total gasto: 0,00 €"))
        assertTrue(report.contains("Top categorias:\nSem dados."))
    }

    @Test
    fun test26_subscriptionReport_isCalculatedAndExposedInUiState() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 1599L,
                yearMonth = 202609,
                epochDay = epochDay - 30,
                categoryId = "exp_entertainment",
                contextTag = "CASA",
                merchant = "Netflix",
                paymentMethod = "DEBITO_DIRETO",
                isFixed = false
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 1599L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_entertainment",
                contextTag = "CASA",
                merchant = "Netflix",
                paymentMethod = "DEBITO_DIRETO",
                isFixed = false
            ),
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 3490L,
                yearMonth = 202609,
                epochDay = epochDay - 30,
                categoryId = "exp_utilities",
                contextTag = "CASA",
                merchant = "Vodafone",
                paymentMethod = "DEBITO_DIRETO",
                isFixed = true
            ),
            TransactionEntity(
                id = 4,
                type = "EXPENSE",
                amountCents = 3690L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_utilities",
                contextTag = "CASA",
                merchant = "Vodafone",
                paymentMethod = "DEBITO_DIRETO",
                isFixed = true
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val subReport = viewModel.uiState.value.subscriptionReport
        assertEquals(2, subReport.subscriptions.size)
        assertEquals(1, subReport.hikesCount)
        assertEquals(200L, subReport.totalHikeIncreaseCents)
        assertEquals(1599L + 3690L, subReport.totalMonthlyCents)
        assertEquals((1599L + 3690L) * 12L, subReport.totalAnnualCents)

        val vodafone = subReport.subscriptions.find { it.merchant == "Vodafone" }
        assertNotNull(vodafone)
        assertTrue(vodafone!!.isPriceHike)
        assertEquals(200L, vodafone.priceDifferenceCents)
    }

    @Test
    fun test27_annualAnalysis_calculatesTotalsAndMonthsForSelectedYear() = runTest {
        val epoch2025 = LocalDate.of(2025, 5, 10).toEpochDay()
        val epoch2026 = LocalDate.of(2026, 10, 24).toEpochDay()

        val transactions = listOf(
            // 2025 Transaction
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 150000L, // 1500 €
                yearMonth = 202505,
                epochDay = epoch2025,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 40000L, // 400 €
                yearMonth = 202505,
                epochDay = epoch2025,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Pingo Doce",
                paymentMethod = "MBWAY",
                isFixed = false
            ),
            // 2026 Transactions
            TransactionEntity(
                id = 3,
                type = "INCOME",
                amountCents = 200000L, // 2000 €
                yearMonth = 202610,
                epochDay = epoch2026,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Empresa",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 4,
                type = "EXPENSE",
                amountCents = 30000L, // 300 €
                yearMonth = 202610,
                epochDay = epoch2026,
                categoryId = "exp_shopping_gifts",
                contextTag = "LAZER",
                merchant = "Casamento Diana",
                paymentMethod = "TRANSFER",
                isFixed = false
            ),
            TransactionEntity(
                id = 5,
                type = "EXPENSE",
                amountCents = 6500L, // 65 €
                yearMonth = 202610,
                epochDay = epoch2026,
                categoryId = "exp_transport_fuel",
                contextTag = "CARRO",
                merchant = "Abastecimento Repsol",
                paymentMethod = "CARTAO_DEBITO",
                isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        // Initial year is 2026
        val annual2026 = viewModel.uiState.value.annualAnalysis
        assertEquals(2026, annual2026.selectedYear)
        assertEquals(200000L, annual2026.totalIncomeCents)
        assertEquals(36500L, annual2026.totalExpenseCents)
        assertEquals(163500L, annual2026.netSavingsCents)

        // Verify categories drill-down in 2026
        val shoppingCat = annual2026.categories.find { it.id == "exp_shopping" }
        assertNotNull(shoppingCat)
        assertEquals(30000L, shoppingCat!!.amountCents)
        val giftsSub = shoppingCat.subcategories.find { it.id == "exp_shopping_gifts" }
        assertNotNull(giftsSub)
        assertEquals(1, giftsSub!!.transactions.size)
        assertEquals("Casamento Diana", giftsSub.transactions.first().merchant)

        // Switch to 2025
        viewModel.selectAnnualYear(2025)
        val annual2025 = viewModel.uiState.value.annualAnalysis
        assertEquals(2025, annual2025.selectedYear)
        assertEquals(150000L, annual2025.totalIncomeCents)
        assertEquals(40000L, annual2025.totalExpenseCents)
        assertEquals(110000L, annual2025.netSavingsCents)

        val foodCat2025 = annual2025.categories.find { it.id == "exp_food" }
        assertNotNull(foodCat2025)
        assertEquals(40000L, foodCat2025!!.amountCents)
        assertEquals("Pingo Doce", foodCat2025.transactions.first().merchant)
    }

    @Test
    fun test28_discriminatedTransactions_inSubcategoriesAreOrderedByDateDesc() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 24).toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 10,
                type = "EXPENSE",
                amountCents = 3000L,
                yearMonth = yearMonth,
                epochDay = epochDay - 5,
                categoryId = "exp_transport_fuel",
                contextTag = "CARRO",
                merchant = "Abastecimento 1",
                paymentMethod = "CARTAO_DEBITO",
                isFixed = false
            ),
            TransactionEntity(
                id = 11,
                type = "EXPENSE",
                amountCents = 4000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_transport_fuel",
                contextTag = "CARRO",
                merchant = "Abastecimento 2",
                paymentMethod = "CARTAO_DEBITO",
                isFixed = false
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val parentTransport = viewModel.uiState.value.parentCategories.find { it.id == "exp_transport" }
        assertNotNull(parentTransport)
        val fuelSub = parentTransport!!.subcategories.find { it.id == "exp_transport_fuel" }
        assertNotNull(fuelSub)
        assertEquals(2, fuelSub!!.transactions.size)
        // Most recent first (epochDay DESC)
        assertEquals("Abastecimento 2", fuelSub.transactions[0].merchant)
        assertEquals("Abastecimento 1", fuelSub.transactions[1].merchant)
    }

    @Test
    fun test29_quarterlyHistory_computes3MonthComparisonAndAverage() = runTest {
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 100000L, // 1.000 € in August
                yearMonth = 202608,
                epochDay = LocalDate.of(2026, 8, 15).toEpochDay(),
                categoryId = "exp_food",
                contextTag = "CASA",
                merchant = "Supermercado",
                paymentMethod = "CARD"
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 120000L, // 1.200 € in September
                yearMonth = 202609,
                epochDay = LocalDate.of(2026, 9, 15).toEpochDay(),
                categoryId = "exp_food",
                contextTag = "CASA",
                merchant = "Supermercado",
                paymentMethod = "CARD"
            ),
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 140000L, // 1.400 € in October
                yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, 15).toEpochDay(),
                categoryId = "exp_food",
                contextTag = "CASA",
                merchant = "Supermercado",
                paymentMethod = "CARD"
            )
        )
        transactionDao.insertAll(transactions)

        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        val quarterly = viewModel.uiState.value.quarterlyHistory
        assertEquals(3, quarterly.items.size)

        assertEquals(202608, quarterly.items[0].yearMonth)
        assertEquals(100000L, quarterly.items[0].expenseCents)
        assertFalse(quarterly.items[0].isCurrent)

        assertEquals(202609, quarterly.items[1].yearMonth)
        assertEquals(120000L, quarterly.items[1].expenseCents)
        assertFalse(quarterly.items[1].isCurrent)

        assertEquals(202610, quarterly.items[2].yearMonth)
        assertEquals(140000L, quarterly.items[2].expenseCents)
        assertTrue(quarterly.items[2].isCurrent)

        // Average of 1000€, 1200€, 1400€ = 1200€ (120000L)
        assertEquals(120000L, quarterly.averageExpenseCents)
        assertEquals("1.200,00 €", quarterly.formattedAverageExpense)
    }

    @Test
    fun privacyModeTogglesMasksFormattedValuesInAnalytics() = runTest {
        val userProfileStore = FakeUserProfileStore(privacyMode = false)
        transactionDao.insert(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 150000L,
                yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, 5).toEpochDay(),
                categoryId = "exp_food",
                contextTag = "CASA",
                merchant = "Supermercado",
                paymentMethod = "CARD"
            )
        )
        val viewModel = AnalyticsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            userProfileStore = userProfileStore,
            initialYearMonth = 202610
        )

        assertEquals("1.500,00 €", viewModel.uiState.value.formattedTotalExpense)
        assertEquals("1.500,00 €", viewModel.uiState.value.annualAnalysis.formattedTotalExpense)

        userProfileStore.setPrivacyModeEnabled(true)
        advanceUntilIdle()

        assertEquals("•••• €", viewModel.uiState.value.formattedTotalExpense)
        assertEquals("•••• €", viewModel.uiState.value.annualAnalysis.formattedTotalExpense)

        userProfileStore.setPrivacyModeEnabled(false)
        advanceUntilIdle()

        assertEquals("1.500,00 €", viewModel.uiState.value.formattedTotalExpense)
        assertEquals("1.500,00 €", viewModel.uiState.value.annualAnalysis.formattedTotalExpense)
    }
}
