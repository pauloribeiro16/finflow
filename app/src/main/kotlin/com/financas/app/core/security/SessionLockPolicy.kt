package com.financas.app.core.security

object SessionLockPolicy {
    const val BACKGROUND_LOCK_TIMEOUT_MS = 30_000L
    const val EXTERNAL_FLOW_GRACE_MS = 120_000L
    val ALLOWED_TIMEOUTS_MS = listOf(0L, 30_000L, 60_000L, 300_000L)

    fun normalizeTimeout(timeoutMs: Long): Long =
        if (timeoutMs in ALLOWED_TIMEOUTS_MS) timeoutMs else BACKGROUND_LOCK_TIMEOUT_MS

    fun shouldLock(
        elapsedBackgroundMs: Long,
        biometricsEnabled: Boolean,
        timeoutMs: Long = BACKGROUND_LOCK_TIMEOUT_MS,
        externalFlowExpected: Boolean = false
    ): Boolean {
        if (!biometricsEnabled) return false
        if (elapsedBackgroundMs < 0L) return true
        if (externalFlowExpected) return elapsedBackgroundMs > maxOf(timeoutMs, EXTERNAL_FLOW_GRACE_MS)
        return timeoutMs == 0L || elapsedBackgroundMs > timeoutMs
    }
}

class SessionBackgroundClock {
    var lastBackgroundAtMs: Long = 0L

    @Volatile
    var externalFlowExpected: Boolean = false
}
