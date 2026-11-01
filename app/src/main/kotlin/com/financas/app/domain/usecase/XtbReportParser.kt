package com.financas.app.domain.usecase

import com.financas.app.domain.model.XtbCashOperation
import com.financas.app.domain.model.XtbOpenPosition
import com.financas.app.domain.model.XtbParsedReport
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

class XtbReportParser {

    private enum class Section {
        NONE, CLOSED_POSITIONS, CASH_OPERATIONS, OPEN_POSITIONS
    }

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parse(csvContent: String): XtbParsedReport {
        if (csvContent.isBlank()) {
            return XtbParsedReport(emptyList(), emptyList())
        }

        var currentSection = Section.NONE
        val openPositions = mutableListOf<XtbOpenPosition>()
        val cashOperations = mutableListOf<XtbCashOperation>()
        var reportDateEpochDay: Long? = null

        val lines = csvContent.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Section boundary detection
            if (line.startsWith("Closed Positions", ignoreCase = true)) {
                currentSection = Section.CLOSED_POSITIONS
                continue
            }
            if (line.startsWith("Cash Operations", ignoreCase = true)) {
                currentSection = Section.CASH_OPERATIONS
                continue
            }
            if (line.startsWith("Open Positions", ignoreCase = true)) {
                currentSection = Section.OPEN_POSITIONS
                continue
            }

            // Report timestamp
            if (line.startsWith("Data as of report generated,", ignoreCase = true)) {
                val parts = splitCsvLine(line)
                if (parts.size >= 2) {
                    reportDateEpochDay = parseEpochDay(parts[1])
                }
                continue
            }

            when (currentSection) {
                Section.OPEN_POSITIONS -> {
                    parseOpenPositionLine(line)?.let { openPositions.add(it) }
                }
                Section.CASH_OPERATIONS -> {
                    parseCashOperationLine(line)?.let { cashOperations.add(it) }
                }
                else -> {
                    // Closed positions or metadata
                }
            }
        }

        val totalValue = openPositions.sumOf { it.currentValueCents }
        val totalProfit = openPositions.sumOf { it.profitLossCents }

        return XtbParsedReport(
            openPositions = openPositions,
            cashOperations = cashOperations,
            reportDateEpochDay = reportDateEpochDay,
            openPositionValueCents = totalValue,
            openPositionProfitCents = totalProfit
        )
    }

    private fun parseOpenPositionLine(line: String): XtbOpenPosition? {
        val cols = splitCsvLine(line)
        if (cols.size < 14) return null

        // Ignore headers, summaries and position sub-rows
        val productOrCol0 = cols[0]
        val instrumentOrId = cols[1]
        val ticker = cols[2]
        val category = cols[3]
        val type = cols[4]

        if (instrumentOrId.equals("Instrument/Position", ignoreCase = true)) return null
        if (productOrCol0.equals("Product", ignoreCase = true)) return null
        if (productOrCol0.equals("Note", ignoreCase = true)) return null
        if (instrumentOrId.isBlank() || ticker.isBlank()) return null

        // In XTB CSV, individual order sub-rows have Type="BUY" or Type="SELL" and numeric Position IDs
        // The aggregated instrument row has Type="" (empty) and category filled (e.g. "ETF", "ETC")
        if (type.isNotBlank()) return null

        val volume = cols[5].toDoubleOrNull() ?: return null
        val valueStr = cols[6]
        val netProfitPercent = cols[12].toDoubleOrNull() ?: 0.0
        val netProfitStr = cols[13]

        val valueCents = parseDecimalToCents(valueStr)
        val profitLossCents = parseDecimalToCents(netProfitStr)
        val investedCents = valueCents - profitLossCents

        return XtbOpenPosition(
            ticker = ticker,
            name = instrumentOrId,
            category = category.ifBlank { "ETF" },
            volume = volume,
            currentValueCents = valueCents,
            investedCents = investedCents.coerceAtLeast(0L),
            profitLossCents = profitLossCents,
            profitLossPercentage = netProfitPercent
        )
    }

    private fun parseCashOperationLine(line: String): XtbCashOperation? {
        val cols = splitCsvLine(line)
        if (cols.size < 7) return null

        val type = cols[0]
        if (type.equals("Type", ignoreCase = true)) return null
        if (type.equals("Total", ignoreCase = true)) return null

        val instrument = cols.getOrElse(1) { "" }
        val ticker = cols.getOrElse(2) { "" }
        val category = cols.getOrElse(3) { "" }
        val timeStr = cols.getOrElse(4) { "" }
        val amountStr = cols.getOrElse(5) { "" }
        val id = cols.getOrElse(6) { "" }
        val comment = cols.getOrElse(7) { "" }

        if (id.isBlank() || amountStr.isBlank()) return null

        val epochDay = parseEpochDay(timeStr) ?: LocalDate.now().toEpochDay()
        val localDate = LocalDate.ofEpochDay(epochDay)
        val yearMonth = localDate.year * 100 + localDate.monthValue
        val amountCents = parseDecimalToCents(amountStr)

        return XtbCashOperation(
            id = id,
            type = type,
            instrument = instrument,
            ticker = ticker,
            category = category,
            amountCents = amountCents,
            epochDay = epochDay,
            yearMonth = yearMonth,
            comment = comment
        )
    }

    private fun parseDecimalToCents(raw: String): Long {
        val cleaned = raw.trim().replace(" ", "").replace(",", ".")
        val d = cleaned.toDoubleOrNull() ?: 0.0
        return (d * 100.0).roundToLong()
    }

    private fun parseEpochDay(timeStr: String): Long? {
        val trimmed = timeStr.trim()
        if (trimmed.isBlank()) return null
        return try {
            if (trimmed.contains(" ")) {
                val datePart = trimmed.substringBefore(" ")
                LocalDate.parse(datePart, dateFormatter).toEpochDay()
            } else {
                LocalDate.parse(trimmed, dateFormatter).toEpochDay()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }
}
