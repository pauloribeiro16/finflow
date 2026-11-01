package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.model.DetectedSubscription
import com.financas.app.domain.model.SubscriptionReport

class SubscriptionDetector {

    companion object {
        private val SUBSCRIPTION_CATEGORIES = setOf(
            "exp_leisure_subscriptions",
            "exp_housing_telecom",
            "exp_health_gym",
            "exp_housing_rent",
            "exp_housing_energy",
            "exp_housing_water",
            "exp_utilities"
        )

        private val EXCLUDED_CATEGORIES = setOf(
            "exp_leisure_bars",
            "exp_leisure_culture",
            "exp_leisure_hobbies",
            "exp_food",
            "exp_food_supermarket",
            "exp_food_lunch",
            "exp_food_restaurant",
            "exp_food_cafe",
            "exp_transport_fuel",
            "exp_transport_tolls",
            "exp_transport_public",
            "exp_health_appointments",
            "exp_health_pharmacy",
            "exp_health_personal_care",
            "exp_shopping_clothing",
            "exp_shopping_tech",
            "exp_shopping_education",
            "exp_shopping_gifts",
            "exp_travel",
            "exp_travel_lodging",
            "exp_travel_flights",
            "exp_travel_activities",
            "exp_financial_taxes",
            "exp_financial_other"
        )

        private val EXCLUDED_MERCHANT_KEYWORDS = listOf(
            "saída", "saida", "bar", "restaurante", "restauracao", "restauração", "café", "cafe",
            "jantar", "almoço", "almoco", "copos", "discoteca",
            "continente", "pingo doce", "auchan", "lidl", "intermarche", "aldi", "supermercado", "mercado", "minipreço", "minipreco",
            "iuc", "imposto", "taxa", "finanças", "financas", "multa",
            "phd", "inscrição", "inscricao", "matrícula", "matricula", "propina", "universidade", "faculdade",
            "combustível", "combustivel", "gasóleo", "gasolina", "galp", "repsol", "bp", "cepsa", "via verde", "portagem", "uber", "bolt"
        )

        private val KNOWN_SUBSCRIPTION_KEYWORDS = listOf(
            "netflix", "spotify", "apple", "google", "disney", "hbo", "max", "youtube", "amazon prime", "prime video",
            "chatgpt", "openai", "github", "notion", "adobe", "microsoft", "playstation", "xbox", "nintendo", "dazn", "crunchyroll", "deezer", "tidal",
            "meo", "nos", "vodafone", "nowo", "digi", "altice",
            "edp", "endesa", "iberdrola", "galp energia", "goldenergy", "epal", "águas", "aguas",
            "ginasio", "ginásio", "fitness hut", "solinca", "holmes place", "element", "fitness",
            "senhorio", "renda", "seguro", "fidelidade", "tranquilidade", "allianz", "ageas", "generali", "zurich", "multicare", "médis", "medis"
        )
    }

    fun isSubscriptionCandidate(tx: TransactionEntity): Boolean {
        if (!tx.type.equals("EXPENSE", ignoreCase = true) || tx.amountCents <= 0L) {
            return false
        }
        val merchantLower = tx.merchant.trim().lowercase()

        // 1. Exclude if merchant explicitly contains non-subscription keywords (taxes, dining, tuition, fuel)
        if (EXCLUDED_MERCHANT_KEYWORDS.any { merchantLower.contains(it) }) {
            return false
        }

        // 2. Exclude non-subscription categories unless merchant is explicitly a known subscription service
        val isKnownSubscriptionMerchant = KNOWN_SUBSCRIPTION_KEYWORDS.any { merchantLower.contains(it) }
        if (tx.categoryId in EXCLUDED_CATEGORIES && !isKnownSubscriptionMerchant) {
            return false
        }

        val isSubscriptionCategory = tx.categoryId in SUBSCRIPTION_CATEGORIES
        val isFixedRecurring = tx.isFixed && (
            isSubscriptionCategory ||
            isKnownSubscriptionMerchant ||
            tx.categoryId.startsWith("exp_housing") ||
            tx.categoryId == "exp_transport_maintenance"
        )

        return isSubscriptionCategory || isKnownSubscriptionMerchant || isFixedRecurring
    }

