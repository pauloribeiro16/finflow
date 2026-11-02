package com.financas.app.ui.i18n

object CategoryTranslator {

    private val PT_TO_EN = mapOf(
        // Parent Categories
        "Rendimento Fixo" to "Fixed Income",
        "Rendimento Extra" to "Additional Income",
        "Habitação" to "Housing & Utilities",
        "Alimentação" to "Food & Dining",
        "Transportes" to "Transport & Auto",
        "Saúde & Bem-Estar" to "Health & Wellness",
        "Educação & Carreira" to "Education & Career",
        "Lazer & Social" to "Leisure & Social",
        "Lazer / Social" to "Leisure & Social",
        "Tecnologia & Serviços" to "Tech & Services",
        "Compras Pessoais" to "Personal Shopping",
        "Finanças & Seguros" to "Finance & Insurance",
        "Família & Animais" to "Family & Pets",
        "Imprevistos & Doações" to "Emergency & Gifts",

        // Subcategories
        "Salário Base" to "Base Salary",
        "Subsídio de Alimentação" to "Meal Allowance",
        "Subsidio Alimentacao" to "Meal Allowance",
        "Subsídio de Férias/Natal" to "Holiday Bonus",
        "Trabalho Extra / Freelance" to "Freelance & Side Gig",
        "Reembolso de Despesas" to "Expense Reimbursement",
        "Rendas / Imóveis" to "Rental Income",
        "Dividendos & Juros" to "Dividends & Interest",
        "Vendas / Usados" to "Sales & Used Items",
        "Prendas / Outros" to "Gifts & Other",
        "Renda / Prestação Casa" to "Rent / Mortgage",
        "Condomínio" to "HOA / Condo Fee",
        "Eletricidade" to "Electricity",
        "Água" to "Water",
        "Gás" to "Gas",
        "Internet & TV" to "Internet & Cable",
        "Manutenção & Obras" to "Home Maintenance",
        "Seguro Multirriscos" to "Home Insurance",
        "Supermercado" to "Groceries",
        "Refeições Fora / Restaurantes" to "Restaurants & Dining",
        "Cafés & Lanches" to "Cafes & Bakeries",
        "Takeaway / Entregas" to "Takeaway & Delivery",
        "Cantina / Almoços Trabalho" to "Work Lunch",
        "Combustível" to "Fuel & Gas",
        "Transportes Públicos / Passes" to "Public Transit",
        "Portagens & Via Verde" to "Tolls & Bridges",
        "Estacionamento" to "Parking",
        "Manutenção & Revisão Auto" to "Auto Maintenance",
        "Seguro Automóvel" to "Car Insurance",
        "IUC (Imposto Único de Circulação)" to "Vehicle Tax (IUC)",
        "Farmácia & Medicamentos" to "Pharmacy & Medicine",
        "Consultas & Exames" to "Doctor & Tests",
        "Dentista" to "Dental Care",
        "Ginásio & Desporto" to "Gym & Fitness",
        "Seguro de Saúde" to "Health Insurance",
        "Streaming (Netflix, Spotify...)" to "Streaming (Netflix, Spotify...)",
        "Subscrições Software & Apps" to "Software & App Subscriptions",
        "Telemóvel & Comunicações" to "Mobile & Phone Bill",
        "Roupa & Calçado" to "Clothing & Shoes",
        "PPR" to "Retirement Plan (PPR)",
        "Investimentos" to "Investments"
    )

    private val CONTEXT_PT_TO_EN = mapOf(
        "Trabalho" to "Work",
        "Casa" to "Home",
        "Saída / Social" to "Social / Night Out",
        "Férias / Viagem" to "Vacation / Travel",
        "Online" to "Online",
        "Comércio / Rua" to "Retail / Store"
    )

    private val PAYMENT_PT_TO_EN = mapOf(
        "Cartão" to "Debit/Credit Card",
        "Débito Direto" to "Direct Debit",
        "Transferência" to "Bank Transfer",
        "Dinheiro" to "Cash",
        "MB Way" to "MB Way"
    )

    fun translateCategory(name: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) PT_TO_EN[name] ?: name else name

    fun translateContext(label: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) CONTEXT_PT_TO_EN[label] ?: label else label

    fun translatePayment(label: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) PAYMENT_PT_TO_EN[label] ?: label else label
}
