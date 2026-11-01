package com.financas.app.domain.model

data class DetectedSubscription(
    val merchant: String,
    val categoryId: String,
    val contextTag: String,
    val monthlyAmountCents: Long,
    val annualizedCostCents: Long,
    val occurrenceCount: Int,
    val lastYearMonth: Int,
    val previousAmountCents: Long? = null,
    val priceDifferenceCents: Long? = null,
    val isPriceHike: Boolean = false,
    val isFixed: Boolean = false
)

data class SubscriptionReport(
    val subscriptions: List<DetectedSubscription> = emptyList(),
    val totalMonthlyCents: Long = 0L,
    val totalAnnualCents: Long = 0L,
    val hikesCount: Int = 0,
    val totalHikeIncreaseCents: Long = 0L
)
