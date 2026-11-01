# CONTRACT — Phase 10: Bateria Exaustiva de Testes das Fases 7–9

**Date:** 2026-10-27
**Planner:** ZCode (Planner Role)
**Status:** APPROVED (Ready for Executor)
**Phase:** 10 of 10
**Depends On:** Phase 9 (commit da fase 9)
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

## Scope

Matriz de cenários de integridade e funcionais para as funcionalidades das fases 7, 8 e 9, no mesmo estilo da fase 6. **Só testes** — não alterar código de produção exceto se um teste expuser um bug real (nesse caso: reportar ao Planner antes de corrigir).

### A. Integridade (novas classes)

**A1. `data/Phase8And9IntegrityTest.kt`**
1. `BudgetEntity`/`GoalEntity` — campos de montante são `Long` (reflexão); `limitCents > 0` e `targetCents > 0` nos dados de teste; ids únicos.
2. **Backup JSON v2** — a exportação inclui `version: 2` + arrays `budgets`/`goals` com todos os campos; a importação de um JSON **v1** (sem as novas chaves) funciona e devolve listas vazias; JSON com chaves novas malformadas (ex.: `limitCents` em vez de número) não rebenta a app (ou lança exceção tipada e é apanhada — documentar qual dos dois e testar esse comportamento explicitamente).
3. **Mapeamento de destinos por mês** — um movimento com `yearMonth` de um mês e `epochDay` de outro é sinalizado como inconsistente (o teste de integridade do asset já cobre `yearMonth == derivado de epochDay`; replicar a regra para dados criados em teste).
4. **Idempotência de metas** — `GoalEntity.deterministicId(name, startYearMonth)` é estável e distingue nomes/mês diferentes.

**A2. `data/repository/BackupRoundTripFidelityTest.kt`**
1. Round-trip completo v2: categorias + transações + orçamentos + metas → importar → comparar campo a campo com o original.
2. CSV continua só de transações (sem orçamentos/metas) e com o header exato.
3. Escape de strings especiais (`;`, `"`, `\n`, `€`, acentos) em merchant e nome de meta, ida e volta.

### B. Funcionais — fase 7 (fundamentos)

**B1. `EditFlowFidelityTest`** (novo, ou continuar `AddTransactionViewModelTest`): editar preserva TODOS os campos não editados; alterar só o valor mantém categoria/contexto/método/data; editar e gravar não altera a contagem do DAO; o valor 0 bloqueia a gravação em modo edição; `startNewEntry` limpa o id de edição.
**B2. `DateSelectionTest`** (novo): a data escolhida define `yearMonth` e `epochDay`; mudar a data para o mês anterior/anterior ano produz o `yearMonth` correto; data limite (dia 1 e último dia do mês).
**B3. `ContextTagsPersistenceTest`** (novo): o store grava e lê; a app inicia com as tags persistidas; adicionar uma tag persiste e a app recarrega-a; tags normalizadas (maiúsculas + underscore); a folha de registo mostra as tags personalizadas e permite selecioná-las.
**B4. `CollectorLifecycleTest`** (novo, via fakes que contam subscrições): após N mudanças de mês no `AnalyticsViewModel`, o número de subscrições ativas não cresce (usa um fake DAO que conta `collect` calls e permite cancelar); idem no `HomeViewModel`.

### C. Funcionais — fase 8 (orçamentos/metas/projeção)

**C1. `BudgetProgressCalculatorTest`** (expandir): limites de estado (79,9 / 80 / 100 / 100,1); roll-up pai↔sub; agregado sem contagem dupla (inclui o caso id=0 por identidade de campos); categoria desconhecida; ordenação; limite 0.
**C2. `GoalProgressCalculatorTest`** (expandir): meses negativos; projeção nula; `onTrack` com/sem prazo; meses restantes no fim do intervalo; objetivo já atingido.
**C3. `MonthProjectionCalculatorTest`** (expandir): dia 1, dia intermédio, mês passado completo, mês futuro, mês vazio, rendimento zero.
**C4. `AccountBudgetsGoalsFlowTest`** (novo ou expandir `AccountViewModelTest`): criar orçamento (id determinístico, limites), editar, eliminar; um orçamento por categoria (a segunda criação substitui); criar meta com/sem prazo; arquivar; eliminar; mensagens exatas.

### D. Funcionais — fase 9 (calendário/relatório/filtro)

