package com.financas.app.domain.usecase

object FinancialAppFilter {

    private val financialPackageTokens = listOf(
        "mbway",
        "moey",
        "paypal",
        "revolut",
        "cgd",
        "caixadirecta",
        "caixageral",
        "caixa",
        "millennium",
        "bcp",
        "santander",
        "activobank",
        "novobanco",
        "novo banco",
        "montepio",
        "bankinter",
        "bpi",
        "bancoctt",
        "ctt",
        "creditoagricola",
        "bestbank",
        "abanca",
        "xtb",
        "traderepublic",
        "degiro",
        "wise",
        "transferwise",
        "n26",
        "number26",
        "curve",
        "openbank",
        "coverflex",
        "edenred",
        "pluxee",
        "sodexo",
        "ticket",
        "wizink",
        "unibanco",
        "cetelem",
        "cofidis",
        "banco",
        "bank"
    )

    fun isFinancialApp(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        if (pkg.isBlank()) return false
        return financialPackageTokens.any { pkg.contains(it) }
    }
}
