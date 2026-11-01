# CONTRACT — Phase 8: Orçamentos, Metas & Previsão (BD v1→v2)

**Date:** 2026-10-26
**Planner:** ZCode (Planner Role)
**Status:** APPROVED (Ready for Executor)
**Phase:** 8 of 10
**Depends On:** Phase 7 + commit `7325a6b`
**Parent Goal:** `GOAL_DECOMPOSITION.md`
**User approval:** alteração de esquema (ADD TABLE) e orçamentos em € fixos aprovadas pelo utilizador no plano de 2026-10-26.

---

## Trials

`trials: 3` (default)

## Scope

1. **Esquema (v1→v2, apenas aditivo):**
   - `BudgetEntity` — `@Entity(tableName = "budgets", indices = [Index("categoryId")])`: `id: String` PK (formato `budget_{categoryId}`), `categoryId: String`, `limitCents: Long`, `isActive: Boolean = true`.
   - `GoalEntity` — `@Entity(tableName = "goals")`: `id: String` PK, `name: String`, `targetCents: Long`, `startYearMonth: Int`, `deadlineYearMonth: Int?`, `archived: Boolean = false`.
   - `AppDatabase` version 2, entities + DAOs registados, `MIGRATION_1_2` com `CREATE TABLE IF NOT EXISTS` em SQL idêntico ao esquema Room (colunas e afins), registado em `.addMigrations(...)`; **proibido** `fallbackToDestructiveMigration()`.
   - `BudgetDao`: `getAll(): Flow<List<BudgetEntity>>`, `getAllActive(): Flow<...>`, `getAllSync()`, `getById(id)`, `insert(budget)` (REPLACE), `deleteById(id)`, `count()`. `GoalDao`: análogo (getAll incluindo arquivadas, `getActive()`, insert, update? apenas REPLACE, deleteById, count).
   - `AppContainer`: `budgetDao`, `goalDao` (+ fakes nos testes).
2. **Motores puros (`domain/usecase/`, 100% testáveis):**
   - `BudgetProgressCalculator` — entrada: lista de orçamentos ativos, transações do mês, mapa de categorias; saída por orçamento: `spentCents` (despesas da categoria OU da sua subcategoria/pai roll-up: um orçamento numa categoria pai soma as subs), `limitCents`, `remainingCents` (limit − spent), `percentUsed` (guard limit ≤ 0 → 0.0), `status`: OK (<80%), WARNING (≥80% e ≤100%), EXCEEDED (>100%); ordenado por percentUsed desc; total agregado.
   - `GoalProgressCalculator` — entrada: meta + transações de `startYearMonth..até mês selecionado`; saída: `accumulatedSavingsCents` (soma de netSavings mensais desde o início, incluindo meses negativos), `percentComplete` (guard target ≤ 0 → 0.0), `monthsRemaining`, `averageMonthlySavingsCents` (média só de meses dentro do intervalo, mesmo os negativos), `projectedMonthsToComplete` (restante ÷ média, null se média ≤ 0), `onTrack: Boolean?` (null sem deadline; senão compara projeção com deadline).
   - `MonthProjectionCalculator` — entrada: transações do mês + `yearMonth` + data de hoje (injetável para testes); saída: `daysElapsed`, `daysInMonth`, `dailyAverageExpenseCents` (spent ÷ daysElapsed; guard 0 dias), `projectedMonthEndExpenseCents` (média × dias do mês), `projectedRemainingCents` (rendimento já recebido − projeção), `projectedSavingsRatePercent`. Mês passado/futuro completo → projeção = gasto real.
3. **UI:**
   - **Conta** — nova secção "Orçamentos & Metas" (acima de "Cópias de Segurança"): lista de orçamentos ativos (categoria, limite PT-PT, apagar/editar via diálogo com seletor de categoria — apenas categorias sem orçamento ativo para criar) e criação de orçamento com campo de € ; secção de metas: lista (nome, alvo, prazo, % completo) + diálogo criar (nome, alvo €, prazo opcional) e arquivar/eliminar. Mensagens de feedback no banner existente.
   - **Início** — cartão "Orçamentos do Mês" (top 5 por % usado: nome, barra com cor por estado — emerald/amber/coral, "gasto de limite"), cartão "A Este Ritmo" (média diária, projeção de fecho do mês, taxa projetada; só se o mês selecionado for o mês atual ou tiver dados), cartão "Metas" (nome + barra % + projeção). Todos escondidos quando não há dados/orçamentos.
