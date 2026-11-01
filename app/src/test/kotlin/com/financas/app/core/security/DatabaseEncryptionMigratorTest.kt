package com.financas.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DatabaseEncryptionMigratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sqliteHeaderMagic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    @Test
    fun fileWithSqliteHeader_isPlaintextDatabase() {
        val file = tempFolder.newFile("legacy.db")
        file.writeBytes(sqliteHeaderMagic + ByteArray(64) { it.toByte() })
        assertTrue(DatabaseEncryptionMigrator.isPlaintextDatabase(file))
    }

    @Test
    fun fileWithEncryptedHeader_isNotPlaintextDatabase() {
        val file = tempFolder.newFile("encrypted.db")
        file.writeBytes(ByteArray(80) { (it * 37 + 11).toByte() })
        assertFalse(DatabaseEncryptionMigrator.isPlaintextDatabase(file))
    }

    @Test
    fun fileShorterThanHeader_isNotPlaintextDatabase() {
        val file = tempFolder.newFile("short.db")
        file.writeBytes(sqliteHeaderMagic.copyOfRange(0, 8))
        assertFalse(DatabaseEncryptionMigrator.isPlaintextDatabase(file))
    }

    @Test
    fun emptyFile_isNotPlaintextDatabase() {
        val file = tempFolder.newFile("empty.db")
        assertFalse(DatabaseEncryptionMigrator.isPlaintextDatabase(file))
    }

    @Test
    fun missingFile_isNotPlaintextDatabase() {
        assertFalse(DatabaseEncryptionMigrator.isPlaintextDatabase(File(tempFolder.root, "missing.db")))
    }
}
