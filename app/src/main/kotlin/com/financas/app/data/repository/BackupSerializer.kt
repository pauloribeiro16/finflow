package com.financas.app.data.repository

import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import java.time.LocalDate
import com.financas.app.domain.model.AppSettingsBackup
import java.util.Locale

data class BackupData(
    val version: Int,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val investments: List<InvestmentAssetEntity> = emptyList(),
    val settings: AppSettingsBackup? = null
)

data class RestoreResult(
    val categoriesImported: Int,
    val transactionsImported: Int,
    val transactionsSkipped: Int,
    val budgetsImported: Int = 0,
    val goalsImported: Int = 0,
    val investmentsImported: Int = 0
)

class BackupSerializer {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        const val LEGACY_SCHEMA_VERSION = 1
        private val FORMULA_TRIGGERS = setOf('=', '+', '-', '@', '\t', '\r')
        val CSV_HEADER = "Data;Mês;Tipo;Valor €;Categoria;Subcategoria;Onde (Contexto);Estabelecimento/Nota;Como (Método);Fixo"
    }

    fun exportToJson(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity> = emptyList(),
        goals: List<GoalEntity> = emptyList(),
        investments: List<InvestmentAssetEntity> = emptyList(),
        version: Int = CURRENT_SCHEMA_VERSION,
        settings: AppSettingsBackup? = null
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": ").append(version).append(",\n")
        sb.append("  \"categories\": [\n")
        categories.forEachIndexed { index, cat ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escapeJsonString(cat.id)).append(",\n")
            sb.append("      \"parentId\": ").append(cat.parentId?.let { escapeJsonString(it) } ?: "null").append(",\n")
            sb.append("      \"name\": ").append(escapeJsonString(cat.name)).append(",\n")
            sb.append("      \"icon\": ").append(escapeJsonString(cat.icon)).append(",\n")
            sb.append("      \"colorHex\": ").append(cat.colorHex).append(",\n")
            sb.append("      \"type\": ").append(escapeJsonString(cat.type)).append(",\n")
            sb.append("      \"isDefaultFixed\": ").append(cat.isDefaultFixed).append(",\n")
            sb.append("      \"sortOrder\": ").append(cat.sortOrder).append("\n")
            sb.append("    }").append(if (index < categories.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"transactions\": [\n")
        transactions.forEachIndexed { index, tx ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(tx.id).append(",\n")
            sb.append("      \"type\": ").append(escapeJsonString(tx.type)).append(",\n")
            sb.append("      \"amountCents\": ").append(tx.amountCents).append(",\n")
            sb.append("      \"yearMonth\": ").append(tx.yearMonth).append(",\n")
            sb.append("      \"epochDay\": ").append(tx.epochDay).append(",\n")
            sb.append("      \"categoryId\": ").append(escapeJsonString(tx.categoryId)).append(",\n")
            sb.append("      \"contextTag\": ").append(escapeJsonString(tx.contextTag)).append(",\n")
            sb.append("      \"merchant\": ").append(escapeJsonString(tx.merchant)).append(",\n")
            sb.append("      \"paymentMethod\": ").append(escapeJsonString(tx.paymentMethod)).append(",\n")
            sb.append("      \"isFixed\": ").append(tx.isFixed)
            if (tx.linkedTransactionId != null) {
                sb.append(",\n      \"linkedTransactionId\": ").append(tx.linkedTransactionId)
            }
            if (tx.investmentAssetId != null) {
                sb.append(",\n      \"investmentAssetId\": ").append(escapeJsonString(tx.investmentAssetId))
            }
            sb.append("\n    }").append(if (index < transactions.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"budgets\": [\n")
        budgets.forEachIndexed { index, budget ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escapeJsonString(budget.id)).append(",\n")
            sb.append("      \"categoryId\": ").append(escapeJsonString(budget.categoryId)).append(",\n")
            sb.append("      \"limitCents\": ").append(budget.limitCents).append(",\n")
            sb.append("      \"isActive\": ").append(budget.isActive).append("\n")
            sb.append("    }").append(if (index < budgets.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"goals\": [\n")
        goals.forEachIndexed { index, goal ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escapeJsonString(goal.id)).append(",\n")
            sb.append("      \"name\": ").append(escapeJsonString(goal.name)).append(",\n")
            sb.append("      \"targetCents\": ").append(goal.targetCents).append(",\n")
            sb.append("      \"startYearMonth\": ").append(goal.startYearMonth).append(",\n")
            sb.append("      \"deadlineYearMonth\": ").append(goal.deadlineYearMonth?.toString() ?: "null").append(",\n")
            sb.append("      \"archived\": ").append(goal.archived).append("\n")
            sb.append("    }").append(if (index < goals.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"investments\": [\n")
        investments.forEachIndexed { index, asset ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(escapeJsonString(asset.id)).append(",\n")
            sb.append("      \"name\": ").append(escapeJsonString(asset.name)).append(",\n")
            sb.append("      \"type\": ").append(escapeJsonString(asset.type)).append(",\n")
            sb.append("      \"institution\": ").append(escapeJsonString(asset.institution)).append(",\n")
            sb.append("      \"investedCents\": ").append(asset.investedCents).append(",\n")
            sb.append("      \"currentValueCents\": ").append(asset.currentValueCents).append(",\n")
            sb.append("      \"monthlyContributionCents\": ").append(asset.monthlyContributionCents).append(",\n")
            sb.append("      \"targetCents\": ").append(asset.targetCents?.toString() ?: "null").append(",\n")
            sb.append("      \"notes\": ").append(escapeJsonString(asset.notes)).append(",\n")
            sb.append("      \"lastUpdatedEpochDay\": ").append(asset.lastUpdatedEpochDay).append(",\n")
            sb.append("      \"ticker\": ").append(asset.ticker?.let { escapeJsonString(it) } ?: "null").append(",\n")
            sb.append("      \"units\": ").append(asset.units?.toString() ?: "null").append(",\n")
            sb.append("      \"archived\": ").append(asset.archived).append("\n")
            sb.append("    }").append(if (index < investments.size - 1) "," else "").append("\n")
        }
        sb.append("  ]")
        if (settings != null) {
            sb.append(",\n  \"settings\": {\n")
            sb.append("    \"userName\": ").append(escapeJsonString(settings.userName)).append(",\n")
            sb.append("    \"defaultCurrency\": ").append(escapeJsonString(settings.defaultCurrency)).append(",\n")
            sb.append("    \"monthlyCycleDay\": ").append(settings.monthlyCycleDay).append(",\n")
            sb.append("    \"themeMode\": ").append(escapeJsonString(settings.themeMode)).append(",\n")
            sb.append("    \"appLanguage\": ").append(escapeJsonString(settings.appLanguage)).append(",\n")
            sb.append("    \"biometricsEnabled\": ").append(settings.biometricsEnabled).append(",\n")
            sb.append("    \"notificationCaptureEnabled\": ").append(settings.notificationCaptureEnabled).append(",\n")
            sb.append("    \"customContextTags\": [")
                .append(settings.customContextTags.joinToString(", ") { escapeJsonString(it) }).append("]")
            if (settings.profilePhotoBase64 != null) {
                sb.append(",\n    \"profilePhotoBase64\": ").append(escapeJsonString(settings.profilePhotoBase64))
            }
            sb.append("\n  }")
        }
        sb.append("\n}")
        return sb.toString()
    }

    fun importFromJson(jsonString: String): BackupData {
        val root = JsonParser(jsonString).parse().asObject()
        val version = root["version"]?.asInt(LEGACY_SCHEMA_VERSION) ?: LEGACY_SCHEMA_VERSION

        val categoriesJson = root["categories"]?.asArray() ?: emptyList()
        val categories = categoriesJson.mapNotNull { item ->
            val obj = item.asObject()
            val id = obj["id"]?.asString() ?: return@mapNotNull null
            val parentId = obj["parentId"]?.asString()
            val name = obj["name"]?.asString() ?: ""
            val icon = obj["icon"]?.asString() ?: "category"
            val colorHex = obj["colorHex"]?.asLong(0xFF607D8B) ?: 0xFF607D8B
            val type = obj["type"]?.asString() ?: "EXPENSE"
            val isDefaultFixed = obj["isDefaultFixed"]?.asBoolean(false) ?: false
            val sortOrder = obj["sortOrder"]?.asInt(0) ?: 0
            CategoryEntity(
                id = id,
                parentId = parentId,
                name = name,
                icon = icon,
                colorHex = colorHex,
                type = type,
                isDefaultFixed = isDefaultFixed,
                sortOrder = sortOrder
            )
        }

        val transactionsJson = root["transactions"]?.asArray() ?: emptyList()
        val transactions = transactionsJson.mapNotNull { item ->
            val obj = item.asObject()
            val id = obj["id"]?.asLong(0L) ?: 0L
            val type = obj["type"]?.asString() ?: "EXPENSE"
            val amountCents = obj["amountCents"]?.asLong(0L) ?: 0L
            val yearMonth = obj["yearMonth"]?.asInt(0) ?: 0
            val epochDay = obj["epochDay"]?.asLong(0L) ?: 0L
            val categoryId = obj["categoryId"]?.asString() ?: ""
            val contextTag = obj["contextTag"]?.asString() ?: ""
            val merchant = obj["merchant"]?.asString() ?: ""
            val paymentMethod = obj["paymentMethod"]?.asString() ?: ""
            val isFixed = obj["isFixed"]?.asBoolean(false) ?: false
            val linkedTransactionId = obj["linkedTransactionId"]?.asLongOrNull()
            val investmentAssetId = obj["investmentAssetId"]?.asString()?.takeIf { it.isNotBlank() }
            TransactionEntity(
                id = id,
                type = type,
                amountCents = amountCents,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = categoryId,
                contextTag = contextTag,
                merchant = merchant,
                paymentMethod = paymentMethod,
                isFixed = isFixed,
                linkedTransactionId = linkedTransactionId,
                investmentAssetId = investmentAssetId
            )
        }

        val budgetsJson = root["budgets"]?.asArray() ?: emptyList()
        val budgets = budgetsJson.mapNotNull { item ->
            val obj = item.asObject()
            val categoryId = obj["categoryId"]?.asString() ?: ""
            val rawId = obj["id"]?.asString()
            val id = when {
                !rawId.isNullOrBlank() -> rawId
                categoryId.isNotBlank() -> "budget_$categoryId"
                else -> return@mapNotNull null
            }
            BudgetEntity(
                id = id,
                categoryId = categoryId,
                limitCents = obj["limitCents"]?.asLong(0L) ?: 0L,
                isActive = obj["isActive"]?.asBoolean(true) ?: true
            )
        }

        val goalsJson = root["goals"]?.asArray() ?: emptyList()
        val goals = goalsJson.mapNotNull { item ->
            val obj = item.asObject()
            val name = obj["name"]?.asString() ?: ""
            val startYearMonth = obj["startYearMonth"]?.asInt(0) ?: 0
            val rawId = obj["id"]?.asString()
            val id = when {
                !rawId.isNullOrBlank() -> rawId
                name.isNotBlank() -> GoalEntity.deterministicId(name, startYearMonth)
                else -> return@mapNotNull null
            }
            GoalEntity(
                id = id,
                name = name,
                targetCents = obj["targetCents"]?.asLong(0L) ?: 0L,
                startYearMonth = startYearMonth,
                deadlineYearMonth = obj["deadlineYearMonth"]?.asIntOrNull(),
                archived = obj["archived"]?.asBoolean(false) ?: false
            )
        }

        val investmentsJson = root["investments"]?.asArray() ?: emptyList()
        val investments = investmentsJson.mapNotNull { item ->
            val obj = item.asObject()
            val id = obj["id"]?.asString() ?: return@mapNotNull null
            val name = obj["name"]?.asString() ?: ""
            val type = obj["type"]?.asString() ?: "OUTRO"
            val institution = obj["institution"]?.asString() ?: ""
            val investedCents = obj["investedCents"]?.asLong(0L) ?: 0L
            val currentValueCents = obj["currentValueCents"]?.asLong(0L) ?: 0L
            val monthlyContributionCents = obj["monthlyContributionCents"]?.asLong(0L) ?: 0L
            val targetCents = obj["targetCents"]?.asLongOrNull()
            val notes = obj["notes"]?.asString() ?: ""
            val lastUpdatedEpochDay = obj["lastUpdatedEpochDay"]?.asLong(LocalDate.now().toEpochDay()) ?: LocalDate.now().toEpochDay()
            val archived = obj["archived"]?.asBoolean(false) ?: false
            val ticker = com.financas.app.data.network.TickerPolicy.normalize(obj["ticker"]?.asString())
            val units = obj["units"]?.asDoubleOrNull()?.takeIf {
                it.isFinite() && it > 0.0 && it <= com.financas.app.domain.usecase.PortfolioMarketSyncUseCase.MAX_UNITS
            }

            InvestmentAssetEntity(
                id = id,
                name = name,
                type = type,
                institution = institution,
                investedCents = investedCents,
                currentValueCents = currentValueCents,
                monthlyContributionCents = monthlyContributionCents,
                targetCents = targetCents,
                notes = notes,
                lastUpdatedEpochDay = lastUpdatedEpochDay,
                archived = archived,
                ticker = ticker,
                units = units
            )
        }

        val settingsObj = root["settings"]?.asObject()
        val settings = if (settingsObj != null && settingsObj.isNotEmpty()) {
            AppSettingsBackup(
                userName = settingsObj["userName"]?.asString() ?: "",
                defaultCurrency = settingsObj["defaultCurrency"]?.asString() ?: "EUR (€)",
                monthlyCycleDay = settingsObj["monthlyCycleDay"]?.asInt(1) ?: 1,
                themeMode = settingsObj["themeMode"]?.asString() ?: "SYSTEM",
                appLanguage = settingsObj["appLanguage"]?.asString() ?: "EN",
                biometricsEnabled = settingsObj["biometricsEnabled"]?.asBoolean(false) ?: false,
                notificationCaptureEnabled = settingsObj["notificationCaptureEnabled"]?.asBoolean(true) ?: true,
                customContextTags = settingsObj["customContextTags"]?.asArray()?.mapNotNull { it.asString() } ?: emptyList(),
                profilePhotoBase64 = settingsObj["profilePhotoBase64"]?.asString()?.takeIf { it.isNotBlank() }
            )
        } else {
            null
        }

        return BackupData(
            version = version,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            investments = investments,
            settings = settings
        )
    }

    fun exportToCsv(
        transactions: List<TransactionEntity>,
        categoryMap: Map<String, CategoryEntity>
    ): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

        transactions.forEach { tx ->
            val dateStr = try {
                LocalDate.ofEpochDay(tx.epochDay).toString()
            } catch (e: Exception) {
                ""
            }
            val monthStr = if (tx.yearMonth > 0) {
                String.format(Locale.ROOT, "%04d-%02d", tx.yearMonth / 100, tx.yearMonth % 100)
            } else {
                ""
            }
            val amountPt = formatAmountPt(tx.amountCents)

            val cat = categoryMap[tx.categoryId]
            val parentCategoryName: String
            val subcategoryName: String

            when {
                cat == null -> {
                    parentCategoryName = ""
                    subcategoryName = ""
                }
                cat.parentId == null -> {
                    parentCategoryName = cat.name
                    subcategoryName = ""
                }
                else -> {
                    val parent = categoryMap[cat.parentId]
                    parentCategoryName = parent?.name ?: ""
                    subcategoryName = cat.name
                }
            }

            val fixedStr = if (tx.isFixed) "Sim" else "Não"

            val line = listOf(
                escapeCsv(dateStr),
                escapeCsv(monthStr),
                escapeCsv(tx.type),
                escapeCsv(amountPt),
                escapeCsv(parentCategoryName),
                escapeCsv(subcategoryName),
                escapeCsv(tx.contextTag),
                escapeCsv(tx.merchant),
                escapeCsv(tx.paymentMethod),
                escapeCsv(fixedStr)
            ).joinToString(";")

            sb.append(line).append("\n")
        }

        return sb.toString()
    }

    suspend fun restoreBackup(
        backupData: BackupData,
        categoryDao: CategoryDao,
        transactionDao: TransactionDao,
        budgetDao: BudgetDao? = null,
        goalDao: GoalDao? = null,
        investmentAssetDao: InvestmentAssetDao? = null
    ): RestoreResult {
        if (backupData.categories.isNotEmpty()) {
            categoryDao.insertAll(backupData.categories)
        }

        val existingTransactions = transactionDao.getAllSync()

        val (duplicates, newTransactions) = backupData.transactions.partition { incoming ->
            existingTransactions.any { existing -> isDuplicateTransaction(existing, incoming) }
        }

        if (newTransactions.isNotEmpty()) {
            transactionDao.insertAll(newTransactions)
        }

        if (budgetDao != null && backupData.budgets.isNotEmpty()) {
            for (budget in backupData.budgets) {
                budgetDao.insert(budget)
            }
        }

        if (goalDao != null && backupData.goals.isNotEmpty()) {
            val existingGoals = goalDao.getAllSync()
            val consumedGoalIds = mutableSetOf<String>()
            for (incoming in backupData.goals) {
                val match = existingGoals.firstOrNull { it.id !in consumedGoalIds && it.id == incoming.id }
                    ?: existingGoals.firstOrNull {
                        it.id !in consumedGoalIds &&
                            it.name == incoming.name &&
                            it.startYearMonth == incoming.startYearMonth
                    }
                val toPersist = if (match != null) {
                    consumedGoalIds.add(match.id)
                    incoming.copy(id = match.id)
                } else {
                    incoming
                }
                goalDao.insert(toPersist)
            }
        }

        if (investmentAssetDao != null && backupData.investments.isNotEmpty()) {
            for (asset in backupData.investments) {
                investmentAssetDao.insert(asset)
            }
        }

        return RestoreResult(
            categoriesImported = backupData.categories.size,
            transactionsImported = newTransactions.size,
            transactionsSkipped = duplicates.size,
            budgetsImported = if (budgetDao != null) backupData.budgets.size else 0,
            goalsImported = if (goalDao != null) backupData.goals.size else 0,
            investmentsImported = if (investmentAssetDao != null) backupData.investments.size else 0
        )
    }

    fun isDuplicateTransaction(a: TransactionEntity, b: TransactionEntity): Boolean {
        if (a.id > 0 && b.id > 0 && a.id == b.id) return true
        return a.type == b.type &&
                a.amountCents == b.amountCents &&
                a.epochDay == b.epochDay &&
                a.categoryId == b.categoryId &&
                a.contextTag == b.contextTag &&
                a.merchant == b.merchant &&
                a.paymentMethod == b.paymentMethod &&
                a.isFixed == b.isFixed &&
                a.linkedTransactionId == b.linkedTransactionId &&
                a.investmentAssetId == b.investmentAssetId
    }

    private fun formatAmountPt(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val absCents = Math.abs(cents)
        val euros = absCents / 100
        val remainingCents = absCents % 100
        return String.format(Locale.ROOT, "%s%d,%02d", sign, euros, remainingCents)
    }

    private fun escapeCsv(value: String): String {
        val safe = neutralizeFormula(value)
        return if (safe.contains(';') || safe.contains('"') || safe.contains('\n') || safe.contains('\r')) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    internal fun neutralizeFormula(value: String): String {
        if (value.isEmpty()) return value
        if (value.first() !in FORMULA_TRIGGERS) return value
        if (value.first() == '-' && value.drop(1).replace(',', '.').toDoubleOrNull() != null) return value
        return "'$value"
    }

    private fun escapeJsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') {
                    sb.append(String.format(Locale.ROOT, "\\u%04x", ch.code))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}

sealed class JsonValue {
    data class JsonObject(val map: Map<String, JsonValue>) : JsonValue()
    data class JsonArray(val list: List<JsonValue>) : JsonValue()
    data class JsonString(val value: String) : JsonValue()
    data class JsonNumber(val raw: String) : JsonValue() {
        fun toLong(): Long = raw.toLong()
        fun toInt(): Int = raw.toInt()
        fun toDouble(): Double = raw.toDouble()
    }
    data class JsonBoolean(val value: Boolean) : JsonValue()
    object JsonNull : JsonValue()

    fun asObject(): Map<String, JsonValue> = (this as? JsonObject)?.map ?: emptyMap()
    fun asArray(): List<JsonValue> = (this as? JsonArray)?.list ?: emptyList()
    fun asString(): String? = when (this) {
        is JsonString -> value
        is JsonNull -> null
        else -> null
    }
    fun asLong(default: Long = 0L): Long = when (this) {
        is JsonNumber -> raw.toLongOrNull() ?: default
        is JsonString -> value.toLongOrNull() ?: default
        else -> default
    }
    fun asLongOrNull(): Long? = when (this) {
        is JsonNumber -> raw.toLongOrNull()
        is JsonString -> value.toLongOrNull()
        else -> null
    }
    fun asInt(default: Int = 0): Int = when (this) {
        is JsonNumber -> raw.toIntOrNull() ?: default
        is JsonString -> value.toIntOrNull() ?: default
        else -> default
    }
    fun asIntOrNull(): Int? = when (this) {
        is JsonNumber -> raw.toIntOrNull()
        is JsonString -> value.toIntOrNull()
        else -> null
    }
    fun asDoubleOrNull(): Double? = when (this) {
        is JsonNumber -> raw.toDoubleOrNull()
        is JsonString -> value.toDoubleOrNull()
        else -> null
    }
    fun asBoolean(default: Boolean = false): Boolean = when (this) {
        is JsonBoolean -> value
        is JsonString -> value.toBoolean()
        else -> default
    }
}

class JsonParser(private val src: String, private val maxDepth: Int = MAX_DEPTH) {
    private var pos = 0
    private var depth = 0

    companion object {
        const val MAX_DEPTH = 32
    }

    private inline fun <T> nested(block: () -> T): T {
        depth++
        if (depth > maxDepth) throw IllegalArgumentException("JSON demasiado aninhado (máx. $maxDepth níveis)")
        try {
            return block()
        } finally {
            depth--
        }
    }

    private fun skipWhitespace() {
        while (pos < src.length && src[pos].isWhitespace()) {
            pos++
        }
    }

    private fun peek(): Char? {
        skipWhitespace()
        return if (pos < src.length) src[pos] else null
    }

    private fun next(): Char {
        skipWhitespace()
        if (pos >= src.length) throw IllegalArgumentException("Unexpected EOF at pos $pos")
        return src[pos++]
    }

    fun parse(): JsonValue {
        skipWhitespace()
        val ch = peek() ?: throw IllegalArgumentException("Empty JSON input")
        return when (ch) {
            '{' -> nested { parseObject() }
            '[' -> nested { parseArray() }
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> if (ch == '-' || ch.isDigit()) parseNumber() else throw IllegalArgumentException("Unexpected token '$ch' at pos $pos")
        }
    }

    private fun parseObject(): JsonValue.JsonObject {
        next()
        val map = mutableMapOf<String, JsonValue>()
        skipWhitespace()
        if (peek() == '}') {
            next()
            return JsonValue.JsonObject(map)
        }
        while (true) {
            skipWhitespace()
            if (peek() != '"') throw IllegalArgumentException("Expected string key at pos $pos")
            val key = parseString().value
            skipWhitespace()
            val colon = next()
            if (colon != ':') throw IllegalArgumentException("Expected ':' at pos $pos")
            val value = parse()
            map[key] = value
            skipWhitespace()
            val after = peek()
            if (after == ',') {
                next()
            } else if (after == '}') {
                next()
                break
            } else {
                throw IllegalArgumentException("Expected ',' or '}' at pos $pos")
            }
        }
        return JsonValue.JsonObject(map)
    }

    private fun parseArray(): JsonValue.JsonArray {
        next()
        val list = mutableListOf<JsonValue>()
        skipWhitespace()
        if (peek() == ']') {
            next()
            return JsonValue.JsonArray(list)
        }
        while (true) {
            val item = parse()
            list.add(item)
            skipWhitespace()
            val after = peek()
            if (after == ',') {
                next()
            } else if (after == ']') {
                next()
                break
            } else {
                throw IllegalArgumentException("Expected ',' or ']' at pos $pos")
            }
        }
        return JsonValue.JsonArray(list)
    }

    private fun parseString(): JsonValue.JsonString {
        next()
        val sb = StringBuilder()
        while (pos < src.length) {
            val c = src[pos++]
            if (c == '"') {
                return JsonValue.JsonString(sb.toString())
            } else if (c == '\\') {
                if (pos >= src.length) throw IllegalArgumentException("Unterminated escape sequence")
                when (val esc = src[pos++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (pos + 4 > src.length) throw IllegalArgumentException("Incomplete unicode escape")
                        val hex = src.substring(pos, pos + 4)
                        pos += 4
                        sb.append(hex.toInt(16).toChar())
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
        }
        throw IllegalArgumentException("Unterminated string")
    }

    private fun parseNumber(): JsonValue.JsonNumber {
        val start = pos
        if (pos < src.length && src[pos] == '-') pos++
        while (pos < src.length && src[pos].isDigit()) pos++
        if (pos < src.length && src[pos] == '.') {
            pos++
            while (pos < src.length && src[pos].isDigit()) pos++
        }
        if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
            pos++
            if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
            while (pos < src.length && src[pos].isDigit()) pos++
        }
        val raw = src.substring(start, pos)
        return JsonValue.JsonNumber(raw)
    }

    private fun parseBoolean(): JsonValue.JsonBoolean {
        return if (src.startsWith("true", pos)) {
            pos += 4
            JsonValue.JsonBoolean(true)
        } else if (src.startsWith("false", pos)) {
            pos += 5
            JsonValue.JsonBoolean(false)
        } else {
            throw IllegalArgumentException("Invalid boolean at pos $pos")
        }
    }

    private fun parseNull(): JsonValue.JsonNull {
        if (src.startsWith("null", pos)) {
            pos += 4
            return JsonValue.JsonNull
        }
        throw IllegalArgumentException("Invalid null at pos $pos")
    }
}
