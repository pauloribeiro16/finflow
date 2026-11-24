# Spec 07 — Budgets, Goals & Investments

## Objectivo
Definir o cálculo de orçamentos mensais por categoria, metas de poupança, valorização de carteira de investimentos, importação de relatórios XTB e sincronização opcional (opt-in) de cotações via Yahoo Finance.

## Ficheiros Cobertos
- `app/src/main/kotlin/com/financas/app/domain/usecase/BudgetProgressCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/GoalProgressCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/InvestmentPortfolioCalculator.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/InvestmentTransferRules.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/XtbReportParser.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/XtbSyncEngine.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/XtbTickerMapper.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/PortfolioMarketSyncUseCase.kt`
- `app/src/main/kotlin/com/financas/app/data/network/YahooFinanceQuoteService.kt`

## Requisitos Funcionais e de Segurança (EARS)
1. **Orçamentos e Metas:** WHEN `BudgetProgressCalculator` ou `GoalProgressCalculator` avaliam progresso, THE SYSTEM SHALL usar cêntimos (`Long`) e evitar divisões por zero quando `limitCents <= 0` ou `targetCents <= 0`.
2. **Sincronização de mercado opt-in:** THE SYSTEM SHALL manter cotações automáticas (Yahoo Finance) desligadas por omissão; pedidos manuais ou automáticos SHALL usar `SafeHttp` com `TickerPolicy` (`^[A-Z0-9.^=-]{1,20}$`).
3. **Integridade de cotações e câmbio:** WHEN `PortfolioMarketSyncUseCase` recebe uma cotação, THE SYSTEM SHALL converter moedas suportadas (incluindo subunidades como `GBp`/`GBX` ÷ 100), SHALL NOT gravar valores com câmbio falhado, e SHALL rejeitar variações anómalas (>3× ou <⅓) sem sobrescrever silenciosamente.
4. **Deduplicação XTB:** WHEN `XtbSyncEngine` importa operações/dividendos XTB, THE SYSTEM SHALL incluir o instrumento e o índice de ocorrência na chave anónima de deduplicação para não descartar dividendos legítimos de ativos distintos no mesmo dia.

## Acceptance Criteria
- [ ] GIVEN um ticker inválido (ex. `"../etc"` ou `"AAPL?foo=1"`) WHEN `TickerPolicy.normalize` é chamado THEN devolve `null`.
- [ ] GIVEN falha na taxa de câmbio EUR/USD WHEN `PortfolioMarketSyncUseCase` sincroniza um ativo em USD THEN o ativo é marcado como falhado e `currentValueCents` permanece inalterado.
- [ ] GIVEN dois dividendos do mesmo valor no mesmo dia de ativos diferentes WHEN `XtbSyncEngine` importa THEN ambos são preservados (chaves de deduplicação distintas).

## Fora de Âmbito
- Recomendações financeiras ou ligação direta a APIs autenticadas de corretoras.
