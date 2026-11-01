package com.financas.app.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAppFilterTest {

    @Test
    fun portugueseFinancialPackages_pass() {
        val packages = listOf(
            "pt.sibs.android.mbway",
            "pt.sibs.mbway",
            "com.sibs.android.mbway",
            "com.paypal.android.p2pmobile",
            "com.revolut.revolut",
            "com.revolut.revolut.test",
            "com.moey.android",
            "pt.cgd.caixadirecta",
            "com.santander.app",
            "pt.bcp.mobile",
            "com.activobank.mobile",
            "pt.novobanco.mobile",
            "pt.montepio.geral",
            "com.bankinter.app",
            "pt.bpi.mobile"
        )
        packages.forEach { pkg ->
            assertTrue("Expected $pkg to be financial", FinancialAppFilter.isFinancialApp(pkg))
        }
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertTrue(FinancialAppFilter.isFinancialApp("PT.SIBS.ANDROID.MBWAY"))
        assertTrue(FinancialAppFilter.isFinancialApp("Com.PayPal.Android.P2PMobile"))
        assertTrue(FinancialAppFilter.isFinancialApp("COM.REVOLUT.REVOLUT"))
    }

    @Test
    fun nonFinancialPackages_fail() {
        val packages = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.gm",
            "com.android.chrome",
            "com.instagram.android",
            "com.example.app",
            "com.spotify.music"
        )
        packages.forEach { pkg ->
            assertFalse("Expected $pkg to be non-financial", FinancialAppFilter.isFinancialApp(pkg))
        }
    }

    @Test
    fun notificationText_neverQualifiesAnUnknownApp() {
        assertFalse(FinancialAppFilter.isFinancialApp("com.unknown.app"))
        assertFalse(FinancialAppFilter.isFinancialApp(""))
    }
}
