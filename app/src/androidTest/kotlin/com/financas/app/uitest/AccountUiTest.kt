package com.financas.app.uitest

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financas.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ecrã Conta: edição do perfil, criação de subcategoria e de contexto
 * personalizado e histórico de versões.
 */
@RunWith(AndroidJUnit4::class)
class AccountUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun editsProfileName() {
        composeRule.waitForAppReady()
        composeRule.openAccountScreen()
        val newName = uniqueToken("UIUtilizador")

        composeRule.onNodeWithText("Editar").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Configurações do Perfil").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.fillTextField("Nome de Utilizador", newName, fallbackIndex = 0)
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(newName).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "O novo nome do perfil deve ser apresentado",
            composeRule.onAllNodesWithText(newName).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText(newName).onFirst().assertIsDisplayed()
    }

    @Test
    fun createsSubcategory() {
        composeRule.waitForAppReady()
        composeRule.openAccountScreen()
        composeRule.onNodeWithText("Preferências").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Categorias & Contextos").fetchSemanticsNodes().isNotEmpty()
        }

        val subName = uniqueToken("UISub")
        // O diálogo pré-seleciona a primeira categoria pai; calculamos o nome
        // para depois expandir o grupo certo na lista.
        val parentName = firstParentCategoryName()

        composeRule.onNodeWithText("Nova Subcategoria").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Nome da Subcategoria").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.fillTextField("Nome da Subcategoria", subName, fallbackIndex = 0)
        composeRule.onNodeWithText("Adicionar").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Nome da Subcategoria").fetchSemanticsNodes().isEmpty()
        }

        // Expandir o grupo da categoria pai para confirmar que a subcategoria está listada
        composeRule.onNodeWithText(parentName).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(subName).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "A subcategoria criada deve estar listada",
            composeRule.onAllNodesWithText(subName).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText(subName).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun createsCustomContext() {
        composeRule.waitForAppReady()
        composeRule.openAccountScreen()
        composeRule.onNodeWithText("Preferências").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Categorias & Contextos").fetchSemanticsNodes().isNotEmpty()
        }

        // O contexto é guardado em maiúsculas (ex.: "UI_CTX_123").
        val contextTag = uniqueToken("UI_CTX_")

        composeRule.onNodeWithText("Novo Contexto").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Nome do Contexto").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.fillTextField("Nome do Contexto", contextTag, fallbackIndex = 0)
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Nome do Contexto").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(contextTag).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "O contexto criado deve aparecer como chip na lista de contextos",
            composeRule.onAllNodesWithText(contextTag).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onAllNodesWithText(contextTag).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun showsReleaseHistoryDialog() {
        composeRule.waitForAppReady()
        composeRule.openAccountScreen()

        composeRule.onNodeWithText("Sistema & Versão").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Histórico de Versões & Patches").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Histórico de Versões & Patches").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Histórico de Versões").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            "O diálogo deve mostrar o título 'Histórico de Versões'",
            composeRule.onAllNodesWithText("Histórico de Versões").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "O diálogo deve listar pelo menos uma release",
            composeRule.onAllNodes(hasVersionText()).fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Histórico de Versões").fetchSemanticsNodes().isEmpty()
        }
    }

    private fun hasVersionText(): SemanticsMatcher = SemanticsMatcher("texto de versão") { node ->
        nodeTexts(node).any { VERSION_PATTERN.matches(it) }
    }

    private companion object {
        val VERSION_PATTERN = Regex("^v\\d+\\.\\d+(\\.\\d+)?$")
    }
}
