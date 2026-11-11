package com.financas.app.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException
import javax.crypto.spec.SecretKeySpec

class LocalFileCipherTest {

    private val key = SecretKeySpec(ByteArray(32) { (it * 7).toByte() }, "AES")
    private val cipher = LocalFileCipher(masterKeyProvider = { key })
    private val payload = """{"transactions":[{"amountCents":123456,"merchant":"Mercearia"}]}""".toByteArray()

    @Test
    fun roundTrip_restoresPlaintext() {
        assertArrayEquals(payload, cipher.decrypt(cipher.encrypt(payload)))
    }

    @Test
    fun envelope_hidesPlaintextAndIsRandomized() {
        val a = cipher.encrypt(payload)
        val b = cipher.encrypt(payload)
        assertTrue(LocalFileCipher.isEncrypted(a))
        assertFalse(a.contentEquals(b))
        assertFalse(String(a, Charsets.ISO_8859_1).contains("Mercearia"))
        assertFalse(LocalFileCipher.isEncrypted(payload))
    }

    @Test(expected = GeneralSecurityException::class)
    fun tamperedCiphertext_isRejected() {
        val blob = cipher.encrypt(payload)
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
        cipher.decrypt(blob)
    }

    @Test(expected = GeneralSecurityException::class)
    fun otherMasterKey_cannotDecrypt() {
        val other = LocalFileCipher(masterKeyProvider = { SecretKeySpec(ByteArray(32) { 9 }, "AES") })
        other.decrypt(cipher.encrypt(payload))
    }

    @Test(expected = GeneralSecurityException::class)
    fun plaintextInput_isRejected() {
        cipher.decrypt(payload)
    }
}
