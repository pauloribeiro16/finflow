package com.financas.app.uitest

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.financas.app.FinancasApplication

/**
 * Utilitários partilhados pelos testes de UI.
 *
 * Regras de isolamento adoptadas:
 * - a base de dados Room (e as SharedPreferences) persistem entre testes da
 *   mesma execução, por isso tudo o que é criado usa tokens únicos;
 * - nunca se assume um número fixo de linhas: as asserções são relativas.
 */

/** Token único por teste (merchant, meta, aplicação, subcategoria, contexto...). */
fun uniqueToken(prefix: String = "UI"): String = "$prefix${System.currentTimeMillis()}"

/**
 * Aguarda que a app esteja utilizável: barra inferior presente e splash animado
 * já desmontado (o splash cobre a UI durante ~1,3 s e intercepta os toques).
 */
fun ComposeTestRule.waitForAppReady(timeoutMillis: Long = 30_000) {
    forcePortuguese()
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithContentDescription("Início").fetchSemanticsNodes().isNotEmpty()
    }
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText("FINFLOW").fetchSemanticsNodes().isEmpty()
    }
    waitForIdle()
}

/** Os testes usam os textos em português; a app arranca em inglês por omissão. */
fun ComposeTestRule.forcePortuguese() {
    val store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FinancasApplication)
        .container.userProfileStore
    if (store.getAppLanguage() != "PT") {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.setAppLanguage("PT") }
        waitForIdle()
    }
}

/** Iniciais apresentadas no avatar para o utilizador atualmente autenticado. */
fun currentUserInitials(): String {
    val app = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .applicationContext as FinancasApplication
    return app.container.userProfileStore
        .getUserName()
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "PR" }
}

/**
 * Abre o ecrã "Conta" a partir do avatar do topo.
 *
 * O avatar só expõe contentDescription quando existe fotografia de perfil;
 * caso contrário mostra apenas as iniciais do utilizador (que podem mudar
 * porque outro teste altera o nome do perfil).
 */
fun ComposeTestRule.openAccountScreen(timeoutMillis: Long = 20_000) {
    val byPhoto = onAllNodesWithContentDescription("Foto de perfil")
    if (byPhoto.fetchSemanticsNodes().isNotEmpty()) {
        byPhoto.onFirst().performSemanticsAction(SemanticsActions.OnClick)
    } else {
        onAllNodesWithText(currentUserInitials()).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText("Sistema & Versão").fetchSemanticsNodes().isNotEmpty()
    }
}

/** Todos os textos semânticos de um nó. */
fun nodeTexts(node: SemanticsNode): List<String> {
    @Suppress("UNCHECKED_CAST")
    val texts = node.config.getOrElseNullable(SemanticsProperties.Text) { null } as? List<AnnotatedString>
    return texts?.map { it.text } ?: emptyList()
}

/** Primeiro texto semântico de um nó (string vazia se não houver). */
fun nodeText(node: SemanticsNode): String = nodeTexts(node).firstOrNull() ?: ""

/** Texto currently editável de um nó (string vazia se não for um campo de texto). */
fun nodeEditableText(node: SemanticsNode): String {
    val editable = node.config.getOrElseNullable(SemanticsProperties.EditableText) { null } as? AnnotatedString
    return editable?.text ?: ""
}

/** Matcher para campos de texto ainda vazios (usado para distinguir campos de diálogo). */
fun hasEmptyEditableText(): SemanticsMatcher = SemanticsMatcher("texto editável vazio") { node ->
    nodeEditableText(node).isEmpty()
}

/**
 * Escreve num campo de texto editável.
 *
 * Estratégia 1: localizar pelo rótulo visível (o rótulo pode estar fundido na
 * semântica do campo).
 * Estratégia 2 (fallback): usar a posição do campo na ordem de composição,
 * conforme documentado em cada chamador.
 */
fun ComposeTestRule.fillTextField(
    label: String,
    value: String,
    fallbackIndex: Int = 0
) {
    val byLabel = onAllNodes(hasSetTextAction() and hasText(label, substring = true))
    val target = if (byLabel.fetchSemanticsNodes().size == 1) {
        byLabel.onFirst()
    } else {
        val all = onAllNodes(hasSetTextAction())
        check(fallbackIndex < all.fetchSemanticsNodes().size) {
            "Campo '$label' não encontrado por rótulo e o índice $fallbackIndex está fora de range"
        }
        all[fallbackIndex]
    }
    runCatching { target.performScrollTo() }
    if (nodeEditableText(target.fetchSemanticsNode()).isNotEmpty()) {
        target.performTextClearance()
    }
    target.performTextInput(value)
    waitForIdle()
}

