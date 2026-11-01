# CONTRACT — Phase 11: Deteção de Subscrições, Custos Recorrentes & Alertas de Subida de Preço

**Date:** 2026-10-27
**Planner:** Antigravity (Planner Role)
**Status:** DRAFT (Awaiting User Review)
**Phase:** 11 of 16
**Depends On:** Phase 10 + Commit `1122676` (Captura Local & Review Inbox)
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## 1. Scope & Objective

Implementar o **Motor de Deteção de Subscrições & Custos Recorrentes** (Subscription Tracker) e **Alertas de Aumento de Preço** (Price Hike Detection):
1. Identificar automaticamente despesas que se repetem com periodicidade mensal ou anual (ex.: Netflix, Spotify, Ginásio, Vodafone, Eletricidade, Renda, Seguros).
2. Calcular o custo total mensal e anualizado desse compromisso financeiro.
3. Detetar e alertar subidas de preço face aos meses anteriores (ex.: Vodafone subiu de 34,90 € para 36,90 €: `+2,00 €`).
4. Apresentar os dados de forma clara no separador de Análise/Conta com badges de alerta de aumento.

---

## 2. Technical Blueprint

### A. Domain Layer (Pure Kotlin, 100% JVM Testable)
- Criar `com.financas.app.domain.model.DetectedSubscription`:
  - `merchant: String`
  - `categoryId: String`
  - `contextTag: String`
  - `monthlyAmountCents: Long`
  - `annualizedCostCents: Long`
  - `occurrenceCount: Int`
  - `lastYearMonth: Int`
  - `previousAmountCents: Long?`
  - `priceDifferenceCents: Long?` (positivo = aumento de preço)
  - `isPriceHike: Boolean`
- Criar `com.financas.app.domain.usecase.SubscriptionDetector`:
  - `detect(transactions: List<TransactionEntity>, currentYearMonth: Int): SubscriptionReport`
  - Agrupa por comerciante / categoria / regularidade.
  - Compara o valor cobrado no mês atual com o mês imediatamente anterior para sinalizar subida de mensalidade.

### B. UI Layer (Material 3 Dark)
- Adicionar cartão ou vista dedicada "Subscrições & Recorrências" na `AnalyticsScreen`:
  - Métrica de topo: `Total Mensal em Subscrições` e `Impacto Anual`.
  - Alerta de Destaque: `⚠️ 1 Subscrição com Aumento de Preço` (ex.: Vodafone +2,00 €).
  - Lista de cartões limpos com comerciante, valor mensal, categoria e badge de periodicidade.

### C. Testing Suite
- `SubscriptionDetectorTest`:
  - Deteção de mensalidades com montante fixo regular.
  - Deteção de aumento de preço (ex.: 34,90 € → 36,90 €).
  - Deteção de descida ou estabilidade de preço.
  - Cálculo exato do custo anualizado em cêntimos (`Long`).
  - Filtragem de compras avulsas não recorrentes.

---

## 3. Validation Tiers

- **Tier 1 (Compiler):** `./gradlew :app:compileDebugKotlin` sem erros.
- **Tier 2 (Static Analysis):** Nenhum uso de `Double`/`Float` para montantes; código em `domain/` sem dependências de framework Android.
- **Tier 3 (Behavioral):** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.usecase.SubscriptionDetectorTest"` verde.
- **Tier 4 (Regression):** A suite completa de 354 testes existentes continua a passar (0 falhas).

---

## 4. Exit Criteria

- [ ] Todas as classes de domínio implementadas em cêntimos (`Long`).
- [ ] Deteção de aumentos de preço a funcionar e testada.
- [ ] UI integrada de forma fluida no tema escuro existente.
- [ ] 0 falhas na bateria de testes unitários.
- [ ] Validador independente aprova.
