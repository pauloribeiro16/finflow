# GOAL DECOMPOSITION — App Android de Finanças Pessoais (4 Dimensões vs. Rendimento)

**Date:** 2026-10-25
**Planner:** Antigravity (Planner Role)
**Status:** COMPLETED (All 4 Phases Implemented and Independently Validated)

---

## Original Goal

> "Eu quero criar uma app android em que quero meter os meus gastos, aquilo que recebo, no que gasto como gasto, onde gasto e quanto gasto e em relação aquilo que eu recebo, por mes, que depois dá para fazer analises em graficos, onde gasto mais as coisas."

---

## Decomposition Rationale

| Criterion | Applied? | Evidence |
|-----------|----------|----------|
| C1: Múltiplas funcionalidades distintas | Sim | Motor de dados 4 dimensões + Registo rápido com calculadora e auto-prefill + 4 Vistas de Gráficos + Gerador mensal de fixos + Backup JSON/CSV |
| C2: 3+ componentes/arquiteturas | Sim | Camada Room SQLite (`data/`), Motor Analítico e Prefill (`domain/`), UI Compose + Canvas (`ui/`) |
| C3: 5+ ficheiros estimados | Sim | 73 ficheiros Kotlin e testes unitários implementados e validados |
| C4: Conectores de sequência | Sim | Fundação de dados → Registo e Cockpit → Motor Gráfico de Análise → Backup, Biometria e Polimento |
| C5: Decisões arquiteturais | Sim | Hierarquia 2 níveis de categorias, Contextos `Onde` + Estabelecimentos, Cêntimos em `Long`, Dark Mode exclusivo |

---

## Phases & Execution Status

| Phase | Name | Description | Files | Depends On | Contract | Status | Result |
|-------|------|-------------|-------|------------|----------|--------|--------|
| 1 | Fundação & Motor Financeiro | Room DB (Categorias 2 níveis + Transações 4 dimensões), Motor de Cálculo (% do recebido, média mensal, auto-prefill e gerador de fixos) + Testes Unitários | 9 | — | `contracts/CONTRACT-phase-1.md` | COMPLETED | **PASS: 100%** |
| 2 | Navegação, Cockpit `Início`, `Movimentos` & Registo `< 3s` | Tema Ultra-Dark, Barra de 5 pontos com `+` central, `CalculatorNumpad`, auto-prefill por estabelecimento, Cockpit do Mês (Barra de Esforço + 1-toque fixos) e Feed de Movimentos com pesquisa | 13 | Phase 1 | `contracts/CONTRACT-phase-2.md` | COMPLETED | **PASS: 100%** |
| 3 | Motor Gráfico Interativo (`Análise`) | Ecrã `Análise` com toggle `[ € \| % do Recebido ]` e as 4 Vistas em Compose `Canvas` (`No Quê`, `Onde`, `Como`, `Evolução Anual` com linha de média) | 5 | Phase 2 | `contracts/CONTRACT-phase-3.md` | COMPLETED | **PASS: 100%** |
| 4 | Aba `Conta`, Backup JSON/CSV & Biometria Opcional | Gestão de categorias/subcategorias e contextos, Exportação/Importação atómica JSON/CSV e switch de Biometria AndroidX | 6 | Phase 3 | `contracts/CONTRACT-phase-4.md` | COMPLETED | **PASS: 100%** |
| 5 | Análises Avançadas (Comparar, Tendências & Ano vs Ano) | Motor de comparação vs. 3 bases (Mês Anterior / Média Mensal / Ano Anterior), tendências de categoria com média não diluída, taxa de poupança com meta 20%, gráficos de linha e barras agrupadas por ano em `Canvas` | 9 | Phase 4 + `07931ff` | `contracts/CONTRACT-phase-5.md` | COMPLETED | **PASS: 100%** |
| 6 | Suite Exaustiva de Testes (Integridade & Funcionais) | 2 classes novas de integridade (paridade asset↔seed e invariantes das 361 transações; round-trip/CSV/idempotência do backup) + 13 classes de teste expandidas cobrindo 86 cenários funcionais em JVM pura, sem tocar em código de produção | 15 | Phase 5 + `9d502cf` | `contracts/CONTRACT-phase-6.md` | COMPLETED | **PASS: 100%** |
| 7 | Fundamentos do Dia a Dia | Editar movimentos (update em vez de insert), seletor de data no registo, confirmação antes de apagar, contextos personalizados persistentes (SharedPreferences), correção de acumulação de coletores, total líquido no filtro "Todos" | 13 | Phase 6 + `d351649` | `contracts/CONTRACT-phase-7.md` | COMPLETED | **PASS: 100%** |
| 8 | Orçamentos, Metas & Previsão | Tabelas `budgets`/`goals` com migração aditiva Room v1→v2 (sem migração destrutiva), 3 motores puros (orçamento/meta/projeção), cartões no Início, gestão na Conta e backup JSON v2 que inclui orçamentos e metas | 24 | Phase 7 + `7325a6b` | `contracts/CONTRACT-phase-8.md` | COMPLETED | **PASS: 100%** |
| 9 | Calendário, Relatório & Pesquisa por Valor | 6.ª vista "Calendário" (heatmap mensal em Canvas), `MonthlyReportGenerator` com relatório partilhável (construído no momento do clique), filtro por valor nos Movimentos | 16 | Phase 8 + `400bfbc` | `contracts/CONTRACT-phase-9.md` | COMPLETED | **PASS: 100%** |
| 10 | Bateria Exaustiva das Fases 7–9 | 7 classes novas (integridade de entidades/backup v2, fidelidade de edição, datas, persistência de contextos, ciclo de vida de coletores, fluxos de orçamentos/metas) + 7 classes expandidas, 119 testes novos | 16 | Phase 9 + `8b2fb36` | `contracts/CONTRACT-phase-10.md` | COMPLETED | **PASS: 100%** |
| 11 | Deteção de Subscrições & Custos Recorrentes | Motor `SubscriptionDetector` para identificar contratos/mensalidades ativas no mês visualizado, cálculo de custo anualizado e alerta de subida de preço vs mês anterior (`isPriceHike = true`), com cartão dedicado no Análise | 6 | Phase 10 + `1122676` | `contracts/CONTRACT-phase-11.md` | COMPLETED | **PASS: 100%** |
| 12 | Reembolsos MB WAY & Divisão de Contas | Migração Room v3→v4 com `linkedTransactionId`, motor `NetExpenseCalculator` para abater despesas reembolsadas sem inflacionar o salário/poupança, seleção de despesa no registo e badges na lista de movimentos | 18 | Phase 11 + `e156343` | `contracts/CONTRACT-phase-12.md` | COMPLETED | **PASS: 100%** |

