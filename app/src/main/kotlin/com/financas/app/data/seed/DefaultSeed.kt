package com.financas.app.data.seed

import com.financas.app.data.db.CategoryDao
import com.financas.app.data.entity.CategoryEntity

object DefaultSeed {

    data class ContextItem(
        val tag: String,
        val label: String,
        val icon: String
    )

    data class PaymentMethodItem(
        val code: String,
        val label: String,
        val icon: String
    )

    val CONTEXTS: List<ContextItem> = listOf(
        ContextItem("TRABALHO", "Trabalho", "work"),
        ContextItem("CASA", "Casa", "home"),
        ContextItem("SAIDA", "Saída / Social", "celebration"),
        ContextItem("FERIAS", "Férias / Viagem", "flight"),
        ContextItem("ONLINE", "Online", "shopping_cart"),
        ContextItem("COMERCIO", "Comércio / Rua", "storefront")
    )

    val PAYMENT_METHODS: List<PaymentMethodItem> = listOf(
        PaymentMethodItem("MBWAY", "MB Way", "phone_android"),
        PaymentMethodItem("CARD", "Cartão", "credit_card"),
        PaymentMethodItem("DIRECT_DEBIT", "Débito Direto", "account_balance"),
        PaymentMethodItem("TRANSFER", "Transferência", "swap_horiz"),
        PaymentMethodItem("CASH", "Dinheiro", "payments")
    )

    fun getDefaultCategories(): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        var sortOrder = 0

        // ================= RECEITAS (INCOME) =================
        // Grupos Pais
        val incomeFixed = CategoryEntity(
            id = "inc_fixed",
            parentId = null,
            name = "Rendimento Fixo",
            icon = "payments",
            colorHex = 0xFF2E7D32, // Verde escuro
            type = "INCOME",
            isDefaultFixed = true,
            sortOrder = sortOrder++
        )
        val incomeExtra = CategoryEntity(
            id = "inc_extra",
            parentId = null,
            name = "Rendimento Extra",
            icon = "savings",
            colorHex = 0xFF4CAF50, // Verde claro
            type = "INCOME",
            isDefaultFixed = false,
            sortOrder = sortOrder++
        )
        list.add(incomeFixed)
        list.add(incomeExtra)

        // Subcategorias de Rendimento Fixo
        list.add(CategoryEntity("inc_base_salary", "inc_fixed", "Salário Base", "payments", 0xFF2E7D32, "INCOME", true, sortOrder++))
        list.add(CategoryEntity("inc_meal_allowance", "inc_fixed", "Subsídio de Alimentação", "restaurant", 0xFF388E3C, "INCOME", true, sortOrder++))

        // Subcategorias de Rendimento Extra
        list.add(CategoryEntity("inc_bonus", "inc_extra", "Prémios / Bónus", "military_tech", 0xFF66BB6A, "INCOME", false, sortOrder++))
        list.add(CategoryEntity("inc_vacation_xmas", "inc_extra", "Subsídio Férias / Natal", "beach_access", 0xFF81C784, "INCOME", false, sortOrder++))
        list.add(CategoryEntity("inc_refunds", "inc_extra", "Reembolsos / MB Way", "currency_exchange", 0xFFA5D6A7, "INCOME", false, sortOrder++))
        list.add(CategoryEntity("inc_other", "inc_extra", "Outros Rendimentos", "attach_money", 0xFFC8E6C9, "INCOME", false, sortOrder++))

        // ================= GASTOS (EXPENSE) =================
        // 1. Habitação & Contas
        val housing = CategoryEntity("exp_housing", null, "Habitação & Contas", "home", 0xFF1976D2, "EXPENSE", true, sortOrder++)
        list.add(housing)
        list.add(CategoryEntity("exp_housing_rent", "exp_housing", "Renda / Prestação", "real_estate_agent", 0xFF1976D2, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_housing_energy", "exp_housing", "Eletricidade & Gás", "bolt", 0xFF2196F3, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_housing_water", "exp_housing", "Água", "water_drop", 0xFF42A5F5, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_housing_telecom", "exp_housing", "Internet & Telecom", "wifi", 0xFF64B5F6, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_housing_maintenance", "exp_housing", "Manutenção Casa", "build", 0xFF90CAF9, "EXPENSE", false, sortOrder++))

