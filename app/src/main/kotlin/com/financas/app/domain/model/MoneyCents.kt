package com.financas.app.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@JvmInline
value class MoneyCents(val cents: Long) {

    fun toEurosDouble(): Double = cents / 100.0

    fun formatPt(): String = formatCents(cents)

    companion object {
        val ZERO = MoneyCents(0L)

        fun fromEuros(euros: Double): MoneyCents {
            return MoneyCents(Math.round(euros * 100.0))
        }

        fun formatCents(cents: Long): String {
            val symbols = DecimalFormatSymbols(Locale("pt", "PT")).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
            val df = DecimalFormat("#,##0.00 '€'", symbols)
            return df.format(cents / 100.0)
        }

        fun formatPercent(percent: Double): String {
            val symbols = DecimalFormatSymbols(Locale("pt", "PT")).apply {
                decimalSeparator = ','
            }
            val df = DecimalFormat("0.0'%'", symbols)
            return df.format(percent)
        }
    }
}
