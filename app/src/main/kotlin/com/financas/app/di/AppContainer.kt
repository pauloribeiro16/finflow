package com.financas.app.di

import android.content.Context
import com.financas.app.core.security.DatabaseEncryptionMigrator
import com.financas.app.core.security.DatabaseKeyManager
import com.financas.app.core.security.SessionBackgroundClock
import com.financas.app.data.db.AppDatabase
import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.PendingTransactionDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.prefs.ContextTagsStore
import com.financas.app.data.prefs.SharedPrefsContextTagsStore
import com.financas.app.data.prefs.SharedPrefsUserProfileStore
import com.financas.app.data.prefs.UserProfileStore
import com.financas.app.data.repository.BackupSerializer
import com.financas.app.domain.usecase.InvestmentPortfolioCalculator
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.ui.screens.account.BiometricLockGate
import com.financas.app.ui.screens.account.BiometricPreferenceStore
import com.financas.app.ui.screens.account.SharedPrefsBiometricPreferenceStore
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

interface AppContainer {
    val database: AppDatabase
    val transactionDao: TransactionDao
    val categoryDao: CategoryDao
    val budgetDao: BudgetDao
    val goalDao: GoalDao
    val pendingTransactionDao: PendingTransactionDao
    val investmentAssetDao: InvestmentAssetDao
    val smartPrefillEngine: SmartPrefillEngine
    val monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator
    val investmentPortfolioCalculator: InvestmentPortfolioCalculator
    val biometricPreferenceStore: BiometricPreferenceStore
    val biometricLockGate: BiometricLockGate
    val backupSerializer: BackupSerializer
    val contextTagsStore: ContextTagsStore
    val userProfileStore: UserProfileStore
    val sessionBackgroundClock: SessionBackgroundClock
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        System.loadLibrary("sqlcipher")
        val keyManager = DatabaseKeyManager(File(context.filesDir, DatabaseKeyManager.KEY_FILE_NAME))
        val passphrase = keyManager.getOrCreatePassphrase()
        val openHelperFactory = SupportOpenHelperFactory(passphrase)
        runBlocking {
            DatabaseEncryptionMigrator.migrateIfNeeded(
                context = context,
                databaseName = AppDatabase.DATABASE_NAME,
                backupSerializer = backupSerializer,
                encryptedOpenHelperFactory = openHelperFactory
            )
        }
        AppDatabase.getInstance(context, openHelperFactory)
    }

    override val transactionDao: TransactionDao by lazy {
        database.transactionDao()
    }

    override val categoryDao: CategoryDao by lazy {
        database.categoryDao()
    }

    override val budgetDao: BudgetDao by lazy {
        database.budgetDao()
    }

    override val goalDao: GoalDao by lazy {
        database.goalDao()
    }

    override val pendingTransactionDao: PendingTransactionDao by lazy {
        database.pendingTransactionDao()
    }

    override val investmentAssetDao: InvestmentAssetDao by lazy {
        database.investmentAssetDao()
    }

    override val smartPrefillEngine: SmartPrefillEngine by lazy {
        SmartPrefillEngine(transactionDao)
    }

    override val monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator by lazy {
        MonthlyAnalyticsCalculator()
    }

    override val investmentPortfolioCalculator: InvestmentPortfolioCalculator by lazy {
        InvestmentPortfolioCalculator()
    }

    override val biometricPreferenceStore: BiometricPreferenceStore by lazy {
        SharedPrefsBiometricPreferenceStore(context)
    }

    override val biometricLockGate: BiometricLockGate by lazy {
        BiometricLockGate(biometricPreferenceStore)
    }

    override val backupSerializer: BackupSerializer by lazy {
        BackupSerializer()
    }

    override val contextTagsStore: ContextTagsStore by lazy {
        SharedPrefsContextTagsStore(context)
    }

    override val userProfileStore: UserProfileStore by lazy {
        SharedPrefsUserProfileStore(context)
    }

    override val sessionBackgroundClock: SessionBackgroundClock by lazy {
        SessionBackgroundClock()
    }
}
