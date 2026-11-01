# CONTRACT — Phase 3: Motor Gráfico Interativo (`Análise` — 4 Vistas)

**Date:** 2026-10-25
**Planner:** Antigravity
**Status:** APPROVED (Ready for Executor)
**Phase:** 3 of 4
**Depends On:** Phase 2
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

---

## Scope

Implementar a aba `📊 Análise` (`AnalyticsScreen.kt` + `AnalyticsViewModel.kt`) e os gráficos interativos nativos em Compose `Canvas`:
1. **Cabeçalho Global de Análise:**
   - Seletor `< Mês Ano >` (ou `< Ano >`) de 1 toque.
   - Interruptor Global **`[ € | % do que Recebo ]`** que transforma instantaneamente todos os gráficos e listas entre valores absolutos em Euros e peso percentual face ao rendimento do período.
2. **Vista 1 — `No Quê` (Categorias Pai > Subcategorias):**
   - `InteractiveDonutCanvas` onde tocar numa fatia destaca a Categoria Pai no centro (`€`, `% do recebido`, `% dos gastos` e diferença face à Média Mensal).
   - Drill-down de 2 níveis: tocar numa Categoria Pai (ex: *Alimentação*) expande as suas Subcategorias (*Supermercado*, *Almoços*, *Restaurantes*) com barras de progresso e acesso às transações.
3. **Vista 2 — `Onde` (Contextos & Top Estabelecimentos):**
   - Gráfico de barras comparativo pelos Contextos (`Trabalho`, `Casa`, `Saída`, `Férias`, `Online`, `Comércio`) em `€` e `% do que recebo`.
   - Ranking **Pareto Top 10 Estabelecimentos** mostrando montante total, `% do rendimento`, número de idas no mês e valor médio por transação.
4. **Vista 3 — `Como` (Fixos vs. Variáveis & Meios de Pagamento):**
   - Gráfico de estrutura de rigidez (`Gastos Fixos` vs. `Gastos Variáveis` vs. `Poupança Líquida`).
   - Breakdown visual por Meio de Pagamento (`MB Way`, `Cartão`, `Débito Direto`, `Transferência`, `Dinheiro`).
5. **Vista 4 — `Evolução` (Comparação Anual Mês a Mês):**
   - `InteractiveMonthlyBarChart` com **Touch Scrubber** (tocar num mês mostra Recebido, Gasto, Poupança e `% vs. Média Anual`), **Linha de Média Mensal tracejada** e curva da Taxa de Poupança `%`.

---

## Output Criteria (what was produced)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `AnalyticsViewModel` alterna entre modo `EURO` e modo `INCOME_PERCENTAGE` atualizando os valores formatados nas 4 vistas (`No Quê`, `Onde`, `Como`, `Evolução`) | MUST | — |
| 2 | Na Vista 1 (`No Quê`), selecionar uma Categoria Pai filtra/expande exatamente as suas Subcategorias filhas cuja soma bate 100% com o total da Categoria Pai | MUST | — |
| 3 | Na Vista 2 (`Onde`), o ranking de Contextos e o Top Estabelecimentos ordenam por valor decrescente e calculam corretamente o nº de transações e o ticket médio em cêntimos | MUST | — |
| 4 | Na Vista 4 (`Evolução`), o cálculo anual devolve os 12 meses com a Média Mensal calculada apenas sobre os meses com atividade (não diluída por meses futuros a zero) | MUST | — |
| 5 | Gráficos em `Canvas` suportam seleção por toque (`pointerInput`) atualizando o estado selecionado no `AnalyticsViewModel` | SHOULD | — |

---

## Outcome Criteria (system state after)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | Todos os testes unitários das Phases 1, 2 e 3 passam (`./gradlew :app:testDebugUnitTest`) | MUST | — |
| 2 | `./gradlew :app:assembleDebug` compila sem erros | MUST | — |

---

## Validation Commands

### Criterion 1, 2, 3 & 4 — Lógica das 4 Vistas Analíticas e Toggle `[ € | % do Recebido ]`
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.ui.screens.analytics.AnalyticsViewModelTest"`
- **Expected:** `BUILD SUCCESSFUL` validando: (a) toggle `€` vs `% do recebido`; (b) drill-down `Categoria Pai -> Subcategorias`; (c) cálculo de frequência e ticket médio por estabelecimento na vista `Onde`; (d) média anual sem diluição por meses vazios.
- **Pass condition:** Exit code 0.

### Outcome Criterion 2 — Compilação Completa com Gráficos Canvas
- **Tier:** 4 (Integration / Build)
- **Command:** `./gradlew :app:assembleDebug`
- **Expected:** `BUILD SUCCESSFUL`.
- **Pass condition:** Exit code 0.

---

## Files to Change

| File | Action | Why |
|------|--------|-----|
| `app/src/main/kotlin/com/financas/app/ui/components/InteractiveChartsCanvas.kt` | create | Donut Interativo, Barras de Pareto e Gráfico Anual com Touch Scrubber e Linha de Média |
| `app/src/main/kotlin/com/financas/app/ui/screens/analytics/AnalyticsViewModel.kt` | create | Estado reativo das 4 vistas, seleção de fatia/mês e toggle `[ € \| % do Recebido ]` |
| `app/src/main/kotlin/com/financas/app/ui/screens/analytics/AnalyticsScreen.kt` | create | Ecrã unificado `Análise` com as 4 sub-vistas e drill-down |
| `app/src/test/kotlin/com/financas/app/ui/screens/analytics/AnalyticsViewModelTest.kt` | create | Testes comportamentais das 4 lentes analíticas |
