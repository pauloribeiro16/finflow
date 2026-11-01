# CONTRACT — Phase 7: Fundamentos do Dia a Dia

**Date:** 2026-10-26
**Planner:** ZCode (Planner Role)
**Status:** APPROVED (Ready for Executor)
**Phase:** 7 of 10
**Depends On:** Phase 6 + commit `d351649`
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

## Scope

Sem alterar a base de dados, sem dependências novas:

1. **Editar movimentos:** `MainScaffold` passa a ter `editingTransaction: TransactionEntity?`; `TransactionsScreen` e `HomeScreen` recebem `onEdit: (TransactionEntity) -> Unit` e a linha (`TransactionRowItem`, definida em `HomeScreen.kt`, usada por ambos) ganha ícone de lápis (`Icons.Default.Edit`, contentDescription "Editar") que dispara `onEdit(tx)`. O `AddTransactionViewModel` ganha `startEdit(tx)` (preenche type, rawAmount derivado dos cêntimos "15,50", amountCents, merchant, parent+subcategoria resolvidos via mapa de categorias, contextTag, paymentMethod, isFixed, epochDay e `editingTransactionId: Long?` no estado) e `startNewEntry()` (reset ao estado default + merchants recentes). `saveTransaction` faz `transactionDao.update(entity com id = editingTransactionId)` quando a editar, `insert` caso contrário; o guard de valor 0 mantém-se. Título "A editar movimento" na folha quando em edição. Abrir o `( + )` chama `startNewEntry()` antes de mostrar a folha (corrige estado velho persistente).
2. **Seletor de data:** linha "Data" na folha de registo com a data formatada PT-PT (`26 de outubro de 2026`) e ícone de calendário; toque abre `DatePickerDialog` do Material 3 (`rememberDatePickerState`, inicial = epochDay atual do estado) ligado ao `onDateSelected(epochDay)` que já existe no ViewModel; a data escolhida aparece na linha e o `saveTransaction` deriva `yearMonth` dela (já o faz).
3. **Confirmação antes de apagar:** em `TransactionsScreen` e `HomeScreen`, o caixote passa a marcar `pendingDelete` e um `AlertDialog` (padrão de `AccountScreen.kt`, containerColor DarkSurface) confirma: título "Apagar movimento?", texto "Esta ação é definitiva e não pode ser anulada.", botões "Apagar" (AccentCoral) / "Cancelar". Em `AccountScreen`, o mesmo para subcategorias ("Apagar subcategoria?"). Contextos não são apagáveis (sem alteração).
4. **Contextos persistentes:** novo `data/prefs/ContextTagsStore.kt` — `interface ContextTagsStore { fun getCustomTags(): Set<String>; fun addTag(tag: String) }` + `SharedPrefsContextTagsStore(context)` (prefs `"financas_prefs"`, chave `"custom_context_tags"`, `putStringSet`) + `FakeContextTagsStore` nos fakes de teste. Propriedade nova no `AppContainer`. `AccountViewModel` passa a inicializar `customContextTags` do store e a persistir em `addContextTag`. `AddTransactionViewModel` recebe o store e as cápsulas de contexto da folha passam a listar também os contextos personalizados (hoje só `DefaultSeed.CONTEXTS`).
5. **Correção de coletores:** em `AnalyticsViewModel.setYearMonth`, cancelar os Jobs anteriores de `observeMonthTransactions`/`observeYearTransactions` antes de relançar (campos privados `Job?`). Auditar `HomeViewModel.loadMonthData` e `TransactionsViewModel` para o mesmo padrão e corrigir se existir.
6. **"Todos" líquido:** `TransactionsUiState` ganha `filteredExpenseCents` e `filteredIncomeCents`; o cartão de soma com fluxo "Todos" mostra `Gasto: X · Recebido: Y` (e o peso no recebido passa a ser sobre o gasto); com fluxo Fixos/Variáveis mantém o total atual, com Receitas mostra o total de rendimentos.

