package com.financas.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPasswordPolicyTest {

    @Test
    fun shortPasswords_areWeak() {
        assertEquals(PasswordStrength.WEAK, BackupPasswordPolicy.strength("Ab1!xY9?"))
        assertEquals(PasswordStrength.WEAK, BackupPasswordPolicy.strength(""))
    }

    @Test
    fun predictablePasswords_areWeakEvenWhenLong() {
        listOf("1234567890123", "aaaaaaaaaaaaaa", "password123456", "qwertyqwerty", "abcdefghijkl", "finflow2026!").forEach {
            assertEquals(it, PasswordStrength.WEAK, BackupPasswordPolicy.strength(it))
            assertFalse(BackupPasswordPolicy.isAcceptable(it))
        }
    }

    @Test
    fun mixedPasswords_areAcceptable() {
        assertTrue(BackupPasswordPolicy.isAcceptable("Tr7!mKp2#vQz"))
        assertTrue(BackupPasswordPolicy.isAcceptable("cavalo-bateria-agrafo-certo"))
    }

    @Test
    fun longRandomPasswords_areStrong() {
        assertEquals(PasswordStrength.STRONG, BackupPasswordPolicy.strength("Tr7!mKp2#vQz9\$Lw"))
        assertEquals(PasswordStrength.STRONG, BackupPasswordPolicy.strength("cavalo-bateria-agrafo-certo"))
    }

    @Test
    fun moderatePasswords_areFair() {
        assertEquals(PasswordStrength.FAIR, BackupPasswordPolicy.strength("Tr7!mKp2#v"))
    }
}
