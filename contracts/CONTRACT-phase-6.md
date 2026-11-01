# CONTRACT — Phase 6: Suite Exaustiva de Testes de Integridade e Funcionais

**Date:** 2026-10-26
**Planner:** ZCode (Planner Role)
**Status:** APPROVED (Ready for Executor)
**Phase:** 6 of 6
**Depends On:** Phase 5 + commit `9d502cf`
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

## Scope

Bateria exaustiva de testes **JVM (JUnit 4 + kotlinx-coroutines-test)**, sem alterar `app/build.gradle.kts`, sem dependências novas, sem alterar código de produção exceto se um teste expuser um bug real (nesse caso: parar e reportar ao Planner antes de corrigir).

### A. Integridade de dados (novas classes de teste)

**A1. `data/SeedIntegrityTest.kt`** — invariantes estruturais do seed predefinido (a app já não embute nem importa qualquer asset histórico: a base de dados começa vazia e o utilizador adiciona os movimentos; o teste de verificação do histórico foi removido em conjunto com o asset):
1. Hierarquia sem órfãos e 2 níveis: subcategoria tem pai existente; pai tem `parentId == null`; tipo da sub == tipo do pai; 47 categorias com ids únicos; 8 rendimentos (2 pais), 8 grupos-pai de despesa, 31 subcategorias.
2. Regressão dos flags Fixo: `isDefaultFixed` de `exp_health_pharmacy` e `exp_transport_maintenance` == true; ids únicos; sortOrder sequencial (0..46).
3. Regra de dinheiro: nenhum campo de valor em `Double`/`Float` nas entidades (`TransactionEntity`/`CategoryEntity` — verificação via reflexão dos campos de montante; `amountCents` é o único campo monetário e é `Long`).

**A2. `data/repository/BackupSerializerIntegrityTest.kt`**:
1. Round-trip: `exportToJson` → `importFromJson` → igual campo a campo (categorias + transações).
2. CSV: header exato (`CSV_HEADER`); escape de merchant com `;`, `"` e quebra de linha; `Sim`/`Não`; data ISO e mês `YYYY-MM`; valor PT-PT (`formatAmountPt`); categoria desconhecida → colunas pai/sub vazias.
3. `importFromJson` com JSON malformado lança exceção (documentar tipo).
4. Campos em falta no JSON → defaults seguros (versão default 1; transação sem id aceitável; mapa `mapNotNull` descarta entradas sem `id` de categoria).
5. Restauro misto: base com metade dos movimentos já existentes → `transactionsImported`/`transactionsSkipped` exatos; idempotência total na 2.ª passagem (garantida mesmo sobre dados parciais).
6. `isDuplicateTransaction`: match por id; match por campos com ids diferentes; diferença num único campo → não duplicado.

### B. Funcionais — domínio (expandir classes existentes)

**B1. `MoneyCentsTest`** (2 → 6): `fromEuros` arredondamento (12,345 → 1235? usar casos exatos `Math.round`); formatCents negativo; formatCents grande (1.234.567,89 €); formatPercent negativo e com 1 casa (59,2%).
**B2. `MonthlyAnalyticsCalculatorTest`** (6 → 10): classificação Base vs Extras (isFixed + ids `inc_fixed`/`inc_base_salary`/`inc_meal_allowance`); guards de rendimento zero (todas as % a 0.0); shares por contexto/estabelecimento/método; `calculateMerchantShares` exclui merchant em branco e faz trim; `calculateHistoricalAverage` com lista vazia e só zeros → 0; `calculateDeviationVsAverage` com média 0 → null.
**B3. `RecurringMonthGeneratorTest`** (3 → 6): clamping do dia (fixo do dia 31 → fev dia 28/29 conforme ano); idempotência (2.ª geração devolve lista vazia); chave ignora maiúsculas/espaços no merchant (`keyOf`); `defaultEpochDay` sobrepõe a data; copia apenas `isFixed == true`; preserva todos os restantes campos.
**B4. `SmartPrefillEngineTest`** (4 → 7): match case-insensitive; desempate por `epochDay` depois `id`; query com espaços; sem match → null; `getRecentMerchants` distinto/mais recente primeiro/limite 10/exclui vazios; variante suspend com dao null → null.
**B5. `CalculatorNumpadEvaluatorTest`** (10 → 14): "00" no inteiro/1 decimal/2 decimais; vírgula após "+" → "0,"; "C" a meio de expressão; backspace de 1 char → "0"; dígito substitui "0" isolado; "12," trailing → 1200 cêntimos; 3+ decimais truncadas; soma grande "999,99+999,99" = 199998.

### C. Funcionais — ViewModels (expandir existentes, padrão FakeDaos)