4. **Testes** (adições às classes existentes + novas): `BudgetProgressCalculatorTest` (novo), `GoalProgressCalculatorTest` (novo), `MonthProjectionCalculatorTest` (novo), `HomeViewModelTest` (cartões no estado), `AccountViewModelTest` (CRUD de orçamentos/metas com fakes, mensagens exatas).

## Output Criteria

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `AppDatabase` versão 2 com `MIGRATION_1_2` SQL correto (tabelas `budgets` e `goals` com colunas exatas das entities), `.addMigrations` registado, sem fallback destrutivo; `git grep fallbackToDestructive` vazio | MUST | — |
| 2 | Budgets: spent com roll-up pai↔subs exato em cêntimos; percent/status nos limites (79,9→OK, 80→WARNING, 100→WARNING, 100,1→EXCEEDED); ordenação desc; totals | MUST | — |
| 3 | Goals: acumulado inclui meses negativos; percent com guard; projeção null com média ≤ 0; onTrack com e sem deadline | MUST | — |
| 4 | Projeção: média diária × dias do mês exata; guards mês vazio/dia 1; mês completo → gasto real | MUST | — |
| 5 | UI Conta: criar/editar/apagar orçamento (id determinístico `budget_{categoryId}`, um ativo por categoria); criar/arquivar/eliminar meta; mensagens exatas no banner | MUST | — |
| 6 | UI Início: 3 cartões novos com dados exatos do estado, escondidos quando vazios; nada rebenta sem orçamentos/metas | MUST | — |
| 7 | Suite completa verde (141 + novos), `assembleDebug` OK, `build.gradle.kts` intacto | MUST | — |
| 8 | Docs atualizadas | SHOULD | — |

## Validation Commands

### Criteria 2–6 — comportamento (Tier 3)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.usecase.BudgetProgressCalculatorTest" --tests "com.financas.app.domain.usecase.GoalProgressCalculatorTest" --tests "com.financas.app.domain.usecase.MonthProjectionCalculatorTest" --tests "com.financas.app.ui.screens.home.HomeViewModelTest" --tests "com.financas.app.ui.screens.account.AccountViewModelTest"`
- **Expected:** valores exatos em cêntimos e PT-PT; limites de estado exatos; guards sem exceções. **Pass condition:** Exit code 0.

### Criterion 1 & 7 — migração, suite, build (Tier 4)
- **Command:** `./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug && git diff --exit-code app/build.gradle.kts && grep -rn "fallbackToDestructive" app/src/main || true`
- **Expected:** suite 100%; APK compila; nenhuma dependência nova; nenhuma ocorrência de fallback destrutivo. **Pass condition:** Exit code 0 no diff e suite; grep sem resultados.

## Files to Change

| File | Action | Why |
|---|---|---|
| `data/entity/BudgetEntity.kt` | create | Orçamentos |
| `data/entity/GoalEntity.kt` | create | Metas |
| `data/db/BudgetDao.kt` | create | CRUD orçamentos |
| `data/db/GoalDao.kt` | create | CRUD metas |
| `data/db/AppDatabase.kt` | edit | v2 + MIGRATION_1_2 + DAOs |
| `di/AppContainer.kt` | edit | budgetDao/goalDao |
| `domain/usecase/BudgetProgressCalculator.kt` | create | Progresso de orçamentos |
| `domain/usecase/GoalProgressCalculator.kt` | create | Progresso de metas |
| `domain/usecase/MonthProjectionCalculator.kt` | create | Projeção mensal |
| `ui/screens/account/AccountScreen.kt` + `AccountViewModel.kt` | edit | Gestão de orçamentos/metas |
| `ui/screens/home/HomeScreen.kt` + `HomeViewModel.kt` | edit | Cartões novos |
| `ui/fakes/FakeDaos.kt` | edit | FakeBudgetDao/FakeGoalDao |
| Testes novos + `HomeViewModelTest`/`AccountViewModelTest` | create/edit | Critérios 2–6 |
