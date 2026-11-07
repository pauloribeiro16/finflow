package com.financas.app.domain.usecase

data class ParsedBankNotification(
    val amountCents: Long,
    val merchant: String,
    val paymentMethod: String,
    val sourceApp: String,
    val direction: String,
    val rawText: String
)

class BankNotificationParser {

    private val incomeDirectionKeywords = listOf(
        "recebido", "recebida", "recebeste", "recebimento", "recebeu"
    )

    private val expenseDirectionKeywords = listOf(
        "enviado", "enviada", "enviaste", "enviou", "pagou", "pagamento", "compra",
        "autorizada", "debitado", "transação", "transacao", "movimento", "gasto",
        "cartão de crédito", "cartão de débito", "cartao"
    )

    private val amountAfterCurrencyRegex = Regex("""(?:€|EUR|Eur|eur)\s*(\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)""")
    private val amountBeforeCurrencyRegex = Regex("""(\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(?:€|EUR|Eur|eur)(?:\s|$|[^\w\d.,])""")
    private val decimalNumberRegex = Regex("""\b(\d{1,6}[.,]\d{2})\b""")

    private val merchantPrefixes = listOf(
        Regex("""(?i)\b(?:no comerciante|no estabelecimento|efetuado a|efetuada em|efetuada no|efetuada na|via mb way para|recebido de|recebida de|enviado a|enviada a|pago a|pago em|compra em|compra no|compra na)\s+([A-Za-z0-9À-ÿ\s&'.-]+)"""),
        Regex("""(?i)\b(?:para|em|no|na|a|ao)\s+([A-Za-z0-9À-ÿ\s&'.-]+)"""),
        Regex("""-\s+([A-Za-z0-9À-ÿ\s&'.-]+)""")
    )