        // 2. Alimentação
        val food = CategoryEntity("exp_food", null, "Alimentação", "restaurant", 0xFFFF9800, "EXPENSE", false, sortOrder++)
        list.add(food)
        list.add(CategoryEntity("exp_food_supermarket", "exp_food", "Supermercado", "shopping_cart", 0xFFFF9800, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_food_lunch", "exp_food", "Almoços / Trabalho", "lunch_dining", 0xFFFFA726, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_food_restaurant", "exp_food", "Restaurantes & Jantares", "dinner_dining", 0xFFFFB74D, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_food_cafe", "exp_food", "Cafés & Pastelaria", "coffee", 0xFFFFCC80, "EXPENSE", false, sortOrder++))

        // 3. Transportes
        val transport = CategoryEntity("exp_transport", null, "Transportes", "directions_car", 0xFF009688, "EXPENSE", false, sortOrder++)
        list.add(transport)
        list.add(CategoryEntity("exp_transport_fuel", "exp_transport", "Combustível", "local_gas_station", 0xFF009688, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_transport_tolls", "exp_transport", "Portagens & Estacionamento", "toll", 0xFF26A69A, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_transport_public", "exp_transport", "Transportes Públicos / TVDE", "directions_subway", 0xFF4DB6AC, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_transport_maintenance", "exp_transport", "Manutenção & Seguro Auto", "car_repair", 0xFF80CBC4, "EXPENSE", true, sortOrder++))

        // 4. Saúde & Bem-Estar
        val health = CategoryEntity("exp_health", null, "Saúde & Bem-Estar", "favorite", 0xFFE91E63, "EXPENSE", false, sortOrder++)
        list.add(health)
        list.add(CategoryEntity("exp_health_appointments", "exp_health", "Consultas & Exames", "medical_services", 0xFFE91E63, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_health_pharmacy", "exp_health", "Farmácia", "local_pharmacy", 0xFFEC407A, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_health_gym", "exp_health", "Ginásio & Desporto", "fitness_center", 0xFFF06292, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_health_personal_care", "exp_health", "Cuidados Pessoais", "spa", 0xFFF48FB1, "EXPENSE", false, sortOrder++))

        // 5. Lazer & Social
        val leisure = CategoryEntity("exp_leisure", null, "Lazer & Social", "nightlife", 0xFF9C27B0, "EXPENSE", false, sortOrder++)
        list.add(leisure)
        list.add(CategoryEntity("exp_leisure_bars", "exp_leisure", "Saídas & Bares", "local_bar", 0xFF9C27B0, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_leisure_culture", "exp_leisure", "Cultura & Eventos", "theater_comedy", 0xFFAB47BC, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_leisure_subscriptions", "exp_leisure", "Subscrições Digitais", "subscriptions", 0xFFBA68C8, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_leisure_hobbies", "exp_leisure", "Hobbies", "palette", 0xFFCE93D8, "EXPENSE", false, sortOrder++))

        // 6. Compras & Educação
        val shopping = CategoryEntity("exp_shopping", null, "Compras & Educação", "shopping_bag", 0xFF00BCD4, "EXPENSE", false, sortOrder++)
        list.add(shopping)
        list.add(CategoryEntity("exp_shopping_clothing", "exp_shopping", "Roupa & Calçado", "checkroom", 0xFF00BCD4, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_shopping_tech", "exp_shopping", "Tecnologia & Equipamento", "devices", 0xFF26C6DA, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_shopping_education", "exp_shopping", "Formação & Livros", "menu_book", 0xFF4DD0E1, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_shopping_gifts", "exp_shopping", "Prendas", "card_giftcard", 0xFF80DEEA, "EXPENSE", false, sortOrder++))

        // 7. Férias & Viagens
        val travel = CategoryEntity("exp_travel", null, "Férias & Viagens", "flight_takeoff", 0xFFF44336, "EXPENSE", false, sortOrder++)
        list.add(travel)
        list.add(CategoryEntity("exp_travel_lodging", "exp_travel", "Alojamento", "hotel", 0xFFF44336, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_travel_flights", "exp_travel", "Voos & Deslocações", "flight", 0xFFEF5350, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_travel_activities", "exp_travel", "Atividades & Refeições em Viagem", "tour", 0xFFE57373, "EXPENSE", false, sortOrder++))

        // 8. Financeiro & Outros
        val financial = CategoryEntity("exp_financial", null, "Financeiro & Outros", "account_balance", 0xFF607D8B, "EXPENSE", false, sortOrder++)
        list.add(financial)
        list.add(CategoryEntity("exp_financial_savings", "exp_financial", "Poupança / Investimento", "trending_up", 0xFF607D8B, "EXPENSE", true, sortOrder++))
        list.add(CategoryEntity("exp_financial_taxes", "exp_financial", "Impostos & Taxas", "receipt_long", 0xFF78909C, "EXPENSE", false, sortOrder++))
        list.add(CategoryEntity("exp_financial_other", "exp_financial", "Outros / Imprevistos", "help_outline", 0xFF90A4AE, "EXPENSE", false, sortOrder++))

        return list
    }

    suspend fun seedIfNeeded(categoryDao: CategoryDao) {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(getDefaultCategories())
        }
    }
}
