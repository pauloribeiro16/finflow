package com.financas.app.domain.usecase

import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class XtbSyncEngineTest {

    private lateinit var investmentDao: FakeInvestmentAssetDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var syncEngine: XtbSyncEngine
    private val parser = XtbReportParser()

    private val sampleXtbCsv = """
Account number,2642443,,,,,,,,
Cash Operations,,,,,,,,,
Type,Instrument,Ticker,Category,Time,Amount,ID,Comment,Product,Position ID
Dividend,Shell,SHELL.NL,STOCK,2025-12-19 13:59:01,0.31,1058436315,SHELL.NL EUR 0.3070/ SHR,My Trades,892631613
Free funds interest,,,,2026-07-06 11:35:20,0.01,1343980571,Free-funds Interest 2026-06,My Trades,
Free funds interest tax,,,,2026-07-06 11:20:56,-0.01,1343851885,Free-funds Interest Tax 2026-06,My Trades,
Deposit,,,,2026-10-02 16:55:05,200,1463181319,"JP_MORGAN deposit",My Trades,
Account number,2642443,,,,,,,,,,,,,,,,,
Open Positions,,,,,,,,,,,,,,,,,,
Data as of report generated,2026-10-30 16:46:58,,,,,,,,,,,,,,,,,
Product,Instrument/Position,Ticker,Category,Type,Volume,Value,Current price,Open price,Open time (UTC),Stop Loss,Take Profit,Net Profit %,Net Profit,Gross Profit,Margin,Open Commission,Swap,Rollover
My Trades,MSCI World ex USA,EXUS.DE,ETF,,12.9154,520.95,,38.009,,,,6.12,30.04,30.04,,,,
My Trades,Physical Gold,EGLN.UK,ETC,,5.6353,381.73,,70.9756,,,,-4.56,-18.25,-18.25,,,,
My Trades,Quantum Computing,QUTM.DE,ETF,,10.8852,239.04,,25.042,,,,-12.31,-33.55,-33.55,,,,
My Trades,Nasdaq-100 Daily Lev (2x),LQQ.FR,ETF,,26.2636,238.68,,9.497,,,,-4.32,-10.77,-10.77,,,,
My Trades,MSCI World SRI,2B7K.DE,ETF,,15.7652,225.03,,12.686,,,,12.52,25.03,25.03,,,,
My Trades,Core S&P 500,CSPX.UK,ETF,,0.2423,177.28,,732.86,,,,16.07,24.54,24.54,,,,
My Trades,MSCI World III,AHYQ.DE,ETF,,0.9999,123.81,,100.27,,,,23.49,23.55,23.55,,,,
My Trades,Crypto and Blckchn Innvtrs,DAVV.DE,ETF,,1.6669,14.87,,11.998,,,,-25.65,-5.13,-5.13,,,,
""".trimIndent()

    @Before
    fun setup() {
        investmentDao = FakeInvestmentAssetDao()
        transactionDao = FakeTransactionDao()
        syncEngine = XtbSyncEngine(investmentDao, transactionDao)
    }

    @Test
    fun sync_firstImport_createsAllPositions() = runTest {
        val report = parser.parse(sampleXtbCsv)
        val result = syncEngine.sync(report, importCashOperations = true)

        assertEquals(8, result.newAssetsCount)
        assertEquals(0, result.updatedAssetsCount)
        assertEquals(8, investmentDao.getAllSync().size)

        // 3 operations (dividend, interest, tax)
        assertEquals(3, result.importedTransactionsCount)
        assertEquals(0, result.skippedTransactionsCount)
        assertEquals(3, transactionDao.getAllSync().size)
    }

    @Test
    fun sync_subsequentImport_updatesWithoutDuplicatingAssetsOrTransactions() = runTest {
        val report = parser.parse(sampleXtbCsv)

        // First sync
        syncEngine.sync(report, importCashOperations = true)
        assertEquals(8, investmentDao.getAllSync().size)
        assertEquals(3, transactionDao.getAllSync().size)

        // Second sync with identical report
        val result2 = syncEngine.sync(report, importCashOperations = true)

        assertEquals(0, result2.newAssetsCount)
        assertEquals(8, result2.updatedAssetsCount) // All 8 updated in place
        assertEquals(8, investmentDao.getAllSync().size) // Still exactly 8 assets

        assertEquals(0, result2.importedTransactionsCount) // Zero duplicate transactions
        assertEquals(3, result2.skippedTransactionsCount) // 3 skipped
        assertEquals(3, transactionDao.getAllSync().size) // Still exactly 3 transactions
    }

    @Test
    fun sync_doesNotTouchOptimizePpr() = runTest {
        val ppr = InvestmentAssetEntity(
            id = "inv_ppr_agr",
            name = "PPR Agressivo",
            type = "PPR",
            institution = "Optimize",
            investedCents = 220000L,
            currentValueCents = 243921L,
            monthlyContributionCents = 20000L,
            notes = "Optimize Agressivo",
            lastUpdatedEpochDay = 20750L
        )
        investmentDao.insert(ppr)

        val report = parser.parse(sampleXtbCsv)
        val result = syncEngine.sync(report, importCashOperations = false)

        assertEquals(8, result.newAssetsCount)
        assertEquals(9, investmentDao.getAllSync().size)

        val pprAfter = investmentDao.getById("inv_ppr_agr")
        assertEquals(243921L, pprAfter!!.currentValueCents)
        assertEquals(20000L, pprAfter.monthlyContributionCents)
        assertEquals("Optimize", pprAfter.institution)
    }
}
