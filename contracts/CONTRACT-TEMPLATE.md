# CONTRACT — Phase N: <título>

> Papéis: **Planeador** (escreve este contrato) → **Executor** (implementa) → **Validador** (verifica, sem implementar).

## 1. Scope & Objective
- Objetivo:
- Fora de âmbito:
- Ficheiros previstos:

## 2. Testable Acceptance Criteria

### MUST Criteria
- **MUST-1 (T3 - Behavioral):**

### MUST — Gate de Segurança & Privacidade (AGENTS.md §6.1)
- **SEC-1 Dados para fora:** <não se aplica | servidor em `SafeHttp`, opt-in na UI, política de privacidade e cartão atualizados>
- **SEC-2 Entrada não confiável:** <validações aplicadas e respetivos testes>
- **SEC-3 Esquema:** <não se aplica | versão N→N+1, migração, `app/schemas/.../N+1.json`, `RoomMigrationSchemaTest` verde>
- **SEC-4 Modo privacidade:** <montantes via `MoneyCents`; campos de edição via `formatForInput`>
- **SEC-5 Segredos e IDs:** <nada sensível gravado/registado>
- **SEC-6 CI:** testes + lint + gate de cobertura + `assembleProdDebugAndroidTest`

### SHOULD Criteria
- **SHOULD-1:**

## 3. Validation Commands

```bash
./gradlew :app:testProdDebugUnitTest --tests "..."
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.data.db.RoomMigrationSchemaTest"
./gradlew :app:createProdDebugUnitTestCoverageReport && python3 .github/scripts/check_domain_coverage.py
./gradlew :app:lintProdDebug
```
