package com.financas.app.domain.usecase

object PendingRetention {
    const val MAX_AGE_DAYS = 30L
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun cutoffMillis(nowMillis: Long): Long = nowMillis - MAX_AGE_DAYS * DAY_MILLIS
}