/**
 * Escreve no único campo de texto editável ainda vazio do ecrã/diálogo atual.
 * Usado quando existe apenas um campo editável (ex.: nome do estabelecimento).
 */
fun ComposeTestRule.fillOnlyEmptyTextField(value: String) {
    val fields = onAllNodes(hasSetTextAction() and hasEmptyEditableText())
    val count = fields.fetchSemanticsNodes().size
    check(count == 1) { "Esperava exactamente 1 campo de texto editável vazio, encontrei $count" }
    fields.onFirst().performTextInput(value)
    waitForIdle()
}

/** Contentor com scroll horizontal que contém o texto indicado (linha de lentes, capsules...). */
fun ComposeTestRule.horizontalScrollerContaining(text: String): SemanticsNodeInteraction =
    onAllNodes(
        hasScrollAction() and hasAnyDescendant(hasText(text)),
        useUnmergedTree = true
    ).onFirst()

/**
 * Desloca o contentor horizontal até o alvo ficar dentro da janela visível.
 *
 * `performScrollTo()` só deals com scroll vertical, por isso usamos um gesto
 * de arrasto (swipe) calculado a partir das coordenadas reais dos nós.
 */
fun ComposeTestRule.scrollHorizontallyTo(
    scroller: SemanticsNodeInteraction,
    target: SemanticsNodeInteraction
) {
    repeat(12) {
        val scrollBounds = scroller.fetchSemanticsNode().boundsInRoot
        val targetBounds = target.fetchSemanticsNode().boundsInRoot
        if (targetBounds.left >= scrollBounds.left && targetBounds.right <= scrollBounds.right) {
            return
        }
        val forward = targetBounds.left > scrollBounds.right
        val distance = if (forward) {
            targetBounds.left - scrollBounds.right
        } else {
            scrollBounds.left - targetBounds.right
        }
        swipeHorizontally(scroller, forward, distance)
    }
    val scrollBounds = scroller.fetchSemanticsNode().boundsInRoot
    val targetBounds = target.fetchSemanticsNode().boundsInRoot
    check(
        targetBounds.left >= scrollBounds.left && targetBounds.right <= scrollBounds.right
    ) { "Não foi possível trazer o alvo para dentro da área visível" }
}

/** Arrasta o contentor horizontal na direcção indicada (forward = conteúdo à direita). */
fun ComposeTestRule.swipeHorizontally(
    scroller: SemanticsNodeInteraction,
    forward: Boolean,
    distance: Float
) {
    scroller.performTouchInput {
        val width = (right - left).coerceAtLeast(1f)
        val y = center.y
        val step = distance.coerceIn(1f, width - 4f)
        if (forward) {
            swipe(
                start = Offset(right - 2f, y),
                end = Offset(right - 2f - step, y),
                durationMillis = 600
            )
        } else {
            swipe(
                start = Offset(left + 2f, y),
                end = Offset(left + 2f + step, y),
                durationMillis = 600
            )
        }
    }
    waitForIdle()
}

/**
 * Estabelecimento do movimento mais recente do mês corrente.
 *
 * A lista do Início é ordenada por epochDay DESC, id DESC, por isso o primeiro
 * elemento do DAO é exatamente a primeira linha apresentada no ecrã.
 */
fun mostRecentMerchantOfCurrentMonth(): String {
    val app = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .applicationContext as FinancasApplication
    val now = java.time.LocalDate.now()
    val yearMonth = now.year * 100 + now.monthValue
    val transactions = kotlinx.coroutines.runBlocking {
        app.container.transactionDao.getByMonthSync(yearMonth)
    }
    val merchant = transactions.firstOrNull { it.merchant.isNotBlank() }?.merchant
    checkNotNull(merchant) { "Não existem movimentos com estabelecimento no mês corrente" }
    return merchant
}

/**
 * Nome da primeira categoria pai (ordenadas por sortOrder).
 * É a categoria pré-selecionada no diálogo "Nova Subcategoria".
 */
fun firstParentCategoryName(): String {
    val app = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .applicationContext as FinancasApplication
    val categories = kotlinx.coroutines.runBlocking {
        app.container.categoryDao.getAllSync()
    }
    val parent = categories.firstOrNull { it.parentId == null }
    checkNotNull(parent) { "Não existem categorias pai na base de dados" }
    return parent.name
}

/** Converte um valor monetário em PT-PT ("1.250,00 €", "-12,50 €") para cêntimos. */
fun parseEuroToCents(text: String): Long {
    val clean = text
        .replace("€", "")
        .replace("+", "")
        .replace("-", "")
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
    return ((clean.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
}
