package com.financas.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DatabaseKeyManager(
    private val keyFile: File,
    private val random: SecureRandom = SecureRandom()
) {

    fun getOrCreatePassphrase(): ByteArray {
        if (keyFile.exists()) {
            return unwrap(keyFile.readText())
        }
        val passphrase = ByteArray(PASSPHRASE_LENGTH_BYTES).also { random.nextBytes(it) }
        keyFile.writeText(wrap(passphrase))
        return passphrase
    }

    fun wrap(passphrase: ByteArray): String {
        require(passphrase.size == PASSPHRASE_LENGTH_BYTES) {
            "Passphrase must be ${PASSPHRASE_LENGTH_BYTES} bytes"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val encrypted = cipher.doFinal(passphrase)
        val payload = cipher.iv + encrypted
        return Base64.getEncoder().encodeToString(payload)
    }

    fun unwrap(blob: String): ByteArray {
        require(isValidBlobFormat(blob)) { "Invalid database key blob format" }
        val payload = Base64.getDecoder().decode(blob)
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val encrypted = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MASTER_KEY_SIZE_BITS)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val KEY_ALIAS = "finflow_db_master"
        const val KEY_FILE_NAME = "database.key"
        const val PASSPHRASE_LENGTH_BYTES = 32
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = 16
        const val MASTER_KEY_SIZE_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        fun isValidBlobFormat(base64Blob: String): Boolean {
            if (base64Blob.isBlank()) return false
            val payload = try {
                Base64.getDecoder().decode(base64Blob)
            } catch (_: IllegalArgumentException) {
                return false
            }
            val expectedSize = GCM_IV_LENGTH_BYTES + PASSPHRASE_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES
            return payload.size == expectedSize
        }
    }
}
