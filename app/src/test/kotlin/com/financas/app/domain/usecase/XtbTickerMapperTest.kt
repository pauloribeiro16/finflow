package com.financas.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtbTickerMapperTest {

    @Test
    fun mapToYahooFinanceTicker_mapsBoursesCorrectly() {
        assertEquals("LQQ.PA", XtbTickerMapper.mapToYahooFinanceTicker("LQQ.FR"))
        assertEquals("CSPX.L", XtbTickerMapper.mapToYahooFinanceTicker("CSPX.UK"))
        assertEquals("EGLN.L", XtbTickerMapper.mapToYahooFinanceTicker("EGLN.UK"))
        assertEquals("SHELL.AS", XtbTickerMapper.mapToYahooFinanceTicker("SHELL.NL"))
        assertEquals("ENI.MI", XtbTickerMapper.mapToYahooFinanceTicker("ENI.IT"))
        assertEquals("SAN.MC", XtbTickerMapper.mapToYahooFinanceTicker("SAN.ES"))
        assertEquals("EXUS.DE", XtbTickerMapper.mapToYahooFinanceTicker("EXUS.DE"))
        assertEquals("2B7K.DE", XtbTickerMapper.mapToYahooFinanceTicker("2B7K.DE"))
        assertEquals("AAPL", XtbTickerMapper.mapToYahooFinanceTicker("AAPL"))
    }

    @Test
    fun isLeveragedInstrument_detectsLeveragedPositions() {
        assertTrue(XtbTickerMapper.isLeveragedInstrument("Nasdaq-100 Daily Lev (2x)", "LQQ.FR"))
        assertTrue(XtbTickerMapper.isLeveragedInstrument("S&P 500 3x Leveraged", "3USL.UK"))
        assertTrue(XtbTickerMapper.isLeveragedInstrument("Ultra Short QQQ", "QID"))
        assertFalse(XtbTickerMapper.isLeveragedInstrument("MSCI World ex USA", "EXUS.DE"))
        assertFalse(XtbTickerMapper.isLeveragedInstrument("Core S&P 500", "CSPX.UK"))
    }

    @Test
    fun generateAnonymousDeduplicationKey_producesDeterministicKeyWithoutSensitiveIds() {
        val key1 = XtbTickerMapper.generateAnonymousDeduplicationKey(20000L, 150L, "Dividend")
        val key2 = XtbTickerMapper.generateAnonymousDeduplicationKey(20000L, 150L, "Dividend")
        val key3 = XtbTickerMapper.generateAnonymousDeduplicationKey(20000L, 200L, "Dividend")

        assertEquals(key1, key2)
        assertTrue(key1.startsWith("[XTB:DIV-"))
        assertFalse(key1 == key3)
        // Ensure no internal account or broker reference numbers are in key
        assertFalse(key1.contains("2642443"))
        assertFalse(key1.contains("1468850982"))
    }
}
