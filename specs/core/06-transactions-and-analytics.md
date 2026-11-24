# Spec 06 — Transactions & Monthly Analytics

## Objectivo
Definir o modelo contabilístico em cêntimos (`Long`), as regras de cálculo mensal/anual e o motor de preenchimento inteligente e deduplicação de movimentos.

## Ficheiros Cobertos
- `app/src/main/kotlin/com/financas/app/domain/model/MoneyCents.kt`
- `app/src/main/kotlin/com/financas/app/domain/model/MonthlySummary.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/MonthlyAnalyticsCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/NetExpenseCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/ComparisonCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/TrendCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/CalendarAggregator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/MonthProjectionCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/SmartPrefillEngine.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/TransactionDeduplicator.kt`

## Requisitos Funcionais (EARS)
1. **Aritmética monetária exata:** THE SYSTEM SHALL representar todos os montantes em cêntimos (`Long`), nunca `Float`/`Double`.
2. **Modo privacidade seguro em inputs:** WHEN o modo privacidade está ativo, THE SYSTEM SHALL mascarar valores de leitura (`formatCents`/`formatPt`), enquanto campos de edição SHALL usar `MoneyCents.formatForInput()` sem máscara.
3. **Resumo mensal e proteção contra divisão por zero:** WHEN `MonthlyAnalyticsCalculator` calcula o resumo mensal, THE SYSTEM SHALL computar `totalIncomeCents`, `totalExpenseCents`, `savingsCents = income - expense`, e percentagens sobre rendimento devolvendo `0.0` sempre que `totalIncomeCents <= 0L`.
4. **Despesas líquidas (reembolsos ligados):** WHEN existem reembolsos associados (`linkedTransactionId`), THE SYSTEM SHALL abater o reembolso à despesa original sem produzir despesa líquida negativa na agregação.
5. **Deduplicação multi-app:** WHEN uma notificação bancária coincide em montante e janela temporal com pendente ou movimento confirmado do mesmo dia, THE SYSTEM SHALL devolver `DuplicateDrop`.

## Acceptance Criteria
- [ ] GIVEN `totalIncomeCents == 0L` WHEN `MonthlyAnalyticsCalculator` calcula quotas percentuais THEN nenhuma percentagem é `NaN` ou `Infinity`.
- [ ] GIVEN `privacyMode == true` WHEN `MoneyCents.formatForInput(1250L)` é chamado THEN devolve `"12,50"` (nunca mascarado).
- [ ] GIVEN a suite `domain/` WHEN `./gradlew :app:testProdDebugUnitTest` + `check_domain_coverage.py` correm THEN a cobertura de linhas em `domain/model` e `domain/usecase` é ≥ 95%.

## Fora de Âmbito
- UI Compose dos ecrãs `Home`, `Transactions` e `Analytics` (coberta por testes visuais/manuais).
