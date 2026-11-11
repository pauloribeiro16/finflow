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
 * Navegação principal: os 5 pontos da barra inferior, o hub "Património",
 * o ecrã "Conta" (aberto pelo avatar) e a abertura da folha de edição a
 * partir de uma linha de movimento.
 */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomBarReachesEveryPrimaryTab() {
        composeRule.waitForAppReady()

        // 1. Início
        composeRule.onNodeWithContentDescription("Início").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("DESPESAS DESTE MÊS").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O ecrã Início deve mostrar o título 'Início'",
            composeRule.onAllNodesWithText("Início").fetchSemanticsNodes().isNotEmpty()
        )

        // 2. Movimentos
        composeRule.onNodeWithContentDescription("Movimentos").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Pesquisar descrição, comerciante...")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O ecrã Movimentos deve mostrar o título 'Movimentos'",
            composeRule.onAllNodesWithText("Movimentos").fetchSemanticsNodes().isNotEmpty()
        )

        // 3. Análise
        composeRule.onNodeWithContentDescription("Análise").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Visão Anual").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "A barra inferior deve continuar a mostrar 'Análise'",
            composeRule.onAllNodesWithText("Análise").fetchSemanticsNodes().isNotEmpty()
        )

        // 4. Património
        composeRule.onNodeWithContentDescription("Património").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("TOTAL APLICADO").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O separador Património deve continuar visível",
            composeRule.onAllNodesWithContentDescription("Património").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun centerFabOpensNewTransactionSheet() {
        composeRule.waitForAppReady()

        composeRule.onNodeWithContentDescription("Nova Transação").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Novo Movimento").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O FAB central deve abrir a folha 'Nova Despesa'",
            composeRule.onAllNodesWithText("Novo Movimento").fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithContentDescription("Fechar").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Novo Movimento").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun wealthHubExposesInvestmentsGoalsAndBudgets() {
        composeRule.waitForAppReady()
        composeRule.onNodeWithContentDescription("Património").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("TOTAL APLICADO").fetchSemanticsNodes().isNotEmpty()
        }

        // Investimentos (separador por omissão)
        assertTrue(
            "O hub deve mostrar o título 'Investimentos'",
            composeRule.onAllNodesWithText("Investimentos").fetchSemanticsNodes().isNotEmpty()
        )

        // Metas
        composeRule.onNodeWithText("🎯 Metas").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Metas de Poupança").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O hub deve mostrar o título 'Metas'",
            composeRule.onAllNodesWithText("Metas").fetchSemanticsNodes().isNotEmpty()
        )

        // Orçamentos
        composeRule.onNodeWithText("📊 Orçamentos").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("ORÇAMENTO GLOBAL").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O hub deve mostrar o título 'Orçamentos'",
            composeRule.onAllNodesWithText("Orçamentos").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun accountScreenOpensFromAvatarWithThreeTabs() {
        composeRule.waitForAppReady()
        composeRule.openAccountScreen()

        assertTrue(
            "O ecrã Conta deve mostrar o separador 'Perfil'",
            composeRule.onAllNodesWithText("Perfil").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "O ecrã Conta deve mostrar o separador 'Configurações'",
            composeRule.onAllNodesWithText("Preferências").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "O ecrã Conta deve mostrar o separador 'Versão & Sistema'",
            composeRule.onAllNodesWithText("Sistema & Versão").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun tappingTransactionRowOpensEditSheet() {
        composeRule.waitForAppReady()

        // Determinamos o movimento mais recente do mês corrente (a lista do
        // Início está ordenada por epochDay DESC, id DESC).
        val merchant = mostRecentMerchantOfCurrentMonth()

        // O Início mostra apenas 5 movimentos: expandimos a lista para poder
        // tocar num movimento determinístico.
        val showMore = composeRule.onAllNodes(hasText("Mostrar mais", substring = true))
        if (showMore.fetchSemanticsNodes().isNotEmpty()) {
            showMore.onFirst().performScrollTo().performClick()
            composeRule.waitForIdle()
        }

        composeRule.onAllNodesWithText(merchant).onFirst().performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Editar Movimento").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Tocar numa linha de movimento deve abrir a folha 'Editar Despesa'",
            composeRule.onAllNodesWithText("Editar Movimento").fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithText("Editar Movimento").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Fechar").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Editar Movimento").fetchSemanticsNodes().isEmpty()
        }
    }
}
