package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NetExpenseCalculatorTest {

    private val calculator = MonthlyAnalyticsCalculator()
    private val categories = DefaultSeed.getDefaultCategories()
    private val testEpoch = LocalDate.of(2026, 10, 15).toEpochDay()

    @Test
    fun testPartialRefund_dinner60With40Refund_netExpense20InRestaurants() {
        val dinnerExpense = TransactionEntity(
            id = 100L,
            type = "EXPENSE",
            amountCents = 6000L, // 60,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "exp_food_lunch", // Restaurantes / Refeições fora
            contextTag = "SAIDA",
            merchant = "Restaurante O Manel",
            paymentMethod = "CARD"
        )
        val mbwayRefund = TransactionEntity(
            id = 101L,
            type = "INCOME",
            amountCents = 4000L, // 40,00 € MB WAY do amigo
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo MB WAY",
            paymentMethod = "MBWAY",
            linkedTransactionId = 100L
        )

        // Verificação direta com NetExpenseCalculator
        val netForTx = NetExpenseCalculator.calculateNetExpenseForTransaction(
            dinnerExpense,
            listOf(dinnerExpense, mbwayRefund)
        )
        assertEquals(2000L, netForTx)

        val resolved = NetExpenseCalculator.resolveEffectiveExpenses(listOf(dinnerExpense, mbwayRefund))
        assertEquals(1, resolved.size)
        assertEquals(2000L, resolved.first().amountCents)
        assertEquals("exp_food_lunch", resolved.first().categoryId)

        // Verificação integrada no MonthlyAnalyticsCalculator
        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(dinnerExpense, mbwayRefund),
            categories = categories
        )

        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(2000L, summary.totalExpenseCents)
        assertEquals(-2000L, summary.netSavingsCents)

        // Categoria Alimentação reflete custo líquido de 20,00 €
        val foodShare = summary.categoryShares.find { it.categoryId == "exp_food" }
        assertEquals(2000L, foodShare?.amountCents)
    }

    @Test
    fun testTotalRefund_purchase25With25Refund_netCostZero() {
        val purchase = TransactionEntity(
            id = 200L,
            type = "EXPENSE",
            amountCents = 2500L, // 25,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente",
            paymentMethod = "CARD"
        )
        val fullRefund = TransactionEntity(
            id = 201L,
            type = "INCOME",
            amountCents = 2500L, // 25,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "CASA",
            merchant = "Colega Casa",
            paymentMethod = "MBWAY",
            linkedTransactionId = 200L
        )

        val netForTx = NetExpenseCalculator.calculateNetExpenseForTransaction(
            purchase,
            listOf(purchase, fullRefund)
        )
        assertEquals(0L, netForTx)

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(purchase, fullRefund),
            categories = categories
        )

        assertEquals(0L, summary.totalExpenseCents)
        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(0L, summary.netSavingsCents)
    }

    @Test
    fun testMultipleRefundsToSamePurchase_bill45WithTwo15Refunds_netCost15() {
        val sharedBill = TransactionEntity(
            id = 300L,
            type = "EXPENSE",
            amountCents = 4500L, // 45,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "exp_food_lunch",
            contextTag = "SAIDA",
            merchant = "Pizzaria",
            paymentMethod = "CARD"
        )
        val refund1 = TransactionEntity(
            id = 301L,
            type = "INCOME",
            amountCents = 1500L, // 15,00 € Amigo A
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo A MB WAY",
            paymentMethod = "MBWAY",
            linkedTransactionId = 300L
        )
        val refund2 = TransactionEntity(
            id = 302L,
            type = "INCOME",
            amountCents = 1500L, // 15,00 € Amigo B
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo B MB WAY",
            paymentMethod = "MBWAY",
            linkedTransactionId = 300L
        )

        val totalRefunds = NetExpenseCalculator.calculateRefundsTotalForTransaction(
            300L,
            listOf(sharedBill, refund1, refund2)
        )
        assertEquals(3000L, totalRefunds)

        val netForTx = NetExpenseCalculator.calculateNetExpenseForTransaction(
            sharedBill,
            listOf(sharedBill, refund1, refund2)
        )
        assertEquals(1500L, netForTx)

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(sharedBill, refund1, refund2),
            categories = categories
        )
        assertEquals(1500L, summary.totalExpenseCents)
    }

    @Test
    fun testNoIncomeInflationAndAccurateSavingsRate() {
        val salary = TransactionEntity(
            id = 1L,
            type = "INCOME",
            amountCents = 100000L, // 1.000,00 € Salário Base
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_base_salary",
            contextTag = "TRABALHO",
            merchant = "Empresa",
            paymentMethod = "TRANSFER",
            isFixed = true
        )
        val dinner = TransactionEntity(
            id = 2L,
            type = "EXPENSE",
            amountCents = 6000L, // 60,00 € Jantar
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "exp_food_lunch",
            contextTag = "SAIDA",
            merchant = "Restaurante",
            paymentMethod = "CARD"
        )
        val refund = TransactionEntity(
            id = 3L,
            type = "INCOME",
            amountCents = 4000L, // 40,00 € MB WAY
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo MB WAY",
            paymentMethod = "MBWAY",
            linkedTransactionId = 2L
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(salary, dinner, refund),
            categories = categories
        )

        // Rendimento total NÃO deve incluir os 40 € de reembolso de amigos!
        assertEquals(100000L, summary.totalIncomeCents)
        assertEquals(100000L, summary.baseIncomeCents)
        assertEquals(0L, summary.extraIncomeCents)

        // Despesa total reflete o custo líquido (60 - 40 = 20 €)
        assertEquals(2000L, summary.totalExpenseCents)

        // Poupança líquida real: 1000 € - 20 € = 980 €
        assertEquals(98000L, summary.netSavingsCents)

        // Taxa de Poupança: 980 / 1000 = 98.0% (sem a distorção para 94,2% que ocorreria se o rendimento fosse inflacionado para 1040 €)
        assertEquals(98.0, summary.savingsRatePercent, 0.001)
    }

    @Test
    fun testRefundHigherThanOriginalOrWithoutParent_guardedWithMaxZero() {
        // Caso 1: Reembolso superior ao gasto (ex: reembolso de 30 € numa despesa de 20 €)
        val smallExpense = TransactionEntity(
            id = 400L,
            type = "EXPENSE",
            amountCents = 2000L, // 20,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "exp_food_lunch",
            contextTag = "SAIDA",
            merchant = "Café",
            paymentMethod = "CARD"
        )
        val overRefund = TransactionEntity(
            id = 401L,
            type = "INCOME",
            amountCents = 3000L, // 30,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo Generoso",
            paymentMethod = "MBWAY",
            linkedTransactionId = 400L
        )

        val netCost = NetExpenseCalculator.calculateNetExpenseForTransaction(
            smallExpense,
            listOf(smallExpense, overRefund)
        )
        assertEquals(0L, netCost)

        // Caso 2: Reembolso sem despesa correspondente (órfão)
        val orphanRefund = TransactionEntity(
            id = 500L,
            type = "INCOME",
            amountCents = 5000L, // 50,00 €
            yearMonth = 202610,
            epochDay = testEpoch,
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo",
            paymentMethod = "MBWAY",
            linkedTransactionId = 99999L // Inexistente
        )

        val summary = calculator.calculateMonthlySummary(
            yearMonth = 202610,
            transactions = listOf(orphanRefund),
            categories = categories
        )

        // O reembolso órfão não inflaciona o rendimento nem gera despesa negativa
        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(0L, summary.totalExpenseCents)
        assertEquals(0L, summary.netSavingsCents)
    }

    @Test
    fun testUtilityMethodsOfNetExpenseCalculator() {
        val expense = TransactionEntity(
            id = 1L, type = "EXPENSE", amountCents = 1000L, yearMonth = 202610,
            epochDay = testEpoch, categoryId = "exp_food", contextTag = "CASA",
            merchant = "Super", paymentMethod = "CARD"
        )
        val regularIncome = TransactionEntity(
            id = 2L, type = "INCOME", amountCents = 50000L, yearMonth = 202610,
            epochDay = testEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
            merchant = "Empresa", paymentMethod = "TRANSFER"
        )
        val refund = TransactionEntity(
            id = 3L, type = "INCOME", amountCents = 500L, yearMonth = 202610,
            epochDay = testEpoch, categoryId = "inc_other", contextTag = "SAIDA",
            merchant = "Amigo", paymentMethod = "MBWAY", linkedTransactionId = 1L
        )

        assertFalse(NetExpenseCalculator.isRefund(expense))
        assertFalse(NetExpenseCalculator.isRefund(regularIncome))
        assertTrue(NetExpenseCalculator.isRefund(refund))

        val all = listOf(expense, regularIncome, refund)
        assertEquals(listOf(regularIncome), NetExpenseCalculator.filterRegularIncomes(all))
        assertEquals(listOf(refund), NetExpenseCalculator.filterRefunds(all))
    }
}
