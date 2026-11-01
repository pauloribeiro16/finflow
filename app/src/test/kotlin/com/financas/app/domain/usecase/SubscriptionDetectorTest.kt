package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubscriptionDetectorTest {

    private lateinit var detector: SubscriptionDetector

    @Before
    fun setUp() {
        detector = SubscriptionDetector()
    }

    private fun createTx(
        id: Long = 0L,
        type: String = "EXPENSE",
        amountCents: Long,
        yearMonth: Int,
        merchant: String,
        categoryId: String = "exp_utilities",
        contextTag: String = "CASA",
        paymentMethod: String = "DEBITO_DIRETO",
        isFixed: Boolean = false
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            type = type,
            amountCents = amountCents,
            yearMonth = yearMonth,
            epochDay = 0L,
            categoryId = categoryId,
            contextTag = contextTag,
            merchant = merchant,
            paymentMethod = paymentMethod,
            isFixed = isFixed
        )
    }

    @Test
    fun testStableSubscription_detectedWithCorrectAmounts() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 1599L, yearMonth = 202609, merchant = "Netflix"),
            createTx(id = 2, amountCents = 1599L, yearMonth = 202610, merchant = "Netflix")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(1, report.subscriptions.size)
        val netflix = report.subscriptions.first()
        assertEquals("Netflix", netflix.merchant)
        assertEquals(1599L, netflix.monthlyAmountCents)
        assertEquals(19188L, netflix.annualizedCostCents)
        assertEquals(2, netflix.occurrenceCount)
        assertEquals(202610, netflix.lastYearMonth)
        assertEquals(1599L, netflix.previousAmountCents)
        assertEquals(0L, netflix.priceDifferenceCents)
        assertFalse(netflix.isPriceHike)
        assertEquals(0, report.hikesCount)
        assertEquals(0L, report.totalHikeIncreaseCents)
        assertEquals(1599L, report.totalMonthlyCents)
        assertEquals(19188L, report.totalAnnualCents)
    }

    @Test
    fun testPriceHike_detectedWithExactCentsDifferenceAndFlagTrue() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 3490L, yearMonth = 202609, merchant = "Vodafone"),
            createTx(id = 2, amountCents = 3690L, yearMonth = 202610, merchant = "Vodafone")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(1, report.subscriptions.size)
        val vodafone = report.subscriptions.first()
        assertEquals("Vodafone", vodafone.merchant)
        assertEquals(3690L, vodafone.monthlyAmountCents)
        assertEquals(3490L, vodafone.previousAmountCents)
        assertEquals(200L, vodafone.priceDifferenceCents)
        assertTrue(vodafone.isPriceHike)
        assertEquals(44280L, vodafone.annualizedCostCents)
        assertEquals(1, report.hikesCount)
        assertEquals(200L, report.totalHikeIncreaseCents)
        assertEquals(3690L, report.totalMonthlyCents)
        assertEquals(44280L, report.totalAnnualCents)
    }

    @Test
    fun testPriceConstantOrDecreasing_isPriceHikeIsFalse() {
        val transactions = listOf(
            // Spotify: constant 10,99 €
            createTx(id = 1, amountCents = 1099L, yearMonth = 202609, merchant = "Spotify"),
            createTx(id = 2, amountCents = 1099L, yearMonth = 202610, merchant = "Spotify"),
            // Ginásio: decreased from 35,00 € to 30,00 €
            createTx(id = 3, amountCents = 3500L, yearMonth = 202609, merchant = "Ginásio"),
            createTx(id = 4, amountCents = 3000L, yearMonth = 202610, merchant = "Ginásio")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(2, report.subscriptions.size)
        val spotify = report.subscriptions.find { it.merchant == "Spotify" }
        assertNotNull(spotify)
        assertEquals(0L, spotify!!.priceDifferenceCents)
        assertFalse(spotify.isPriceHike)

        val ginasio = report.subscriptions.find { it.merchant == "Ginásio" }
        assertNotNull(ginasio)
        assertEquals(3500L, ginasio!!.previousAmountCents)
        assertEquals(-500L, ginasio.priceDifferenceCents)
        assertFalse(ginasio.isPriceHike)

        assertEquals(0, report.hikesCount)
        assertEquals(0L, report.totalHikeIncreaseCents)
    }

    @Test
    fun testAnnualizedCost_exactCalculationInLong() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 8499L, yearMonth = 202609, merchant = "Plano Saúde"),
            createTx(id = 2, amountCents = 8499L, yearMonth = 202610, merchant = "Plano Saúde")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        val sub = report.subscriptions.first()
        assertEquals(8499L, sub.monthlyAmountCents)
        assertEquals(8499L * 12L, sub.annualizedCostCents)
        assertEquals(101988L, sub.annualizedCostCents)
        assertEquals(101988L, report.totalAnnualCents)
    }

    @Test
    fun testExclusionOfIncomeAndOneOffPurchases() {
        val transactions = listOf(
            // Income transaction must be completely ignored
            createTx(id = 1, type = "INCOME", amountCents = 250000L, yearMonth = 202610, merchant = "Salário Empresa"),
            // One-off purchase with 1 occurrence and isFixed = false
            createTx(id = 2, type = "EXPENSE", amountCents = 4999L, yearMonth = 202610, merchant = "Zara", isFixed = false),
            // Multiple one-off purchases in the SAME month with isFixed = false
            createTx(id = 3, type = "EXPENSE", amountCents = 2500L, yearMonth = 202610, merchant = "Continente", isFixed = false),
            createTx(id = 4, type = "EXPENSE", amountCents = 3500L, yearMonth = 202610, merchant = "Continente", isFixed = false),
            // Fixed expense with single occurrence -> MUST be included
            createTx(id = 5, type = "EXPENSE", amountCents = 80000L, yearMonth = 202610, merchant = "Senhorio Renda", isFixed = true)
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(1, report.subscriptions.size)
        val renda = report.subscriptions.first()
        assertEquals("Senhorio Renda", renda.merchant)
        assertEquals(80000L, renda.monthlyAmountCents)
        assertTrue(renda.isFixed)
        assertEquals(1, renda.occurrenceCount)
    }

    @Test
    fun testAggregatedReport_multipleSubscriptionsWithHikesAndStable() {
        val transactions = listOf(
            // Netflix: stable 15,99 €
            createTx(id = 1, amountCents = 1599L, yearMonth = 202609, merchant = "Netflix"),
            createTx(id = 2, amountCents = 1599L, yearMonth = 202610, merchant = "Netflix"),
            // Vodafone: hike from 34,90 € to 36,90 € (+2,00 €)
            createTx(id = 3, amountCents = 3490L, yearMonth = 202609, merchant = "Vodafone"),
            createTx(id = 4, amountCents = 3690L, yearMonth = 202610, merchant = "Vodafone"),
            // EDP: hike from 60,00 € to 65,00 € (+5,00 €)
            createTx(id = 5, amountCents = 6000L, yearMonth = 202609, merchant = "EDP"),
            createTx(id = 6, amountCents = 6500L, yearMonth = 202610, merchant = "EDP")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(3, report.subscriptions.size)
        // Subscriptions with price hike are sorted first
        assertTrue(report.subscriptions[0].isPriceHike)
        assertTrue(report.subscriptions[1].isPriceHike)
        assertFalse(report.subscriptions[2].isPriceHike)

        assertEquals(2, report.hikesCount)
        assertEquals(700L, report.totalHikeIncreaseCents) // 200L + 500L = 700L (+7,00 €)
        val expectedMonthly = 1599L + 3690L + 6500L // 11789L
        assertEquals(expectedMonthly, report.totalMonthlyCents)
        assertEquals(expectedMonthly * 12L, report.totalAnnualCents)
    }

    @Test
    fun testEmptyTransactionsReturnsEmptyReport() {
        val report = detector.detect(emptyList(), currentYearMonth = 202610)
        assertTrue(report.subscriptions.isEmpty())
        assertEquals(0L, report.totalMonthlyCents)
        assertEquals(0L, report.totalAnnualCents)
        assertEquals(0, report.hikesCount)
        assertEquals(0L, report.totalHikeIncreaseCents)
    }

    @Test
    fun testCanceledSubscriptionInPast_notDetectedAsActiveInCurrentMonth() {
        val transactions = listOf(
            // Disney+ was subscribed in 2024, but cancelled
            createTx(id = 1, amountCents = 899L, yearMonth = 202401, merchant = "Disney+"),
            createTx(id = 2, amountCents = 899L, yearMonth = 202402, merchant = "Disney+"),
            // Netflix is active currently
            createTx(id = 3, amountCents = 1599L, yearMonth = 202609, merchant = "Netflix"),
            createTx(id = 4, amountCents = 1599L, yearMonth = 202610, merchant = "Netflix")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(1, report.subscriptions.size)
        assertEquals("Netflix", report.subscriptions.first().merchant)
        assertNull(report.subscriptions.find { it.merchant == "Disney+" })
    }

    @Test
    fun testEmptyMerchantWithoutFixedFlag_notDetectedAsSubscription() {
        val transactions = listOf(
            // Random expenses with empty merchant in multiple months
            createTx(id = 1, amountCents = 1200L, yearMonth = 202609, merchant = "", categoryId = "sub_pharmacy", isFixed = false),
            createTx(id = 2, amountCents = 1500L, yearMonth = 202610, merchant = "", categoryId = "sub_pharmacy", isFixed = false)
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertTrue(report.subscriptions.isEmpty())
    }

    @Test
    fun testPendingCurrentMonthSubscription_doesNotTriggerPriceHike() {
        val transactions = listOf(
            // Billed in August (30 €) and September (35 €). In October (current), not billed yet
            createTx(id = 1, amountCents = 3000L, yearMonth = 202608, merchant = "Ginásio"),
            createTx(id = 2, amountCents = 3500L, yearMonth = 202609, merchant = "Ginásio")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertEquals(1, report.subscriptions.size)
        val ginasio = report.subscriptions.first()
        assertEquals("Ginásio", ginasio.merchant)
        assertEquals(3500L, ginasio.monthlyAmountCents)
        // Must NOT report hike in October because October was not billed yet
        assertFalse(ginasio.isPriceHike)
        assertEquals(0, report.hikesCount)
    }

    @Test
    fun testExclusionOfSaidasAndBares_notDetectedEvenAcrossMultipleMonths() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 4500L, yearMonth = 202609, merchant = "Saídas", categoryId = "exp_leisure_bars"),
            createTx(id = 2, amountCents = 6200L, yearMonth = 202610, merchant = "Saídas", categoryId = "exp_leisure_bars")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertTrue("Saídas must not be detected as subscription", report.subscriptions.isEmpty())
    }

    @Test
    fun testExclusionOfIucAutomovel_notDetectedEvenWithFixedFlag() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 15000L, yearMonth = 202610, merchant = "IUC Automóvel", categoryId = "exp_transport_maintenance", isFixed = true)
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertTrue("IUC must not be detected as monthly subscription", report.subscriptions.isEmpty())
    }

    @Test
    fun testExclusionOfPhdEnrollment_notDetected() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 27500L, yearMonth = 202509, merchant = "Inscrição PhD", categoryId = "exp_shopping_education"),
            createTx(id = 2, amountCents = 27500L, yearMonth = 202610, merchant = "Inscrição PhD", categoryId = "exp_shopping_education")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertTrue("Inscrição PhD must not be detected as monthly subscription", report.subscriptions.isEmpty())
    }

    @Test
    fun testExclusionOfSupermarketGroceries_notDetectedAcrossMonths() {
        val transactions = listOf(
            createTx(id = 1, amountCents = 8500L, yearMonth = 202609, merchant = "Continente", categoryId = "exp_food_supermarket"),
            createTx(id = 2, amountCents = 9200L, yearMonth = 202610, merchant = "Continente", categoryId = "exp_food_supermarket")
        )

        val report = detector.detect(transactions, currentYearMonth = 202610)

        assertTrue("Supermercado Continente must not be detected as subscription", report.subscriptions.isEmpty())
    }
}