**C1. `AddTransactionViewModelTest`** (3 → 10): gasto por omissão (COMERCIO/MBWAY/EXPENSE/data hoje); receita com categoria de rendimento; valor "0" não grava (DAO inalterado); expressão "12,5+3" → 1550; merchant com espaços é cortado; sem categoria → fallback `exp_financial_other`; switch Fixo persistido no movimento; sugestão de estabelecimento com histórico faz prefill completo (categoria/contexto/método/fixo); chips recentes distintos/limite 10/mais recente primeiro; display formatado acompanha a digitação.
**C2. `HomeViewModelTest`** (3 → 9): navegação atravessa anos (202601 ← → 202512); `canGenerateRecurring` true/false nos 3 casos (alvo sem fixos + anterior com; alvo já com fixos; anterior sem fixos); "Gerar" copia só fixos com `yearMonth`/dia clampado do mês alvo; apagar remove do DAO; lista de recentes ≤ 5; mês vazio → totais zero e taxa 0.
**C3. `TransactionsViewModelTest`** (3 → 10): período (Mês Atual/Ano/Todos); fluxo (Fixos/Variáveis/Receitas); contexto toggle on/off; método toggle on/off; pesquisa por merchant (case-insensitive); pesquisa por categoria/contexto/método; combinação de filtros; cartão de soma (count/total/peso no recebido, 0 sem rendimento no período); ordenação mais recente primeiro; apagar.
**C4. `AccountViewModelTest`** (6 → 9): criar subcategoria (id sanitizado único, herda cor/tipo do pai, badge Fixo, mensagem); nome em branco → no-op; eliminar subcategoria + mensagem; novo contexto "Casa Nova" → `CASA_NOVA` + mensagem; toggle biometria persiste preferência; exportJson inclui versão e tudo; exportCsv tem header+linhas; restaurar texto válido → mensagem com contagens; restaurar texto inválido → mensagem de erro.
**C5. `AnalyticsViewModelTest`** (8 → 14): categoria desconhecida → fallback cinza com nome = id; comparison `MONTHLY_AVERAGE` com matemática exata; comparison `SAME_MONTH_LAST_YEAR`; savingsRateTrend pontos + média; yearTotals de 2 anos; estabilidade da seleção de chips ao mudar de mês; `setDisplayMode` para o mesmo modo é no-op; navegação de mês atravessa ano no ViewModel.

### D. Critério global

Suite completa a 100% (0 falhas, 0 ignorados), **≥ 115 testes** no total, e cada cenário desta matriz com ≥ 1 teste com asserções reais (validador verifica método a método).

## Output Criteria (what was produced)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | A1: todas as invariantes 1–3 com testes reais (hierarquia sem órfãos, regressão Fixo/ids/ordenação, reflexão Long) | MUST | — |
| 2 | A2: round-trip, CSV com escapes, JSON malformado, defaults, restauro misto e `isDuplicateTransaction` cobertos | MUST | — |
| 3 | B1–B5: todos os cenários listados com asserções de valores exatos (cêntimos, PT-PT) | MUST | — |
| 4 | C1–C5: todos os cenários listados via FakeDaos, assertando estado e mensagens | MUST | — |
| 5 | Suite completa ≥ 115 testes, 0 falhas, 0 ignorados (`./gradlew :app:testDebugUnitTest --rerun-tasks`) | MUST | — |
| 6 | `./gradlew :app:assembleDebug` compila; `git diff --exit-code app/build.gradle.kts app/src/main/kotlin/com/financas/app/data/entity/` limpo; nenhum ficheiro de produção alterado sem aprovação do Planner | MUST | — |
| 7 | `GOAL_DECOMPOSITION.md` e `REQUISITOS_FUNCIONAIS.md` atualizados (fase 6 + tabela de testes) | SHOULD | — |

## Validation Commands

### Criteria 1–4 — matriz por área
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.data.*" --tests "com.financas.app.domain.*" --tests "com.financas.app.ui.*"`
- **Expected:** todos os cenários A1–A2, B1–B5 e C1–C5 verdes; validador cruza a matriz com os métodos de teste existentes.
- **Pass condition:** Exit code 0.

### Criteria 5 & 6 — suite completa, build e restrições
- **Tier:** 4 (Integration / Build)
- **Command:** `./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug && git diff --exit-code app/build.gradle.kts app/src/main/kotlin/com/financas/app/data/entity/`
- **Expected:** ≥ 115 testes, 0 falhas; APK compila; build e entidades intactos.
- **Pass condition:** Exit code 0.

## Files to Change

| File | Action | Why |
|---|---|---|
| `app/src/test/kotlin/com/financas/app/data/SeedIntegrityTest.kt` | create | A1 |
| `app/src/test/kotlin/com/financas/app/data/repository/BackupSerializerIntegrityTest.kt` | create | A2 |
| `app/src/test/kotlin/com/financas/app/domain/MoneyCentsTest.kt` | edit | B1 |
| `app/src/test/kotlin/com/financas/app/domain/MonthlyAnalyticsCalculatorTest.kt` | edit | B2 |
| `app/src/test/kotlin/com/financas/app/domain/RecurringMonthGeneratorTest.kt` | edit | B3 |
| `app/src/test/kotlin/com/financas/app/domain/SmartPrefillEngineTest.kt` | edit | B4 |
| `app/src/test/kotlin/com/financas/app/ui/components/CalculatorNumpadEvaluatorTest.kt` | edit | B5 |
| `app/src/test/kotlin/com/financas/app/ui/screens/addsheet/AddTransactionViewModelTest.kt` | edit | C1 |
| `app/src/test/kotlin/com/financas/app/ui/screens/home/HomeViewModelTest.kt` | edit | C2 |
| `app/src/test/kotlin/com/financas/app/ui/screens/transactions/TransactionsViewModelTest.kt` | edit | C3 |
| `app/src/test/kotlin/com/financas/app/ui/screens/account/AccountViewModelTest.kt` | edit | C4 |
| `app/src/test/kotlin/com/financas/app/ui/screens/analytics/AnalyticsViewModelTest.kt` | edit | C5 |
| `app/src/test/kotlin/com/financas/app/domain/ComparisonCalculatorTest.kt` | edit | reforço (casos vazios) |
| `app/src/test/kotlin/com/financas/app/domain/TrendCalculatorTest.kt` | edit | reforço (casos vazios) |
| `app/src/test/kotlin/com/financas/app/data/repository/BackupSerializerTest.kt` | edit | reforço |
| `GOAL_DECOMPOSITION.md` / `REQUISITOS_FUNCIONAIS.md` | edit (após validação) | fase 6 + tabela de testes |