    private val ignoredTitleWords = setOf(
        "pagamento", "compra", "transferência", "envio", "autorização", "notificação",
        "mb way", "mbway", "moey", "moey!", "carteira", "banco",
        "alerta", "aviso", "caixadirecta", "cgd", "caixa", "millennium", "millennium bcp",
        "bcp", "santander", "activobank", "revolut", "novo banco", "novobanco", "montepio",
        "bankinter", "bpi", "ctt", "banco ctt", "cartão", "cartao", "cartão de crédito",
        "cartao de credito", "cartão de débito", "cartao de debito", "crédito", "credito",
        "débito", "debito", "transação", "transacao", "movimento", "extrato"
    )

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        subText: String? = null
    ): ParsedBankNotification? {
        val safeTitle = title?.trim().orEmpty()
        val safeText = text?.trim().orEmpty()
        val safeSubText = subText?.trim().orEmpty()
        val fullContent = "$safeTitle $safeText $safeSubText".trim()

        if (fullContent.isBlank()) return null

        val isFinancial = isKnownFinancialApp(packageName)
        val amountCents = extractAmountCents(fullContent, isFinancial) ?: return null
        if (amountCents <= 0L) return null

        val (sourceApp, defaultMethod) = resolveSourceAndMethod(packageName, fullContent)

        val merchant = extractMerchant(safeTitle, safeText, safeSubText)
        val finalMerchant = if (merchant.isBlank()) "Comerciante Desconhecido" else merchant

        val finalMethod = if (fullContent.contains("mb way", ignoreCase = true) || fullContent.contains("mbway", ignoreCase = true)) {
            "MBWAY"
        } else {
            defaultMethod
        }

        return ParsedBankNotification(
            amountCents = amountCents,
            merchant = finalMerchant,
            paymentMethod = finalMethod,
            sourceApp = sourceApp,
            direction = detectDirection(fullContent),
            rawText = fullContent
        )
    }

    private fun detectDirection(content: String): String {
        val lower = content.lowercase()
        val firstIncomeIndex = incomeDirectionKeywords
            .map { lower.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()
            ?: Int.MAX_VALUE
        val firstExpenseIndex = expenseDirectionKeywords
            .map { lower.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()
            ?: Int.MAX_VALUE
        return if (firstIncomeIndex < firstExpenseIndex) "INCOME" else "EXPENSE"
    }

    fun extractAmountCents(text: String, isFinancialApp: Boolean = true): Long? {
        val matchBefore = amountBeforeCurrencyRegex.find(text)
        val matchAfter = amountAfterCurrencyRegex.find(text)
        val matchedGroup = matchBefore?.groupValues?.getOrNull(1)
            ?: matchAfter?.groupValues?.getOrNull(1)
            ?: if (isFinancialApp && (text.contains("€") || text.contains("EUR", ignoreCase = true) || text.contains("pagamento", ignoreCase = true) || text.contains("compra", ignoreCase = true))) {
                decimalNumberRegex.find(text)?.groupValues?.getOrNull(1)
            } else null

        return matchedGroup?.let { parseStringToCents(it) }?.takeIf { it > 0L }
    }

    private fun parseStringToCents(numberStr: String): Long {
        var normalized = numberStr.replace(" ", "").trim()
        if (normalized.contains(".") && normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(",", ".")
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".")
        } else if (normalized.contains(".")) {
            val parts = normalized.split(".")
            if (parts.size == 2 && parts[1].length == 3) {
                normalized = parts[0] + parts[1]
            }
        }
        val parts = normalized.split(".")
        val euros = parts[0].toLongOrNull() ?: return 0L
        val cents = if (parts.size > 1) {
            val c = parts[1].padEnd(2, '0').take(2)
            c.toLongOrNull() ?: 0L
        } else {
            0L
        }
        return euros * 100L + cents
    }

    private fun isKnownFinancialApp(packageName: String): Boolean {
        return FinancialAppFilter.isFinancialApp(packageName)
    }

    private fun extractMerchant(title: String, text: String, subText: String): String {
        // 1. Title-is-merchant pattern (e.g. "Continente", "Uber", "Pingo Doce")
        if (title.isNotBlank()) {
            val cleanTitle = title.trim()
            val normalizedTitle = cleanTitle.lowercase().trimEnd('!', '?', '.', ':', ';', '-', ' ')
            val isGenericTitle = ignoredTitleWords.any { 
                normalizedTitle == it || normalizedTitle.startsWith("$it ") || normalizedTitle.startsWith("$it!") 
            }
            val containsDigitsOnly = cleanTitle.all { it.isDigit() || it.isWhitespace() }
            if (!isGenericTitle && !containsDigitsOnly && !cleanTitle.contains("€") && cleanTitle.length in 2..50) {
                return cleanTitle
            }
        }

        // 2. Regex prefix search in text
        for (regex in merchantPrefixes) {
            val matches = regex.findAll(text)
            for (match in matches) {
                val candidate = match.groupValues[1]
                    .replace(Regex("""(?i)\s+(?:com sucesso|no valor de|com o cartão.*|com o teu cartão.*|com cartão.*|no cartão.*|no teu cartão.*|via mb way.*|no dia.*)"""), "")
                    .replace(Regex("""[.,;:]+$"""), "")
                    .trim()
                if (candidate.isNotBlank() && candidate.length in 2..60 && !candidate.contains("€") && !candidate.matches(Regex("""^\d+[.,]\d+.*"""))) {
                    return candidate
                }
            }
        }

        // 3. Fallback to subText if available and non-generic
        if (subText.isNotBlank()) {
            val lowerSub = subText.lowercase()
            if (!ignoredTitleWords.any { lowerSub.contains(it) } && !subText.contains("€")) {
                return subText.trim()
            }
        }

        return ""
    }

    private fun resolveSourceAndMethod(packageName: String, content: String): Pair<String, String> {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("mbway") -> Pair("MB WAY", "MBWAY")
            pkg.contains("moey") -> Pair("Moey", if (content.contains("mb way", ignoreCase = true)) "MBWAY" else "CARD")
            pkg.contains("cgd") || pkg.contains("caixadireta") -> Pair("Caixa Geral de Depósitos", "CARD")
            pkg.contains("millennium") || pkg.contains("bcp") -> Pair("Millennium BCP", "CARD")
            pkg.contains("santander") -> Pair("Santander", "CARD")
            pkg.contains("activobank") -> Pair("ActivoBank", "CARD")
            pkg.contains("revolut") -> Pair("Revolut", "CARD")
            pkg.contains("novobanco") -> Pair("Novo Banco", "CARD")
            pkg.contains("montepio") -> Pair("Montepio", "CARD")
            pkg.contains("bankinter") -> Pair("Bankinter", "CARD")
            pkg.contains("bpi") -> Pair("BPI", "CARD")
            pkg.contains("bancoctt") || pkg.contains("ctt") -> Pair("Banco CTT", "CARD")
            pkg.contains("xtb") -> Pair("XTB", "TRANSFER")
            pkg.contains("traderepublic") -> Pair("Trade Republic", "TRANSFER")
            pkg.contains("degiro") -> Pair("DEGIRO", "TRANSFER")
            pkg.contains("wise") || pkg.contains("transferwise") -> Pair("Wise", "TRANSFER")
            pkg.contains("n26") || pkg.contains("number26") -> Pair("N26", "CARD")
            pkg.contains("paypal") -> Pair("PayPal", "OTHER")
            pkg.contains("coverflex") || pkg.contains("edenred") || pkg.contains("pluxee") || pkg.contains("sodexo") || pkg.contains("ticket") -> Pair("Cartão Refeição", "CARD")
            pkg.contains("openbank") -> Pair("Openbank", "CARD")
            pkg.contains("wizink") -> Pair("WiZink", "CARD")
            pkg.contains("unibanco") -> Pair("Unibanco", "CARD")
            pkg.contains("cetelem") -> Pair("Cetelem", "CARD")
            pkg.contains("cofidis") -> Pair("Cofidis", "CARD")
            pkg.contains("curve") -> Pair("Curve", "CARD")
            else -> Pair("Banco", "CARD")
        }
    }
}
