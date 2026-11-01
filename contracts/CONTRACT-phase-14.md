# CONTRACT — Phase 14: Carteira de Ativos & Património Aplicado (PPRs, Contas a Prazo, ETFs)

**Date:** 2026-10-29
**Planner:** Antigravity (Planner Role)
**Status:** COMPLETED & VERIFIED (Tier 1-4 Approved)
**Phase:** 14 of 16
**Depends On:** Phase 13 + Biometric Delay + Finflow Branding
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## 1. Scope & Objective

Permitir ao utilizador consolidar e gerir todo o património financeiro investido fora da conta corrente:
1. **Registo Multi-Ativo:**
   - PPRs (ex.: Alves Ribeiro, Stoik, Golden SGF, etc.)
   - Contas/Depósitos a Prazo (ex.: CGD, ActivoBank, Trade Republic, Certificados de Aforro)
   - ETFs (ex.: Vanguard FTSE All-World VWCE, iShares Core MSCI World IWDA, VUAA S&P 500)
   - Criptoativos e Outros Investimentos
2. **5º Separador — Hub de Riqueza & Poupança:**
   - Segmented Top Bar com 3 abas: `[ 🏛️ Investimentos | 🎯 Metas | 📊 Orçamentos ]`
   - Na aba **Investimentos**:
     - Hero Card com Total Aplicado (valor de mercado atual), Total Investido e Mais-Valias Totais (€ e %).
     - Gráfico de Alocação de Ativos em Compose `Canvas` nativo (% por tipo de ativo com legendas coloridas).
     - Filtros rápidos por classe de ativo.
     - Lista de ativos com métricas individuais, entidade/banco e botão rápido para atualizar o valor atual.
     - Diálogo para adicionar/editar aplicações com suporte a reforço mensal opcional e notas.
3. **Ecrã de Início (Home):**
   - Card resumo "Património Aplicado" com valor total consolidado e navegação direta em 1 toque para o separador de Investimentos.
4. **Room SQLite Migration (v4 -> v5):**
   - Migração puramente aditiva para tabela `investment_assets` sem perda de dados.
5. **Cálculos Financeiros Puros:**
   - `InvestmentPortfolioCalculator` puro Kotlin com proteção contra divisão por zero, arredondamentos e 100% de cobertura unitária.

---

## 2. Technical Blueprint

### A. Room SQLite Migration (v4 -> v5)
- Entidade `InvestmentAssetEntity`:
  - `id: String` (PK UUID/deterministic)
  - `name: String` (ex.: "PPR Alves Ribeiro")
  - `type: String` (`PPR`, `DEPOSITO_PRAZO`, `ETF`, `CERTIFICADOS_AFORRO`, `CRYPTO`, `OUTRO`)
  - `institution: String` (ex.: "Banco Invest", "Degiro", "Trade Republic", "CGD", "IGCP")
  - `investedCents: Long` (capital total investido em cêntimos)
  - `currentValueCents: Long` (valor de mercado / saldo atual em cêntimos)
  - `monthlyContributionCents: Long = 0L` (reforço mensal recorrente opcional)
  - `targetCents: Long? = null` (meta patrimonial para o ativo, opcional)
  - `notes: String = ""` (notas adicionais)
  - `lastUpdatedEpochDay: Long` (data da última atualização do valor)
  - `archived: Boolean = false`
- Índices:
  - `index_investment_assets_type`
  - `index_investment_assets_archived`
- DAO `InvestmentAssetDao`:
  - `getAll(): Flow<List<InvestmentAssetEntity>>`
  - `getAllSync(): List<InvestmentAssetEntity>`
  - `insert(asset: InvestmentAssetEntity)`
  - `update(asset: InvestmentAssetEntity)`
  - `updateCurrentValue(id: String, newValueCents: Long, epochDay: Long)`
  - `deleteById(id: String)`

### B. Domain Layer (Pure Kotlin)
- Modelos:
  - `InvestmentType` enum (`PPR`, `DEPOSITO_PRAZO`, `ETF`, `CERTIFICADOS_AFORRO`, `CRYPTO`, `OUTRO`) com labels e ícones/cores em PT-PT.
  - `AssetAllocationItem(type: InvestmentType, totalValueCents: Long, percentage: Double)`
  - `PortfolioSummary(totalInvestedCents: Long, totalCurrentValueCents: Long, totalProfitLossCents: Long, totalProfitLossPercentage: Double, monthlyContributionsCents: Long, allocations: List<AssetAllocationItem>, assetsCount: Int)`
- Usecase:
  - `InvestmentPortfolioCalculator.calculate(assets: List<InvestmentAssetEntity>): PortfolioSummary`

### C. UI Layer (Material 3 Dark)
- Segmented Control no topo do ecrã de Poupança/Orçamentos:
  - `enum class WealthTab { INVESTMENTS, GOALS, BUDGETS }`
- Aba Investimentos com listagem rica, hero card, badge de lucro positivo/negativo e diálogo de edição/adição.
- Integração no `HomeScreen`:
  - Card "Património Aplicado" com redireccionamento imediato.

---

## 3. Validation Tiers

- **Tier 1 (Compiler):** `./gradlew :app:compileDebugKotlin` compila sem avisos nem erros.
- **Tier 2 (Static Analysis):** Dinheiro sempre em cêntimos (`Long`), sem bibliotecas externas de gráficos, dark mode `#0B0E14`.
- **Tier 3 (Behavioral):** Testes unitários do `InvestmentPortfolioCalculatorTest` passam a 100%.
- **Tier 4 (Regression):** Suite de testes existente corre sem falhas.
