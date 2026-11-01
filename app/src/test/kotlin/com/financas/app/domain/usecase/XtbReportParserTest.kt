package com.financas.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtbReportParserTest {

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
Stock purchase,Physical Gold,EGLN.UK,ETC,2026-10-06 15:01:38,-71.64,1468850982,OPEN BUY EGLN.UK 1/1.3957 @ 71.6400,My Trades,2858152034
Deposit,,,,2026-10-02 16:55:05,200,1463181319,"JP_MORGAN deposit, JP_MORGAN provider transaction id=TXN-C-5045018-1868462750, JP_MORGAN merchant reference id=TXN-C-5045018-1868462750, id=38778925",My Trades,
Free funds interest,,,,2026-07-06 11:35:20,0.01,1343980571,Free-funds Interest 2026-06,My Trades,
Free funds interest tax,,,,2026-07-06 11:20:56,-0.01,1343851885,Free-funds Interest Tax 2026-06,My Trades,
Dividend,Shell,SHELL.NL,STOCK,2025-12-19 13:59:01,0.31,1058436315,SHELL.NL EUR 0.3070/ SHR,My Trades,892631613
Dividend,MSCI World III,AHYQ.DE,ETF,2025-12-11 10:49:00,1.45,1047147648,AHYQ.DE USD 1.7000/ SHR,My Trades,1964129479
Stock sell,Shell,SHELL.NL,STOCK,2026-02-03 13:14:33,32.1,1120606307,CLOSE BUY 1 @ 32.095,My Trades,892631613
Total,,,,,2.0399999999999991,,,,
Account number,2642443,,,,,,,,,,,,,,,,,
Open Positions,,,,,,,,,,,,,,,,,,
Data as of report generated,2026-10-30 16:46:58,,,,,,,,,,,,,,,,,
Product,Metric,Amount,Currency,,,,,,,,,,,,,,,
My Trades,Open position value,1921.39,EUR,,,,,,,,,,,,,,,
My Trades,Open position profit,35.46,EUR,,,,,,,,,,,,,,,
,,,,,,,,,,,,,,,,,,
Note,Summary values and open positions are shown as of the report generation time,,,,,,,,,,,,,,,,,
Product,Instrument/Position,Ticker,Category,Type,Volume,Value,Current price,Open price,Open time (UTC),Stop Loss,Take Profit,Net Profit %,Net Profit,Gross Profit,Margin,Open Commission,Swap,Rollover
My Trades,MSCI World ex USA,EXUS.DE,ETF,,12.9154,520.95,,38.009,,,,6.12,30.04,30.04,,,,
My Trades,2348756030,EXUS.DE,,BUY,2.7238,109.86,40.335,36.695,2026-02-03 13:04:47,,,9.91,9.91,9.91,,,,
My Trades,Physical Gold,EGLN.UK,ETC,,5.6353,381.73,,70.9756,,,,-4.56,-18.25,-18.25,,,,
My Trades,2732180366,EGLN.UK,,BUY,1.4671,99.38,67.74,68.16,2026-08-03 11:25:55,,,-0.62,-0.62,-0.62,,,,
My Trades,Quantum Computing,QUTM.DE,ETF,,10.8852,239.04,,25.042,,,,-12.31,-33.55,-33.55,,,,
My Trades,Nasdaq-100 Daily Lev (2x),LQQ.FR,ETF,,26.2636,238.68,,9.497,,,,-4.32,-10.77,-10.77,,,,
My Trades,MSCI World SRI,2B7K.DE,ETF,,15.7652,225.03,,12.686,,,,12.52,25.03,25.03,,,,
My Trades,Core S&P 500,CSPX.UK,ETF,,0.2423,177.28,,732.86,,,,16.07,24.54,24.54,,,,
My Trades,MSCI World III,AHYQ.DE,ETF,,0.9999,123.81,,100.27,,,,23.49,23.55,23.55,,,,
My Trades,Crypto and Blckchn Innvtrs,DAVV.DE,ETF,,1.6669,14.87,,11.998,,,,-25.65,-5.13,-5.13,,,,
""".trimIndent()

    private val parser = XtbReportParser()

    @Test
    fun parse_extractsAll8OpenPositions() {
        val report = parser.parse(sampleXtbCsv)

        assertEquals(8, report.openPositions.size)

        val msciEx = report.openPositions.find { it.ticker == "EXUS.DE" }
        assertNotNull(msciEx)
        assertEquals("MSCI World ex USA", msciEx!!.name)
        assertEquals("ETF", msciEx.category)
        assertEquals(12.9154, msciEx.volume, 0.0001)
        assertEquals(52095L, msciEx.currentValueCents) // 520.95 €
        assertEquals(49091L, msciEx.investedCents) // 520.95 - 30.04 = 490.91 €
        assertEquals(3004L, msciEx.profitLossCents) // +30.04 €
        assertEquals(6.12, msciEx.profitLossPercentage, 0.01)

        val gold = report.openPositions.find { it.ticker == "EGLN.UK" }
        assertNotNull(gold)
        assertEquals("Physical Gold", gold!!.name)
        assertEquals("ETC", gold.category)
        assertEquals(38173L, gold.currentValueCents) // 381.73 €
        assertEquals(39998L, gold.investedCents) // 381.73 - (-18.25) = 399.98 €
        assertEquals(-1825L, gold.profitLossCents)

        val sp500 = report.openPositions.find { it.ticker == "CSPX.UK" }
        assertNotNull(sp500)
        assertEquals(17728L, sp500!!.currentValueCents)
        assertEquals(15274L, sp500.investedCents) // 177.28 - 24.54 = 152.74 €
        assertEquals(2454L, sp500.profitLossCents)
    }

    @Test
    fun parse_extractsCashOperationsWithQuotedFields() {
        val report = parser.parse(sampleXtbCsv)

        assertTrue(report.cashOperations.isNotEmpty())

        val deposit = report.cashOperations.find { it.id == "1463181319" }
        assertNotNull(deposit)
        assertEquals("Deposit", deposit!!.type)
        assertEquals(20000L, deposit.amountCents) // 200.00 €
        assertTrue(deposit.comment.contains("JP_MORGAN deposit"))

        val divShell = report.cashOperations.find { it.id == "1058436315" }
        assertNotNull(divShell)
        assertEquals("Dividend", divShell!!.type)
        assertEquals("Shell", divShell.instrument)
        assertEquals(31L, divShell.amountCents) // 0.31 €

        val interest = report.cashOperations.find { it.id == "1343980571" }
        assertNotNull(interest)
        assertEquals("Free funds interest", interest!!.type)
        assertEquals(1L, interest.amountCents) // 0.01 €

        val tax = report.cashOperations.find { it.id == "1343851885" }
        assertNotNull(tax)
        assertEquals("Free funds interest tax", tax!!.type)
        assertEquals(-1L, tax.amountCents) // -0.01 €
    }

    @Test
    fun parse_emptyInputReturnsEmptyReport() {
        val report = parser.parse("")
        assertEquals(0, report.openPositions.size)
        assertEquals(0, report.cashOperations.size)
    }
}
