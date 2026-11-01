# CONTRACT — Phase 15: Leitor Offline XTB (Parser CSV/Excel & Sincronização Inteligente Anti-Duplicação)

**Date:** 2026-10-30
**Planner:** Antigravity (Planner Role)
**Status:** DRAFT (Ready for Execution)
**Phase:** 15 of 16
**Depends On:** Phase 14 (Investment Assets & Património)
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## 1. Scope & Objective

Permitir ao utilizador importar relatórios periódicos da corretora XTB (formato `.csv` ou `.txt` exportado do portal/app XTB) diretamente no telemóvel de forma **100% offline, privada e sem duplicação de dados**:

1. **Parser Puro Kotlin (`XtbReportParser`):**
   - Extrai as posições abertas (`Open Positions`) agregadas por instrumento:
     - Volume, Valor atual de mercado em cêntimos (`currentValueCents`), Preço médio de abertura / Capital investido em cêntimos (`investedCents`), Ticker e Categoria (ETF, ETC/Outro, Stock).
   - Extrai as operações de caixa (`Cash Operations`):
     - Dividendos, Juros de fundos livres (`Free funds interest`), Impostos retidos (`Free funds interest tax`), Depósitos e Compras/Vendas.
     - Cada operação possui o seu ID único da XTB (`ID` numérico determinístico).
2. **Motor de Sincronização Inteligente Anti-Duplicação (`XtbSyncEngine`):**
   - **Para Ativos de Investimento (`investment_assets`):**
     - Faz correspondência dos ativos existentes da `institution = 'XTB'` por Ticker ou Nome.
     - Se o ativo já existir: **Atualiza** os valores (`currentValueCents`, `investedCents`, data de atualização), mantendo configurações personalizadas (reforço mensal, metas, notas).
     - Se for um novo ativo: **Insere** como novo ativo na carteira.
     - Resultado: Zero ativos duplicados.
   - **Para Movimentos Financeiros (`transactions`):**
     - Identifica cada operação com marcador determinístico no campo `merchant` ou `paymentMethod` (ex.: `[XTB:1468850982]`).
     - Se o ID já existir na base de dados: **Ignora** a operação (contador de duplicados ignorados).
     - Se for nova (ex.: novos dividendos ou juros): **Insere** no histórico de movimentos com a data correta.
3. **Interface de Utilizador (UI Compose Dark):**
   - Botão discreto e elegante no separador de Investimentos: `Importar XTB` (com ícone de download/documento).
   - Suporte a seleção de ficheiro via `ActivityResultContracts.GetContent()` (ficheiro `.csv` no telemóvel) e alternativa para colar texto.
   - Folha de confirmação / pré-visualização (`XtbImportPreviewSheet`) com resumo das posições detetadas, novos valores e operações encontradas antes de aplicar.
4. **Zero Dependências Externas & 100% Offline:**
   - Implementação em Kotlin puro (stdlib), sem bibliotecas externas pesadas, preservando o desempenho e privacidade total.

---

## 2. Technical Blueprint

### A. Domain Models (`domain/model/XtbModels.kt`)
```kotlin
data class XtbOpenPosition(
    val ticker: String,
    val name: String,
    val category: String, // ETF, ETC, STOCK
    val volume: Double,
    val currentValueCents: Long,
    val investedCents: Long,
    val profitLossCents: Long,
    val profitLossPercentage: Double
)

data class XtbCashOperation(
    val id: String, // ID único da XTB (ex: 1468850982)
    val type: String, // Dividend, Free funds interest, Free funds interest tax, Deposit, etc.
    val instrument: String,
    val ticker: String,
    val amountCents: Long,
    val epochDay: Long,
    val yearMonth: Int,
    val comment: String
)

data class XtbParsedReport(
    val openPositions: List<XtbOpenPosition>,
    val cashOperations: List<XtbCashOperation>,
    val reportDateEpochDay: Long? = null
)

data class XtbSyncResult(
    val updatedAssetsCount: Int,
    val newAssetsCount: Int,
    val importedTransactionsCount: Int,
    val skippedTransactionsCount: Int,
    val updatedPositions: List<XtbOpenPosition>
)
```

### B. Parser (`domain/usecase/XtbReportParser.kt`)
- `fun parse(csvContent: String): XtbParsedReport`
- Lida com cabeçalhos de secções (`Closed Positions`, `Cash Operations`, `Open Positions`).
- Faz parsing seguro de valores monetários com vírgula ou ponto para cêntimos (`Long`).
- Descarte seguro de metadados bancários (números de conta e referências de clearing são descartados na memória).

### C. Sync Engine (`domain/usecase/XtbSyncEngine.kt`)
- `suspend fun sync(report: XtbParsedReport, importCashOperations: Boolean): XtbSyncResult`
- Consulta `investmentAssetDao.getAllSync()` e `transactionDao.getAllSync()`.
- Executa a correspondência determinística e aplica as alterações numa única transação atómica.

---

## 3. Validation Tiers

- **Tier 1 (Compiler):** `./gradlew :app:compileDebugKotlin` compila sem erros.
- **Tier 2 (Static Analysis):** Dinheiro sempre em cêntimos (`Long`), sem bibliotecas externas, total conformidade com `AGENTS.md`.
- **Tier 3 (Behavioral):** `XtbReportParserTest` valida:
  - Parsing exato das 8 posições abertas reais fornecidas pelo utilizador.
  - Parsing de operações de dividendos, juros e taxas.
  - `XtbSyncEngineTest` valida que reimportar o mesmo ficheiro resulta em 0 duplicados (8 atualizações, 0 novas posições duplicadas).
- **Tier 4 (Smoke / UI Test):** Instalação no telemóvel/emulador e teste do fluxo de importação.
