package com.financas.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        PendingTransactionEntity::class,
        InvestmentAssetEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun investmentAssetDao(): InvestmentAssetDao

    companion object {
        const val DATABASE_NAME = "financas.db"

        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budgets (id TEXT NOT NULL, categoryId TEXT NOT NULL, limitCents INTEGER NOT NULL, isActive INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(id))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets (categoryId)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS goals (id TEXT NOT NULL, name TEXT NOT NULL, targetCents INTEGER NOT NULL, startYearMonth INTEGER NOT NULL, deadlineYearMonth INTEGER, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        rawMerchant TEXT NOT NULL,
                        suggestedCategoryId TEXT NOT NULL,
                        suggestedContextTag TEXT NOT NULL,
                        suggestedPaymentMethod TEXT NOT NULL,
                        isFixed INTEGER NOT NULL DEFAULT 0,
                        sourceApp TEXT NOT NULL,
                        detectedEpochDay INTEGER NOT NULL,
                        receivedTimestamp INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_transactions_receivedTimestamp ON pending_transactions (receivedTimestamp)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN linkedTransactionId INTEGER DEFAULT NULL"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_linkedTransactionId ON transactions (linkedTransactionId)"
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS investment_assets (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        institution TEXT NOT NULL,
                        investedCents INTEGER NOT NULL,
                        currentValueCents INTEGER NOT NULL,
                        monthlyContributionCents INTEGER NOT NULL DEFAULT 0,
                        targetCents INTEGER,
                        notes TEXT NOT NULL DEFAULT '',
                        lastUpdatedEpochDay INTEGER NOT NULL,
                        archived INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_investment_assets_type ON investment_assets (type)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_investment_assets_archived ON investment_assets (archived)"
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pending_transactions ADD COLUMN suggestedType TEXT NOT NULL DEFAULT 'EXPENSE'"
                )
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN investmentAssetId TEXT")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
        )

        fun getInstance(context: Context, openHelperFactory: SupportSQLiteOpenHelper.Factory): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(openHelperFactory)
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
