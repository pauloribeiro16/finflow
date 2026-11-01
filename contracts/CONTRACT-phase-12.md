# CONTRACT — Phase 12: Reembolsos MB WAY & Divisão de Contas (Expense Split & Net Calculations)

**Date:** 2026-10-27
**Planner:** Antigravity (Planner Role)
**Status:** DRAFT (Awaiting User Review)
**Phase:** 12 of 16
**Depends On:** Phase 11 + Commit `5724f2b`
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## 1. Scope & Objective

Permitir associar dinheiro recebido (ex.: via MB WAY de amigos por um jantar, compras de casa partilhadas ou prendas conjuntas) como **Reembolso direto de uma despesa**:
1. Evitar a distorção da Taxa de Poupança (dinheiro de amigos não é salário nem rendimento do trabalho).
2. Abater o valor recebido à categoria correspondente (ex.: conta de restaurante de 60,00 € com reembolso de 40,00 € resulta num custo líquido real de 20,00 € em Restaurantes).
3. Apresentar visualmente na lista de movimentos o montante bruto, o reembolso associado e o custo líquido final.

---

## 2. Technical Blueprint

### A. Room SQLite Migration (v3 -> v4, Puramente Aditiva)
- Adicionar coluna opcional em `TransactionEntity`:
  - `linkedTransactionId: Long? = null` (aponta para o ID da despesa reembolsada)
- Adicionar migration `MIGRATION_3_4`:
  ```sql
  ALTER TABLE transactions ADD COLUMN linkedTransactionId INTEGER DEFAULT NULL;
  CREATE INDEX IF NOT EXISTS index_transactions_linkedTransactionId ON transactions (linkedTransactionId);
  ```

### B. Domain Layer (Pure Kotlin)
- Atualizar `MonthlyAnalyticsCalculator`:
  - Quando um movimento de receita tem `linkedTransactionId != null`, é tratado como **reembolso**:
    - Abate diretamente ao total gasto na categoria da despesa de origem.
    - Não é somado ao `totalIncomeCents` (evitando inflacionar artificialmente o rendimento base e a taxa de poupança).
    - Custo líquido calculado: `max(0L, originalExpenseCents - totalRefundsCents)`.

### C. UI Layer (Material 3 Dark)
- Na folha de registo `AddTransactionBottomSheet`:
  - Seletor / Switch "É reembolso de uma despesa? (ex.: MB WAY de amigos)".
  - Lista rápida para escolher o movimento a reembolsar.
- No ecrã `TransactionsScreen`:
  - Movimentos com reembolso exibem badge: `Reembolsado: X € · Líquido: Y €`.

### D. Testing Suite
- `NetExpenseCalculatorTest` / `MonthlyAnalyticsCalculatorTest`:
  - Reembolso parcial (60 € gasto, 40 € recebido -> 20 € custo líquido).
  - Reembolso total (25 € gasto, 25 € recebido -> 0 € custo líquido).
  - Vários reembolsos ao mesmo movimento (ex.: 2 amigos pagam 15 € cada para conta de 45 €).
  - Verificação de não inflação do rendimento total nem da poupança.

---

## 3. Validation Tiers

- **Tier 1 (Compiler):** `./gradlew :app:compileDebugKotlin` sem erros.
- **Tier 2 (Static Analysis):** Todos os montantes em `Long` (cêntimos); Room migration 3->4 não destrutiva.
- **Tier 3 (Behavioral):** Testes unitários novos de cálculo de reembolsos verdes.
- **Tier 4 (Regression):** Todos os 365 testes existentes continuam verdes.
