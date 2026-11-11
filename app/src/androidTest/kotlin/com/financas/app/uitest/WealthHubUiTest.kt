package com.financas.app.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
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
 * Hub "Património": criação de uma aplicação de investimento, de uma meta de
 * poupança e de um orçamento por categoria.
 */
@RunWith(AndroidJUnit4::class)
class WealthHubUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createsInvestmentWithCapitalAndCurrentValue() {
        openWealthHub()
        val assetName = uniqueToken("UIAtivo")

        composeRule.onNodeWithText("Nova Aplicação").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Nova Aplicação / Ativo").fetchSemanticsNodes().isNotEmpty()
        }

        // Ordem de composição dos campos do diálogo:
        // 0 Nome, 1 Entidade, 2 Capital investido, 3 Valor atual, 4 Reforço, 5 Notas
        composeRule.fillTextField("Nome da Aplicação", assetName, fallbackIndex = 0)
        composeRule.fillTextField("Entidade / Corretora / Banco", "Banco Teste", fallbackIndex = 1)
        composeRule.fillTextField("Capital Investido (€)", "1000", fallbackIndex = 2)
        composeRule.fillTextField("Saldo Atual (€)", "1250", fallbackIndex = 3)

        composeRule.onNodeWithText("Guardar Aplicação").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(assetName).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "O cartão da aplicação deve mostrar o nome criado",
            composeRule.onAllNodesWithText(assetName).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText(assetName).onFirst().performScrollTo().assertIsDisplayed()

        assertTrue(
            "O cartão da aplicação deve mostrar o valor atual 1.250,00 €",
            composeRule.onAllNodesWithText("1.250,00 €").fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText("1.250,00 €").onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun createsSavingsGoal() {
        openWealthHub()
        val goalName = uniqueToken("UIMeta")

        composeRule.onNodeWithText("🎯 Metas").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Metas de Poupança").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("+ Nova Meta").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Nova Meta de Poupança").fetchSemanticsNodes().isNotEmpty()
        }

        // Ordem de composição: 0 Nome da meta, 1 Montante alvo
        composeRule.fillTextField("Nome da Meta", goalName, fallbackIndex = 0)
        composeRule.fillTextField("Montante Alvo (€)", "1500", fallbackIndex = 1)

        composeRule.onNodeWithText("Criar").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(goalName).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "O cartão da meta deve mostrar o nome criado",
            composeRule.onAllNodesWithText(goalName).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText(goalName).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun createsCategoryBudget() {
        openWealthHub()
        val categoryName = "Transportes"

        composeRule.onNodeWithText("📊 Orçamentos").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("ORÇAMENTO GLOBAL").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Novo Orçamento de Categoria").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Novo Orçamento").fetchSemanticsNodes().isNotEmpty()
        }

        // Dropdown de categoria (o item do menu é o único nó clicável com este texto)
        composeRule.onNodeWithText("Categoria").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodes(hasText(categoryName) and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText(categoryName) and hasClickAction()).performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }

        // Teto mensal (único campo de texto ainda vazio do diálogo)
        composeRule.fillOnlyEmptyTextField("400")

        composeRule.onNodeWithText("Criar").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Categorias Ativas").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "A secção 'Categorias Ativas' deve listar a categoria escolhida",
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText(categoryName).onFirst().performScrollTo().assertIsDisplayed()

        assertTrue(
            "Deve existir um resumo de orçamentos sob monitorização",
            composeRule.onAllNodesWithText("orçamentos sob monitorização", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    private fun openWealthHub() {
        composeRule.waitForAppReady()
        composeRule.onAllNodesWithContentDescription("Património").onFirst().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("TOTAL APLICADO").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
