package com.financas.app.core.security

object SessionLockPolicy {
    const val BACKGROUND_LOCK_TIMEOUT_MS = 30_000L

    fun shouldLock(elapsedBackgroundMs: Long, biometricsEnabled: Boolean): Boolean =
        biometricsEnabled && (elapsedBackgroundMs < 0L || elapsedBackgroundMs > BACKGROUND_LOCK_TIMEOUT_MS)
}

class SessionBackgroundClock {
    var lastBackgroundAtMs: Long = 0L
}
