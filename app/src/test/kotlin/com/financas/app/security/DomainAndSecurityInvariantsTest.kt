package com.financas.app.security

import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.network.QuoteService
import com.financas.app.data.network.TickerPolicy
import com.financas.app.data.network.YahooQuote
import com.financas.app.data.repository.BackupData
import com.financas.app.data.repository.BackupSanitizer
import com.financas.app.data.repository.BackupSerializer
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.usecase.DeduplicationResult
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.domain.usecase.NetExpenseCalculator
import com.financas.app.domain.usecase.PortfolioMarketSyncUseCase
import com.financas.app.domain.usecase.TransactionDeduplicator
import com.financas.app.domain.usecase.XtbTickerMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DomainAndSecurityInvariantsTest {

    @Test
    fun backupSanitizer_dropsInvalidAndClampsNonFiniteUnits() {
        val raw = BackupData(
            version = 3,
            categories = listOf(
                CategoryEntity("cat_ok", null, "Alimentação", "restaurant", 0xFF10B981, "EXPENSE", false, 1),
                CategoryEntity("", null, "Inválida", "x", 0L, "EXPENSE", false, 2)
            ),
            transactions = listOf(
                TransactionEntity(1L, "EXPENSE", 1500L, 202611, 20760L, "cat_ok", "CASA", "Continente", "CARD", false),
                TransactionEntity(2L, "EXPENSE", Long.MIN_VALUE, 202611, 20760L, "cat_ok", "CASA", "Overflow", "CARD", false),
                TransactionEntity(3L, "bad type!", 1000L, 202611, 20760L, "cat_ok", "CASA", "BadType", "CARD", false)
            ),
            investments = listOf(
                InvestmentAssetEntity(
                    id = "inv_1",
                    name = "ETF",
                    type = "ETF",
                    institution = "XTB",
                    investedCents = 100_000L,
                    currentValueCents = 110_000L,
                    monthlyContributionCents = 0L,
                    targetCents = null,
                    notes = "",
                    lastUpdatedEpochDay = 20760L,
                    ticker = "../evil?x=1",
                    units = Double.POSITIVE_INFINITY
                )
            )
        )

        val sanitized = BackupSanitizer.sanitize(raw)
        assertEquals(3, sanitized.droppedRecords)
        assertTrue(sanitized.data.categories.all { it.id.isNotBlank() })
        assertEquals(1, sanitized.data.transactions.size)
        val inv = sanitized.data.investments.first()
        assertNull("Malicious ticker must be stripped", inv.ticker)
        assertNull("Non-finite units must be stripped", inv.units)
    }

    @Test
    fun accountingConservation_randomizedMonthlyLedgersAndLinkedRefunds() {
        val rng = Random(42)
        val calc = MonthlyAnalyticsCalculator()

        repeat(60) { trial ->
            val txs = mutableListOf<TransactionEntity>()
            var nextId = 1L
            val expenseIds = mutableListOf<Long>()

            repeat(rng.nextInt(1, 20)) {
                val id = nextId++
                val isIncome = rng.nextBoolean()
                val amount = rng.nextLong(1L, 500_000L)
                txs.add(
                    TransactionEntity(
                        id = id,
                        type = if (isIncome) "INCOME" else "EXPENSE",
                        amountCents = amount,
                        yearMonth = 202611,
                        epochDay = 20760L + rng.nextInt(0, 28),
                        categoryId = if (isIncome) "inc_base_salary" else "sub_groceries",
                        contextTag = "CASA",
                        merchant = "M$id",
                        paymentMethod = "CARD",
                        isFixed = rng.nextBoolean()
                    )
                )
                if (!isIncome) expenseIds.add(id)
            }

            for (expId in expenseIds.take(3)) {
                val refundAmount = rng.nextLong(1L, 800_000L)
                txs.add(
                    TransactionEntity(
                        id = nextId++,
                        type = "INCOME",
                        amountCents = refundAmount,
                        yearMonth = 202611,
                        epochDay = 20765L,
                        categoryId = "sub_groceries",
                        contextTag = "CASA",
                        merchant = "Refund",
                        paymentMethod = "CARD",
                        isFixed = false,
                        linkedTransactionId = expId
                    )
                )
            }

            val summary = calc.calculateMonthlySummary(202611, txs)
            assertEquals(
                "Trial $trial: netSavings must equal income - expense",
                summary.totalIncomeCents - summary.totalExpenseCents,
                summary.netSavingsCents
            )
            assertEquals(
                "Trial $trial: fixed + variable expenses must equal total expenses",
                summary.totalExpenseCents,
                summary.fixedExpenseCents + summary.variableExpenseCents
            )
            assertEquals(
                "Trial $trial: base + extra income must equal total income",
                summary.totalIncomeCents,
                summary.baseIncomeCents + summary.extraIncomeCents
            )
            assertTrue("Trial $trial: totalExpenseCents must never be negative", summary.totalExpenseCents >= 0L)
            assertTrue("Trial $trial: savingsRatePercent must be finite", summary.savingsRatePercent.isFinite())
            assertTrue("Trial $trial: expenseIncomeRatioPercent must be finite", summary.expenseIncomeRatioPercent.isFinite())

            val effectiveExpenses = NetExpenseCalculator.resolveEffectiveExpenses(txs)
            assertTrue("No effective expense may be negative", effectiveExpenses.all { it.amountCents >= 0L })
        }

        val zeroIncomeSummary = calc.calculateMonthlySummary(
            202611,
            listOf(
                TransactionEntity(1L, "EXPENSE", 5000L, 202611, 20760L, "sub_groceries", "CASA", "Loja", "CARD", false)
            )
        )
        assertEquals(0.0, zeroIncomeSummary.savingsRatePercent, 0.0)
        assertEquals(0.0, zeroIncomeSummary.expenseIncomeRatioPercent, 0.0)
    }

    @Test
    fun deduplicationAndXtbKeys_preserveDistinctAndDropMultiAppDuplicates() {
        val dedup = TransactionDeduplicator(debounceWindowMs = 180_000L)
        val now = 1_700_000_000_000L
        val pending = listOf(
            PendingTransactionEntity(
                id = 1L,
                amountCents = 2499L,
                rawMerchant = "Continente",
                suggestedCategoryId = "sub_groceries",
                suggestedContextTag = "CASA",
                suggestedPaymentMethod = "MBWAY",
                isFixed = false,
                sourceApp = "MB WAY",
                detectedEpochDay = 20760L,
                receivedTimestamp = now - 30_000L
            )
        )

        val dup = dedup.evaluate(2499L, "Continente", "Moey", now, 20760L, 202611, pending, emptyList(), emptyList())
        assertTrue("Multi-app duplicate within 3m must be dropped", dup is DeduplicationResult.DuplicateDrop)

        val distinctAmount = dedup.evaluate(1999L, "Continente", "Moey", now, 20760L, 202611, pending, emptyList(), emptyList())
        assertTrue("Distinct amount must be accepted", distinctAmount is DeduplicationResult.NewUnique)

        val outsideWindow = dedup.evaluate(2499L, "Continente", "Moey", now + 200_000L, 20760L, 202611, pending, emptyList(), emptyList())
        assertTrue("Same amount outside debounce window must be accepted", outsideWindow is DeduplicationResult.NewUnique)

        val divA = XtbTickerMapper.generateAnonymousDeduplicationKey(20760L, 1250L, "Dividend", "VWCE.DE", 0)
        val divB = XtbTickerMapper.generateAnonymousDeduplicationKey(20760L, 1250L, "Dividend", "AAPL.US", 0)
        val divA2 = XtbTickerMapper.generateAnonymousDeduplicationKey(20760L, 1250L, "Dividend", "VWCE.DE", 1)
        assertNotEquals("Same-day identical dividends on different tickers must not collide", divA, divB)
        assertNotEquals("Multiple occurrences on same day/ticker must not collide", divA, divA2)
    }

    @Test
    fun portfolioMarketSync_neverOverwritesOnFxFailureOrAnomalyAndValidatesTickers() = runTest {
        val badTickers = listOf("", "  ", "../etc/passwd", "AAPL?foo=1", "AAPL#frag", "A".repeat(25), "BAD TICKER")
        for (bad in badTickers) {
            assertNull("Ticker '$bad' must be rejected", TickerPolicy.normalize(bad))
        }
        assertEquals("VWCE.DE", TickerPolicy.normalize("  vwce.de "))

        val initialAssets = mutableListOf(
            InvestmentAssetEntity(
                id = "a_fx_fail", name = "US ETF", type = "ETF", institution = "XTB",
                investedCents = 100_000L, currentValueCents = 100_000L, monthlyContributionCents = 0L,
                targetCents = null, notes = "", lastUpdatedEpochDay = 20700L, ticker = "VOO", units = 2.0
            ),
            InvestmentAssetEntity(
                id = "a_jump", name = "Jump ETF", type = "ETF", institution = "XTB",
                investedCents = 100_000L, currentValueCents = 100_000L, monthlyContributionCents = 0L,
                targetCents = null, notes = "", lastUpdatedEpochDay = 20700L, ticker = "VWCE.DE", units = 10.0
            ),
            InvestmentAssetEntity(
                id = "a_ok", name = "Good ETF", type = "ETF", institution = "XTB",
                investedCents = 100_000L, currentValueCents = 100_000L, monthlyContributionCents = 0L,
                targetCents = null, notes = "", lastUpdatedEpochDay = 20700L, ticker = "IWDA.AS", units = 10.0
            )
        )

        val fakeDao = object : InvestmentAssetDao {
            override fun getAll(): Flow<List<InvestmentAssetEntity>> = flowOf(initialAssets.toList())
            override fun getActive(): Flow<List<InvestmentAssetEntity>> = flowOf(initialAssets.filter { !it.archived })
            override suspend fun getAllSync(): List<InvestmentAssetEntity> = initialAssets.toList()
            override suspend fun getById(id: String): InvestmentAssetEntity? = initialAssets.find { it.id == id }
            override suspend fun insert(asset: InvestmentAssetEntity) {
                initialAssets.removeAll { it.id == asset.id }
                initialAssets.add(asset)
            }
            override suspend fun update(asset: InvestmentAssetEntity) {
                val idx = initialAssets.indexOfFirst { it.id == asset.id }
                if (idx >= 0) initialAssets[idx] = asset
            }
            override suspend fun updateCurrentValue(id: String, newValueCents: Long, epochDay: Long) {}
            override suspend fun deleteById(id: String) { initialAssets.removeAll { it.id == id } }
            override suspend fun deleteAll() { initialAssets.clear() }
            override suspend fun count(): Int = initialAssets.size
        }

        val fakeQuotes = object : QuoteService {
            override suspend fun fetchQuotes(tickers: List<String>): Map<String, YahooQuote> = mapOf(
                "VOO" to YahooQuote("VOO", 500.0, "USD", 0.5),
                "VWCE.DE" to YahooQuote("VWCE.DE", 500.0, "EUR", 0.5),
                "IWDA.AS" to YahooQuote("IWDA.AS", 110.0, "EUR", 0.5)
            )
            override suspend fun fetchEurRate(currency: String): Double? = when (currency) {
                "EUR" -> 1.0
                else -> null
            }
        }

        val useCase = PortfolioMarketSyncUseCase(fakeDao, fakeQuotes)
        val summary = useCase.syncAllActiveAssets(todayEpochDay = 20760L)

        assertEquals(1, summary.updatedCount)
        assertTrue("USD asset with failed FX must be in failedTickers", "VOO" in summary.failedTickers)
        assertTrue("5x jump asset must be in suspiciousTickers", "VWCE.DE" in summary.suspiciousTickers)
        assertEquals(100_000L, fakeDao.getById("a_fx_fail")!!.currentValueCents)
        assertEquals(100_000L, fakeDao.getById("a_jump")!!.currentValueCents)
        assertEquals(110_000L, fakeDao.getById("a_ok")!!.currentValueCents)
    }

    @Test
    fun privacyModeAndCsvFormulaNeutralization_holdAcrossAdversarialInputs() {
        val prev = MoneyCents.isPrivacyModeEnabled
        try {
            MoneyCents.isPrivacyModeEnabled = true
            val rng = Random(99)
            repeat(100) {
                val cents = rng.nextLong(-1_000_000_000L, 1_000_000_000L)
                val masked = MoneyCents.formatCents(cents)
                assertEquals(MoneyCents.MASKED_STRING, masked)
                assertFalse("Masked value must not leak digits", masked.any { it.isDigit() })

                val input = MoneyCents.formatForInput(cents)
                assertFalse("Input format must never be masked", input.contains('•'))
                assertTrue("Input format must contain digits", input.any { it.isDigit() })
            }
        } finally {
            MoneyCents.isPrivacyModeEnabled = prev
        }

        val serializer = BackupSerializer()
        val payloads = listOf(
            "=HYPERLINK(\"http://evil\",\"click\")",
            "+1+1",
            "-2+3",
            "@SUM(A1:A10)",
            "\t=cmd|' /C calc'!A0",
            "\r=1+1"
        )
        val txs = payloads.mapIndexed { idx, payload ->
            TransactionEntity(
                id = idx.toLong() + 1L,
                type = "EXPENSE",
                amountCents = 1000L,
                yearMonth = 202611,
                epochDay = 20760L,
                categoryId = "cat_1",
                contextTag = payload,
                merchant = payload,
                paymentMethod = "CARD",
                isFixed = false
            )
        }
        val csv = serializer.exportToCsv(txs, mapOf("cat_1" to CategoryEntity("cat_1", null, "Cat", "c", 0L, "EXPENSE", false, 1)))
        val dataLines = csv.split("\n").drop(1).filter { it.isNotBlank() }
        assertEquals(payloads.size, dataLines.size)
        for (line in dataLines) {
            val cells = line.split(";")
            val contextCell = cells[6].trim('"')
            val merchantCell = cells[7].trim('"')
            assertTrue("Context cell '$contextCell' must be formula-neutralized", contextCell.startsWith("'"))
            assertTrue("Merchant cell '$merchantCell' must be formula-neutralized", merchantCell.startsWith("'"))
        }
    }
}
