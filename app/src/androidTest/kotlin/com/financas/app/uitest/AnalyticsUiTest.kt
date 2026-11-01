package com.financas.app.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financas.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ecrã Análise: as 7 lentes (Visão Anual, No Quê, Onde, Como, Evolução,
 * Comparar, Calendário), o toggle € / % do que recebo e a seleção de baselines.
 */
@RunWith(AndroidJUnit4::class)
class AnalyticsUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstLensesRenderTheirContentMarker() {
        openAnalytics()

        // Visão Anual
        selectLens("Visão Anual", "DESPESA TOTAL ANUAL")

        // No Quê
        selectLens("No Quê", "Despesa Total")

        // Onde
        selectLens("Onde", "Gastos por Contexto")

        // Como
        selectLens("Como", "Estrutura de Rigidez")
    }

    @Test
    fun remainingLensesRenderTheirContentMarker() {
        openAnalytics()

        // Evolução
        selectLens("Evolução", "Evolução Anual Mês a Mês")

        // Comparar
        selectLens("Comparar", "Comparação com", markerSubstring = true)

        // Calendário
        selectLens("Calendário", "Calendário de Gastos")
    }

    @Test
    fun euroToggleReplacesMonetaryLabelWithPercentage() {
        openAnalytics()
        selectLens("No Quê", "Despesa Total")

        assertTrue(
            "Antes do toggle não deve existir nenhum valor em percentagem",
            composeRule.onAllNodes(hasText("% do recebido", substring = true))
                .fetchSemanticsNodes().isEmpty()
        )

        composeRule.onNodeWithText("% do que recebo").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("% do recebido", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "Depois do toggle o valor monetário deve ser substituído por uma percentagem",
            composeRule.onAllNodes(hasText("% do recebido", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun comparisonBaselineChangesCardTitle() {
        openAnalytics()
        selectLens("Comparar", "Comparação com")

        // Baseline por omissão: mês anterior
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Comparação com Mês Anterior").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O cartão deve comparar com o mês anterior por omissão",
            composeRule.onAllNodesWithText("Comparação com Mês Anterior").fetchSemanticsNodes().isNotEmpty()
        )

        // Baseline: média mensal
        composeRule.onNodeWithText("Média Mensal").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Comparação com Média Mensal").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O cartão deve passar a comparar com a média mensal",
            composeRule.onAllNodesWithText("Comparação com Média Mensal").fetchSemanticsNodes().isNotEmpty()
        )

        // Baseline: mesmo mês do ano anterior
        composeRule.onNodeWithText("Ano Anterior").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Comparação com Ano Anterior").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O cartão deve passar a comparar com o ano anterior",
            composeRule.onAllNodesWithText("Comparação com Ano Anterior").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun calendarDaySelectionShowsMovements() {
        openAnalytics()
        selectLens("Calendário", "Calendário de Gastos")

        composeRule.onNodeWithText("15").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("Movimentos de", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Tocar num dia do calendário deve mostrar os movimentos desse dia",
            composeRule.onAllNodes(hasText("Movimentos de", substring = true)).fetchSemanticsNodes().isNotEmpty()
        )
    }

    private fun openAnalytics() {
        composeRule.waitForAppReady()
        composeRule.onAllNodesWithContentDescription("Análise").onFirst().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Visão Anual").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Seleciona uma lente. A linha de lentes é horizontalmente deslocável,
     * por isso garantimos primeiro que o chip está dentro da área visível.
     */
    private fun selectLens(lens: String, marker: String, markerSubstring: Boolean = false) {
        val scroller = composeRule.horizontalScrollerContaining("Visão Anual")
        composeRule.scrollHorizontallyTo(scroller, composeRule.onNodeWithText(lens))
        composeRule.onNodeWithText(lens).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(marker, substring = markerSubstring))
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "A lente '$lens' deve mostrar o marcador '$marker'",
            composeRule.onAllNodes(hasText(marker, substring = markerSubstring))
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodes(hasText(marker, substring = markerSubstring)).onFirst().assertIsDisplayed()
    }
}
