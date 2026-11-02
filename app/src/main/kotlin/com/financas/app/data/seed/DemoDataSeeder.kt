package com.financas.app.data.seed

import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.prefs.UserProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

object DemoDataSeeder {

    suspend fun seedDemoData(
        categoryDao: CategoryDao,
        transactionDao: TransactionDao,
        budgetDao: BudgetDao,
        goalDao: GoalDao,
        investmentAssetDao: InvestmentAssetDao,
        userProfileStore: UserProfileStore?,
        clearExisting: Boolean = true
    ) = withContext(Dispatchers.IO) {
        // 1. Ensure categories are present
        DefaultSeed.seedIfNeeded(categoryDao)

        if (clearExisting) {
            transactionDao.deleteAll()
        }

        val now = LocalDate.now()
        val currentYearMonth = YearMonth.from(now)
        val m0 = currentYearMonth
        val m1 = currentYearMonth.minusMonths(1)
        val m2 = currentYearMonth.minusMonths(2)

        val transactions = mutableListOf<TransactionEntity>()

        // Generate transactions for M0, M1, M2
        listOf(m2, m1, m0).forEach { ym ->
            val ymInt = ym.year * 100 + ym.monthValue
            val isCurrentMonth = ym == m0
            val maxDay = if (isCurrentMonth) minOf(now.dayOfMonth, ym.lengthOfMonth()) else ym.lengthOfMonth()

            fun dateFor(day: Int): LocalDate {
                val clamped = day.coerceIn(1, ym.lengthOfMonth())
                return ym.atDay(clamped)
            }

            // Fixed Incomes
            if (maxDay >= 1) {
                transactions.add(
                    TransactionEntity(
                        type = "INCOME",
                        amountCents = 245000L, // 2.450,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(1).toEpochDay(),
                        categoryId = "inc_base_salary",
                        contextTag = "TRABALHO",
                        merchant = "Tech Corp Inc.",
                        paymentMethod = "TRANSFER",
                        isFixed = true
                    )
                )
            }

            if (maxDay >= 5) {
                transactions.add(
                    TransactionEntity(
                        type = "INCOME",
                        amountCents = 19800L, // 198,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(5).toEpochDay(),
                        categoryId = "inc_meal_allowance",
                        contextTag = "TRABALHO",
                        merchant = "Cartão Refeição",
                        paymentMethod = "CARD",
                        isFixed = true
                    )
                )
            }

            if (maxDay >= 15 && ym == m1) {
                transactions.add(
                    TransactionEntity(
                        type = "INCOME",
                        amountCents = 14500L, // 145,00 € extra
                        yearMonth = ymInt,
                        epochDay = dateFor(15).toEpochDay(),
                        categoryId = "inc_bonus",
                        contextTag = "ONLINE",
                        merchant = "Dividendos ETF / Investimentos",
                        paymentMethod = "TRANSFER",
                        isFixed = false
                    )
                )
            }

            // Fixed Expenses (Monthly Commitments)
            if (maxDay >= 1) {
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 75000L, // 750,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(1).toEpochDay(),
                        categoryId = "exp_housing_rent",
                        contextTag = "CASA",
                        merchant = "Senhorio / Renda",
                        paymentMethod = "TRANSFER",
                        isFixed = true
                    )
                )
            }

