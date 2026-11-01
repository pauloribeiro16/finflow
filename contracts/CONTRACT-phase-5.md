# CONTRACT — Phase 5: Análises Avançadas (Comparações, Tendências e Ano vs Ano)

**Date:** 2026-10-26
**Planner:** ZCode (Planner Role)
**Status:** APPROVED (Ready for Executor)
**Phase:** 5 of 5
**Depends On:** Phase 4 + commit `07931ff` (dados históricos)
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

## Scope

1. **Motor de comparação (`ComparisonCalculator`, `domain/usecase/`, classe pura):**
   - `MonthComparison` com deltas (cêntimos + %) de Gasto, Recebido e Poupança entre o mês atual e uma base.
   - `CategoryDelta` por categoria pai (roll-up subcategorias → pai, com fallback cinza para ids desconhecidos), ordenado por delta descendente; guard: base zero → percent `null`.
2. **Motor de tendências (`TrendCalculator`, `domain/usecase/`, classe pura):**
   - Série mensal (12 slots, zero-fill) do gasto de uma categoria pai num ano, com média **não diluída** (só meses com gastos) e total do ano.
   - Série da taxa de poupança (%) dos 12 meses + média sobre meses com atividade.
   - Agregados por ano presentes nos dados: recebido, gasto, poupança, taxa, série mensal de gastos (12 slots) e top 3 categorias.
3. **Gráficos Canvas nativos (`ui/components/InteractiveChartsCanvas.kt`):**
   - `LinePoint` + `InteractiveLineChart`: linha com pontos, linha tracejada de média, linha de referência opcional, toque/arrasto para selecionar índice, legenda e rótulos dos meses.
   - `YearlyBarData` + `InteractiveYearComparisonChart`: barras agrupadas por mês (uma série por ano), legenda com totais, rótulos Jan–Dez.
4. **ViewModel (`AnalyticsViewModel`):**
   - Lens nova `COMPARAR("Comparar")` no enum `AnalyticsLens` (5.º separador).
   - `ComparisonBaseline` (`PREVIOUS_MONTH`, `MONTHLY_AVERAGE`, `SAME_MONTH_LAST_YEAR`) com re cálculo ao trocar.
   - Observação `transactionDao.getAll()` para dados multi-anuais.
   - Estado novo: `comparison` (com strings pré-formatadas no modo €/% ativo), `categoryTrend` + chips de categorias (top 8 do ano, primeira selecionada por defeito), `savingsRateTrend` (referência 20%), `yearComparison` + `yearTotals`. Seleção de mês dos gráficos de linha partilha `selectedEvolutionMonth` (índice = mês − 1).
5. **Ecrã (`AnalyticsScreen`):**
   - `CompararView`: cápsulas de base, cartão de deltas (Gasto/Recebido/Poupança com ícone TrendingUp/Down colorido: subida de gasto = AccentCoral, descida de gasto = AccentEmerald; recebido/poupança ao contrário) e listas "Maiores Subidas" / "Maiores Descidas" (top 5).
   - `EvolucaoView`: cartão "Evolução de {Categoria}" (chips + `InteractiveLineChart` na cor da categoria), cartão "Taxa de Poupança" (`InteractiveLineChart` AccentEmerald + referência "Meta 20%" AccentAmber), cartão "Comparação Anual" (`InteractiveYearComparisonChart`) e cartão "Totais por Ano".
6. **Testes:** `ComparisonCalculatorTest`, `TrendCalculatorTest` (novos) e `AnalyticsViewModelTest` (novos casos, padrão FakeDaos atual).