    fun detect(
        transactions: List<TransactionEntity>,
        currentYearMonth: Int = defaultYearMonth(transactions)
    ): SubscriptionReport {
        val expenseTxs = transactions.filter { tx ->
            tx.yearMonth <= currentYearMonth && isSubscriptionCandidate(tx)
        }

        if (expenseTxs.isEmpty()) {
            return SubscriptionReport()
        }

        val grouped = expenseTxs.groupBy { tx ->
            val norm = tx.merchant.trim().lowercase()
            if (norm.isNotEmpty()) norm else "fixed_${tx.categoryId}"
        }

        val subscriptions = mutableListOf<DetectedSubscription>()
        val prevYmOfCurrent = previousYearMonth(currentYearMonth)

        for ((_, groupTxs) in grouped) {
            val distinctMonths = groupTxs.map { it.yearMonth }.distinct()
            val hasFixedFlag = groupTxs.any { it.isFixed }
            val isDigitalSub = groupTxs.any { it.categoryId == "exp_leisure_subscriptions" }

            if (!hasFixedFlag && !isDigitalSub && distinctMonths.size < 2) {
                continue
            }

            val lastBilledYm = groupTxs.maxOf { it.yearMonth }
            val isRecentlyActive = (lastBilledYm == currentYearMonth || lastBilledYm == prevYmOfCurrent)
            if (!isRecentlyActive) {
                continue
            }

            val txsByMonth = groupTxs.groupBy { it.yearMonth }
            val currentMonthTxs = txsByMonth[currentYearMonth]
            val monthlyAmountCents: Long
            val previousAmountCents: Long?
            val priceDifferenceCents: Long?
            val isPriceHike: Boolean

            if (!currentMonthTxs.isNullOrEmpty()) {
                monthlyAmountCents = currentMonthTxs.sumOf { it.amountCents }
                val prevTxs = txsByMonth[prevYmOfCurrent]
                if (!prevTxs.isNullOrEmpty()) {
                    val prevAmount = prevTxs.sumOf { it.amountCents }
                    previousAmountCents = prevAmount
                    val diff = monthlyAmountCents - prevAmount
                    priceDifferenceCents = diff
                    isPriceHike = diff > 0L
                } else {
                    previousAmountCents = null
                    priceDifferenceCents = null
                    isPriceHike = false
                }
            } else {
                monthlyAmountCents = txsByMonth[lastBilledYm]?.sumOf { it.amountCents } ?: 0L
                previousAmountCents = null
                priceDifferenceCents = null
                isPriceHike = false
            }

            val latestTx = groupTxs.maxByOrNull { it.yearMonth } ?: groupTxs.last()
            val displayMerchant = if (latestTx.merchant.isNotBlank()) latestTx.merchant.trim() else latestTx.categoryId

            subscriptions.add(
                DetectedSubscription(
                    merchant = displayMerchant,
                    categoryId = latestTx.categoryId,
                    contextTag = latestTx.contextTag,
                    monthlyAmountCents = monthlyAmountCents,
                    annualizedCostCents = monthlyAmountCents * 12L,
                    occurrenceCount = groupTxs.size,
                    lastYearMonth = lastBilledYm,
                    previousAmountCents = previousAmountCents,
                    priceDifferenceCents = priceDifferenceCents,
                    isPriceHike = isPriceHike,
                    isFixed = hasFixedFlag
                )
            )
        }

        val sortedSubscriptions = subscriptions.sortedWith(
            compareByDescending<DetectedSubscription> { it.isPriceHike }
                .thenByDescending { it.monthlyAmountCents }
        )

        val totalMonthlyCents = sortedSubscriptions.sumOf { it.monthlyAmountCents }
        val totalAnnualCents = sortedSubscriptions.sumOf { it.annualizedCostCents }
        val hikeList = sortedSubscriptions.filter { it.isPriceHike }

        return SubscriptionReport(
            subscriptions = sortedSubscriptions,
            totalMonthlyCents = totalMonthlyCents,
            totalAnnualCents = totalAnnualCents,
            hikesCount = hikeList.size,
            totalHikeIncreaseCents = hikeList.sumOf { it.priceDifferenceCents ?: 0L }
        )
    }

    private fun previousYearMonth(yearMonth: Int): Int {
        val year = yearMonth / 100
        val month = yearMonth % 100
        return if (month == 1) {
            (year - 1) * 100 + 12
        } else {
            year * 100 + (month - 1)
        }
    }

    private fun defaultYearMonth(transactions: List<TransactionEntity>): Int {
        return transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .maxOfOrNull { it.yearMonth }
            ?: run {
                val now = java.time.LocalDate.now()
                now.year * 100 + now.monthValue
            }
    }
}
