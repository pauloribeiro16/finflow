package com.financas.app.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financas.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Piloto: valida a infraestrutura de testes de UI no emulador
 * (seleção por contentDescription e por texto, ignorando o splash).
 */
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndShowsBottomNavigation() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithContentDescription("Início").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "O separador Início deve existir na barra inferior",
            composeRule.onAllNodesWithContentDescription("Início").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun canSwitchToTransactionsTab() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithContentDescription("Movimentos").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Movimentos").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Movimentos").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
