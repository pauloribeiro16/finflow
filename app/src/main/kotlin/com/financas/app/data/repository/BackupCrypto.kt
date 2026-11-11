package com.financas.app.data.repository

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongBackupPasswordException : Exception("Palavra-passe errada ou ficheiro corrompido.")

object BackupCrypto {

    const val FORMAT = "finflow-encrypted-backup"
    const val VERSION = 1
    const val MIN_PASSWORD_LENGTH = 8
    const val DEFAULT_ITERATIONS = 310_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 5_000_000
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    private val random = SecureRandom()

    fun isEncrypted(content: String): Boolean {
        val trimmed = content.trimStart()
        if (!trimmed.startsWith("{")) return false
        return try {
            JSONObject(trimmed).optString("format") == FORMAT
        } catch (_: Exception) {
            false
        }
    }

    fun encrypt(plainText: String, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): String {
        require(password.size >= MIN_PASSWORD_LENGTH) { "A palavra-passe tem de ter pelo menos $MIN_PASSWORD_LENGTH caracteres." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(iterations))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return JSONObject()
            .put("format", FORMAT)
            .put("v", VERSION)
            .put("kdf", KDF)
            .put("iter", iterations)
            .put("salt", encoder.encodeToString(salt))
            .put("iv", encoder.encodeToString(iv))
            .put("data", encoder.encodeToString(cipherText))
            .toString()
    }

    fun decrypt(envelope: String, password: CharArray): String {
        val json = try {
            JSONObject(envelope.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Ficheiro de backup cifrado inválido.", e)
        }
        require(json.optString("format") == FORMAT && json.optInt("v") == VERSION && json.optString("kdf") == KDF) {
            "Formato de backup cifrado não suportado."
        }
        val iterations = json.optInt("iter")
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Parâmetros de cifra inválidos." }
        val decoder = Base64.getDecoder()
        val salt = decoder.decode(json.getString("salt"))
        val iv = decoder.decode(json.getString("iv"))
        val data = decoder.decode(json.getString("data"))
        require(salt.size == SALT_BYTES && iv.size == IV_BYTES) { "Parâmetros de cifra inválidos." }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(iterations))
        return try {
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (_: AEADBadTagException) {
            throw WrongBackupPasswordException()
        }
    }

    private fun aad(iterations: Int): ByteArray = "$FORMAT:$VERSION:$KDF:$iterations".toByteArray(Charsets.UTF_8)

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val keyBytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            return SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
