package com.financas.app.core.security

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper.Factory
import com.financas.app.data.db.AppDatabase
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.repository.BackupSerializer
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

object DatabaseEncryptionMigrator {

    suspend fun migrateIfNeeded(
        context: Context,
        databaseName: String,
        backupSerializer: BackupSerializer,
        encryptedOpenHelperFactory: Factory
    ) {
        val dbFile = context.getDatabasePath(databaseName)
        val legacyDbFile = File(dbFile.parentFile, databaseName + LEGACY_SUFFIX)

        when {
            legacyDbFile.exists() -> completeMigration(
                context,
                databaseName,
                backupSerializer,
                encryptedOpenHelperFactory
            )

            dbFile.exists() && isPlaintextDatabase(dbFile) -> {
                preserveLegacyFiles(dbFile)
                completeMigration(context, databaseName, backupSerializer, encryptedOpenHelperFactory)
            }
        }
    }

    fun isPlaintextDatabase(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_HEADER_MAGIC.size) return false
        return try {
            RandomAccessFile(file, "r").use { reader ->
                val header = ByteArray(SQLITE_HEADER_MAGIC.size)
                reader.readFully(header)
                header.contentEquals(SQLITE_HEADER_MAGIC)
            }
        } catch (_: IOException) {
            false
        }
    }

    private fun preserveLegacyFiles(dbFile: File) {
        if (!dbFile.renameTo(File(dbFile.parentFile, dbFile.name + LEGACY_SUFFIX))) {
            throw IllegalStateException("Falha ao preservar a base de dados legada para migração")
        }
        for (extra in listOf("-wal", "-shm")) {
            val source = File(dbFile.parentFile, dbFile.name + extra)
            if (source.exists() && !source.renameTo(File(dbFile.parentFile, dbFile.name + LEGACY_SUFFIX + extra))) {
                throw IllegalStateException("Falha ao preservar $extra da base de dados legada")
            }
        }
    }

    private suspend fun completeMigration(
        context: Context,
        databaseName: String,
        backupSerializer: BackupSerializer,
        encryptedOpenHelperFactory: Factory
    ) {
        val (backupJson, pendingTransactions) = exportLegacyPlaintext(context, databaseName, backupSerializer)
        importIntoEncryptedDatabase(
            context,
            databaseName,
            backupJson,
            pendingTransactions,
            backupSerializer,
            encryptedOpenHelperFactory
        )
        deleteLegacyFiles(context, databaseName)
    }

    private suspend fun exportLegacyPlaintext(
        context: Context,
        databaseName: String,
        backupSerializer: BackupSerializer
    ): Pair<String, List<PendingTransactionEntity>> {
        val legacyDb = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            databaseName + LEGACY_SUFFIX
        )
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        try {
            val categories = legacyDb.categoryDao().getAllSync()
            val transactions = legacyDb.transactionDao().getAllSync()
            val budgets = legacyDb.budgetDao().getAllSync()
            val goals = legacyDb.goalDao().getAllSync()
            val investments = legacyDb.investmentAssetDao().getAllSync()
            val pendingTransactions = legacyDb.pendingTransactionDao().getAllSync()
            val backupJson = backupSerializer.exportToJson(categories, transactions, budgets, goals, investments)
            return backupJson to pendingTransactions
        } finally {
            legacyDb.close()
        }
    }

    private suspend fun importIntoEncryptedDatabase(
        context: Context,
        databaseName: String,
        backupJson: String,
        pendingTransactions: List<PendingTransactionEntity>,
        backupSerializer: BackupSerializer,
        encryptedOpenHelperFactory: Factory
    ) {
        val encryptedDb = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            databaseName
        )
            .openHelperFactory(encryptedOpenHelperFactory)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        try {
            val backupData = backupSerializer.importFromJson(backupJson)
            val result = backupSerializer.restoreBackup(
                backupData,
                encryptedDb.categoryDao(),
                encryptedDb.transactionDao(),
                encryptedDb.budgetDao(),
                encryptedDb.goalDao(),
                encryptedDb.investmentAssetDao()
            )
            val totalTransactionsPersisted = result.transactionsImported + result.transactionsSkipped
            if (totalTransactionsPersisted != backupData.transactions.size ||
                result.categoriesImported != backupData.categories.size
            ) {
                throw IllegalStateException("Importação incompleta dos dados legados durante migração de encriptação")
            }
            encryptedDb.pendingTransactionDao().insertAll(pendingTransactions)
            val persistedPendingCount = encryptedDb.pendingTransactionDao().getAllSync().size
            if (persistedPendingCount != pendingTransactions.size) {
                throw IllegalStateException("Importação incompleta dos movimentos pendentes durante migração de encriptação")
            }
        } finally {
            encryptedDb.close()
        }
    }

    private fun deleteLegacyFiles(context: Context, databaseName: String) {
        val parent = context.getDatabasePath(databaseName).parentFile
        val legacyNames = listOf(
            databaseName + LEGACY_SUFFIX,
            databaseName + LEGACY_SUFFIX + "-wal",
            databaseName + LEGACY_SUFFIX + "-shm"
        )
        for (name in legacyNames) {
            val file = File(parent, name)
            if (file.exists() && !file.delete()) {
                throw IllegalStateException("Falha ao remover ficheiro legado $name após migração")
            }
        }
    }

    private val SQLITE_HEADER_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    private const val LEGACY_SUFFIX = ".plaintext"
}
