package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity

object NetExpenseCalculator {

    fun isRefund(tx: TransactionEntity): Boolean {
        return tx.type.equals("INCOME", ignoreCase = true) && tx.linkedTransactionId != null
    }

    fun calculateNetExpenseCents(originalExpenseCents: Long, refundCents: Long): Long {
        return maxOf(0L, originalExpenseCents - refundCents)
    }

    fun calculateRefundsTotalForTransaction(
        parentTxId: Long,
        transactions: List<TransactionEntity>
    ): Long {
        return transactions
            .filter { isRefund(it) && it.linkedTransactionId == parentTxId }
            .sumOf { it.amountCents }
    }

    fun calculateNetExpenseForTransaction(
        expense: TransactionEntity,
        transactions: List<TransactionEntity>
    ): Long {
        val totalRefunds = calculateRefundsTotalForTransaction(expense.id, transactions)
        return calculateNetExpenseCents(expense.amountCents, totalRefunds)
    }

    fun resolveEffectiveExpenses(transactions: List<TransactionEntity>): List<TransactionEntity> {
        val refundMap = transactions
            .filter { isRefund(it) }
            .groupBy { it.linkedTransactionId!! }

        return transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .map { tx ->
                val refunds = refundMap[tx.id] ?: emptyList()
                if (refunds.isEmpty()) {
                    tx
                } else {
                    val totalRefunds = refunds.sumOf { it.amountCents }
                    tx.copy(amountCents = calculateNetExpenseCents(tx.amountCents, totalRefunds))
                }
            }
    }

    fun filterRegularIncomes(transactions: List<TransactionEntity>): List<TransactionEntity> {
        return transactions.filter {
            it.type.equals("INCOME", ignoreCase = true) && it.linkedTransactionId == null
        }
    }

    fun filterRefunds(transactions: List<TransactionEntity>): List<TransactionEntity> {
        return transactions.filter { isRefund(it) }
    }
}
