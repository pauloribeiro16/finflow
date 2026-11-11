package com.financas.app.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val payload = """{"version":3,"transactions":[{"merchant":"Continente","amountCents":1250}]}"""
    private val iterations = 100_000

    @Test
    fun encryptThenDecrypt_roundTrips() {
        val envelope = BackupCrypto.encrypt(payload, "palavra-forte".toCharArray(), iterations)

        assertTrue(BackupCrypto.isEncrypted(envelope))
        assertFalse(envelope.contains("Continente"))
        assertEquals(payload, BackupCrypto.decrypt(envelope, "palavra-forte".toCharArray()))
    }

    @Test
    fun encrypt_usesFreshSaltAndIv() {
        val a = BackupCrypto.encrypt(payload, "palavra-forte".toCharArray(), iterations)
        val b = BackupCrypto.encrypt(payload, "palavra-forte".toCharArray(), iterations)
        assertNotEquals(JSONObject(a).getString("salt"), JSONObject(b).getString("salt"))
        assertNotEquals(JSONObject(a).getString("iv"), JSONObject(b).getString("iv"))
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun decrypt_wrongPasswordFails() {
        val envelope = BackupCrypto.encrypt(payload, "palavra-forte".toCharArray(), iterations)
        BackupCrypto.decrypt(envelope, "outra-palavra".toCharArray())
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun decrypt_tamperedDataFails() {
        val json = JSONObject(BackupCrypto.encrypt(payload, "palavra-forte".toCharArray(), iterations))
        val data = java.util.Base64.getDecoder().decode(json.getString("data"))
        data[0] = (data[0].toInt() xor 1).toByte()
        json.put("data", java.util.Base64.getEncoder().encodeToString(data))
        BackupCrypto.decrypt(json.toString(), "palavra-forte".toCharArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun decrypt_rejectsDowngradedIterations() {
        val json = JSONObject(BackupCrypto.encrypt(payload, "palavra-forte".toCharArray(), iterations))
        json.put("iter", 1)
        BackupCrypto.decrypt(json.toString(), "palavra-forte".toCharArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun encrypt_rejectsShortPassword() {
        BackupCrypto.encrypt(payload, "curta".toCharArray(), iterations)
    }

    @Test
    fun isEncrypted_falseForPlainBackupAndGarbage() {
        assertFalse(BackupCrypto.isEncrypted(payload))
        assertFalse(BackupCrypto.isEncrypted("not json"))
    }
}
