package com.financas.app.data

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.repository.BackupSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.LocalDate

class Phase8And9IntegrityTest {

    private val serializer = BackupSerializer()

    private val categories = listOf(
        CategoryEntity(
            id = "exp_food",
            parentId = null,
            name = "Alimentação",
            icon = "restaurant",
            colorHex = 0xFFFF9800,
            type = "EXPENSE",
            isDefaultFixed = false,
            sortOrder = 1
        ),
        CategoryEntity(
            id = "exp_food_supermarket",
            parentId = "exp_food",
            name = "Supermercado",
            icon = "shopping_cart",
            colorHex = 0xFFFF9800,
            type = "EXPENSE",
            isDefaultFixed = false,
            sortOrder = 2
        )
    )

    private val transactions = listOf(
        TransactionEntity(
            id = 1L,
            type = "EXPENSE",
            amountCents = 1250L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 3).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente",
            paymentMethod = "CARD",
            isFixed = false
        )
    )

    private val budgets = listOf(
        BudgetEntity(id = "budget_exp_food", categoryId = "exp_food", limitCents = 40000L, isActive = true)
    )

    private val goals = listOf(
        GoalEntity(
            id = GoalEntity.deterministicId("Fundo de Emergência", 202601),
            name = "Fundo de Emergência",
            targetCents = 300000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612,
            archived = false
        )
    )

    private fun instanceFields(type: Class<*>): List<java.lang.reflect.Field> =
        type.declaredFields.filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }

    private fun moneyFields(type: Class<*>): List<java.lang.reflect.Field> =
        instanceFields(type).filter {
            val name = it.name.lowercase()
            name.contains("cents") || name.contains("amount")
        }

    private fun monthOf(epochDay: Long): Int {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.year * 100 + date.monthValue
    }

    private fun isMonthConsistent(tx: TransactionEntity): Boolean = tx.yearMonth == monthOf(tx.epochDay)

    @Test
    fun test1_budgetAndGoalMoneyFieldsAreLongNotFloatingPoint() {
        val budgetMoneyFields = moneyFields(BudgetEntity::class.java)
        assertEquals(listOf("limitCents"), budgetMoneyFields.map { it.name })
        budgetMoneyFields.forEach {
            assertEquals("Campo de dinheiro tem de ser Long", java.lang.Long.TYPE, it.type)
        }

        val goalMoneyFields = moneyFields(GoalEntity::class.java)
        assertEquals(listOf("targetCents"), goalMoneyFields.map { it.name })
        goalMoneyFields.forEach {
            assertEquals("Campo de dinheiro tem de ser Long", java.lang.Long.TYPE, it.type)
        }

        (instanceFields(BudgetEntity::class.java) + instanceFields(GoalEntity::class.java)).forEach {
            assertTrue(
                "Campo ${it.name} não pode ser Double/Float",
                it.type != java.lang.Double.TYPE && it.type != java.lang.Double::class.java &&
                    it.type != java.lang.Float.TYPE && it.type != java.lang.Float::class.java
            )
        }
    }

    @Test
    fun test2_budgetAndGoalTestDataHasPositiveAmountsAndUniqueIds() {
        budgets.forEach {
            assertTrue("Orçamento ${it.id} tem de ter limite > 0", it.limitCents > 0L)
            assertTrue("Orçamento ${it.id} tem de ter categoria não vazia", it.categoryId.isNotBlank())
        }
        goals.forEach {
            assertTrue("Meta ${it.id} tem de ter alvo > 0", it.targetCents > 0L)
            assertTrue("Meta ${it.id} tem de ter nome não vazio", it.name.isNotBlank())
            assertTrue("Meta ${it.id} tem de ter mês de início válido", it.startYearMonth > 0)
        }

        assertEquals(budgets.size, budgets.map { it.id }.distinct().size)
        assertEquals(goals.size, goals.map { it.id }.distinct().size)
        assertTrue(budgets.all { it.id.startsWith("budget_") })
        assertTrue(goals.all { it.id.startsWith("goal_") })
    }

    @Test
    fun test3_exportJsonV2_ContainsVersionAndBudgetsAndGoalsWithEveryField() {
        val json = serializer.exportToJson(categories, transactions, budgets, goals)

        assertTrue(json.startsWith("{\n  \"version\": 3,\n"))
        assertTrue(json.contains("\"budgets\": ["))
        assertTrue(json.contains("\"goals\": ["))

        assertTrue(json.contains("\"id\": \"budget_exp_food\""))
        assertTrue(json.contains("\"categoryId\": \"exp_food\""))
        assertTrue(json.contains("\"limitCents\": 40000"))
        assertTrue(json.contains("\"isActive\": true"))

        assertTrue(json.contains("\"id\": \"${goals.first().id}\""))
        assertTrue(json.contains("\"name\": \"Fundo de Emergência\""))
        assertTrue(json.contains("\"targetCents\": 300000"))
        assertTrue(json.contains("\"startYearMonth\": 202601"))
        assertTrue(json.contains("\"deadlineYearMonth\": 202612"))
        assertTrue(json.contains("\"archived\": false"))

        val restored = serializer.importFromJson(json)
        assertEquals(3, restored.version)
        assertEquals(budgets, restored.budgets)
        assertEquals(goals, restored.goals)
    }

    @Test
    fun test4_importJsonV1_WithoutBudgetsAndGoalsKeys_ReturnsEmptyLists() {
        val v1Json = """
            {
              "version": 1,
              "categories": [
                {"id": "exp_food", "name": "Alimentação"}
              ],
              "transactions": [
                {"id": 1, "type": "EXPENSE", "amountCents": 1250, "yearMonth": 202610, "epochDay": 20662}
              ]
            }
        """.trimIndent()

        val restored = serializer.importFromJson(v1Json)

        assertEquals(1, restored.version)
        assertEquals(1, restored.categories.size)
        assertEquals(1, restored.transactions.size)
        assertTrue(restored.budgets.isEmpty())
        assertTrue(restored.goals.isEmpty())
    }

    @Test
    fun test5_importJson_MalformedNewKeysFallBackToSafeDefaultsWithoutCrashing() {
        val malformed = """
            {
              "version": 2,
              "categories": [],
              "transactions": [],
              "budgets": [
                {"id": "budget_exp_food", "categoryId": "exp_food", "limitCents": "abc", "isActive": "nao-e-boolean"}
              ],
              "goals": [
                {"name": "Meta", "targetCents": "xyz", "startYearMonth": "202601", "deadlineYearMonth": "abc", "archived": 1}
              ]
            }
        """.trimIndent()

        val restored = serializer.importFromJson(malformed)

        assertEquals(2, restored.version)
        assertEquals(1, restored.budgets.size)
        assertEquals(0L, restored.budgets.first().limitCents)
        assertFalse(restored.budgets.first().isActive)

        assertEquals(1, restored.goals.size)
        assertEquals(0L, restored.goals.first().targetCents)
        assertEquals(202601, restored.goals.first().startYearMonth)
        assertNull(restored.goals.first().deadlineYearMonth)
        assertFalse(restored.goals.first().archived)
        assertEquals("goal_meta_202601", restored.goals.first().id)
    }

    @Test
    fun test6_importJson_StructurallyBrokenPayloadThrowsTypedExceptionAndIsCatchable() {
        val brokenPayloads = listOf(
            "{\"version\": 2, \"budgets\": [",
            "{\"version\": 2, \"goals\": [{\"name\": \"Meta\"}",
            "not json at all"
        )

        brokenPayloads.forEach { payload ->
            try {
                serializer.importFromJson(payload)
                fail("Era esperado IllegalArgumentException para: $payload")
            } catch (expected: IllegalArgumentException) {
                assertNotNull(expected.message)
            }
        }
    }

    @Test
    fun test7_transactionMonthMappingIsConsistentForWellFormedData() {
        val exported = serializer.importFromJson(
            serializer.exportToJson(categories, transactions, budgets, goals)
        )

        assertEquals(1, exported.transactions.size)
        val tx = exported.transactions.first()
        assertTrue("yearMonth tem de derivar de epochDay", isMonthConsistent(tx))
        assertEquals(202610, tx.yearMonth)
        assertEquals(LocalDate.of(2026, 10, 3).toEpochDay(), tx.epochDay)
    }

    @Test
    fun test8_transactionWithMonthAndDateFromDifferentMonthsIsFlaggedAsInconsistent() {
        val inconsistent = transactions.first().copy(yearMonth = 202611)

        assertFalse("Mês e data de meses diferentes têm de ser sinalizados", isMonthConsistent(inconsistent))
        assertEquals(202611, inconsistent.yearMonth)
        assertEquals(202610, monthOf(inconsistent.epochDay))

        val consistentAgain = inconsistent.copy(yearMonth = monthOf(inconsistent.epochDay))
        assertTrue(isMonthConsistent(consistentAgain))
    }

    @Test
    fun test9_deterministicGoalId_IsStableAndDistinguishesNameAndStartMonth() {
        val first = GoalEntity.deterministicId("Fundo de Emergência", 202601)
        val second = GoalEntity.deterministicId("Fundo de Emergência", 202601)
        assertEquals(first, second)

        assertEquals("goal_fundo_de_emergência_202601", first)

        assertNotEquals(first, GoalEntity.deterministicId("Fundo de Emergência", 202602))
        assertNotEquals(first, GoalEntity.deterministicId("Fundo Extra", 202601))
        assertNotEquals(
            GoalEntity.deterministicId("Fundo de Emergência", 202601),
            GoalEntity.deterministicId("Fundo de Emergência", 202602)
        )
    }

    @Test
    fun test10_deterministicGoalId_NormalizesSpacesCaseAndPunctuation() {
        assertEquals(
            GoalEntity.deterministicId("Fundo de Emergência", 202601),
            GoalEntity.deterministicId("  Fundo de Emergência  ", 202601)
        )
        assertEquals(
            GoalEntity.deterministicId("Fundo de Emergência", 202601),
            GoalEntity.deterministicId("fundo de emergência", 202601)
        )
        assertEquals(
            GoalEntity.deterministicId("Viagem ao Japão", 202603),
            GoalEntity.deterministicId("Viagem ao Japão!", 202603)
        )
        assertEquals("goal_meta_202601", GoalEntity.deterministicId("!!!", 202601))
    }
}
