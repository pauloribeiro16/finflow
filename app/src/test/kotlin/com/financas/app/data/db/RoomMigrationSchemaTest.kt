package com.financas.app.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager

class RoomMigrationSchemaTest {

    private val schemaDir = File("schemas/com.financas.app.data.db.AppDatabase")

    private data class Column(val name: String, val affinity: String, val notNull: Boolean, val pkPosition: Int)
    private data class Index(val name: String, val unique: Boolean, val columns: List<String>)
    private data class Table(val name: String, val columns: Set<Column>, val indices: Set<Index>)

    private fun latestSchemaVersion(): Int =
        schemaDir.listFiles().orEmpty().mapNotNull { it.nameWithoutExtension.toIntOrNull() }.maxOrNull()
            ?: error("Sem esquemas exportados em ${schemaDir.absolutePath}")

    private fun expectedTables(version: Int): Map<String, Table> {
        val db = JSONObject(File(schemaDir, "$version.json").readText()).getJSONObject("database")
        val entities = db.getJSONArray("entities")
        return (0 until entities.length()).associate { i ->
            val e = entities.getJSONObject(i)
            val name = e.getString("tableName")
            val pk = e.getJSONObject("primaryKey").getJSONArray("columnNames").let { a -> (0 until a.length()).map { a.getString(it) } }
            val fields = e.getJSONArray("fields")
            val columns = (0 until fields.length()).map { j ->
                val f = fields.getJSONObject(j)
                val col = f.getString("columnName")
                Column(col, f.getString("affinity"), f.optBoolean("notNull", false), pk.indexOf(col) + 1)
            }.toSet()
            val idx = e.optJSONArray("indices")
            val indices = (0 until (idx?.length() ?: 0)).map { j ->
                val ix = idx!!.getJSONObject(j)
                val cols = ix.getJSONArray("columnNames").let { a -> (0 until a.length()).map { a.getString(it) } }
                Index(ix.getString("name"), ix.optBoolean("unique", false), cols)
            }.toSet()
            name to Table(name, columns, indices)
        }
    }

