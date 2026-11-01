# CONTRACT — Phase 2: Navegação, Cockpit `Início`, `Movimentos` & Registo `< 3s`

**Date:** 2026-10-25
**Planner:** Antigravity
**Status:** APPROVED (Ready for Executor)
**Phase:** 2 of 4
**Depends On:** Phase 1
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

---

## Scope

Implementar a camada de UI diária em Jetpack Compose (Dark Mode Premium) e os respetivos ViewModels:
1. Tema Material 3 Ultra-Dark (`ui/theme/`) e Barra de Navegação Inferior de 5 pontos: `[ 🏠 Início | 🧾 Movimentos | ( ➕ ) | 📊 Análise | ⚙️ Conta ]`.
2. Folha de Registo Rápido (`AddTransactionBottomSheet.kt`) com:
   - Toggle `[ 💸 Gasto | 💰 Receita ]`
   - `CalculatorNumpad` escuro nativo (com operação de soma `+` inline, ex: `12,50 + 3,00`, sem abrir o teclado branco do sistema para valores)
   - Chips de sugestão de **Estabelecimento/Nota** que acionam o `SmartPrefillEngine` e preenchem automaticamente **Categoria > Subcategoria (`No Quê`)**, **Contexto (`Onde`)**, **Método (`Como`)** e **Fixo/Variável**.
3. Ecrã `Início` (`HomeScreen.kt` + `HomeViewModel.kt`):
   - Seletor `< Mês Ano >` de 1 toque
   - Cartão de 1 toque no início do mês para gerar Salário Base e Despesas Fixas do mês anterior (quando aplicável)
   - Hero Card com Recebido, Gasto (`€` e `% do recebido` + delta vs. Média Mensal), Poupança Líquida e **Barra de Esforço Tripartida (`Fixos % | Variáveis % | Poupado %`)**
   - Resumo rápido de Top Categorias, Top Contextos e últimos movimentos.
4. Ecrã `Movimentos` (`TransactionsScreen.kt` + `TransactionsViewModel.kt`):
   - Barra de pesquisa instantânea (filtra por estabelecimento, nota, subcategoria ou contexto)
   - Cartão de soma dinâmica do filtro (`X movimentos · Total: Y,YY € · Z% do recebido`)
   - Filtros em cápsulas por Período, Rigidez (`Fixos`/`Variáveis`/`Receitas`), Contexto (`Onde`) e Método (`Como`).

---

## Output Criteria (what was produced)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `CalculatorNumpadEvaluator` processa digitação em cêntimos e somas inline (`"12,50+3,50"` -> `1600L`) sem erros de arredondamento | MUST | — |
| 2 | `AddTransactionViewModel` preenche automaticamente `categoryId`, `contextTag`, `paymentMethod` e `isFixed` ao selecionar um chip de estabelecimento existente | MUST | — |
| 3 | `HomeViewModel` expõe o resumo do mês com percentagens corretas da Barra de Esforço (`fixedPct + variablePct + savedPct == 100%` quando `gasto <= recebido`) e aciona a geração de fixos de 1 toque | MUST | — |
| 4 | `TransactionsViewModel` filtra movimentos combinando texto de pesquisa + `contextTag` (`Onde`) + `paymentMethod` (`Como`) e recalcula a soma filtrada em `€` e `% do recebido` | MUST | — |
| 5 | Barra de navegação de 5 pontos abre o `AddTransactionBottomSheet` a partir de qualquer aba ao tocar no botão central `(+)` | SHOULD | — |

---

## Outcome Criteria (system state after)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | Todos os testes unitários da Phase 1 e Phase 2 passam (`./gradlew :app:testDebugUnitTest`) | MUST | — |
| 2 | A aplicação compila o APK de Debug sem erros (`./gradlew :app:assembleDebug`) | MUST | — |

---

## Validation Commands

### Criterion 1 — Avaliador do Teclado Calculadora (`CalculatorNumpad`)
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.ui.components.CalculatorNumpadEvaluatorTest"`
- **Expected:** `BUILD SUCCESSFUL` validando entrada simples, backspace, vírgula decimal e soma múltipla (`10,25 + 4,75 = 1500L`).
- **Pass condition:** Exit code 0.

### Criterion 2, 3 & 4 — Comportamento dos ViewModels (`Home`, `Transactions`, `AddTransaction`)
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.ui.screens.*ViewModelTest"`
- **Expected:** `BUILD SUCCESSFUL` validando o auto-prefill, a atualização reativa da Barra de Esforço e a soma dinâmica de pesquisa/filtros na aba `Movimentos`.
- **Pass condition:** Exit code 0.

### Outcome Criterion 2 — Compilação Completa do APK
- **Tier:** 4 (Integration / Build)
- **Command:** `./gradlew :app:assembleDebug`
- **Expected:** `BUILD SUCCESSFUL` e APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.
- **Pass condition:** Exit code 0.

---

## Files to Change

| File | Action | Why |
|------|--------|-----|
| `app/src/main/kotlin/com/financas/app/ui/theme/*` | create | Paleta Ultra-Dark (Revolut/Copilot) e tipografia financeira |
| `app/src/main/kotlin/com/financas/app/ui/components/CalculatorNumpad.kt` | create | Teclado calculadora escuro integrado com soma inline |
| `app/src/main/kotlin/com/financas/app/ui/screens/addsheet/AddTransactionBottomSheet.kt` | create | Folha de registo `< 3s` com chips `Onde`, `No Quê`, `Como` e auto-prefill |
| `app/src/main/kotlin/com/financas/app/ui/screens/home/HomeScreen.kt` | create | Cockpit do Mês + Barra de Esforço + Geração 1-toque de fixos |
| `app/src/main/kotlin/com/financas/app/ui/screens/transactions/TransactionsScreen.kt` | create | Feed global pesquisável com soma dinâmica e filtros pelas 4 dimensões |
| `app/src/main/kotlin/com/financas/app/ui/navigation/MainScaffold.kt` | create | Barra inferior de 5 pontos com botão central `(+)` |
| `app/src/test/kotlin/com/financas/app/ui/**/*Test.kt` | create | Testes unitários comportamentais de UI state e calculadora |
