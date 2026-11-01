# CONTRACT — Phase 4: Aba `Conta`, Backup JSON/CSV & Biometria Opcional

**Date:** 2026-10-25
**Planner:** Antigravity
**Status:** APPROVED (Ready for Executor)
**Phase:** 4 of 4
**Depends On:** Phase 3
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## Trials

`trials: 3` (default)

---

## Scope

Concluir a aplicação com a aba `⚙️ Conta` (`AccountScreen.kt`), gestão de categorias/contextos, portabilidade total de dados e segurança biométrica opcional:
1. **Gestão de Categorias (2 Níveis) e Contextos `Onde`:**
   - Adicionar ou editar Subcategorias dentro de qualquer Categoria Pai e adicionar novos Chips de Contexto `Onde`.
2. **Exportação e Importação de Backup (`BackupSerializer.kt`):**
   - **Backup Completo (`JSON`):** Exporta e importa de forma atómica e idempotente todas as categorias personalizadas e transações com as 4 dimensões intactas.
   - **Exportação Analítica (`CSV` compatível com Excel):** Gera ficheiro CSV UTF-8 (`Data;Mês;Tipo;Valor €;Categoria;Subcategoria;Onde (Contexto);Estabelecimento/Nota;Como (Método);Fixo`).
3. **Biometria Opcional (`BiometricLockGate.kt`):**
   - Desligada por defeito (abertura instantânea em 0 segundos).
   - Switch na aba `Conta` para ativar autenticação via `AndroidX BiometricPrompt` ao abrir a app.

---

## Output Criteria (what was produced)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | `BackupSerializer` faz round-trip completo (`exportToJson` → `importFromJson`) preservando 100% das categorias de 2 níveis e todas as colunas das transações ao cêntimo | MUST | — |
| 2 | `BackupSerializer.exportToCsv` gera cabeçalho e linhas formatadas em PT-PT com todas as 4 dimensões (`No Quê`, `Onde`, `Como`, `Quanto`) | MUST | — |
| 3 | Importar o mesmo backup JSON duas vezes é idempotente (não duplica transações nem categorias existentes) | MUST | — |
| 4 | O estado de Biometria arranca `false` por defeito (abertura imediata) e pode ser alternado nas definições da aba `Conta` | SHOULD | — |
| 5 | Utilizador consegue criar uma nova Subcategoria associada a uma Categoria Pai existente | SHOULD | — |

---

## Outcome Criteria (system state after)

| # | Criterion | Weight | Result |
|---|-----------|--------|--------|
| 1 | Suite completa de testes unitários (Phases 1 a 4) passa com 100% de sucesso (`./gradlew :app:testDebugUnitTest`) | MUST | — |
| 2 | Lint e compilação final passam sem erros (`./gradlew :app:lintDebug :app:assembleDebug`) | MUST | — |

---

## Validation Commands

### Criterion 1, 2 & 3 — Round-Trip de Backup JSON/CSV e Idempotência
- **Tier:** 3 (Behavioral)
- **Command:** `./gradlew :app:testDebugUnitTest --tests "com.financas.app.data.repository.BackupSerializerTest"`
- **Expected:** `BUILD SUCCESSFUL` validando: (a) serialização e desserialização sem perda de nenhum campo; (b) idempotência ao reimportar; (c) formatação correta do CSV.
- **Pass condition:** Exit code 0.

### Outcome Criterion 1 & 2 — Verificação Global do Projeto
- **Tier:** 4 (Integration)
- **Command:** `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- **Expected:** `BUILD SUCCESSFUL` em todas as tasks e APK pronto em `app/build/outputs/apk/debug/app-debug.apk`.
- **Pass condition:** Exit code 0.

---

## Files to Change

| File | Action | Why |
|------|--------|-----|
| `app/src/main/kotlin/com/financas/app/data/repository/BackupSerializer.kt` | create | Exportação/Importação atómica em JSON e exportação CSV |
| `app/src/main/kotlin/com/financas/app/ui/screens/account/AccountScreen.kt` | create | Aba `Conta` com gestão de categorias/contextos, backup SAF e switch biométrico |
| `app/src/main/kotlin/com/financas/app/ui/screens/account/BiometricLockGate.kt` | create | Wrapper opcional `BiometricPrompt` (desativado por defeito) |
| `app/src/test/kotlin/com/financas/app/data/repository/BackupSerializerTest.kt` | create | Testes comportamentais de round-trip JSON e exportação CSV |