    private fun createSqlFor(version: Int): List<String> {
        val db = JSONObject(File(schemaDir, "$version.json").readText()).getJSONObject("database")
        val entities = db.getJSONArray("entities")
        return (0 until entities.length()).flatMap { i ->
            val e = entities.getJSONObject(i)
            val name = e.getString("tableName")
            val idx = e.optJSONArray("indices")
            listOf(e.getString("createSql").replace("\${TABLE_NAME}", name)) +
                (0 until (idx?.length() ?: 0)).map { j -> idx!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name) }
        }
    }

    private fun legacyCreateSql(fromVersion: Int): List<String> {
        val removedColumns = mutableMapOf<String, MutableSet<String>>()
        val removedTables = mutableSetOf<String>()
        val removedIndices = mutableSetOf<String>()
        if (fromVersion < 8) removedColumns.getOrPut("investment_assets") { mutableSetOf() } += listOf("ticker", "units")
        if (fromVersion < 7) removedColumns.getOrPut("transactions") { mutableSetOf() } += "investmentAssetId"
        if (fromVersion < 6) removedColumns.getOrPut("pending_transactions") { mutableSetOf() } += "suggestedType"
        if (fromVersion < 5) removedTables += "investment_assets"
        if (fromVersion < 4) {
            removedColumns.getOrPut("transactions") { mutableSetOf() } += "linkedTransactionId"
            removedIndices += "index_transactions_linkedTransactionId"
        }
        if (fromVersion < 3) removedTables += "pending_transactions"
        if (fromVersion < 2) removedTables += listOf("budgets", "goals")

        return createSqlFor(latestSchemaVersion()).mapNotNull { sql ->
            val table = Regex("ON `([^`]+)`|TABLE IF NOT EXISTS `([^`]+)`").find(sql)!!.groupValues.drop(1).first { it.isNotEmpty() }
            val indexName = Regex("INDEX IF NOT EXISTS `([^`]+)`").find(sql)?.groupValues?.get(1)
            when {
                table in removedTables -> null
                indexName != null && indexName in removedIndices -> null
                else -> removedColumns[table].orEmpty().fold(sql) { acc, col ->
                    acc.replace(Regex(", `$col` [A-Z]+( NOT NULL)?"), "")
                }
            }
        }
    }

    private fun Connection.asSupportDatabase(): SupportSQLiteDatabase =
        Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args!![0] as String
                    val bind = (args.getOrNull(1) as? Array<*>).orEmpty()
                    prepareStatement(sql).use { st ->
                        bind.forEachIndexed { i, v -> st.setObject(i + 1, v) }
                        st.execute()
                    }
                    null
                }
                "toString" -> "JdbcSupportDatabase"
                else -> throw UnsupportedOperationException("Migração usou ${method.name}; acrescentar ao adaptador de teste.")
            }
        } as SupportSQLiteDatabase

    private fun Connection.actualTables(): Map<String, Table> {
        val names = createStatement().use { st ->
            st.executeQuery("SELECT name FROM sqlite_master WHERE type='table'").use { rs ->
                buildList { while (rs.next()) add(rs.getString(1)) }
            }
        }.filterNot { it.startsWith("sqlite_") || it == "room_master_table" || it == "android_metadata" }

        return names.associateWith { table ->
            val columns = createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                    buildSet {
                        while (rs.next()) add(Column(rs.getString("name"), affinityOf(rs.getString("type")), rs.getInt("notnull") == 1, rs.getInt("pk")))
                    }
                }
            }
            val indexNames = createStatement().use { st ->
                st.executeQuery("PRAGMA index_list(`$table`)").use { rs ->
                    buildList { while (rs.next()) if (rs.getString("origin") == "c") add(rs.getString("name") to (rs.getInt("unique") == 1)) }
                }
            }
            val indices = indexNames.map { (name, unique) ->
                val cols = createStatement().use { st ->
                    st.executeQuery("PRAGMA index_info(`$name`)").use { rs ->
                        buildList { while (rs.next()) add(rs.getInt("seqno") to rs.getString("name")) }
                    }
                }.sortedBy { it.first }.map { it.second }
                Index(name, unique, cols)
            }.toSet()
            Table(table, columns, indices)
        }
    }

    private fun affinityOf(type: String): String {
        val t = type.uppercase()
        return when {
            "INT" in t -> "INTEGER"
            "CHAR" in t || "CLOB" in t || "TEXT" in t -> "TEXT"
            "BLOB" in t || t.isEmpty() -> "BLOB"
            "REAL" in t || "FLOA" in t || "DOUB" in t -> "REAL"
            else -> "NUMERIC"
        }
    }

    private fun migrateFrom(fromVersion: Int, seed: (Connection) -> Unit = {}): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { st -> legacyCreateSql(fromVersion).forEach { st.execute(it) } }
        seed(conn)
        val support = conn.asSupportDatabase()
        var version = fromVersion
        while (version < latestSchemaVersion()) {
            val migration = AppDatabase.ALL_MIGRATIONS.single { it.startVersion == version }
            migration.migrate(support)
            version = migration.endVersion
        }
        return conn
    }

    @Test
    fun migrations_coverEveryVersionUpToLatestSchema() {
        val latest = latestSchemaVersion()
        val starts = AppDatabase.ALL_MIGRATIONS.map { it.startVersion }.sorted()
        assertEquals((1 until latest).toList(), starts)
        AppDatabase.ALL_MIGRATIONS.forEach { assertEquals(it.startVersion + 1, it.endVersion) }
    }

    @Test
    fun everyStartingVersion_migratesToExactRoomSchema() {
        val expected = expectedTables(latestSchemaVersion())
        for (from in 1..latestSchemaVersion()) {
            migrateFrom(from).use { conn ->
                val actual = conn.actualTables()
                assertEquals("Tabelas depois de migrar de v$from", expected.keys, actual.keys)
                expected.forEach { (name, table) ->
                    assertEquals("Colunas de $name (de v$from)", table.columns, actual.getValue(name).columns)
                    assertEquals("Índices de $name (de v$from)", table.indices, actual.getValue(name).indices)
                }
            }
        }
    }

    @Test
    fun migrationFromV1_preservesExistingData() {
        migrateFrom(1) { conn ->
            conn.createStatement().use { st ->
                st.execute("INSERT INTO categories (id, parentId, name, icon, colorHex, type, isDefaultFixed, sortOrder) VALUES ('exp_food', NULL, 'Alimentação', 'restaurant', 1, 'EXPENSE', 0, 0)")
                st.execute("INSERT INTO transactions (id, type, amountCents, yearMonth, epochDay, categoryId, contextTag, merchant, paymentMethod, isFixed) VALUES (7, 'EXPENSE', 1250, 202611, 20400, 'exp_food', 'PESSOAL', 'Continente', 'CARTAO', 0)")
            }
        }.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT amountCents, merchant, linkedTransactionId, investmentAssetId FROM transactions WHERE id = 7").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1250L, rs.getLong(1))
                    assertEquals("Continente", rs.getString(2))
                    assertNull(rs.getObject(3))
                    assertNull(rs.getObject(4))
                }
                st.executeQuery("SELECT COUNT(*) FROM categories").use { rs -> rs.next(); assertEquals(1, rs.getInt(1)) }
            }
        }
    }

    @Test
    fun migrationFromV7_keepsInvestmentsAndAddsEmptyTickerAndUnits() {
        migrateFrom(7) { conn ->
            conn.createStatement().use { st ->
                st.execute("INSERT INTO investment_assets (id, name, type, institution, investedCents, currentValueCents, monthlyContributionCents, targetCents, notes, lastUpdatedEpochDay, archived) VALUES ('a', 'ETF', 'ETF', 'XTB', 1000, 1100, 0, NULL, '', 20000, 0)")
            }
        }.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT currentValueCents, ticker, units FROM investment_assets WHERE id = 'a'").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1100L, rs.getLong(1))
                    assertNull(rs.getObject(2))
                    assertNull(rs.getObject(3))
                }
            }
        }
    }
}
