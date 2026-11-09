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

    private val merchantChars = """[\p{L}0-9\s&'.*/+-]"""

    private val merchantPrefixes = listOf(
        Regex("""(?i)\b(?:no comerciante|no estabelecimento|efetuado a|efetuada em|efetuada no|efetuada na|via mb way para|recebido de|recebida de|recebeste de|recebeu de|enviado a|enviada a|enviado para|enviada para|pago a|pago à|pago em|compra em|compra no|compra na|junto de|spent at|paid to|received from|sent to)\s+($merchantChars+)"""),
        Regex("""(?i)(?<![\p{L}])(?:para|em|no|na|nos|nas|a|à|ao|aos|de|do|da|at|from|to)\s+($merchantChars+)"""),
        Regex("""\s[-–:]\s+($merchantChars+)""")
    )

    private val cardReferenceRegex = Regex("""(?i)(?:\b(?:no|com o|com|pelo|do)\s+)?(?:(?:teu|seu)\s+)?(?:cart[aã]o|card)?\s*(?:de (?:cr[eé]dito|d[eé]bito)\s*)?(?:terminado em|terminada em|ending in|n\.?º)?\s*[*xX•]{2,}\s?\d{2,4}""")

    private val trailingNoiseRegex = Regex(
        """(?i)\s+(?:com sucesso|no valor de.*|com o cart[aã]o.*|com o teu cart[aã]o.*|com cart[aã]o.*|no cart[aã]o.*|no teu cart[aã]o.*|via mb ?way.*|por mb ?way.*|pelo mb ?way.*|no dia.*|em \d{1,2}[/-]\d{1,2}.*|[aà]s? \d{1,2}(?:[:h]\d{2})?.*|foi .*|está .*|esta .*|was .*|has been .*|on \d.*)$"""
    )

    private val uppercaseRunRegex = Regex("""\b[A-Z0-9][A-Z0-9&'.*/ -]{2,}[A-Z0-9]\b""")

    private val rejectedMerchantStarts = listOf(
        "cartão", "cartao", "conta", "sua conta", "tua conta", "seu cartão", "teu cartão", "saldo", "card", "your", "a sua", "o seu"
    )

    private val ignoredTitleWords = setOf(
        "pagamento", "compra", "transferência", "envio", "autorização", "notificação",
        "mb way", "mbway", "moey", "moey!", "carteira", "banco",
        "alerta", "aviso", "caixadirecta", "cgd", "caixa", "millennium", "millennium bcp",
        "bcp", "santander", "activobank", "revolut", "novo banco", "novobanco", "montepio",
        "bankinter", "bpi", "ctt", "banco ctt", "cartão", "cartao", "cartão de crédito",
        "cartao de credito", "cartão de débito", "cartao de debito", "crédito", "credito",
        "débito", "debito", "transação", "transacao", "movimento", "extrato",
        "eur", "euro", "euros", "multibanco", "recebimento", "transferência recebida",
        "paypal", "xtb", "caixa geral de depósitos", "sibs"
    )

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        subText: String? = null,
        extraLines: List<String> = emptyList()
    ): ParsedBankNotification? {
        val safeTitle = title?.trim().orEmpty()
        val safeText = text?.trim().orEmpty()
        val safeSubText = subText?.trim().orEmpty()
        val safeExtras = extraLines.map { it.trim() }.filter { it.isNotBlank() && !safeText.contains(it) }.distinct()
        val fullContent = listOf(safeTitle, safeText, safeSubText).plus(safeExtras).filter { it.isNotBlank() }.joinToString(" ").trim()

        if (fullContent.isBlank()) return null

        val isFinancial = isKnownFinancialApp(packageName)
        val amountCents = extractAmountCents(fullContent, isFinancial) ?: return null
        if (amountCents <= 0L) return null

        val (sourceApp, defaultMethod) = resolveSourceAndMethod(packageName, fullContent)

        val direction = detectDirection(fullContent)
        val merchant = extractMerchant(safeTitle, listOf(safeText) + safeExtras, safeSubText)
        val finalMerchant = merchant.ifBlank { fallbackMerchant(sourceApp, direction) }

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
            direction = direction,
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

    fun isFallbackMerchant(merchant: String): Boolean =
        merchant.startsWith(FALLBACK_EXPENSE_PREFIX) || merchant.startsWith(FALLBACK_INCOME_PREFIX)

    private fun fallbackMerchant(sourceApp: String, direction: String): String =
        if (direction == "INCOME") "$FALLBACK_INCOME_PREFIX$sourceApp" else "$FALLBACK_EXPENSE_PREFIX$sourceApp"

    private fun extractMerchant(title: String, texts: List<String>, subText: String): String {
        if (title.isNotBlank()) {
            val cleanTitle = stripEmoji(title).trim()
            val normalizedTitle = cleanTitle.lowercase().trimEnd('!', '?', '.', ':', ';', '-', ' ')
            val isGenericTitle = ignoredTitleWords.any {
                normalizedTitle == it || normalizedTitle.startsWith("$it ") || normalizedTitle.startsWith("$it!") || normalizedTitle.startsWith("$it:")
            }
            val looksLikeSentence = DIRECTION_WORDS.any { normalizedTitle.contains(it) }
            val containsDigitsOnly = cleanTitle.all { it.isDigit() || it.isWhitespace() }
            if (!isGenericTitle && !looksLikeSentence && !containsDigitsOnly && !cleanTitle.contains("€") && cleanTitle.length in 2..50) {
                return prettify(cleanTitle)
            }
        }

        val cleanedTexts = texts.filter { it.isNotBlank() }.map { cardReferenceRegex.replace(it, " ").replace(Regex("""\s{2,}"""), " ") }

        for (regex in merchantPrefixes) {
            for (text in cleanedTexts) {
                for (match in regex.findAll(text)) {
                    val candidate = cleanCandidate(match.groupValues[1])
                    if (isPlausibleMerchant(candidate)) return prettify(candidate)
                }
            }
        }

        for (text in cleanedTexts) {
            val upper = uppercaseRunRegex.findAll(text)
                .map { cleanCandidate(it.value) }
                .filter { isPlausibleMerchant(it) && it.count { c -> c.isLetter() } >= 3 }
                .maxByOrNull { it.length }
            if (upper != null) return prettify(upper)
        }

        if (subText.isNotBlank()) {
            val lowerSub = subText.lowercase()
            if (!ignoredTitleWords.any { lowerSub.contains(it) } && !subText.contains("€")) {
                return prettify(subText.trim())
            }
        }

        return ""
    }

    private fun cleanCandidate(raw: String): String {
        var candidate = raw.trim()
        repeat(3) { candidate = trailingNoiseRegex.replace(candidate, "").trim() }
        return candidate
            .replace(Regex("""[.,;:*/+\-–\s]+$"""), "")
            .replace(Regex("""^[*/+\-–\s]+"""), "")
            .replace(Regex("""(?i)^(?:(?:em|no|na|de|do|da|a|à|ao|at)\s+)+"""), "")
            .trim()
    }

    private fun isPlausibleMerchant(candidate: String): Boolean {
        if (candidate.isBlank() || candidate.length !in 2..60) return false
        if (candidate.contains("€")) return false
        if (candidate.matches(Regex("""^\d+[.,]\d+.*"""))) return false
        if (candidate.none { it.isLetterOrDigit() }) return false
        if (candidate.all { it.isDigit() } && candidate.length < 6) return false
        val lower = candidate.lowercase()
        if (ignoredTitleWords.contains(lower)) return false
        if (rejectedMerchantStarts.any { lower == it || lower.startsWith("$it ") }) return false
        if (lower.matches(Regex("""^(eur|euros?)\b.*"""))) return false
        return true
    }

    private fun prettify(merchant: String): String {
        val letters = merchant.filter { it.isLetter() }
        val words = merchant.split(" ").filter { it.isNotBlank() }
        if (words.size < 2 || letters.length < 8 || letters.any { it.isLowerCase() }) return merchant
        return words.joinToString(" ") { word ->
            val lower = word.lowercase()
            when {
                lower in setOf("de", "da", "do", "dos", "das", "e") -> lower
                word.length <= 2 && word.all { it.isLetter() } -> word
                else -> {
                    val firstLetter = lower.indexOfFirst { it.isLetter() }
                    if (firstLetter < 0) lower
                    else lower.substring(0, firstLetter) + lower[firstLetter].titlecase() + lower.substring(firstLetter + 1)
                }
            }
        }
    }

    private fun stripEmoji(value: String): String =
        value.filter { ch -> Character.getType(ch) != Character.SURROGATE.toInt() && Character.getType(ch) != Character.OTHER_SYMBOL.toInt() }

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

    companion object {
        const val FALLBACK_EXPENSE_PREFIX = "Pagamento "
        const val FALLBACK_INCOME_PREFIX = "Recebimento "
        private val DIRECTION_WORDS = listOf("pagou", "pagaste", "enviou", "enviaste", "recebeu", "recebeste", "compra de", "you spent", "you paid")
    }
}
