package com.financas.app.uitest

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financas.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Filtros do ecrã Movimentos: pesquisa por texto, filtro por faixa de valor e
 * toggle de um filtro de contexto.
 *
 * Todas as comparações são relativas (a base de dados é partilhada entre testes).
 */
@RunWith(AndroidJUnit4::class)
class TransactionsFilterUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun searchByMerchantFiltersTheList() {
        composeRule.waitForAppReady()
        composeRule.onAllNodesWithContentDescription("Movimentos").onFirst().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Pesquisar descrição, comerciante...")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val countBefore = requireNotNull(movementsSummaryCount()) { "Resumo de movimentos não encontrado" }
        assertTrue("Deve existir pelo menos um movimento no mês corrente", countBefore > 0)

        // Termo de pesquisa: o estabelecimento do movimento mais recente.
        val merchant = mostRecentMerchantOfCurrentMonth()
        composeRule.fillOnlyEmptyTextField(merchant)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            val current = movementsSummaryCount()
            current != null && current != countBefore
        }
        val countAfter = movementsSummaryCount()
        assertTrue(
            "A pesquisa deve reduzir o número de movimentos ($countBefore -> $countAfter)",
            countAfter != null && countAfter < countBefore
        )
        assertTrue(
            "O movimento pesquisado deve continuar visível",
            composeRule.onAllNodesWithText(merchant).fetchSemanticsNodes().isNotEmpty()
        )

        // Limpar a pesquisa repõe a lista completa.
        composeRule.onAllNodesWithContentDescription("Limpar").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            movementsSummaryCount() == countBefore
        }
        assertEquals(countBefore, movementsSummaryCount() ?: -1)
    }

    @Test
    fun amountFilterKeepsOnlySmallAmounts() {
        composeRule.waitForAppReady()
        composeRule.onAllNodesWithContentDescription("Movimentos").onFirst().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Pesquisar descrição, comerciante...")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val countBefore = requireNotNull(movementsSummaryCount()) { "Resumo de movimentos não encontrado" }
        assertTrue("Deve existir pelo menos um movimento no mês corrente", countBefore > 0)

        composeRule.onNodeWithText("≤ 10 €").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            val current = movementsSummaryCount()
            current != null && current != countBefore
        }
        val countAfter = movementsSummaryCount()
        assertTrue(
            "O filtro '≤ 10 €' deve reduzir o número de movimentos ($countBefore -> $countAfter)",
            countAfter != null && countAfter < countBefore
        )

        val amounts = visibleRowAmounts()
        assertTrue("Deve continuar a existir pelo menos um movimento visível", amounts.isNotEmpty())
        amounts.forEach { cents ->
            assertTrue(
                "Só devem permanecer movimentos até 10,00 € (encontrado $cents cêntimos)",
                cents <= 1000L
            )
        }

        // Um segundo toque na mesma cápsula volta ao estado inicial.
        composeRule.onNodeWithText("≤ 10 €").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            movementsSummaryCount() == countBefore
        }
        assertEquals(countBefore, movementsSummaryCount() ?: -1)
    }

    @Test
    fun contextCapsuleCanBeToggledOnAndOff() {
        composeRule.waitForAppReady()
        composeRule.onAllNodesWithContentDescription("Movimentos").onFirst().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Pesquisar descrição, comerciante...")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val countBefore = requireNotNull(movementsSummaryCount()) { "Resumo de movimentos não encontrado" }
        assertTrue("Deve existir pelo menos um movimento no mês corrente", countBefore > 0)

        // A cápsula de contexto é identificada pelo rótulo visível + ação de clique
        // (as linhas de movimento mostram a etiqueta de contexto em maiúsculas).
        val casaCapsule = composeRule.onAllNodes(hasText("Casa") and hasClickAction())
        assertEquals(
            "Deve existir exactamente uma cápsula de contexto 'Casa'",
            casaCapsule.fetchSemanticsNodes().size,
            1
        )

        casaCapsule.onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            val current = movementsSummaryCount()
            current != null && current != countBefore
        }
        val countFiltered = movementsSummaryCount()
        assertTrue(
            "O filtro de contexto deve alterar o número de movimentos ($countBefore -> $countFiltered)",
            countFiltered != null && countFiltered != countBefore
        )

        // Segundo toque: a seleção é limpa e a lista volta ao estado inicial.
        composeRule.onAllNodes(hasText("Casa") and hasClickAction()).onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            movementsSummaryCount() == countBefore
        }
        assertEquals(countBefore, movementsSummaryCount() ?: -1)
    }

    /** Número de movimentos apresentado no cartão de resumo ("N movimentos"). */
    private fun movementsSummaryCount(): Int? {
        val nodes = composeRule.onAllNodes(hasText("movimentos", substring = true)).fetchSemanticsNodes()
        if (nodes.isEmpty()) return null
        val text = nodeText(nodes.first())
        return text.trim().substringBefore(" ").toIntOrNull()
    }

    /** Montantes (em cêntimos) das linhas de movimento atualmente compostas. */
    private fun visibleRowAmounts(): List<Long> {
        val matcher = SemanticsMatcher("montante de movimento") { node ->
            nodeTexts(node).any { isRowAmount(it) }
        }
        return composeRule.onAllNodes(matcher)
            .fetchSemanticsNodes()
            .flatMap { node ->
                nodeTexts(node).filter { isRowAmount(it) }.map { parseEuroToCents(it) }
            }
    }

    private fun isRowAmount(text: String): Boolean =
        (text.startsWith("-") || text.startsWith("+")) && text.endsWith(" €")
}
