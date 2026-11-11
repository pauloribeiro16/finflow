package com.financas.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLockPolicyTest {

    @Test
    fun zeroElapsed_doesNotLock() {
        assertFalse(SessionLockPolicy.shouldLock(0L, true))
    }

    @Test
    fun justBelowTimeout_doesNotLock() {
        assertFalse(SessionLockPolicy.shouldLock(SessionLockPolicy.BACKGROUND_LOCK_TIMEOUT_MS - 1, true))
    }

    @Test
    fun exactlyAtTimeout_doesNotLock() {
        assertFalse(SessionLockPolicy.shouldLock(SessionLockPolicy.BACKGROUND_LOCK_TIMEOUT_MS, true))
    }

    @Test
    fun justAboveTimeout_locks() {
        assertTrue(SessionLockPolicy.shouldLock(SessionLockPolicy.BACKGROUND_LOCK_TIMEOUT_MS + 1, true))
    }

    @Test
    fun biometricsDisabled_neverLocks() {
        assertFalse(SessionLockPolicy.shouldLock(0L, false))
        assertFalse(SessionLockPolicy.shouldLock(29_999L, false))
        assertFalse(SessionLockPolicy.shouldLock(30_000L, false))
        assertFalse(SessionLockPolicy.shouldLock(30_001L, false))
        assertFalse(SessionLockPolicy.shouldLock(Long.MAX_VALUE, false))
    }

    @Test
    fun timeout_isThirtySeconds() {
        assertEquals(30_000L, SessionLockPolicy.BACKGROUND_LOCK_TIMEOUT_MS)
    }

    @Test
    fun negativeElapsed_locksWhenClockGoesBackwards() {
        assertTrue(SessionLockPolicy.shouldLock(-1L, true))
        assertTrue(SessionLockPolicy.shouldLock(-3_600_000L, true))
        assertFalse(SessionLockPolicy.shouldLock(-1L, false))
    }

    @Test
    fun immediateTimeout_locksEvenWithoutElapsedTime() {
        assertTrue(SessionLockPolicy.shouldLock(0L, true, timeoutMs = 0L))
        assertFalse(SessionLockPolicy.shouldLock(0L, false, timeoutMs = 0L))
    }

    @Test
    fun customTimeout_isRespected() {
        assertFalse(SessionLockPolicy.shouldLock(200_000L, true, timeoutMs = 300_000L))
        assertTrue(SessionLockPolicy.shouldLock(300_001L, true, timeoutMs = 300_000L))
    }

    @Test
    fun externalFlow_getsBoundedGrace() {
        assertFalse(SessionLockPolicy.shouldLock(60_000L, true, timeoutMs = 0L, externalFlowExpected = true))
        assertTrue(
            SessionLockPolicy.shouldLock(
                SessionLockPolicy.EXTERNAL_FLOW_GRACE_MS + 1,
                true,
                timeoutMs = 0L,
                externalFlowExpected = true
            )
        )
        assertFalse(SessionLockPolicy.shouldLock(200_000L, true, timeoutMs = 300_000L, externalFlowExpected = true))
    }

    @Test
    fun unknownTimeout_fallsBackToDefault() {
        assertEquals(SessionLockPolicy.BACKGROUND_LOCK_TIMEOUT_MS, SessionLockPolicy.normalizeTimeout(123L))
        assertEquals(0L, SessionLockPolicy.normalizeTimeout(0L))
    }
}
