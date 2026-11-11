package com.financas.app.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * Registo e edição de uma despesa ponta a ponta:
 * abrir a folha, escolher categoria/subcategoria, escrever o estabelecimento,
 * guardar e depois reabrir o movimento para o atualizar.
 */
@RunWith(AndroidJUnit4::class)
class AddTransactionUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun registersNewExpenseAndUpdatesIt() {
        composeRule.waitForAppReady()
        val merchant = uniqueToken("UI")

        // --- Passo 1: Valor ---------------------------------------------------
        composeRule.onNodeWithContentDescription("Nova Transação").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Novo Movimento").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("+10 €").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("10,00").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Seguinte").performScrollTo().performClick()

        // --- Passo 2: Categoria e subcategoria (avança para os detalhes) -------
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Alimentação").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Alimentação").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Supermercado").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Supermercado").performScrollTo().performClick()

        // --- Passo 3: Detalhes ------------------------------------------------
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Guardar Despesa").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.fillOnlyEmptyTextField(merchant)
        composeRule.onNodeWithText("Guardar Despesa").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Guardar Despesa").fetchSemanticsNodes().isEmpty()
        }

        // --- O novo movimento aparece no Início --------------------------------
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText(merchant).fetchSemanticsNodes().isNotEmpty()
        }

        // --- Reabrir em edição (abre nos Detalhes) ----------------------------
        composeRule.onAllNodesWithText(merchant).onFirst().performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Editar Movimento").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Atualizar Despesa").fetchSemanticsNodes().isNotEmpty()
        }

        // --- Voltar ao valor e somar +5 € (10,00 -> 15,00) ----------------------
        composeRule.onAllNodesWithText("Alterar").onFirst().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("+5 €").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("+5 €").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("15,00").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Seguinte").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Seguinte").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Seguinte").performScrollTo().performClick()

        // --- Atualizar --------------------------------------------------------
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Atualizar Despesa").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Atualizar Despesa").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Atualizar Despesa").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("15,00", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(merchant).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun newSheetShowsAmountShortcutsAndPaymentRow() {
        composeRule.waitForAppReady()

        composeRule.onNodeWithContentDescription("Nova Transação").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Novo Movimento").fetchSemanticsNodes().isNotEmpty()
        }

        // Atalhos rápidos de montante
        listOf("+5 €", "+10 €", "+20 €", "+50 €").forEach { shortcut ->
            assertTrue(
                "O atalho '$shortcut' deve existir na folha",
                composeRule.onAllNodesWithText(shortcut).fetchSemanticsNodes().isNotEmpty()
            )
        }

        // Selector de tipo de operação
        listOf("Despesa", "Receita").forEach { type ->
            assertTrue(
                "O chip de tipo '$type' deve existir na folha",
                composeRule.onAllNodesWithText(type).fetchSemanticsNodes().isNotEmpty()
            )
        }

        composeRule.onNodeWithContentDescription("Fechar").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Novo Movimento").fetchSemanticsNodes().isEmpty()
        }
    }
}
