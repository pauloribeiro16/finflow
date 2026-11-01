package com.financas.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class DatabaseKeyManagerTest {

    private val expectedBlobSize =
        DatabaseKeyManager.GCM_IV_LENGTH_BYTES +
            DatabaseKeyManager.PASSPHRASE_LENGTH_BYTES +
            DatabaseKeyManager.GCM_TAG_LENGTH_BYTES

    private fun blobOfSize(size: Int): String {
        val payload = ByteArray(size) { it.toByte() }
        return Base64.getEncoder().encodeToString(payload)
    }

    @Test
    fun blobWithExpectedSize_isValidFormat() {
        assertTrue(DatabaseKeyManager.isValidBlobFormat(blobOfSize(expectedBlobSize)))
    }

    @Test
    fun blobTooShort_isInvalidFormat() {
        assertFalse(DatabaseKeyManager.isValidBlobFormat(blobOfSize(expectedBlobSize - 1)))
    }

    @Test
    fun blobTooLong_isInvalidFormat() {
        assertFalse(DatabaseKeyManager.isValidBlobFormat(blobOfSize(expectedBlobSize + 1)))
    }

    @Test
    fun emptyBlob_isInvalidFormat() {
        assertFalse(DatabaseKeyManager.isValidBlobFormat(""))
        assertFalse(DatabaseKeyManager.isValidBlobFormat("   "))
    }

    @Test
    fun nonBase64Blob_isInvalidFormat() {
        assertFalse(DatabaseKeyManager.isValidBlobFormat("not-base64!!!"))
    }

    @Test
    fun blobConstants_areConsistent() {
        assertEquals(32, DatabaseKeyManager.PASSPHRASE_LENGTH_BYTES)
        assertEquals(12, DatabaseKeyManager.GCM_IV_LENGTH_BYTES)
        assertEquals(16, DatabaseKeyManager.GCM_TAG_LENGTH_BYTES)
        assertEquals("finflow_db_master", DatabaseKeyManager.KEY_ALIAS)
        assertEquals("database.key", DatabaseKeyManager.KEY_FILE_NAME)
    }
}
