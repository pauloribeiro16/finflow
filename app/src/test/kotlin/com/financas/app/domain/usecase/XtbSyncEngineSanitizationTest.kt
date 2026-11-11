package com.financas.app.domain.usecase

import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakeTransactionDao
import com.financas.app.domain.model.XtbCashOperation
import com.financas.app.domain.model.XtbParsedReport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class XtbSyncEngineSanitizationTest {

    private lateinit var investmentDao: InvestmentAssetDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var syncEngine: XtbSyncEngine
    private val parser = XtbReportParser()

    private val sampleXtbCsv = """
Account number,2642443,,,,,,,,,,,,,,,,,,,,,,,
Closed Positions,,,,,,,,,,,,,,,,,,,,,,,,
Date from (UTC),2006-01-01 00:00:00,,,,,,,,,,,,,,,,,,,,,,,
Date to (UTC),2026-10-30 16:46:47,,,,,,,,,,,,,,,,,,,,,,,
Instrument,Ticker,Category,Type,Volume,Open Price,Open Time (UTC),Close Price,Close Time (UTC),Product,Profit/Loss,Gross Profit,Purchase Value,Sale Value,Stop Loss,Take Profit,Commission,Margin,Swap,Rollover,Open Conversion Rate,Close Conversion Rate,Close Origin,Position ID,Comment
Shell,SHELL.NL,STOCK,BUY,1,0.0001,2023-03-06 08:41:22,32.095,2026-02-03 13:14:33,My Trades,32.1,32.1,0,32.1,,,0,,,,,,Android,892631613,Free Stock
Profit/loss,,,,,,,,,,32.1,32.1,,,,,,,,,,,,,
Account number,2642443,,,,,,,,
Cash Operations,,,,,,,,,
Date from (UTC),2006-01-01 00:00:00,,,,,,,,
Date to (UTC),2026-10-30 16:46:47,,,,,,,,
Type,Instrument,Ticker,Category,Time,Amount,ID,Comment,Product,Position ID
Dividend,Shell,SHELL.NL,STOCK,2025-12-19 13:59:01,0.31,1058436315,SHELL.NL EUR 0.3070/ SHR,My Trades,892631613
Free funds interest,,,,2026-07-06 11:35:20,0.01,1343980571,Free-funds Interest 2026-06,My Trades,
Free funds interest tax,,,,2026-07-06 11:20:56,-0.01,1343851885,Free-funds Interest Tax 2026-06,My Trades,
Deposit,,,,2026-10-02 16:55:05,200,1463181319,"JP_MORGAN deposit, JP_MORGAN provider transaction id=TXN-C-5045018-1868462750, JP_MORGAN merchant reference id=TXN-C-5045018-1868462750, id=38778925",My Trades,
Total,,,,,2.0399999999999991,,,,
Account number,2642443,,,,,,,,,,,,,,,,,
Open Positions,,,,,,,,,,,,,,,,,,
Data as of report generated,2026-10-30 16:46:58,,,,,,,,,,,,,,,,,
Product,Metric,Amount,Currency,,,,,,,,,,,,,,,
My Trades,Open position value,1921.39,EUR,,,,,,,,,,,,,,,
My Trades,Open position profit,35.46,EUR,,,,,,,,,,,,,,,
Note,Summary values and open positions are shown as of the report generation time,,,,,,,,,,,,,,,,,
Product,Instrument/Position,Ticker,Category,Type,Volume,Value,Current price,Open price,Open time (UTC),Stop Loss,Take Profit,Net Profit %,Net Profit,Gross Profit,Margin,Open Commission,Swap,Rollover
My Trades,MSCI World ex USA,EXUS.DE,ETF,,12.9154,520.95,,38.009,,,,6.12,30.04,30.04,,,,
My Trades,2348756030,EXUS.DE,,BUY,2.7238,109.86,40.335,36.695,2026-02-03 13:04:47,,,9.91,9.91,9.91,,,,
My Trades,Nasdaq-100 Daily Lev (2x),LQQ.FR,ETF,,26.2636,238.68,,9.497,,,,-4.32,-10.77,-10.77,,,,
    """.trimIndent()

    @Before
    fun setup() {
        investmentDao = FakeInvestmentAssetDao()
        transactionDao = FakeTransactionDao()
        syncEngine = XtbSyncEngine(investmentDao, transactionDao)
    }

    @Test
    fun sync_sanitizesSensitiveBrokerData_andCatalogsInitialPositions() = runTest {
        val report = parser.parse(sampleXtbCsv)
        val result = syncEngine.sync(report, importCashOperations = true)

        assertEquals(2, result.newAssetsCount)
        assertEquals(3, result.importedTransactionsCount)

        val assets = investmentDao.getAllSync()
        val transactions = transactionDao.getAllSync()

        // 1. Check Nasdaq Leveraged ETF is mapped to Yahoo Finance ticker LQQ.PA
        val lqqAsset = assets.firstOrNull { it.name.contains("Nasdaq", ignoreCase = true) }
        assertNotNull(lqqAsset)
        assertEquals("LQQ.PA", lqqAsset?.ticker)
        assertEquals(26.2636, lqqAsset?.units ?: 0.0, 0.0001)
        assertTrue(lqqAsset?.notes?.contains("Posição inicial: 26.2636 un.") == true)
        assertTrue(lqqAsset?.notes?.contains("Ticker: LQQ.PA") == true)

        // 2. Check MSCI World ex USA
        val exusAsset = assets.firstOrNull { it.name.contains("MSCI World ex USA", ignoreCase = true) }
        assertNotNull(exusAsset)
        assertEquals("EXUS.DE", exusAsset?.ticker)
        assertEquals(12.9154, exusAsset?.units ?: 0.0, 0.0001)

        // 3. STRICT PRIVACY: Verify NO account number or internal position IDs exist anywhere in DB
        assets.forEach { asset ->
            assertFalse("Asset notes should not contain Account number", asset.notes.contains("2642443"))
            assertFalse("Asset notes should not contain internal Position ID", asset.notes.contains("2348756030"))
            assertFalse("Asset notes should not contain internal Position ID", asset.notes.contains("892631613"))
            assertFalse("Asset id should not contain internal Position ID", asset.id.contains("2348756030"))
        }

        transactions.forEach { tx ->
            assertFalse("Transaction merchant should not contain Account number", tx.merchant.contains("2642443"))
            assertFalse("Transaction merchant should not contain internal provider TXN id", tx.merchant.contains("TXN-C-5045018"))
            assertFalse("Transaction merchant should not contain raw position id", tx.merchant.contains("892631613"))
            assertFalse("Transaction merchant should not contain raw internal operation id", tx.merchant.contains("1058436315"))
            assertTrue("Transaction merchant should use anonymous hash marker", tx.merchant.contains("[XTB:"))
        }
    }

    @Test
    fun sync_reimportIsIdempotent() = runTest {
        val report = parser.parse(sampleXtbCsv)
        syncEngine.sync(report, importCashOperations = true)
        val second = syncEngine.sync(report, importCashOperations = true)

        assertEquals(0, second.importedTransactionsCount)
        assertEquals(3, transactionDao.getAllSync().size)
    }

    @Test
    fun sync_keepsEqualDividendsFromDifferentInstrumentsOnSameDay() = runTest {
        val report = XtbParsedReport(
            openPositions = emptyList(),
            cashOperations = listOf(
                dividend("1", "Shell", "SHELL.NL"),
                dividend("2", "MSCI World ex USA", "EXUS.DE"),
                dividend("3", "MSCI World ex USA", "EXUS.DE")
            )
        )

        val result = syncEngine.sync(report, importCashOperations = true)
        assertEquals(3, result.importedTransactionsCount)

        val again = syncEngine.sync(report, importCashOperations = true)
        assertEquals(0, again.importedTransactionsCount)
    }

    @Test
    fun sync_legacyMarkerOnlyCoversOneOperation() = runTest {
        val legacy = XtbTickerMapper.legacyDeduplicationKey(20000L, 31L, "Dividend")
        transactionDao.insert(
            com.financas.app.data.entity.TransactionEntity(
                type = "INCOME",
                amountCents = 31L,
                yearMonth = 202512,
                epochDay = 20000L,
                categoryId = "inc_other",
                contextTag = "PESSOAL",
                merchant = "XTB Dividendo Shell $legacy",
                paymentMethod = "TRANSFERENCIA",
                isFixed = false
            )
        )
        val report = XtbParsedReport(
            openPositions = emptyList(),
            cashOperations = listOf(dividend("1", "Shell", "SHELL.NL"), dividend("2", "MSCI World ex USA", "EXUS.DE"))
        )

        val result = syncEngine.sync(report, importCashOperations = true)
        assertEquals(1, result.importedTransactionsCount)
        assertEquals(1, result.skippedTransactionsCount)
    }

    private fun dividend(id: String, instrument: String, ticker: String) = XtbCashOperation(
        id = id,
        type = "Dividend",
        instrument = instrument,
        ticker = ticker,
        category = "STOCK",
        amountCents = 31L,
        epochDay = 20000L,
        yearMonth = 202512,
        comment = ""
    )
}
