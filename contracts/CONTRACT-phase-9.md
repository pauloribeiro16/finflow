# CONTRACT — Phase 9: Calendário, Relatório Mensal e Pesquisa por Valor

**Date:** 2026-10-27
**Planner:** ZCode (Planner Role)
**Status:** APPROVED (Ready for Executor)
**Phase:** 9 of 10
**Depends On:** Phase 8 + commit da fase 8
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

## Scope

Sem alterar a base de dados, sem dependências novas.

1. **Vista "Calendário" (6.ª lens):**
   - `AnalyticsLens` ganha `CALENDARIO("Calendário")`; a barra de vistas (`LensSelectorTabs` em `AnalyticsScreen.kt`) passa a `horizontalScroll` para acomodar 6 separadores.
   - `CalendarDayData` + `MonthlyCalendarGrid` (novo, em `ui/components/InteractiveChartsCanvas.kt`): grelha do mês com 7 colunas (Seg–Dom), cabeçalhos de dia da semana, células com o número do dia, pintadas por intensidade de gasto (alpha do `AccentCoral` proporcional ao gasto do dia face ao máximo do mês), marcação do dia atual (borda `AccentBlue`), células vazias fora do mês, e toque numa célula → `onSelectDay(epochDay)`.
   - `AnalyticsViewModel`: `calendarDays: List<CalendarDayItem>` (epochDay, dayOfMonth, totalExpenseCents, formattedTotal, transactionCount) para o mês selecionado + `selectedCalendarDay` com os movimentos desse dia; `selectCalendarDay(epochDay)`.
   - UI: vista com a grelha + cartão "Movimentos de {dia}" (lista dos movimentos do dia selecionado, com os mesmos elementos visuais das linhas existentes) e estado vazio.
2. **Relatório mensal partilhável:**
   - `MonthlyReportGenerator` (classe pura em `domain/usecase/`): entrada = yearMonth, transações do mês, categorias, orçamentos (opcional), metas (opcional); saída = `String` em texto PT-PT multilinha com: título do mês, total recebido, total gasto, poupança líquida e taxa, % do gasto sobre o rendimento, top 3 categorias, top 3 contextos, top 5 estabelecimentos, e (se existirem) estado dos orçamentos e das metas.
   - `shareText` (hoje `private` em `AccountScreen.kt`) passa a utilitário partilhado em `ui/util/ShareUtils.kt` (mesmo comportamento: `ACTION_SEND` + fallback para clipboard com toast).
   - Botão "Partilhar relatório" na barra do Análise (ícone de partilha) que gera o relatório do mês selecionado e abre a folha de partilha (`text/plain`).
3. **Pesquisa por valor nos Movimentos:**
   - `TransactionsViewModel`: `AmountFilter` enum (`ANY("Todos")`, `UP_TO_10("≤ 10 €")`, `BETWEEN_10_50("10–50 €")`, `OVER_50("≥ 50 €")`) + `amountFilter` no UiState + setter com toggle; aplicado no pipeline de `applyFilters()` (apenas a movimentos; receitas com valor ≤ 10 € também entram no filtro "≤ 10 €" — documentar).
   - UI: fila de 4 cápsulas de valor em `TransactionsScreen.kt`, no mesmo estilo das cápsulas existentes.
4. **Testes:** `MonthlyReportGeneratorTest` (novo), `CalendarAggregatorTest` (novo, classe pura `CalendarAggregator` em `domain/usecase/` que produz os `CalendarDayData` a partir das transações do mês — a UI consome-a), `AnalyticsViewModelTest` (novos casos: dias do calendário, seleção de dia), `TransactionsViewModelTest` (novos casos: filtro por valor em cada faixa + toggle).

## Output Criteria

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `CalendarAggregator` agrupa por dia do mês com totais e contagem exatos, ordena por epochDay, dias sem gastos entram com 0 | MUST | — |
| 2 | `MonthlyReportGenerator` produz texto com todos os blocos obrigatórios e valores exatos em PT-PT (ex.: "Total recebido", "Taxa de poupança", top categorias); sem orçamentos/metas o relatório omite esses blocos | MUST | — |
| 3 | `AnalyticsViewModel` expõe `calendarDays` (dias do mês selecionado) e a seleção de dia devolve os movimentos desse dia | MUST | — |
| 4 | Filtro por valor: cada faixa devolve exatamente os movimentos esperados; toggle desativa; interage com os restantes filtros | MUST | — |
| 5 | UI: grelha do calendário desenhada (Canvas/Compose), relatório partilhável com `shareText` reutilizado, cápsulas de valor nos Movimentos | MUST | — |
| 6 | Suite completa verde (171 + novos), `assembleDebug` OK, `build.gradle.kts` intacto | MUST | — |
| 7 | Docs atualizadas | SHOULD | — |

## Validation Commands

### Criteria 1–4 — comportamento (Tier 3)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.usecase.CalendarAggregatorTest" --tests "com.financas.app.domain.usecase.MonthlyReportGeneratorTest" --tests "com.financas.app.ui.screens.analytics.AnalyticsViewModelTest" --tests "com.financas.app.ui.screens.transactions.TransactionsViewModelTest"`
- **Expected:** valores exatos em cêntimos e strings PT-PT; sem NaN/Infinity; guards de listas vazias. **Pass condition:** Exit code 0.

### Criterion 6 — suite e build (Tier 4)
- **Command:** `./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug && git diff --exit-code app/build.gradle.kts`
- **Expected:** suite 100% (≥ 185 testes); APK compila; sem dependências novas. **Pass condition:** Exit code 0.

## Files to Change

| File | Action | Why |
|---|---|---|
| `domain/usecase/CalendarAggregator.kt` | create | Agregação por dia |
| `domain/usecase/MonthlyReportGenerator.kt` | create | Relatório textual |
| `ui/components/InteractiveChartsCanvas.kt` | edit | `MonthlyCalendarGrid` |
| `ui/util/ShareUtils.kt` | create | `shareText` partilhado |
| `ui/screens/account/AccountScreen.kt` | edit | Reutilizar o utilitário partilhado |
| `ui/screens/analytics/AnalyticsViewModel.kt` | edit | Lens CALENDARIO, estado do calendário |
| `ui/screens/analytics/AnalyticsScreen.kt` | edit | Vista Calendário + botão partilhar |
| `ui/screens/transactions/TransactionsViewModel.kt` | edit | Filtro por valor |
| `ui/screens/transactions/TransactionsScreen.kt` | edit | Cápsulas de valor |
| Testes novos + `AnalyticsViewModelTest`/`TransactionsViewModelTest` | create/edit | Critérios 1–4 |
