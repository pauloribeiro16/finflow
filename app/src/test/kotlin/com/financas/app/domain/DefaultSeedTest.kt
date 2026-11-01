package com.financas.app.domain

import com.financas.app.data.seed.DefaultSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSeedTest {

    @Test
    fun testCategoriesHaveNoOrphans() {
        val categories = DefaultSeed.getDefaultCategories()
        assertTrue("Deve conter categorias", categories.isNotEmpty())

        val categoryMap = categories.associateBy { it.id }

        // Verifica que todos os parentIds apontam para uma categoria pai válida existente
        for (category in categories) {
            if (category.parentId != null) {
                val parent = categoryMap[category.parentId]
                assertNotNull("Subcategoria ${category.id} tem parentId órfão: ${category.parentId}", parent)
                assertNull("O pai ${parent!!.id} não deve ter outro parentId (máximo 2 níveis)", parent.parentId)
                assertEquals("Tipo da subcategoria deve bater com o pai", parent.type, category.type)
            }
        }
    }

    @Test
    fun testEightExpenseParentGroupsExist() {
        val categories = DefaultSeed.getDefaultCategories()
        val expenseParents = categories.filter { it.type == "EXPENSE" && it.parentId == null }
        assertEquals("Devem existir exatamente 8 grupos pais de despesas", 8, expenseParents.size)

        val expectedGroupIds = setOf(
            "exp_housing",
            "exp_food",
            "exp_transport",
            "exp_health",
            "exp_leisure",
            "exp_shopping",
            "exp_travel",
            "exp_financial"
        )
        val actualGroupIds = expenseParents.map { it.id }.toSet()
        assertEquals(expectedGroupIds, actualGroupIds)
    }

    @Test
    fun testIncomeCategoriesExist() {
        val categories = DefaultSeed.getDefaultCategories()
        val incomeCategories = categories.filter { it.type == "INCOME" }
        assertTrue(incomeCategories.any { it.id == "inc_base_salary" })
        assertTrue(incomeCategories.any { it.id == "inc_meal_allowance" })
        assertTrue(incomeCategories.any { it.id == "inc_bonus" })
    }

    @Test
    fun testContextsAndPaymentMethods() {
        assertEquals(6, DefaultSeed.CONTEXTS.size)
        val tags = DefaultSeed.CONTEXTS.map { it.tag }.toSet()
        assertTrue(tags.containsAll(listOf("TRABALHO", "CASA", "SAIDA", "FERIAS", "ONLINE", "COMERCIO")))

        assertEquals(5, DefaultSeed.PAYMENT_METHODS.size)
        val methods = DefaultSeed.PAYMENT_METHODS.map { it.code }.toSet()
        assertTrue(methods.containsAll(listOf("MBWAY", "CARD", "DIRECT_DEBIT", "TRANSFER", "CASH")))
    }
}
