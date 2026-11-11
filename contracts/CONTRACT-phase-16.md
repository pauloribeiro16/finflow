# CONTRACT — Phase 16: Estimativa de Portfólio via Yahoo Finance, Catalogação de Posições Iniciais & Sanitização XTB

**Date:** 2026-11-11
**Planner:** Antigravity (Planner Role)
**Status:** DRAFT (Ready for Execution)
**Phase:** 16 of 16
**Depends On:** Phase 14 & Phase 15 (Investment Assets & XTB Engine)
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## 1. Scope & Objective

Implementar uma estimativa diária automatizada das cotações de mercado para os ativos de investimento através da API pública do **Yahoo Finance**, garantindo a **catalogação completa das posições iniciais** (preço médio de abertura, unidades e capital investido) com diferenciação de produtos alavancados (ex.: Nasdaq 2x), e **sanitização absoluta de dados sensíveis na extração da XTB**:

1. **Catalogação das Posições Iniciais:**
   - Para cada ativo/posição importada ou criada:
     - **Preço Médio de Compra Inicial:** calculado a partir do capital investido e unidades ($\frac{\text{investedCents}}{\text{units}}$) ou obtido do `Open price` da XTB.
     - **Unidades / Volume:** quantidade exata de ações/cotas detidas (ex.: `26,2636 un.`).
     - **Capital Investido Inicial:** total pago na compra (`investedCents`).
     - **Classificação e Catalogação:** etiquetagem clara do tipo de produto (ex.: *ETF Alavancado 2x*, *ETF Global*, *Commodities/Ouro*, *Ações*).
   - No cartão do ativo (`AssetCard`): secção dedicada que resume a **Posição Inicial** lado a lado com a **Estimativa Atual** do Yahoo Finance.
2. **Privacidade e Sanitização Absoluta da XTB:**
   - **Zero dados sensíveis armazenados:** O parser e sincronizador NUNCA guardam números de conta (ex.: `Account number`), IDs de ordens/posições (ex.: `Position ID`, `892631613`), referências bancárias internas ou descrições com dados pessoais.
   - Para deduplicação de dividendos e juros, utilizar hashes opacos determinísticos baseados apenas em `(data + quantia + tipo)`, sem referências internas da corretora.
3. **Suporte a Ticker e Unidades no Esquema de Ativos (`InvestmentAssetEntity`):**
   - Adicionar os campos `ticker: String? = null` e `units: Double? = null` a `InvestmentAssetEntity`.
   - Criar `MIGRATION_7_8` no Room sem perda de dados (`ALTER TABLE investment_assets ADD COLUMN ticker TEXT DEFAULT NULL`, `ALTER TABLE investment_assets ADD COLUMN units REAL DEFAULT NULL`).
   - Atualizar `BackupSerializer` para suportar `ticker` e `units` em JSON.
4. **Mapeamento Inteligente de Tickers XTB $\rightarrow$ Yahoo Finance (`XtbTickerMapper`):**
   - Mapear sufixos de bolsas da XTB para as bolsas reconhecidas pelo Yahoo Finance:
     - Euronext Paris (`.FR` $\rightarrow$ `.PA`, ex.: `LQQ.FR` $\rightarrow$ `LQQ.PA` para o *Amundi Nasdaq-100 Daily 2x Leveraged UCITS ETF*).
     - Bolsa de Londres (`.UK` $\rightarrow$ `.L`, ex.: `CSPX.UK` $\rightarrow$ `CSPX.L`, `EGLN.UK` $\rightarrow$ `EGLN.L`).
     - Euronext Amesterdão (`.NL` $\rightarrow$ `.AS`).
     - Bolsa de Milão (`.IT` $\rightarrow$ `.MI`).
     - Bolsa de Madrid (`.ES` $\rightarrow$ `.MC`).
     - XETRA Alemanha (`.DE` $\rightarrow$ `.DE`, ex.: `EXUS.DE`, `2B7K.DE`, `AHYQ.DE`).
5. **Serviço de Cotações Yahoo Finance (`YahooFinanceQuoteService`):**
   - Conexão nativa `HttpURLConnection` em `Dispatchers.IO` (zero dependências externas).
   - Suporte a câmbio automático: se um ativo cotar em dólares (USD, ex.: `CSPX.L`), consulta a taxa `EURUSD=X` e converte a cotação estimada para Euros.
   - Resiliência total a falhas de rede / modo avião (devolve `Result.failure` sem crashes).
6. **Sincronizador de Mercado (`PortfolioMarketSyncUseCase`):**
   - Atualiza `currentValueCents = (units * priceInEur * 100).roundToLong()`.
   - Mantém o capital investido original `investedCents` rigorosamente inalterado.
   - Atualiza `lastUpdatedEpochDay` para o dia de hoje.
7. **Interface de Utilizador (UI Compose Dark):**
   - Botão discreto no topo de Investimentos para "Atualizar Cotações" com indicador de progresso.
   - No cartão do ativo (`AssetCard`): exibe unidades, ticker, tag de catalogação (ex.: ⚡ *Alavancado 2x*), e a ficha da posição inicial vs. cotação de mercado (`P. Médio Compra: 9,50 € · Cotação Yahoo: 8,53 €`).
   - Em `AddEditInvestmentSheet`: campos opcionais para Ticker e Unidades.

---

## 2. Testable Acceptance Criteria

### MUST Criteria
- **MUST-1 (T3 - Behavioral):** `XtbTickerMapper` mapeia corretamente sufixos da XTB para o Yahoo Finance (`.FR` -> `.PA`, `.UK` -> `.L`, `.NL` -> `.AS`, etc.) e categoriza produtos alavancados (ex.: deteta "Lev" ou "2x" como alavancado).
- **MUST-2 (T3 - Behavioral):** `XtbSyncEngine` sanitiza totalmente o relatório, garantindo que nenhum número de conta, ID de posição ou referência interna de transação é gravado em `notes`, `id` ou `merchant`.
- **MUST-3 (T3 - Behavioral):** `InvestmentAssetEntity` armazena `ticker` e `units`, e a migração de base de dados Room 7 -> 8 preserva todos os dados existentes sem exceções.
- **MUST-4 (T3 - Behavioral):** `YahooFinanceQuoteService` faz o parse correto de cotações em JSON (com preço, moeda e variação) e aplica conversão cambial quando a moeda for USD.
- **MUST-5 (T3 - Behavioral):** `PortfolioMarketSyncUseCase` atualiza o `currentValueCents` e `lastUpdatedEpochDay` para todos os ativos com unidades e ticker válidos, preservando o `investedCents` e as posições iniciais.

### SHOULD Criteria
- **SHOULD-1 (T3 - Behavioral):** `BackupSerializer` exporta e importa `ticker` e `units` em ficheiros de cópia de segurança JSON sem corromper versões antigas.
- **SHOULD-2 (T3 - Behavioral):** `AssetCard` na UI apresenta a ficha da posição inicial (`unidades`, `preço médio de compra` e `capital investido inicial`) comparada com a cotação estimada do Yahoo Finance.

---

## 3. Validation Commands

```bash
# Validação T3 de Ticker Mapper e Sanitização XTB
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.domain.usecase.XtbTickerMapperTest"
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.domain.usecase.XtbSyncEngineSanitizationTest"

# Validação T3 do Serviço Yahoo Finance e Sincronização de Portfólio
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.data.network.YahooFinanceQuoteServiceTest"
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.domain.usecase.PortfolioMarketSyncUseCaseTest"

# Validação do Backup e Migração de Base de Dados
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.data.repository.BackupSerializerTest"
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.data.db.DatabaseMigrationTest"
```
