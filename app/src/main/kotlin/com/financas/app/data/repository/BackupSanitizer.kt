package com.financas.app.data.repository

import com.financas.app.domain.model.AppSettingsBackup
import kotlin.math.abs

data class SanitizedBackup(val data: BackupData, val droppedRecords: Int)

object BackupSanitizer {

    const val MAX_AMOUNT_CENTS = 10_000_000_000_000L
    const val MAX_TRANSACTIONS = 500_000
    const val MAX_CATEGORIES = 5_000
    const val MAX_OTHER_RECORDS = 5_000
    const val MAX_ID_CHARS = 96
    const val MAX_LABEL_CHARS = 200
    const val MAX_NOTES_CHARS = 2_000
    const val MAX_CONTEXT_TAGS = 100
    const val MAX_TAG_CHARS = 40
    private const val MIN_YEAR = 1970
    private const val MAX_YEAR = 2200
    private const val MIN_EPOCH_DAY = 0L
    private const val MAX_EPOCH_DAY = 84_371L
    private val CODE_REGEX = Regex("^[A-Z0-9_]{1,32}$")
    private val THEME_MODES = setOf("SYSTEM", "LIGHT", "DARK")
    private val LANGUAGES = setOf("EN", "PT")

    fun sanitize(data: BackupData): SanitizedBackup {
        require(
            data.transactions.size <= MAX_TRANSACTIONS &&
                data.categories.size <= MAX_CATEGORIES &&
                data.budgets.size <= MAX_OTHER_RECORDS &&
                data.goals.size <= MAX_OTHER_RECORDS &&
                data.investments.size <= MAX_OTHER_RECORDS
        ) { "Backup inválido: demasiados registos." }

        val categories = data.categories.filter {
            validId(it.id) && (it.parentId == null || validId(it.parentId)) && CODE_REGEX.matches(it.type)
        }.map { it.copy(name = it.name.take(MAX_LABEL_CHARS), icon = it.icon.take(MAX_ID_CHARS)) }

        val transactions = data.transactions.filter {
            it.id >= 0L &&
                CODE_REGEX.matches(it.type) &&
                validAmount(it.amountCents) &&
                validYearMonth(it.yearMonth) &&
                it.epochDay in MIN_EPOCH_DAY..MAX_EPOCH_DAY &&
                it.categoryId.length <= MAX_ID_CHARS &&
                (it.investmentAssetId == null || validId(it.investmentAssetId))
        }.map {
            it.copy(
                contextTag = it.contextTag.take(MAX_LABEL_CHARS),
                merchant = it.merchant.take(MAX_LABEL_CHARS),
                paymentMethod = it.paymentMethod.take(MAX_LABEL_CHARS)
            )
        }

        val budgets = data.budgets.filter {
            validId(it.id) && it.categoryId.length <= MAX_ID_CHARS && it.limitCents in 0L..MAX_AMOUNT_CENTS
        }

        val goals = data.goals.filter {
            validId(it.id) &&
                it.targetCents in 0L..MAX_AMOUNT_CENTS &&
                validYearMonth(it.startYearMonth) &&
                (it.deadlineYearMonth == null || validYearMonth(it.deadlineYearMonth))
        }.map { it.copy(name = it.name.take(MAX_LABEL_CHARS)) }

        val investments = data.investments.filter {
            validId(it.id) &&
                CODE_REGEX.matches(it.type) &&
                validAmount(it.investedCents) &&
                validAmount(it.currentValueCents) &&
                validAmount(it.monthlyContributionCents) &&
                (it.targetCents == null || validAmount(it.targetCents)) &&
                it.lastUpdatedEpochDay in MIN_EPOCH_DAY..MAX_EPOCH_DAY
        }.map {
            it.copy(
                name = it.name.take(MAX_LABEL_CHARS),
                institution = it.institution.take(MAX_LABEL_CHARS),
                notes = it.notes.take(MAX_NOTES_CHARS)
            )
        }

        val dropped = (data.categories.size - categories.size) +
            (data.transactions.size - transactions.size) +
            (data.budgets.size - budgets.size) +
            (data.goals.size - goals.size) +
            (data.investments.size - investments.size)

        return SanitizedBackup(
            data = data.copy(
                categories = categories,
                transactions = transactions,
                budgets = budgets,
                goals = goals,
                investments = investments,
                settings = data.settings?.let(::sanitizeSettings)
            ),
            droppedRecords = dropped
        )
    }

    fun sanitizeSettings(settings: AppSettingsBackup): AppSettingsBackup = settings.copy(
        userName = settings.userName.take(80),
        defaultCurrency = settings.defaultCurrency.take(16),
        monthlyCycleDay = settings.monthlyCycleDay.coerceIn(1, 28),
        themeMode = settings.themeMode.takeIf { it in THEME_MODES } ?: "DARK",
        appLanguage = settings.appLanguage.uppercase().takeIf { it in LANGUAGES } ?: "EN",
        customContextTags = settings.customContextTags
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= MAX_TAG_CHARS }
            .distinct()
            .take(MAX_CONTEXT_TAGS)
    )

    private fun validId(id: String): Boolean = id.isNotBlank() && id.length <= MAX_ID_CHARS

    private fun validAmount(cents: Long): Boolean = cents != Long.MIN_VALUE && abs(cents) <= MAX_AMOUNT_CENTS

    private fun validYearMonth(yearMonth: Int): Boolean =
        yearMonth / 100 in MIN_YEAR..MAX_YEAR && yearMonth % 100 in 1..12
}
