package com.financas.app.uitest

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financas.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fluxo completo de movimento MB WAY pendente:
 * simular a notificação -> rever na fila -> confirmar -> movimento validado.
 *
 * A fila de pendentes é persistida em Room, por isso o teste começa por
 * descartar eventuais pendentes herdados de execuções anteriores.
 */
@RunWith(AndroidJUnit4::class)
class MbWayPendingUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun simulatesConfirmsAndClearsPendingMbWayMovement() {
        composeRule.waitForAppReady()

        // 1. Limpar a fila de pendentes (herdada de execuções anteriores)
        clearPendingQueue()

        // 2. Simular a notificação MB WAY a partir de Conta > Configurações
        composeRule.openAccountScreen()
        composeRule.onNodeWithText("Configurações").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Captura Automática (MB WAY & Bancos)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText(SIMULATE_LABEL))
        composeRule.onNodeWithText(SIMULATE_LABEL).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Notificação de teste criada", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "A app deve informar que a notificação de teste foi criada",
            composeRule.onAllNodesWithText("Notificação de teste criada", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )

        // 3. Voltar ao Início e abrir a fila de pendentes
        composeRule.onNodeWithContentDescription("Início").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Movimentos Pendentes", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O Início deve mostrar a faixa de movimentos pendentes",
            composeRule.onAllNodesWithText("Movimentos Pendentes", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithText("Rever").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Fila de Pendentes").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "A folha deve mostrar o movimento pendente",
            composeRule.onAllNodesWithText("Continente Bom Dia").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "A folha deve mostrar o montante de 12,50 €",
            composeRule.onAllNodesWithText("- 12,50 €").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "A folha deve identificar a origem como MB WAY",
            composeRule.onAllNodesWithText("MB WAY").fetchSemanticsNodes().isNotEmpty()
        )

        // 4. Confirmar o movimento
        composeRule.onNodeWithText("Confirmar").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Não tens movimentos pendentes para rever.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "A fila de pendentes deve ficar vazia",
            composeRule.onAllNodesWithText("Não tens movimentos pendentes para rever.")
                .fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "O movimento pendente já não deve constar da fila",
            composeRule.onAllNodesWithText("Continente Bom Dia").fetchSemanticsNodes().isEmpty()
        )

        // 5. Fechar a folha: a faixa de pendentes desaparece
        closePendingSheet()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Movimentos Pendentes", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }
        assertTrue(
            "A faixa de movimentos pendentes deve desaparecer",
            composeRule.onAllNodesWithText("Movimentos Pendentes", substring = true)
                .fetchSemanticsNodes().isEmpty()
        )

        // 6. O movimento confirmado passa a estar no Início
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Continente Bom Dia").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O movimento confirmado deve aparecer na lista de movimentos do Início",
            composeRule.onAllNodesWithText("Continente Bom Dia").fetchSemanticsNodes().isNotEmpty()
        )
    }

    /** Descarta todos os movimentos pendentes existentes. */
    private fun clearPendingQueue() {
        val banner = composeRule.onAllNodesWithText("Movimentos Pendentes", substring = true)
        if (banner.fetchSemanticsNodes().isEmpty()) return

        banner.onFirst().performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Fila de Pendentes").fetchSemanticsNodes().isNotEmpty()
        }

        repeat(20) {
            val discard = composeRule.onAllNodesWithContentDescription("Descartar")
            if (discard.fetchSemanticsNodes().isEmpty()) return
            discard.onFirst().performClick()
            composeRule.waitForIdle()
        }

        closePendingSheet()
    }

    /** Fecha a folha de pendentes com a tecla BACK. */
    private fun closePendingSheet() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Fila de Pendentes").fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        const val SIMULATE_LABEL = "Simular Notificação de Teste (12,50 € MB WAY)"
    }
}
