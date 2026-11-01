package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Agregação diária do mês para a vista Calendário.
 *
 * Apenas movimentos do tipo EXPENSE contam para o gasto do dia; receitas não
 * são consideradas despesa (o "total de gasto do dia" exclui receitas).
 * [CalendarDayData.transactionCount] conta os movimentos de despesa agregados
 * no dia, pelo que dias sem gastos entram com 0 e 0.
 */
data class CalendarDayData(
    val epochDay: Long,
    val dayOfMonth: Int,
    val expenseCents: Long,
    val transactionCount: Int,
    val isToday: Boolean
)

class CalendarAggregator {

    fun aggregate(
        yearMonth: Int,
        transactions: List<TransactionEntity>,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): List<CalendarDayData> {
        val year = yearMonth / 100
        val month = yearMonth % 100
        val ym = YearMonth.of(year, month)

        val totalsByDay = HashMap<Long, Long>()
        val countsByDay = HashMap<Long, Int>()

        for (tx in transactions) {
            if (!tx.type.equals("EXPENSE", ignoreCase = true)) continue
            totalsByDay[tx.epochDay] = (totalsByDay[tx.epochDay] ?: 0L) + tx.amountCents
            countsByDay[tx.epochDay] = (countsByDay[tx.epochDay] ?: 0) + 1
        }

        return (1..ym.lengthOfMonth()).map { dayOfMonth ->
            val epochDay = ym.atDay(dayOfMonth).toEpochDay()
            CalendarDayData(
                epochDay = epochDay,
                dayOfMonth = dayOfMonth,
                expenseCents = totalsByDay[epochDay] ?: 0L,
                transactionCount = countsByDay[epochDay] ?: 0,
                isToday = epochDay == todayEpochDay
            )
        }
    }
}
