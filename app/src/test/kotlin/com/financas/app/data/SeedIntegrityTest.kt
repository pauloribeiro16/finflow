package com.financas.app.data

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedIntegrityTest {

    @Test
    fun test1_seedHierarchy_noOrphansTwoLevelsAndTypeConsistency() {
        val seed = DefaultSeed.getDefaultCategories()
        val seedById = seed.associateBy { it.id }

        assertEquals(47, seedById.size)
        assertEquals(47, seed.map { it.id }.distinct().size)

        seed.forEach { category ->
            if (category.parentId != null) {
                val parent = seedById[category.parentId]
                assertTrue("Subcategoria ${category.id} tem pai órfão", parent != null)
                assertEquals("Pai de ${category.id} não pode ter pai", null, parent!!.parentId)
                assertEquals("Tipo de ${category.id} tem de bater com o pai", parent.type, category.type)
            }
        }

        val incomeCategories = seed.filter { it.type == "INCOME" }
        val expenseParents = seed.filter { it.type == "EXPENSE" && it.parentId == null }
        val incomeParents = seed.filter { it.type == "INCOME" && it.parentId == null }
        val expenseSubcategories = seed.filter { it.type == "EXPENSE" && it.parentId != null }
        assertEquals(8, incomeCategories.size)
        assertEquals(2, incomeParents.size)
        assertEquals(8, expenseParents.size)
        assertEquals(31, expenseSubcategories.size)
    }

    @Test
    fun test2_seedDefaultFixedRegression_uniqueIdsAndSequentialSortOrder() {
        val seed = DefaultSeed.getDefaultCategories()

        assertEquals(true, seed.find { it.id == "exp_health_pharmacy" }?.isDefaultFixed)
        assertEquals(true, seed.find { it.id == "exp_transport_maintenance" }?.isDefaultFixed)

        assertEquals(seed.size, seed.map { it.id }.toSet().size)

        assertEquals((0..46).toList(), seed.map { it.sortOrder }.sorted())
    }

    @Test
    fun test3_entities_noFloatingPointMoneyFields() {
        val transactionFields = TransactionEntity::class.java.declaredFields
            .filter { !it.isSynthetic && !it.name.startsWith("$") }
        val categoryFields = CategoryEntity::class.java.declaredFields
            .filter { !it.isSynthetic && !it.name.startsWith("$") }

        val transactionMoneyFields = transactionFields.filter {
            it.name.lowercase().contains("cents") || it.name.lowercase().contains("amount")
        }
        assertEquals(listOf("amountCents"), transactionMoneyFields.map { it.name })
        transactionMoneyFields.forEach {
            assertEquals("Campo de dinheiro tem de ser Long", java.lang.Long.TYPE, it.type)
        }

        (transactionFields + categoryFields).forEach {
            assertTrue(
                "Campo ${it.name} não pode ser Double/Float",
                it.type != java.lang.Double.TYPE && it.type != java.lang.Double::class.java &&
                        it.type != java.lang.Float.TYPE && it.type != java.lang.Float::class.java
            )
        }
    }
}
