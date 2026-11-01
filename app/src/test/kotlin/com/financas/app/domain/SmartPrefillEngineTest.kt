package com.financas.app.domain

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SmartPrefillEngineTest {

    private lateinit var engine: SmartPrefillEngine

    @Before
    fun setUp() {
        engine = SmartPrefillEngine()
    }

    @Test
    fun testPrefillMatchesCaseInsensitive() {
        val epoch = LocalDate.of(2026, 10, 15).toEpochDay()
        val history = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 1250L,
                yearMonth = 202610,
                epochDay = epoch,
                categoryId = "exp_food_lunch",
                contextTag = "TRABALHO",
                merchant = "Café Central",
                paymentMethod = "MBWAY",
                isFixed = false
            )
        )

        // Consulta em minúsculas
        val prefillLower = engine.findPrefill("café central", history)
        assertNotNull(prefillLower)
        assertEquals("exp_food_lunch", prefillLower!!.categoryId)
        assertEquals("TRABALHO", prefillLower.contextTag)
        assertEquals("MBWAY", prefillLower.paymentMethod)
        assertEquals(false, prefillLower.isFixed)

        // Consulta com espaços à volta e maiúsculas mistas
        val prefillMixed = engine.findPrefill("  CAFÉ central  ", history)
        assertNotNull(prefillMixed)
        assertEquals("exp_food_lunch", prefillMixed!!.categoryId)
        assertEquals("TRABALHO", prefillMixed.contextTag)
        assertEquals("MBWAY", prefillMixed.paymentMethod)
        assertEquals(false, prefillMixed.isFixed)
    }

    @Test
    fun testPrefillReturnsMostRecentTransaction() {
        val day1 = LocalDate.of(2026, 10, 1).toEpochDay()
        val day2 = LocalDate.of(2026, 10, 10).toEpochDay()

        val history = listOf(
            // Registo antigo: Continente pago com DINHEIRO para CASA
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 3000L,
                yearMonth = 202610,
                epochDay = day1,
                categoryId = "exp_food_supermarket",
                contextTag = "CASA",
                merchant = "Continente",
                paymentMethod = "CASH",
                isFixed = false
            ),
            // Registo recente: Continente pago com CARTÃO para SAIDA
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 4500L,
                yearMonth = 202610,
                epochDay = day2,
                categoryId = "exp_food_supermarket",
                contextTag = "SAIDA",
                merchant = "Continente",
                paymentMethod = "CARD",
                isFixed = false
            )
        )

        val prefill = engine.findPrefill("continente", history)
        assertNotNull(prefill)
        // Deve preferir o mais recente (dia 10): CARTÃO e SAIDA
        assertEquals("CARD", prefill!!.paymentMethod)
        assertEquals("SAIDA", prefill.contextTag)
    }

    @Test
    fun testPrefillUnknownOrEmptyMerchantReturnsNull() {
        val history = listOf(
            TransactionEntity(
                id = 1,
                type = "EXPENSE",
                amountCents = 1000L,
                yearMonth = 202610,
                epochDay = 1000L,
                categoryId = "exp_food_lunch",
                contextTag = "TRABALHO",
                merchant = "Café Central",
                paymentMethod = "MBWAY",
                isFixed = false
            )
        )

        assertNull(engine.findPrefill("Pingo Doce", history))
        assertNull(engine.findPrefill("", history))
        assertNull(engine.findPrefill("   ", history))
    }

    @Test
    fun testRecentMerchantsList() {
        val day1 = LocalDate.of(2026, 10, 1).toEpochDay()
        val day2 = LocalDate.of(2026, 10, 2).toEpochDay()
        val day3 = LocalDate.of(2026, 10, 3).toEpochDay()

        val history = listOf(
            TransactionEntity(id = 1, type = "EXPENSE", amountCents = 100L, yearMonth = 202610, epochDay = day1, categoryId = "c", contextTag = "t", merchant = "Galp", paymentMethod = "m"),
            TransactionEntity(id = 2, type = "EXPENSE", amountCents = 200L, yearMonth = 202610, epochDay = day2, categoryId = "c", contextTag = "t", merchant = "Café Central", paymentMethod = "m"),
            TransactionEntity(id = 3, type = "EXPENSE", amountCents = 300L, yearMonth = 202610, epochDay = day3, categoryId = "c", contextTag = "t", merchant = "Continente", paymentMethod = "m"),
            TransactionEntity(id = 4, type = "EXPENSE", amountCents = 400L, yearMonth = 202610, epochDay = day3, categoryId = "c", contextTag = "t", merchant = "Café Central", paymentMethod = "m")
        )

        val recent = engine.getRecentMerchants(history, limit = 5)
        assertEquals(3, recent.size)
        // Mais recentes primeiro: Continente e Café Central no day3, depois Galp no day1
        assertEquals("Café Central", recent[0])
        assertEquals("Continente", recent[1])
        assertEquals("Galp", recent[2])
    }

    @Test
    fun testPrefillTieBreak_highestIdWinsWhenEpochDayIsEqual() {
        val sameDay = LocalDate.of(2026, 10, 10).toEpochDay()

        val history = listOf(
            TransactionEntity(
                id = 5, type = "EXPENSE", amountCents = 1000L, yearMonth = 202610, epochDay = sameDay,
                categoryId = "exp_food_cafe", contextTag = "COMERCIO", merchant = "Continente",
                paymentMethod = "CASH", isFixed = false
            ),
            TransactionEntity(
                id = 9, type = "EXPENSE", amountCents = 2000L, yearMonth = 202610, epochDay = sameDay,
                categoryId = "exp_food_supermarket", contextTag = "CASA", merchant = "Continente",
                paymentMethod = "CARD", isFixed = false
            )
        )

        val prefill = engine.findPrefill("Continente", history)
        assertNotNull(prefill)
        assertEquals("CARD", prefill!!.paymentMethod)
        assertEquals("CASA", prefill.contextTag)
        assertEquals("exp_food_supermarket", prefill.categoryId)
    }

    @Test
    fun testRecentMerchants_limitTenTrimsExcludesBlankAndDistinct() {
        val history = (1L..12L).map { i ->
            val merchant = when {
                i == 1L -> ""
                i == 2L -> "   "
                i == 4L -> " Mercado Longo "
                else -> "M$i"
            }
            TransactionEntity(
                id = i, type = "EXPENSE", amountCents = 100L * i, yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, i.toInt()).toEpochDay(),
                categoryId = "c", contextTag = "CASA", merchant = merchant, paymentMethod = "CARD"
            )
        }

        val recent = engine.getRecentMerchants(history)

        assertEquals(10, recent.size)
        assertEquals("M12", recent[0])
        assertEquals("M11", recent[1])
        assertEquals("Mercado Longo", recent[8])
        assertEquals("M3", recent[9])
        assertTrue(recent.none { it.isBlank() })
        assertEquals(recent.size, recent.distinct().size)

        assertEquals(listOf("M12", "M11", "M10"), engine.getRecentMerchants(history, limit = 3))
    }

    @Test
    fun testSuspendFindPrefill_nullDaoReturnsNull_andDaoMatchesLatest() = runTest {
        assertNull(engine.findPrefill("Continente"))

        val daoBasedEngine = SmartPrefillEngine(
            FakeTransactionDao(
                listOf(
                    TransactionEntity(
                        id = 1, type = "EXPENSE", amountCents = 2450L, yearMonth = 202610,
                        epochDay = LocalDate.of(2026, 10, 10).toEpochDay(),
                        categoryId = "exp_food_supermarket", contextTag = "CASA",
                        merchant = "Continente", paymentMethod = "MBWAY", isFixed = false
                    )
                )
            )
        )

        val suggestion = daoBasedEngine.findPrefill("  continente  ")
        assertNotNull(suggestion)
        assertEquals("exp_food_supermarket", suggestion!!.categoryId)
        assertEquals("CASA", suggestion.contextTag)
        assertEquals("MBWAY", suggestion.paymentMethod)
        assertFalse(suggestion.isFixed)

        assertNull(daoBasedEngine.findPrefill(""))
    }
}