**D1. `CalendarAggregatorTest`** (expandir): todos os dias presentes; zeros; ordenação; `isToday` injetado; despesas vs receitas.
**D2. `MonthlyReportGeneratorTest`** (expandir): truncamento top-N (3/3/5) com conjunto maior; blocos de orçamentos/metas presentes e ausentes; determinismo com baralhamento; valores negativos.
**D3. `AnalyticsCalendarStateTest`** (expandir `AnalyticsViewModelTest`): dias do mês selecionado; seleção de dia; limpeza ao mudar de mês; relatório construído no momento do clique reflete o mês atual (não o anterior).
**D4. `TransactionsAmountFilterTest`** (expandir): as 4 faixas e respetivos limites exatos; toggle; combinação com período/fluxo/contexto/método/pesquisa; receitas e despesas no mesmo filtro.

### E. Global

Suite completa a 100% (0 falhas, 0 ignorados), **≥ 230 testes**, `assembleDebug` OK, `build.gradle.kts` intacto, `grep -rn fallbackToDestructive app/src/main` vazio.

## Output Criteria

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | A1–A2: integridade de entidades/backup v2, compatibilidade v1, round-trip com orçamentos e metas, escapes | MUST | — |
| 2 | B1–B4: edição, data, persistência de contextos, ciclo de vida dos coletores | MUST | — |
| 3 | C1–C4: orçamentos, metas, projeção e gestão na Conta | MUST | — |
| 4 | D1–D4: calendário, relatório, filtro por valor | MUST | — |
| 5 | Suite ≥ 230 testes, 0 falhas, 0 ignorados, com `--rerun-tasks` | MUST | — |
| 6 | `assembleDebug` OK; `build.gradle.kts` intacto; sem `fallbackToDestructive`; código de produção inalterado sem aprovação | MUST | — |
| 7 | `GOAL_DECOMPOSITION.md` e `REQUISITOS_FUNCIONAIS.md` com as fases 7–10 e a tabela de testes final | SHOULD | — |

## Validation Commands

### Criteria 1–4 — matriz por área (Tier 3)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.data.*" --tests "com.financas.app.domain.*" --tests "com.financas.app.ui.*"`
- **Expected:** todos os cenários A1–A2, B1–B4, C1–C4, D1–D4 verdes, com asserções de valores exatos. **Pass condition:** Exit code 0.

### Criteria 5–6 — suite, build e restrições (Tier 4)
- **Command:** `./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug && git diff --exit-code app/build.gradle.kts && grep -rn "fallbackToDestructive" app/src/main || true`
- **Expected:** ≥ 230 testes, 0 falhas; APK compila; sem dependências novas nem migração destrutiva. **Pass condition:** Exit code 0.

## Files to Change

| File | Action | Why |
|---|---|---|
| `app/src/test/kotlin/com/financas/app/data/Phase8And9IntegrityTest.kt` | create | A1 |
| `app/src/test/kotlin/com/financas/app/data/repository/BackupRoundTripFidelityTest.kt` | create | A2 |
| `app/src/test/kotlin/com/financas/app/ui/screens/addsheet/EditFlowFidelityTest.kt` | create | B1 |
| `app/src/test/kotlin/com/financas/app/ui/screens/addsheet/DateSelectionTest.kt` | create | B2 |
| `app/src/test/kotlin/com/financas/app/data/prefs/ContextTagsPersistenceTest.kt` | create | B3 |
| `app/src/test/kotlin/com/financas/app/ui/screens/analytics/CollectorLifecycleTest.kt` | create | B4 |
| `app/src/test/kotlin/com/financas/app/domain/usecase/BudgetProgressCalculatorTest.kt` | edit | C1 |
| `app/src/test/kotlin/com/financas/app/domain/usecase/GoalProgressCalculatorTest.kt` | edit | C2 |
| `app/src/test/kotlin/com/financas/app/domain/usecase/MonthProjectionCalculatorTest.kt` | edit | C3 |
| `app/src/test/kotlin/com/financas/app/ui/screens/account/AccountBudgetsGoalsFlowTest.kt` | create | C4 |
| `app/src/test/kotlin/com/financas/app/domain/usecase/CalendarAggregatorTest.kt` | edit | D1 |
| `app/src/test/kotlin/com/financas/app/domain/usecase/MonthlyReportGeneratorTest.kt` | edit | D2 |
| `app/src/test/kotlin/com/financas/app/ui/screens/analytics/AnalyticsViewModelTest.kt` | edit | D3 |
| `app/src/test/kotlin/com/financas/app/ui/screens/transactions/TransactionsViewModelTest.kt` | edit | D4 |
