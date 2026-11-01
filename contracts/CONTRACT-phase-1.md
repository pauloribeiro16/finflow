# CONTRACT — Phase 1: Fundação & Motor Financeiro (4 Dimensões vs. Rendimento)

**Date:** 2026-10-25
**Planner:** Antigravity
**Status:** APPROVED (Ready for Executor)
**Phase:** 1 of 4
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

---

## Scope

Configurar o projeto Android single-module em `/Users/<utilizador>/Desktop/Projetos/AppAndroid` e implementar toda a camada de dados (`data/`) e motor de cálculo financeiro (`domain/`) com testes unitários exaustivos:
1. Configuração Gradle (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`) com Kotlin 2.0, Compose Material 3, Room e JUnit 4.
2. Entidades e DAOs Room: `CategoryEntity` (hierarquia 2 níveis via `parentId`) e `TransactionEntity` (4 dimensões: `amountCents`, `yearMonth`, `categoryId`, `contextTag`, `merchant`, `paymentMethod`, `isFixed`).
3. Seed inicial PT-PT (`DefaultSeed.kt`): 8 Grupos Pais de Despesa + Subcategorias, Categorias de Receita (*Salário Base*, *Subsídios*, *Extras*) e os 6 Contextos `Onde` (*Trabalho, Casa, Saída, Férias, Online, Comércio*).
4. Motor Analítico (`MonthlyAnalyticsCalculator.kt`): cálculo exato de Recebido Total, Gasto Total, Fixo vs. Variável, Poupança Líquida, Taxa de Poupança `%`, Peso `% no Rendimento` e `% nos Gastos` por Categoria Pai, Subcategoria, Contexto (`Onde`), Estabelecimento e Método (`Como`), além do desvio face à Média Mensal histórica.
5. Motor de Auto-Preenchimento (`SmartPrefillEngine.kt`) e Gerador Idempotente de Mês Novo (`RecurringMonthGenerator.kt`).

---

## Output Criteria (what was produced)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `MonthlyAnalyticsCalculator` calcula corretamente Recebido, Gasto, Poupança, `% do Rendimento` e desvio face à Média Mensal (incluindo proteção contra divisão por zero quando `incomeCents == 0`) | MUST | — |
| 2 | `MonthlyAnalyticsCalculator` agrega gastos corretamente nas 4 dimensões: Hierarquia 2 níveis (`Categoria Pai > Subcategoria`), Contexto (`Onde`), Estabelecimento (`merchant`) e Método + Rigidez (`Como`) | MUST | — |
| 3 | `SmartPrefillEngine` devolve automaticamente a última `categoryId`, `contextTag`, `paymentMethod` e `isFixed` associada a um `merchant` (ex: `"Café Central"` ou `"Continente"`) | MUST | — |
| 4 | `RecurringMonthGenerator` copia o Salário Base e as Despesas Fixas do mês anterior para um novo `yearMonth` de forma idempotente (nunca duplica se chamado 2 vezes) | MUST | — |
| 5 | `DefaultSeed` popula todas as Categorias Pai, Subcategorias e Contextos PT-PT sem órfãos (`parentId` válido) | SHOULD | — |
| 6 | Compilação de debug (`assembleDebug`) sem avisos críticos de depreciação | NICE | — |

---

## Outcome Criteria (system state after)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | Todos os testes unitários em `com.financas.app.domain.*` passam em 3 execuções consecutivas | MUST | — |
| 2 | Nenhum ficheiro em `data/` ou `domain/` utiliza `Float` ou `Double` para armazenar valores em Euros (apenas `Long` em cêntimos) | MUST | — |
| 3 | `./gradlew :app:assembleDebug` gera o APK inicial com sucesso | SHOULD | — |

---

## Validation Commands

### Criterion 1 & 2 — Matemática Financeira e Agregação nas 4 Dimensões
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.MonthlyAnalyticsCalculatorTest"`
- **Expected:** `BUILD SUCCESSFUL` com testes para: (a) mês com salário `150000L` (1.500 €) e gastos fixos/variáveis; (b) mês com receita `0L` sem `NaN`/`Infinity`; (c) rollup de Subcategorias para Categoria Pai; (d) cálculo da média mensal histórica.
- **Pass condition:** Exit code 0 e todos os testes verdes.

### Criterion 3 — Motor de Auto-Preenchimento (`SmartPrefillEngine`)
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.SmartPrefillEngineTest"`
- **Expected:** `BUILD SUCCESSFUL` validando que após registar `"Café Central"` com `food_lunch`, `TRABALHO`, `MBWAY`, uma consulta por `"café central"` (case-insensitive) preenche esses exatos 4 campos.
- **Pass condition:** Exit code 0.

### Criterion 4 — Gerador Idempotente de Salário Base e Fixos (`RecurringMonthGenerator`)
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.RecurringMonthGeneratorTest"`
- **Expected:** `BUILD SUCCESSFUL` validando que chamar a geração para `202611` a partir de `202610` copia apenas movimentos `isFixed == true` e uma segunda chamada não cria duplicados.
- **Pass condition:** Exit code 0.

### Outcome Criterion 2 — Verificação de Cêntimos em `Long`
- **Tier:** 3 (Behavioral / Static Gate)
- **Command:** `! grep -rnE "val .*Amount.*: *(Double|Float)" app/src/main/kotlin/`
- **Expected:** Nenhuma ocorrência encontrada (exit 0).
- **Pass condition:** Exit code 0.

---

## Files to Change

| File | Action | Why |
|------|--------|-----|
| `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts` | create | Configuração Gradle Kotlin 2.0 + Compose + Room |
| `app/src/main/kotlin/com/financas/app/data/entity/CategoryEntity.kt` | create | Hierarquia 2 níveis (`id`, `parentId`, `name`, `icon`, `colorHex`, `type`) |
| `app/src/main/kotlin/com/financas/app/data/entity/TransactionEntity.kt` | create | Movimento com as 4 dimensões ortogonais em cêntimos (`Long`) |
| `app/src/main/kotlin/com/financas/app/data/db/AppDatabase.kt` | create | Room Database + `CategoryDao` + `TransactionDao` |
| `app/src/main/kotlin/com/financas/app/data/seed/DefaultSeed.kt` | create | Seed das categorias PT-PT (2 níveis) e contextos `Onde` |
| `app/src/main/kotlin/com/financas/app/domain/usecase/MonthlyAnalyticsCalculator.kt` | create | Cálculos de `% do recebido`, Pareto e média mensal |
| `app/src/main/kotlin/com/financas/app/domain/usecase/SmartPrefillEngine.kt` | create | Memória associativa por estabelecimento/contexto |
| `app/src/main/kotlin/com/financas/app/domain/usecase/RecurringMonthGenerator.kt` | create | Geração idempotente de salário base e despesas fixas no novo mês |
| `app/src/test/kotlin/com/financas/app/domain/*Test.kt` | create | Testes unitários comportamentais (Tier 3) |