## Output Criteria (what was produced)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `ComparisonCalculator.calculatePercent(50000, 40000)` = 25.0; `calculatePercent(40000, 0)` = `null`; `calculateMonthComparison` devolve deltas exatos em cêntimos e `categoryDeltas` ordenado por `deltaCents` descendente, apenas com despesas, com roll-up para a categoria pai | MUST | — |
| 2 | `TrendCalculator.calculateCategoryTrend` devolve exatamente 12 pontos com zero-fill, média sobre meses com gastos > 0 apenas; `calculateSavingsRateSeries` devolve 12 taxas corretas; `calculateYearAggregates` agrupa todos os anos presentes com totais exatos e top 3 categorias | MUST | — |
| 3 | ViewModel: lens `COMPARAR` acessível; as 3 bases recalculam (`comparison` não nulo com valores formatados no modo ativo; `comparison == null` quando a base não tem dados); deltas de gasto usam sinal: positivo = subida | MUST | — |
| 4 | ViewModel: seleção de categoria na evolução atualiza série de 12 pontos e média; `savingsRateTrend` com referência 20.0; `yearComparison` contém uma entrada por ano com dados e `yearTotals` formatado | MUST | — |
| 5 | Todos os testes unitários (52 existentes + novos) passam na suite completa | MUST | — |
| 6 | `./gradlew :app:assembleDebug` compila sem erros | MUST | — |
| 7 | `app/build.gradle.kts` e `data/entity/*` não alterados (zero dependências novas, zero mudanças de esquema) | MUST | — |
| 8 | `GOAL_DECOMPOSITION.md` e `REQUISITOS_FUNCIONAIS.md` atualizados com a fase 5 | SHOULD | — |

## Outcome Criteria (system state after)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | Suite completa `./gradlew :app:testDebugUnitTest` a 100% (0 falhas, 0 ignorados) | MUST | — |
| 2 | APK de debug compila (`./gradlew :app:assembleDebug`) | MUST | — |

## Validation Commands

### Criterion 1 & 2 — motores de comparação e tendências
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.usecase.ComparisonCalculatorTest" --tests "com.financas.app.domain.usecase.TrendCalculatorTest"`
- **Expected:**
  a. Deltas de cêntimos e percentagens exatos (tolerância 0.01 em doubles);
  b. Guard base zero devolve percent `null` sem exceção;
  c. Roll-up de subcategorias para o pai e ordenação por delta;
  d. Séries de 12 slots com zero-fill e média não diluída.
- **Pass condition:** Exit code 0.

### Criterion 3 & 4 — estado das novas vistas no ViewModel
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.ui.screens.analytics.AnalyticsViewModelTest"`
- **Expected:**
  a. Troca de base de comparação recalcula `comparison` (incl. strings "+"/"−" e percentagens PT-PT);
  b. `comparison == null` quando a base escolhida não tem movimentos;
  c. Série de categoria com zero-fill e média correta ao selecionar chips;
  d. `savingsRateTrend` e `yearComparison` corretos com dados multi-ano.
- **Pass condition:** Exit code 0.

### Criteria 5 & 6 & 7 — suite completa, build e restrições
- **Tier:** 4 (Integration / Build)
- **Command:** `./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug && git diff --exit-code app/build.gradle.kts app/src/main/kotlin/com/financas/app/data/entity/`
- **Expected:** suite 100%, APK compilado, nenhum diff em dependências/esquema.
- **Pass condition:** Exit code 0 em todos.

## Files to Change

| File | Action | Why |
|---|---|---|
| `app/src/main/kotlin/com/financas/app/domain/usecase/ComparisonCalculator.kt` | create | Motor de deltas |
| `app/src/main/kotlin/com/financas/app/domain/usecase/TrendCalculator.kt` | create | Motor de séries e agregados |
| `app/src/main/kotlin/com/financas/app/ui/components/InteractiveChartsCanvas.kt` | edit | `InteractiveLineChart` + `InteractiveYearComparisonChart` |
| `app/src/main/kotlin/com/financas/app/ui/screens/analytics/AnalyticsViewModel.kt` | edit | Lens COMPARAR, baselines, séries, multi-ano |
| `app/src/main/kotlin/com/financas/app/ui/screens/analytics/AnalyticsScreen.kt` | edit | `CompararView` + cartões novos na Evolução |
| `app/src/test/kotlin/com/financas/app/domain/usecase/ComparisonCalculatorTest.kt` | create | Testes Tier 3 |
| `app/src/test/kotlin/com/financas/app/domain/usecase/TrendCalculatorTest.kt` | create | Testes Tier 3 |
| `app/src/test/kotlin/com/financas/app/ui/screens/analytics/AnalyticsViewModelTest.kt` | edit | Testes das novas vistas |
| `GOAL_DECOMPOSITION.md` | edit | Registo da fase 5 (após validação) |
| `REQUISITOS_FUNCIONAIS.md` | edit | Documentação das novas funcionalidades (após validação) |
