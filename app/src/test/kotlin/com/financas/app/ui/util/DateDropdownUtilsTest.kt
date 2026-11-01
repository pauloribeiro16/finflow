package com.financas.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateDropdownUtilsTest {

    @Test
    fun testDaysInMonth() {
        assertEquals(31, DateDropdownUtils.daysInMonth(1, 2026)) // Jan
        assertEquals(28, DateDropdownUtils.daysInMonth(2, 2025)) // Feb non-leap
        assertEquals(29, DateDropdownUtils.daysInMonth(2, 2024)) // Feb leap
        assertEquals(30, DateDropdownUtils.daysInMonth(4, 2026)) // Apr
        assertEquals(31, DateDropdownUtils.daysInMonth(10, 2026)) // Oct
    }

    @Test
    fun testClampDay() {
        // Clamping 31 to Feb non-leap -> 28
        assertEquals(28, DateDropdownUtils.clampDay(31, 2, 2025))
        // Clamping 31 to Feb leap -> 29
        assertEquals(29, DateDropdownUtils.clampDay(31, 2, 2024))
        // Clamping 31 to April -> 30
        assertEquals(30, DateDropdownUtils.clampDay(31, 4, 2026))
        // Clamping within valid range
        assertEquals(15, DateDropdownUtils.clampDay(15, 10, 2026))
        // Clamping lower bound
        assertEquals(1, DateDropdownUtils.clampDay(0, 5, 2026))
    }

    @Test
    fun testToAndFromEpochDayRoundtrip() {
        val testDate = LocalDate.of(2026, 10, 27)
        val epochDay = DateDropdownUtils.toEpochDay(27, 10, 2026)
        assertEquals(testDate.toEpochDay(), epochDay)

        val parts = DateDropdownUtils.fromEpochDay(epochDay)
        assertEquals(27, parts.day)
        assertEquals(10, parts.month)
        assertEquals(2026, parts.year)
    }

    @Test
    fun testFormatFullPt() {
        val date = LocalDate.of(2026, 10, 27)
        val formatted = DateDropdownUtils.formatFullPt(date.toEpochDay())
        // Should contain 27, outubro, 2026 and capitalized weekday
        assert(formatted.contains("27"))
        assert(formatted.contains("outubro"))
        assert(formatted.contains("2026"))
    }
}
