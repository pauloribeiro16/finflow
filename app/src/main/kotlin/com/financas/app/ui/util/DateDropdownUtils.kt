package com.financas.app.ui.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateDropdownUtils {

    val MONTH_NAMES_PT = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril",
        "Maio", "Junho", "Julho", "Agosto",
        "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    val MONTH_SHORT_NAMES_PT = listOf(
        "Jan", "Fev", "Mar", "Abr",
        "Mai", "Jun", "Jul", "Ago",
        "Set", "Out", "Nov", "Dez"
    )

    val DEFAULT_YEARS = listOf(2024, 2025, 2026, 2027)

    fun daysInMonth(month: Int, year: Int): Int {
        val safeMonth = month.coerceIn(1, 12)
        return YearMonth.of(year, safeMonth).lengthOfMonth()
    }

    fun clampDay(day: Int, month: Int, year: Int): Int {
        val maxDays = daysInMonth(month, year)
        return day.coerceIn(1, maxDays)
    }

    fun toEpochDay(day: Int, month: Int, year: Int): Long {
        val safeMonth = month.coerceIn(1, 12)
        val safeDay = clampDay(day, safeMonth, year)
        return LocalDate.of(year, safeMonth, safeDay).toEpochDay()
    }

    data class DateParts(val day: Int, val month: Int, val year: Int)

    fun fromEpochDay(epochDay: Long): DateParts {
        val date = LocalDate.ofEpochDay(epochDay)
        return DateParts(day = date.dayOfMonth, month = date.monthValue, year = date.year)
    }

    fun formatFullPt(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
        val text = date.format(formatter)
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
    }
}
