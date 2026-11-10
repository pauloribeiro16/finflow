package com.financas.app.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@JvmInline
value class MoneyCents(val cents: Long) {

    fun toEurosDouble(): Double = cents / 100.0

    fun formatPt(masked: Boolean = isPrivacyModeEnabled): String = formatCents(cents, masked)

    companion object {
        const val MASKED_STRING = "•••• €"

        @Volatile
        var isPrivacyModeEnabled: Boolean = false

        val ZERO = MoneyCents(0L)

        fun fromEuros(euros: Double): MoneyCents {
            return MoneyCents(Math.round(euros * 100.0))
        }

        fun formatCents(cents: Long, masked: Boolean = isPrivacyModeEnabled): String {
            if (masked) return MASKED_STRING
            val symbols = DecimalFormatSymbols(Locale("pt", "PT")).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
            val df = DecimalFormat("#,##0.00 '€'", symbols)
            return df.format(cents / 100.0)
        }

        fun formatPercent(percent: Double, decimals: Int = 1): String {
            val symbols = DecimalFormatSymbols(Locale("pt", "PT")).apply {
                decimalSeparator = ','
            }
            val pattern = if (decimals > 0) "0.${"0".repeat(decimals)}'%'" else "0'%'"
            val df = DecimalFormat(pattern, symbols)
            val halfUnit = 0.5 / Math.pow(10.0, decimals.toDouble())
            val normalized = if (kotlin.math.abs(percent) < halfUnit) 0.0 else percent
            return df.format(normalized)
        }
    }
}
