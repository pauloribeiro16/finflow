package com.financas.app.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun migration_7_8_executesAlterTableStatements() {
        val executedStatements = mutableListOf<String>()

        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        }

        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase

        AppDatabase.MIGRATION_7_8.migrate(mockDb)

        assertEquals(2, executedStatements.size)
        assertTrue(executedStatements[0].contains("ALTER TABLE investment_assets ADD COLUMN ticker TEXT"))
        assertTrue(executedStatements[1].contains("ALTER TABLE investment_assets ADD COLUMN units REAL"))
    }
}
