package com.financas.app.domain.usecase

object FinancialAppFilter {

    private val exactSegmentTokens = setOf(
        "bcp",
        "bpi",
        "cgd",
        "xtb",
        "n26",
        "wise",
        "curve"
    )

    private val brandTokens = listOf(
        "mbway",
        "moey",
        "paypal",
        "revolut",
        "caixadirecta",
        "caixadireta",
        "caixageral",
        "millennium",
        "santander",
        "activobank",
        "novobanco",
        "montepio",
        "bankinter",
        "bancobpi",
        "bancoctt",
        "creditoagricola",
        "bestbank",
        "abanca",
        "traderepublic",
        "degiro",
        "transferwise",
        "number26",
        "imaginecurve",
        "openbank",
        "coverflex",
        "edenred",
        "pluxee",
        "sodexo",
        "wizink",
        "unibanco",
        "cetelem",
        "cofidis"
    )

    fun isFinancialApp(packageName: String): Boolean {
        val segments = packageName.lowercase().split('.').filter { it.isNotBlank() }
        if (segments.isEmpty()) return false
        return segments.any { segment ->
            segment in exactSegmentTokens ||
                brandTokens.any { token -> segment.startsWith(token) || segment.endsWith(token) }
        }
    }
}
