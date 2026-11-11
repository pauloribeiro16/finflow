package com.financas.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class LocalFileCipher(
    private val masterKeyProvider: () -> SecretKey = ::keystoreMasterKey,
    private val random: SecureRandom = SecureRandom()
) {

    fun encrypt(plain: ByteArray): ByteArray {
        val dataKey = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        try {
            val wrapCipher = Cipher.getInstance(TRANSFORMATION)
            wrapCipher.init(Cipher.ENCRYPT_MODE, masterKeyProvider())
            val wrappedKey = wrapCipher.doFinal(dataKey)
            val wrapIv = wrapCipher.iv
            check(wrapIv.size == IV_BYTES && wrappedKey.size == WRAPPED_KEY_BYTES) { "Unexpected wrap output" }

            val dataIv = ByteArray(IV_BYTES).also(random::nextBytes)
            val dataCipher = Cipher.getInstance(TRANSFORMATION)
            dataCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(dataKey, "AES"), GCMParameterSpec(TAG_BITS, dataIv))
            dataCipher.updateAAD(MAGIC)
            return MAGIC + wrapIv + wrappedKey + dataIv + dataCipher.doFinal(plain)
        } finally {
            dataKey.fill(0)
        }
    }

    fun decrypt(blob: ByteArray): ByteArray {
        if (!isEncrypted(blob) || blob.size < HEADER_BYTES + TAG_BITS / 8) {
            throw GeneralSecurityException("Invalid local file envelope")
        }
        var offset = MAGIC.size
        val wrapIv = blob.copyOfRange(offset, offset + IV_BYTES).also { offset += IV_BYTES }
        val wrappedKey = blob.copyOfRange(offset, offset + WRAPPED_KEY_BYTES).also { offset += WRAPPED_KEY_BYTES }
        val dataIv = blob.copyOfRange(offset, offset + IV_BYTES).also { offset += IV_BYTES }

        val unwrapCipher = Cipher.getInstance(TRANSFORMATION)
        unwrapCipher.init(Cipher.DECRYPT_MODE, masterKeyProvider(), GCMParameterSpec(TAG_BITS, wrapIv))
        val dataKey = unwrapCipher.doFinal(wrappedKey)
        try {
            val dataCipher = Cipher.getInstance(TRANSFORMATION)
            dataCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(dataKey, "AES"), GCMParameterSpec(TAG_BITS, dataIv))
            dataCipher.updateAAD(MAGIC)
            return dataCipher.doFinal(blob, offset, blob.size - offset)
        } finally {
            dataKey.fill(0)
        }
    }

    companion object {
        const val KEY_ALIAS = "finflow_local_files_master"
        val MAGIC: ByteArray = "FFLF1".toByteArray(Charsets.US_ASCII)
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DATA_KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val WRAPPED_KEY_BYTES = DATA_KEY_BYTES + TAG_BITS / 8
        private val HEADER_BYTES = MAGIC.size + IV_BYTES + WRAPPED_KEY_BYTES + IV_BYTES

        fun isEncrypted(blob: ByteArray): Boolean =
            blob.size >= MAGIC.size && blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

        @Synchronized
        private fun keystoreMasterKey(): SecretKey {
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
                    .setKeySize(256)
                    .build()
            )
            return generator.generateKey()
        }
    }
}
