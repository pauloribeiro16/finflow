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
}
