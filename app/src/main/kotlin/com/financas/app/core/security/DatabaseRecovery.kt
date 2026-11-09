package com.financas.app.core.security

import android.content.Context
import java.io.File

object DatabaseRecovery {

    private const val PREFS_NAME = "financas_security_prefs"
    private const val KEY_RECOVERY_NOTICE = "database_recovery_notice"
    const val QUARANTINE_SUFFIX = ".unrecoverable-"

    fun quarantine(context: Context, databaseName: String, keyFile: File) {
        val stamp = System.currentTimeMillis()
        val dbFile = context.getDatabasePath(databaseName)
        val candidates = listOf(
            dbFile,
            File(dbFile.parentFile, dbFile.name + "-wal"),
            File(dbFile.parentFile, dbFile.name + "-shm"),
            File(dbFile.parentFile, dbFile.name + "-journal"),
            keyFile
        )
        for (file in candidates) {
            if (!file.exists()) continue
            val target = File(file.parentFile, file.name + QUARANTINE_SUFFIX + stamp)
            if (!file.renameTo(target) && !file.delete()) {
                throw IllegalStateException("Falha ao isolar ${file.name} durante a recuperação")
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RECOVERY_NOTICE, true).commit()
    }

    fun hasPendingNotice(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_RECOVERY_NOTICE, false)

    fun dismissNotice(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RECOVERY_NOTICE, false).apply()
    }
}
