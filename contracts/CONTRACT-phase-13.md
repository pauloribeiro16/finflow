# CONTRACT — Phase 13: Regras Personalizadas de Auto-Categorização (Custom Rules Engine)

**Date:** 2026-10-27
**Planner:** Antigravity (Planner Role)
**Status:** DRAFT (Awaiting User Review)
**Phase:** 13 of 16
**Depends On:** Phase 12 + Commit `ac1ec74`
**Parent Goal:** `GOAL_DECOMPOSITION.md`

---

## 1. Scope & Objective

Permitir ao utilizador configurar **Regras Personalizadas de Auto-Categorização** por palavra-chave:
1. Definir regras como *"Se o comerciante contiver 'Continente' ou 'Pingo Doce' → Categoria: Supermercado, Contexto: Casa"* ou *"Se contiver 'Uber Eats' → Restaurantes, Contexto: Saída"*.
2. As regras têm prioridade sobre o histórico estatístico, sendo aplicadas:
   - No registo manual (`AddTransactionBottomSheet` ao digitar o estabelecimento).
   - Na captura automática de notificações (`FinancialNotificationListenerService` para sugerir logo a categoria/contexto certos na fila de pendentes).
3. Ecrã de gestão de regras no separador **Conta** (adicionar nova regra, ver regras ativas e eliminar regras).

---

## 2. Technical Blueprint

### A. Room SQLite Migration (v4 -> v5, Puramente Aditiva)
- Nova entidade `MerchantRuleEntity`:
  - `id: Long = 0` (PK auto-generate)
  - `keyword: String` (termo a pesquisar no comerciante, ex.: "continente")
  - `categoryId: String`
  - `contextTag: String`
  - `isFixed: Boolean = false`
- Novo DAO `MerchantRuleDao`:
  - `getAll(): Flow<List<MerchantRuleEntity>>`
  - `getAllSync(): List<MerchantRuleEntity>`
  - `insert(rule: MerchantRuleEntity): Long`
  - `deleteById(id: Long)`
- Migração `MIGRATION_4_5`:
  ```sql
  CREATE TABLE IF NOT EXISTS merchant_rules (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      keyword TEXT NOT NULL,
      categoryId TEXT NOT NULL,
      contextTag TEXT NOT NULL,
      isFixed INTEGER NOT NULL DEFAULT 0
  );
  CREATE INDEX IF NOT EXISTS index_merchant_rules_keyword ON merchant_rules (keyword);
  ```

### B. Domain Layer (Pure Kotlin)
- Classe `RuleMatchingEngine`:
  - `findMatch(merchant: String, rules: List<MerchantRuleEntity>): MerchantRuleEntity?`
  - Normalização: remoção de acentos, trim, lowercase.
  - Correspondência por substring inteligente (ex.: "Pingo Doce Chiado" coincide com regra "pingo doce").
- Integração no `SmartPrefillEngine` e no serviço de notificações.

### C. UI Layer (Material 3 Dark)
- No separador **Conta**:
  - Secção "Regras de Auto-Categorização" com diálogo para adicionar regra:
    - Palavra-chave (ex.: "Pingo Doce")
    - Escolha de categoria e contexto
    - Lista de regras ativas com botão de apagar.

### D. Testing Suite
- `RuleMatchingEngineTest`:
  - Correspondência exata e por substring.
  - Case-insensitivity e remoção de acentos ("farmácia" coincide com "Farmacia Silva").
  - Múltiplas regras e regras sem correspondência.
- Testes de ViewModel e integridade de DAO.

---

## 3. Validation Tiers

- **Tier 1 (Compiler):** `./gradlew :app:compileDebugKotlin` sem erros.
- **Tier 2 (Static Analysis):** Clean architecture em `domain/`, migração aditiva v4->v5.
- **Tier 3 (Behavioral):** Testes unitários do motor de regras e fluxos de UI passam a verde.
- **Tier 4 (Regression):** Todos os 376 testes existentes continuam a passar a 100%.
