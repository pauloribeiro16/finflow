package com.financas.app.domain.usecase

import java.security.MessageDigest

object XtbTickerMapper {

    fun mapToYahooFinanceTicker(xtbTicker: String): String {
        val trimmed = xtbTicker.trim().uppercase()
        return when {
            trimmed.endsWith(".FR") -> trimmed.removeSuffix(".FR") + ".PA"
            trimmed.endsWith(".UK") -> trimmed.removeSuffix(".UK") + ".L"
            trimmed.endsWith(".NL") -> trimmed.removeSuffix(".NL") + ".AS"
            trimmed.endsWith(".IT") -> trimmed.removeSuffix(".IT") + ".MI"
            trimmed.endsWith(".ES") -> trimmed.removeSuffix(".ES") + ".MC"
            trimmed.endsWith(".DE") -> trimmed
            else -> trimmed
        }
    }

    fun isLeveragedInstrument(name: String, ticker: String): Boolean {
        val combined = "$name $ticker".lowercase()
        return combined.contains("2x") ||
            combined.contains("3x") ||
            combined.contains("lev") ||
            combined.contains("leveraged") ||
            combined.contains("daily lev") ||
            combined.contains("short") ||
            combined.contains("ultra")
    }

    fun generateAnonymousDeduplicationKey(
        dateEpochDay: Long,
        amountCents: Long,
        operationType: String,
        instrument: String,
        occurrence: Int = 0
    ): String {
        val raw = "v2:$dateEpochDay:$amountCents:${operationType.trim().lowercase()}:${instrument.trim().uppercase()}:$occurrence"
        return "[XTB:${prefixFor(operationType)}-${shortHash(raw, 8)}]"
    }

    fun legacyDeduplicationKey(dateEpochDay: Long, amountCents: Long, operationType: String): String {
        val raw = "$dateEpochDay:$amountCents:$operationType"
        return "[XTB:${prefixFor(operationType)}-${shortHash(raw, 4)}]"
    }

    private fun prefixFor(operationType: String): String = when {
        operationType.contains("Dividend", ignoreCase = true) -> "DIV"
        operationType.contains("Interest", ignoreCase = true) -> "INT"
        else -> "TAX"
    }

    private fun shortHash(raw: String, bytes: Int): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.take(bytes).joinToString("") { "%02x".format(it) }
    }
}
