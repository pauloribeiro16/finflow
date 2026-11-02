package com.financas.app

import android.app.Application
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.di.AppContainer
import com.financas.app.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinancasApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        applicationScope.launch {
            DefaultSeed.seedIfNeeded(container.categoryDao)
            if (BuildConfig.IS_DEMO) {
                if (container.transactionDao.count() == 0) {
                    com.financas.app.data.seed.DemoDataSeeder.seedDemoData(
                        categoryDao = container.categoryDao,
                        transactionDao = container.transactionDao,
                        budgetDao = container.budgetDao,
                        goalDao = container.goalDao,
                        investmentAssetDao = container.investmentAssetDao,
                        userProfileStore = container.userProfileStore,
                        clearExisting = false
                    )
                }
            }
        }
    }
}
