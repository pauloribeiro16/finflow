package com.financas.app.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingFloodGuardTest {

    private val now = 10_000_000L

    @Test
    fun normalVolume_isAccepted() {
        val pending = List(5) { "MB WAY" to now - it * 1000L }
        assertTrue(PendingFloodGuard.shouldAccept(pending, "MB WAY", now))
        assertTrue(PendingFloodGuard.shouldAccept(emptyList(), "MB WAY", now))
    }

    @Test
    fun burstFromOneSource_isRejectedButOtherSourcesPass() {
        val pending = List(PendingFloodGuard.MAX_PER_SOURCE_PER_HOUR) { "Falso" to now - it * 1000L }
        assertFalse(PendingFloodGuard.shouldAccept(pending, "Falso", now))
        assertTrue(PendingFloodGuard.shouldAccept(pending, "Revolut", now))
    }

    @Test
    fun oldPendings_doNotCountTowardsHourlyLimit() {
        val pending = List(PendingFloodGuard.MAX_PER_SOURCE_PER_HOUR) { "MB WAY" to now - 2 * 60 * 60 * 1000L }
        assertTrue(PendingFloodGuard.shouldAccept(pending, "MB WAY", now))
    }

    @Test
    fun fullQueue_rejectsEverything() {
        val pending = List(PendingFloodGuard.MAX_PENDING_TOTAL) { "App$it" to 0L }
        assertFalse(PendingFloodGuard.shouldAccept(pending, "Revolut", now))
    }
}