            if (maxDay >= 3) {
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 6840L, // 68,40 €
                        yearMonth = ymInt,
                        epochDay = dateFor(3).toEpochDay(),
                        categoryId = "exp_housing_energy",
                        contextTag = "CASA",
                        merchant = "EDP Comercial",
                        paymentMethod = "DIRECT_DEBIT",
                        isFixed = true
                    )
                )
            }

            if (maxDay >= 4) {
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 3990L, // 39,90 €
                        yearMonth = ymInt,
                        epochDay = dateFor(4).toEpochDay(),
                        categoryId = "exp_housing_telecom",
                        contextTag = "CASA",
                        merchant = "Vodafone Fibra",
                        paymentMethod = "DIRECT_DEBIT",
                        isFixed = true
                    )
                )
            }

            if (maxDay >= 5) {
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 3499L, // 34,99 €
                        yearMonth = ymInt,
                        epochDay = dateFor(5).toEpochDay(),
                        categoryId = "exp_health_gym",
                        contextTag = "CASA",
                        merchant = "Fitness Hut Ginásio",
                        paymentMethod = "DIRECT_DEBIT",
                        isFixed = true
                    )
                )
            }

            if (maxDay >= 7) {
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 2798L, // 27,98 €
                        yearMonth = ymInt,
                        epochDay = dateFor(7).toEpochDay(),
                        categoryId = "exp_leisure_subscriptions",
                        contextTag = "ONLINE",
                        merchant = "Netflix & Spotify Premium",
                        paymentMethod = "CARD",
                        isFixed = true
                    )
                )
            }

            // Variable Expenses
            val variableItems = listOf(
                Triple(2, 8450L, "Continente Bom Dia" to "exp_food_supermarket"),
                Triple(4, 1250L, "Almoço Equipa" to "exp_food_lunch"),
                Triple(6, 6500L, "Galp Combustível" to "exp_transport_fuel"),
                Triple(8, 4800L, "Jantar com Amigos" to "exp_food_restaurant"),
                Triple(9, 380L, "Café da Manhã" to "exp_food_cafe"),
                Triple(11, 4230L, "Pingo Doce Supermercado" to "exp_food_supermarket"),
                Triple(13, 2150L, "Farmácia Wells" to "exp_health_pharmacy"),
                Triple(15, 1420L, "Via Verde Portagens" to "exp_transport_tolls"),
                Triple(17, 2400L, "Cinema NOS & Pipocas" to "exp_leisure_culture"),
                Triple(19, 7890L, "Compras Supermercado Auchan" to "exp_food_supermarket"),
                Triple(21, 3500L, "Livraria Bertrand" to "exp_shopping"),
                Triple(23, 5400L, "Jantar Sushi" to "exp_food_restaurant"),
                Triple(25, 6200L, "BP Combustível" to "exp_transport_fuel"),
                Triple(27, 2850L, "Padaria & Pastelaria" to "exp_food_cafe")
            )

            variableItems.forEach { (day, amount, pair) ->
                if (maxDay >= day) {
                    val (merchant, catId) = pair
                    val contextTag = when {
                        catId.contains("food") && day % 2 == 0 -> "SAIDA"
                        catId.contains("transport") -> "COMERCIO"
                        catId.contains("health") -> "COMERCIO"
                        else -> "CASA"
                    }
                    val method = when (day % 3) {
                        0 -> "MBWAY"
                        1 -> "CARD"
                        else -> "CASH"
                    }
                    transactions.add(
                        TransactionEntity(
                            type = "EXPENSE",
                            amountCents = amount,
                            yearMonth = ymInt,
                            epochDay = dateFor(day).toEpochDay(),
                            categoryId = catId,
                            contextTag = contextTag,
                            merchant = merchant,
                            paymentMethod = method,
                            isFixed = false
                        )
                    )
                }
            }
        }

        transactionDao.insertAll(transactions)

        // 2. Budgets
        budgetDao.insert(BudgetEntity("b_food", "exp_food", 45000L)) // 450 €
        budgetDao.insert(BudgetEntity("b_housing", "exp_housing", 95000L)) // 950 €
        budgetDao.insert(BudgetEntity("b_transport", "exp_transport", 18000L)) // 180 €
        budgetDao.insert(BudgetEntity("b_leisure", "exp_leisure", 15000L)) // 150 €

        // 3. Goals
        goalDao.insert(
            GoalEntity(
                id = "goal_japan_trip",
                name = "Férias no Japão ⛩️",
                targetCents = 350000L, // 3.500 €
                startYearMonth = m2.year * 100 + m2.monthValue,
                deadlineYearMonth = m0.plusMonths(5).let { it.year * 100 + it.monthValue }
            )
        )
        goalDao.insert(
            GoalEntity(
                id = "goal_emergency_fund",
                name = "Fundo de Emergência (6 Meses)",
                targetCents = 900000L, // 9.000 €
                startYearMonth = m2.minusMonths(6).let { it.year * 100 + it.monthValue },
                deadlineYearMonth = m0.plusMonths(10).let { it.year * 100 + it.monthValue }
            )
        )

        // 4. Investment Assets
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_ppr_alves_ribeiro",
                name = "PPR Alves Ribeiro / Stoik",
                type = "PPR",
                institution = "Banco Invest",
                investedCents = 200000L, // 2.000,00 €
                currentValueCents = 218500L, // 2.185,00 € (+9.25%)
                monthlyContributionCents = 15000L, // 150,00 €/mês
                targetCents = 500000L,
                notes = "Otimizado para dedução máxima de 400 € no IRS (Art. 21º EBF).",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_etf_vwce",
                name = "Vanguard FTSE All-World (VWCE)",
                type = "ETF",
                institution = "Interactive Brokers",
                investedCents = 600000L, // 6.000,00 €
                currentValueCents = 684200L, // 6.842,00 € (+14.0%)
                monthlyContributionCents = 25000L, // 250,00 €/mês
                targetCents = 2000000L,
                notes = "Acumulação global passiva de longo prazo.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_certificados_aforro",
                name = "Certificados de Aforro Série F",
                type = "CERTIFICADOS",
                institution = "IGCP / CTT",
                investedCents = 500000L, // 5.000,00 €
                currentValueCents = 512500L, // 5.125,00 € (+2.5%)
                monthlyContributionCents = 0L,
                targetCents = 600000L,
                notes = "Liquidez de curto prazo / reserva segura.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // 5. User Profile
        userProfileStore?.setUserName("Paulo (Demonstração)")
        userProfileStore?.setDefaultCurrency("EUR (€)")
        userProfileStore?.setMonthlyCycleDay(1)
        userProfileStore?.setThemeMode("DARK")
    }
}
