package com.financas.app.domain.usecase

object PendingFloodGuard {
    const val MAX_PENDING_TOTAL = 500
    const val MAX_PER_SOURCE_PER_HOUR = 30
    private const val HOUR_MILLIS = 60L * 60 * 1000

    fun shouldAccept(pendingTimestampsBySource: List<Pair<String, Long>>, sourceApp: String, nowMillis: Long): Boolean {
        if (pendingTimestampsBySource.size >= MAX_PENDING_TOTAL) return false
        val recentFromSource = pendingTimestampsBySource.count { (source, receivedAt) ->
            source == sourceApp && nowMillis - receivedAt in 0 until HOUR_MILLIS
        }
        return recentFromSource < MAX_PER_SOURCE_PER_HOUR
    }
}