---

## Execution Log

| Phase | Executor Subagent | Validator Subagent | Result | Tests Passed | Commit | Date |
|---|---|---|---|---|---|---|
| **1** | `Phase 1 Executor` (`425f6ec0`) | `Phase 1 Validator` (`b1eba664`) | **PASS** | 18 / 18 | `ed492ef` | 2026-10-25 |
| **2** | `Phase 2 Executor` (`0e9652e3`) | `Phase 2 Validator` (`eef0f658`) | **PASS** | 38 / 38 | `a40f01f` | 2026-10-25 |
| **3** | `Phase 3 Executor` (`47fd0622`) | `Phase 3 Validator` (`9406f4de`) | **PASS** | 42 / 42 | `2b80c1f` | 2026-10-25 |
| **4** | `Phase 4 Executor` (`fdd395f0`) | `Phase 4 Validator` (`625e24ff`) | **PASS** | 51 / 51 | `62c3b6e` | 2026-10-25 |
| **5** | `Phase 5 Executor` (`2f908c18`) | `Phase 5 Validator` (`ab3ca799`) | **PASS** | 65 / 65 | `9d502cf` | 2026-10-26 |
| **6** | `Phase 6 Executor` (`16745758`) | `Phase 6 Validator` (`ca32b0be`) | **PASS** | 131 / 131 | `f7e81fa` | 2026-10-26 |
| **7** | `Phase 7 Executor` (`8016c193`) | `Phase 7 Validator` (`0fad2a7e`) | **PASS** | 141 / 141 | `7325a6b` | 2026-10-26 |
| **8** | `Phase 8 Executor` (`16745758`→`7ee25c62`) | `Phase 8 Validator` (`c22b1cb3`) | **PASS** | 187 / 187 | `400bfbc` | 2026-10-27 |
| **9** | `Phase 9 Executor` (`120e0b35`) | `Phase 9 Validator` (`501ac3c3`) | **PASS** | 210 / 210 | `8b2fb36` | 2026-10-27 |
| **10** | `Phase 10 Executor` (`b0b33da7`) | `Phase 10 Validator` (`ab90f898`) | **PASS** | 329 / 329 | `082ccf1` | 2026-10-27 |
| **11** | `Phase 11 Executor` (`b7056ae4`) | `Phase 11 Validator` (`7118a541`) | **PASS** | 365 / 365 | `e156343` | 2026-10-27 |
| **12** | `Phase 12 Executor` (`46334a81`) | `Phase 12 Validator` (`0b559c0d`) | **PASS** | 376 / 376 | `9c03ab1` | 2026-10-27 |

---

## Final Artifact Summary

- **APK de Debug Gerado:** `app/build/outputs/apk/debug/app-debug.apk` (~19 MB, pronto a instalar via `./gradlew :app:installDebug`)
- **Cobertura de Testes:** 376 testes unitários (34 classes de teste) em 100% de sucesso — integridade de dados, backup v2, formatação, 10 motores de cálculo, reembolsos MB WAY/splits, deteção de subscrições e subidas de preço, captura de notificações com desduplicação, 6 ViewModels e fluxos completos de edição/orçamentos/metas/calendário.
- **Zero erros de Lint:** `./gradlew :app:lintDebug` concluído com sucesso.
- **Conformidade de Engenharia:** 100% cêntimos em `Long`, 0 bibliotecas de gráficos externas, 0 bibliotecas novas de qualquer tipo, migrações de BD puramente aditivas (v1→v2→v3→v4, sem perda de dados), 100% offline-first.

---

## Sign-off

- [x] User approved decomposition (via `/grill-me`)
- [x] All contracts created (`contracts/CONTRACT-phase-1.md` .. `CONTRACT-phase-10.md`)
- [x] All 10 phases executed by independent Executor subagents
- [x] All 10 phases independently verified by Validator subagents
- [x] All commits recorded in Git repository
- [x] Phase 5 executed, independently validated (all MUST criteria PASS) and committed
- [x] Phase 6 (exhaustive integrity & functional test suite) executed, independently validated (86/86 matrix scenarios) and committed
- [x] Phases 7–9 (fundamentals, budgets/goals/projection with DB v2, calendar/report/amount filter) executed, independently validated and committed
- [x] Phase 10 (exhaustive suite for phases 7–9: +119 tests) executed, independently validated (mutation-proven collector assertions) and committed