## Output Criteria

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `startEdit` preenche os 8 campos + id; gravar em edição chama `update` (contagem do DAO inalterada, campos substituídos); gravar sem edição chama `insert`; `startNewEntry` repõe o default | MUST | — |
| 2 | `onDateSelected(epochDay)` reflete-se no estado e o movimento gravado usa o `yearMonth` da data escolhida | MUST | — |
| 3 | Diálogos de confirmação em Movimentos, Início e subcategorias; sem diálogo, nada é apagado | MUST | — |
| 4 | `ContextTagsStore` round-trip; `AccountViewModel` carrega tags persistidas no arranque e persiste ao adicionar; cápsulas de contexto da folha incluem tags personalizadas | MUST | — |
| 5 | Coletores cancelados antes de re-subscrição em `AnalyticsViewModel` (e no mesmo padrão onde existir); nenhum acúmulo | MUST | — |
| 6 | `filteredExpenseCents`/`filteredIncomeCents` exatos em todos os fluxos; UI "Todos" líquida | MUST | — |
| 7 | Suite completa verde (131 existentes intactos + novos), `assembleDebug` OK, `git diff --exit-code app/build.gradle.kts app/src/main/kotlin/com/financas/app/data/entity/ app/src/main/kotlin/com/financas/app/data/db/` limpo | MUST | — |
| 8 | `GOAL_DECOMPOSITION.md`/`REQUISITOS_FUNCIONAIS.md` atualizados | SHOULD | — |

## Validation Commands

### Criteria 1–6 — comportamento (Tier 3)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.ui.screens.addsheet.AddTransactionViewModelTest" --tests "com.financas.app.ui.screens.transactions.TransactionsViewModelTest" --tests "com.financas.app.ui.screens.account.AccountViewModelTest"`
- **Expected:** a) edição grava por `update` com todos os campos preservados; b) reset de folha; c) data → yearMonth; d) store com fake persiste; e) splits de gasto/receita exatos. **Pass condition:** Exit code 0.

### Criterion 7 — suite, build e restrições (Tier 4)
- **Command:** `./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug && git diff --exit-code app/build.gradle.kts app/src/main/kotlin/com/financas/app/data/entity/ app/src/main/kotlin/com/financas/app/data/db/`
- **Expected:** 0 falhas; APK compila; build/entities/DAOs de produção da fase anterior intactos (o DAO não precisa de alterações nesta fase). **Pass condition:** Exit code 0.

## Files to Change

| File | Action | Why |
|---|---|---|
| `ui/navigation/MainScaffold.kt` | edit | Estado de edição + reset + wiring onEdit/onAdd |
| `ui/screens/addsheet/AddTransactionViewModel.kt` | edit | startEdit/startNewEntry/branch update, store de contextos |
| `ui/screens/addsheet/AddTransactionBottomSheet.kt` | edit | Linha de data + DatePickerDialog, cápsulas com tags personalizadas, título de edição |
| `ui/screens/transactions/TransactionsScreen.kt` | edit | onEdit, diálogo de confirmação, cartão líquido |
| `ui/screens/transactions/TransactionsViewModel.kt` | edit | Splits de totais (+ correção de coletores se aplicável) |
| `ui/screens/home/HomeScreen.kt` | edit | onEdit no HomeScreen/TransactionRowItem, diálogo de confirmação |
| `ui/screens/account/AccountScreen.kt` | edit | Diálogo de confirmação de subcategoria |
| `ui/screens/account/AccountViewModel.kt` | edit | Persistência de contextos |
| `ui/screens/analytics/AnalyticsViewModel.kt` | edit | Correção de coletores |
| `data/prefs/ContextTagsStore.kt` | create | Store de contextos |
| `di/AppContainer.kt` | edit | Propriedade `contextTagsStore` |
| `ui/fakes/FakeDaos.kt` (ou novo fake) | edit | `FakeContextTagsStore` |
| Testes: `AddTransactionViewModelTest`, `TransactionsViewModelTest`, `AccountViewModelTest` | edit | Cenários 1–6 |
