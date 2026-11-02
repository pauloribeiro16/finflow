package com.financas.app.data.seed

import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.PendingTransactionDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.PendingTransactionEntity
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
        pendingTransactionDao: PendingTransactionDao? = null,
        clearExisting: Boolean = true
    ) = withContext(Dispatchers.IO) {
        // 1. Ensure categories are present
        DefaultSeed.seedIfNeeded(categoryDao)

        if (clearExisting) {
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            goalDao.deleteAll()
            investmentAssetDao.deleteAll()
            pendingTransactionDao?.clearAll()
        }

        val now = LocalDate.now()
        val currentYear = now.year

        // Generate 27 months: Q4 2024, all of 2025, and all of 2026
        val monthsToGenerate = mutableListOf<YearMonth>()
        // 2024 (Oct, Nov, Dec)
        for (m in 10..12) {
            monthsToGenerate.add(YearMonth.of(2024, m))
        }
        // 2025 (All 12 months)
        for (m in 1..12) {
            monthsToGenerate.add(YearMonth.of(2025, m))
        }
        // 2026 (All 12 months for full annual analytics)
        for (m in 1..12) {
            monthsToGenerate.add(YearMonth.of(2026, m))
        }

        val transactions = mutableListOf<TransactionEntity>()

        monthsToGenerate.forEach { ym ->
            val ymInt = ym.year * 100 + ym.monthValue
            val daysInMonth = ym.lengthOfMonth()

            fun dateFor(day: Int): LocalDate {
                val clamped = day.coerceIn(1, daysInMonth)
                return ym.atDay(clamped)
            }

            // === 1. RENDIMENTOS FIXOS & EXTRAS ===
            // Salário Base (sempre no dia 1)
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

            // Subsídio de Refeição (dia 5)
            transactions.add(
                TransactionEntity(
                    type = "INCOME",
                    amountCents = 19800L, // 198,00 €
                    yearMonth = ymInt,
                    epochDay = dateFor(5).toEpochDay(),
                    categoryId = "inc_meal_allowance",
                    contextTag = "TRABALHO",
                    merchant = "Cartão Refeição Sodexo",
                    paymentMethod = "CARD",
                    isFixed = true
                )
            )

            // Rendimentos sazonais / extras
            when (ym.monthValue) {
                5 -> {
                    // Reembolso IRS em Maio
                    transactions.add(
                        TransactionEntity(
                            type = "INCOME",
                            amountCents = 68000L, // 680,00 €
                            yearMonth = ymInt,
                            epochDay = dateFor(14).toEpochDay(),
                            categoryId = "inc_refunds",
                            contextTag = "ONLINE",
                            merchant = "Autoridade Tributária (IRS)",
                            paymentMethod = "TRANSFER",
                            isFixed = false
                        )
                    )
                }
                6 -> {
                    // Subsídio de Férias em Junho
                    transactions.add(
                        TransactionEntity(
                            type = "INCOME",
                            amountCents = 122500L, // 1.225,00 €
                            yearMonth = ymInt,
                            epochDay = dateFor(15).toEpochDay(),
                            categoryId = "inc_vacation_xmas",
                            contextTag = "TRABALHO",
                            merchant = "Tech Corp (Subsídio Férias)",
                            paymentMethod = "TRANSFER",
                            isFixed = false
                        )
                    )
                }
                11 -> {
                    // Subsídio de Natal em Novembro
                    transactions.add(
                        TransactionEntity(
                            type = "INCOME",
                            amountCents = 122500L, // 1.225,00 €
                            yearMonth = ymInt,
                            epochDay = dateFor(18).toEpochDay(),
                            categoryId = "inc_vacation_xmas",
                            contextTag = "TRABALHO",
                            merchant = "Tech Corp (Subsídio Natal)",
                            paymentMethod = "TRANSFER",
                            isFixed = false
                        )
                    )
                }
                3, 9 -> {
                    // Bónus trimestral de objetivos
                    transactions.add(
                        TransactionEntity(
                            type = "INCOME",
                            amountCents = 35000L, // 350,00 €
                            yearMonth = ymInt,
                            epochDay = dateFor(20).toEpochDay(),
                            categoryId = "inc_bonus",
                            contextTag = "TRABALHO",
                            merchant = "Prémio Performance Trimestral",
                            paymentMethod = "TRANSFER",
                            isFixed = false
                        )
                    )
                }
                1, 4, 7, 10 -> {
                    // Dividendos de ETFs trimestrais
                    transactions.add(
                        TransactionEntity(
                            type = "INCOME",
                            amountCents = 9500L, // 95,00 €
                            yearMonth = ymInt,
                            epochDay = dateFor(12).toEpochDay(),
                            categoryId = "inc_other",
                            contextTag = "ONLINE",
                            merchant = "Dividendos Vanguard All-World ETF",
                            paymentMethod = "TRANSFER",
                            isFixed = false
                        )
                    )
                }
            }

            // === 2. DESPESAS FIXAS MENSAIS (COMPROMISSOS) ===
            // Renda Casa
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 75000L, // 750,00 €
                    yearMonth = ymInt,
                    epochDay = dateFor(1).toEpochDay(),
                    categoryId = "exp_housing_rent",
                    contextTag = "CASA",
                    merchant = "Senhorio / Renda Apartamento",
                    paymentMethod = "TRANSFER",
                    isFixed = true
                )
            )

            // Eletricidade & Gás (variação sazonal inverno/verão)
            val energyAmount = when (ym.monthValue) {
                12, 1, 2 -> 7840L // 78,40 € inverno
                7, 8 -> 5420L    // 54,20 € verão
                else -> 6630L    // 66,30 € média
            }
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = energyAmount,
                    yearMonth = ymInt,
                    epochDay = dateFor(3).toEpochDay(),
                    categoryId = "exp_housing_energy",
                    contextTag = "CASA",
                    merchant = "EDP Comercial Eletricidade",
                    paymentMethod = "DIRECT_DEBIT",
                    isFixed = true
                )
            )

            // Água
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 2480L, // 24,80 €
                    yearMonth = ymInt,
                    epochDay = dateFor(4).toEpochDay(),
                    categoryId = "exp_housing_water",
                    contextTag = "CASA",
                    merchant = "EPAL Águas de Lisboa",
                    paymentMethod = "DIRECT_DEBIT",
                    isFixed = true
                )
            )

            // Internet & Fibra
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 3990L, // 39,90 €
                    yearMonth = ymInt,
                    epochDay = dateFor(4).toEpochDay(),
                    categoryId = "exp_housing_telecom",
                    contextTag = "CASA",
                    merchant = "Vodafone Fibra 1Gbps",
                    paymentMethod = "DIRECT_DEBIT",
                    isFixed = true
                )
            )

            // Ginásio
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

            // Subscrições Digitais
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

            // Passe de Transportes
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 4000L, // 40,00 €
                    yearMonth = ymInt,
                    epochDay = dateFor(10).toEpochDay(),
                    categoryId = "exp_transport_public",
                    contextTag = "COMERCIO",
                    merchant = "Passe Navegante Metropolitano",
                    paymentMethod = "CARD",
                    isFixed = true
                )
            )

            // === 3. DESPESAS VARIÁVEIS DISTRIBUÍDAS POR TODAS AS 4 SEMANAS ===
            // SEMANA 1 (Dias 1 a 7)
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 8450L,
                    yearMonth = ymInt,
                    epochDay = dateFor(2).toEpochDay(),
                    categoryId = "exp_food_supermarket",
                    contextTag = "CASA",
                    merchant = "Continente Bom Dia",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 420L,
                    yearMonth = ymInt,
                    epochDay = dateFor(3).toEpochDay(),
                    categoryId = "exp_food_cafe",
                    contextTag = "SAIDA",
                    merchant = "Café & Tosta Mista",
                    paymentMethod = "MBWAY"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1250L,
                    yearMonth = ymInt,
                    epochDay = dateFor(4).toEpochDay(),
                    categoryId = "exp_food_lunch",
                    contextTag = "TRABALHO",
                    merchant = "Almoço Equipa Trabalho",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1890L,
                    yearMonth = ymInt,
                    epochDay = dateFor(5).toEpochDay(),
                    categoryId = "exp_health_pharmacy",
                    contextTag = "COMERCIO",
                    merchant = "Farmácia Wells",
                    paymentMethod = "MBWAY"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 6500L,
                    yearMonth = ymInt,
                    epochDay = dateFor(6).toEpochDay(),
                    categoryId = "exp_transport_fuel",
                    contextTag = "COMERCIO",
                    merchant = "Galp Combustível",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 3800L,
                    yearMonth = ymInt,
                    epochDay = dateFor(7).toEpochDay(),
                    categoryId = "exp_food_restaurant",
                    contextTag = "SAIDA",
                    merchant = "Jantar Amigos Sexta",
                    paymentMethod = "MBWAY"
                )
            )

            // SEMANA 2 (Dias 8 a 14)
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 2200L,
                    yearMonth = ymInt,
                    epochDay = dateFor(8).toEpochDay(),
                    categoryId = "exp_leisure_culture",
                    contextTag = "SAIDA",
                    merchant = "Cinema NOS & Pipocas",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 380L,
                    yearMonth = ymInt,
                    epochDay = dateFor(9).toEpochDay(),
                    categoryId = "exp_food_cafe",
                    contextTag = "CASA",
                    merchant = "Padaria do Bairro",
                    paymentMethod = "CASH"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1420L,
                    yearMonth = ymInt,
                    epochDay = dateFor(10).toEpochDay(),
                    categoryId = "exp_transport_tolls",
                    contextTag = "COMERCIO",
                    merchant = "Portagens Via Verde",
                    paymentMethod = "DIRECT_DEBIT"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 7640L,
                    yearMonth = ymInt,
                    epochDay = dateFor(11).toEpochDay(),
                    categoryId = "exp_food_supermarket",
                    contextTag = "CASA",
                    merchant = "Pingo Doce Supermercado",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1150L,
                    yearMonth = ymInt,
                    epochDay = dateFor(12).toEpochDay(),
                    categoryId = "exp_food_lunch",
                    contextTag = "TRABALHO",
                    merchant = "Almoço Trabalho Menu Dia",
                    paymentMethod = "CASH"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 2490L,
                    yearMonth = ymInt,
                    epochDay = dateFor(13).toEpochDay(),
                    categoryId = "exp_shopping_education",
                    contextTag = "ONLINE",
                    merchant = "Livraria Bertrand / Livros",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 2850L,
                    yearMonth = ymInt,
                    epochDay = dateFor(14).toEpochDay(),
                    categoryId = "exp_food_restaurant",
                    contextTag = "SAIDA",
                    merchant = "Hamburgueria Artesanal",
                    paymentMethod = "MBWAY"
                )
            )

            // SEMANA 3 (Dias 15 a 21)
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 5500L,
                    yearMonth = ymInt,
                    epochDay = dateFor(15).toEpochDay(),
                    categoryId = "exp_health_appointments",
                    contextTag = "COMERCIO",
                    merchant = "Consulta Médica / Exames",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 980L,
                    yearMonth = ymInt,
                    epochDay = dateFor(16).toEpochDay(),
                    categoryId = "exp_transport_public",
                    contextTag = "COMERCIO",
                    merchant = "Uber Deslocação",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 9230L,
                    yearMonth = ymInt,
                    epochDay = dateFor(17).toEpochDay(),
                    categoryId = "exp_food_supermarket",
                    contextTag = "CASA",
                    merchant = "Auchan Hipermercado",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 5800L,
                    yearMonth = ymInt,
                    epochDay = dateFor(18).toEpochDay(),
                    categoryId = "exp_transport_fuel",
                    contextTag = "COMERCIO",
                    merchant = "BP Combustível",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 4200L,
                    yearMonth = ymInt,
                    epochDay = dateFor(19).toEpochDay(),
                    categoryId = "exp_food_restaurant",
                    contextTag = "SAIDA",
                    merchant = "Trattoria Italiana",
                    paymentMethod = "MBWAY"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 6990L,
                    yearMonth = ymInt,
                    epochDay = dateFor(20).toEpochDay(),
                    categoryId = "exp_shopping_clothing",
                    contextTag = "COMERCIO",
                    merchant = "Zara / Calçado & Roupa",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 540L,
                    yearMonth = ymInt,
                    epochDay = dateFor(21).toEpochDay(),
                    categoryId = "exp_food_cafe",
                    contextTag = "SAIDA",
                    merchant = "Pastelaria Domingo",
                    paymentMethod = "MBWAY"
                )
            )

            // SEMANA 4 (Dias 22 a 28/31)
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1300L,
                    yearMonth = ymInt,
                    epochDay = dateFor(22).toEpochDay(),
                    categoryId = "exp_food_lunch",
                    contextTag = "TRABALHO",
                    merchant = "Almoço Trabalho",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 5200L,
                    yearMonth = ymInt,
                    epochDay = dateFor(23).toEpochDay(),
                    categoryId = "exp_food_restaurant",
                    contextTag = "SAIDA",
                    merchant = "Jantar Sushi & Cocktails",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 6850L,
                    yearMonth = ymInt,
                    epochDay = dateFor(24).toEpochDay(),
                    categoryId = "exp_food_supermarket",
                    contextTag = "CASA",
                    merchant = "Mercadona Compras Semana",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1800L,
                    yearMonth = ymInt,
                    epochDay = dateFor(25).toEpochDay(),
                    categoryId = "exp_health_personal_care",
                    contextTag = "COMERCIO",
                    merchant = "Barba & Cabelo",
                    paymentMethod = "CASH"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 1180L,
                    yearMonth = ymInt,
                    epochDay = dateFor(26).toEpochDay(),
                    categoryId = "exp_transport_tolls",
                    contextTag = "COMERCIO",
                    merchant = "Portagens Autoestrada",
                    paymentMethod = "DIRECT_DEBIT"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 3500L,
                    yearMonth = ymInt,
                    epochDay = dateFor(27).toEpochDay(),
                    categoryId = "exp_leisure_hobbies",
                    contextTag = "ONLINE",
                    merchant = "Fnac Gadgets & Acessórios",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 5200L,
                    yearMonth = ymInt,
                    epochDay = dateFor(28).toEpochDay(),
                    categoryId = "exp_transport_fuel",
                    contextTag = "COMERCIO",
                    merchant = "Galp Combustível",
                    paymentMethod = "CARD"
                )
            )
            transactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountCents = 2400L,
                    yearMonth = ymInt,
                    epochDay = dateFor(minOf(29, daysInMonth)).toEpochDay(),
                    categoryId = "exp_food_restaurant",
                    contextTag = "CASA",
                    merchant = "Jantar Takeaway Pizza",
                    paymentMethod = "MBWAY"
                )
            )

            // Eventos Especiais Sazonais
            if (ym.monthValue == 7) {
                // Férias de Verão - Alojamento
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 52000L, // 520,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(15).toEpochDay(),
                        categoryId = "exp_travel_lodging",
                        contextTag = "FERIAS",
                        merchant = "Booking Alojamento Algarve",
                        paymentMethod = "CARD"
                    )
                )
            } else if (ym.monthValue == 8) {
                // Férias de Verão - Atividades
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 21000L, // 210,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(18).toEpochDay(),
                        categoryId = "exp_travel_activities",
                        contextTag = "FERIAS",
                        merchant = "Passeios & Experiências Férias",
                        paymentMethod = "CARD"
                    )
                )
            } else if (ym.monthValue == 12) {
                // Natal - Prendas e Supermercado Natalício
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 26000L, // 260,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(18).toEpochDay(),
                        categoryId = "exp_shopping_gifts",
                        contextTag = "SAIDA",
                        merchant = "Prendas de Natal Família",
                        paymentMethod = "CARD"
                    )
                )
                transactions.add(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = 13500L, // 135,00 €
                        yearMonth = ymInt,
                        epochDay = dateFor(22).toEpochDay(),
                        categoryId = "exp_food_supermarket",
                        contextTag = "CASA",
                        merchant = "Compras Ceia de Natal",
                        paymentMethod = "CARD"
                    )
                )
            }
        }

        transactionDao.insertAll(transactions)

        // === 2. ORÇAMENTOS (6 CATEGORIAS CHAVE) ===
        budgetDao.insert(BudgetEntity("b_food", "exp_food", 45000L)) // 450 €
        budgetDao.insert(BudgetEntity("b_housing", "exp_housing", 95000L)) // 950 €
        budgetDao.insert(BudgetEntity("b_transport", "exp_transport", 18000L)) // 180 €
        budgetDao.insert(BudgetEntity("b_leisure", "exp_leisure", 15000L)) // 150 €
        budgetDao.insert(BudgetEntity("b_health", "exp_health", 8000L)) // 80 €
        budgetDao.insert(BudgetEntity("b_shopping", "exp_shopping", 12000L)) // 120 €

        // === 3. METAS DE POUPANÇA (4 OBJETIVOS) ===
        goalDao.insert(
            GoalEntity(
                id = "goal_emergency_fund",
                name = "Fundo de Emergência (6 Meses)",
                targetCents = 900000L, // 9.000 €
                startYearMonth = 202501,
                deadlineYearMonth = 202612
            )
        )
        goalDao.insert(
            GoalEntity(
                id = "goal_japan_trip",
                name = "Férias no Japão ⛩️",
                targetCents = 350000L, // 3.500 €
                startYearMonth = 202601,
                deadlineYearMonth = 202705
            )
        )
        goalDao.insert(
            GoalEntity(
                id = "goal_house_downpayment",
                name = "Entrada Casa Própria 🏡",
                targetCents = 2500000L, // 25.000 €
                startYearMonth = 202410,
                deadlineYearMonth = 202812
            )
        )
        goalDao.insert(
            GoalEntity(
                id = "goal_tech_setup",
                name = "Setup & Portátil de Trabalho 💻",
                targetCents = 180000L, // 1.800 €
                startYearMonth = 202603,
                deadlineYearMonth = 202611
            )
        )

        // === 4. PATRIMÓNIO: INVESTIMENTOS DE TODOS OS TIPOS (6 TIPOS) ===
        // Tipo 1: PPR
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
                id = "asset_ppr_casa_global",
                name = "PPR Casa Global Ações",
                type = "PPR",
                institution = "Bankinter",
                investedCents = 150000L, // 1.500,00 €
                currentValueCents = 164000L, // 1.640,00 € (+9.33%)
                monthlyContributionCents = 10000L,
                targetCents = 400000L,
                notes = "Fundo PPR 100% ações globais com benefício fiscal à saída.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // Tipo 2: ETF
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_etf_vwce",
                name = "Vanguard FTSE All-World (VWCE)",
                type = "ETF",
                institution = "Interactive Brokers",
                investedCents = 650000L, // 6.500,00 €
                currentValueCents = 742000L, // 7.420,00 € (+14.15%)
                monthlyContributionCents = 25000L, // 250,00 €/mês
                targetCents = 2500000L,
                notes = "Acumulação passiva mundial em mais de 3.600 empresas cotadas.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_etf_sxr8",
                name = "iShares Core S&P 500 (SXR8)",
                type = "ETF",
                institution = "Degiro",
                investedCents = 320000L, // 3.200,00 €
                currentValueCents = 378000L, // 3.780,00 € (+18.13%)
                monthlyContributionCents = 15000L,
                targetCents = 1500000L,
                notes = "Exposição direta às 500 maiores empresas dos Estados Unidos.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // Tipo 3: CERTIFICADOS_AFORRO
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_certificados_aforro",
                name = "Certificados de Aforro Série F",
                type = "CERTIFICADOS_AFORRO",
                institution = "IGCP / CTT",
                investedCents = 500000L, // 5.000,00 €
                currentValueCents = 516000L, // 5.160,00 € (+3.20%)
                monthlyContributionCents = 0L,
                targetCents = 1000000L,
                notes = "Reserva de capital garantido com prémio de permanência trimestral.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // Tipo 4: DEPOSITO_PRAZO
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_deposito_prazo",
                name = "Depósito a Prazo 3.5% Remunerado",
                type = "DEPOSITO_PRAZO",
                institution = "Trade Republic",
                investedCents = 400000L, // 4.000,00 €
                currentValueCents = 408500L, // 4.085,00 € (+2.13%)
                monthlyContributionCents = 5000L,
                targetCents = 500000L,
                notes = "Remuneração mensal garantida pelo Banco Central Europeu.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // Tipo 5: CRYPTO
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_crypto_btc_eth",
                name = "Bitcoin & Ethereum Cold Storage",
                type = "CRYPTO",
                institution = "Ledger Vault",
                investedCents = 180000L, // 1.800,00 €
                currentValueCents = 245000L, // 2.450,00 € (+36.11%)
                monthlyContributionCents = 5000L,
                targetCents = 500000L,
                notes = "Autocustódia em hardware wallet para diversificação assimétrica.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // Tipo 6: OUTRO (Metais Preciosos)
        investmentAssetDao.insert(
            InvestmentAssetEntity(
                id = "asset_gold_britannia",
                name = "Ouro Físico (1 oz Britannia Bullion)",
                type = "OUTRO",
                institution = "Degussa Cofre",
                investedCents = 210000L, // 2.100,00 €
                currentValueCents = 238000L, // 2.380,00 € (+13.33%)
                monthlyContributionCents = 0L,
                targetCents = 400000L,
                notes = "Moeda de investimento 999.9 guardada em cofre para proteção contra inflação.",
                lastUpdatedEpochDay = now.toEpochDay()
            )
        )

        // === 5. MOVIMENTOS PENDENTES (NOTIFICAÇÕES DE BANCO POR REVER) ===
        pendingTransactionDao?.insertAll(
            listOf(
                PendingTransactionEntity(
                    amountCents = 1840L,
                    rawMerchant = "Continente Bom Dia Telheiras",
                    suggestedType = "EXPENSE",
                    suggestedCategoryId = "exp_food_supermarket",
                    suggestedContextTag = "CASA",
                    suggestedPaymentMethod = "CARD",
                    isFixed = false,
                    sourceApp = "com.bancocgd.app",
                    detectedEpochDay = now.toEpochDay(),
                    receivedTimestamp = System.currentTimeMillis() - 3600000L,
                    notes = "Compra no supermercado detectada por notificação bancária"
                ),
                PendingTransactionEntity(
                    amountCents = 1500L,
                    rawMerchant = "MB WAY - Jantar Pedro",
                    suggestedType = "EXPENSE",
                    suggestedCategoryId = "exp_food_restaurant",
                    suggestedContextTag = "SAIDA",
                    suggestedPaymentMethod = "MBWAY",
                    isFixed = false,
                    sourceApp = "pt.sibs.android.mbway",
                    detectedEpochDay = now.toEpochDay(),
                    receivedTimestamp = System.currentTimeMillis() - 7200000L,
                    notes = "Divisão de conta de jantar"
                )
            )
        )

        // === 6. PERFIL DE UTILIZADOR DEMO ===
        userProfileStore?.setUserName("Paulo (Demonstração)")
        userProfileStore?.setDefaultCurrency("EUR (€)")
        userProfileStore?.setMonthlyCycleDay(1)
        userProfileStore?.setThemeMode("DARK")
    }
}
