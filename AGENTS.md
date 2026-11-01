# AGENTS.md — AppAndroid (Finanças Pessoais)

> **Fonte única de verdade operacional para agentes de IA.**
> Arquitetura detalhada em [`PLANO_ARQUITETURA.md`](./PLANO_ARQUITETURA.md) e contratos de execução em [`GOAL_DECOMPOSITION.md`](./GOAL_DECOMPOSITION.md) + [`contracts/`](./contracts/).

## 1. Commands (File-Scoped First)

```bash
# Correr um único ficheiro de teste unitário (PREFERIDO — < 5s)
./gradlew :app:testDebugUnitTest --tests "com.financas.app.domain.MonthlyAnalyticsCalculatorTest"

# Correr testes de um pacote específico
./gradlew :app:testDebugUnitTest --tests "com.financas.app.data.*"

# Suite completa de testes unitários (fallback)
./gradlew :app:testDebugUnitTest

# Compilar APK de Debug e verificar erros de compilação Kotlin/Compose
./gradlew :app:assembleDebug

# Lint estático
./gradlew :app:lintDebug

# Instalar no emulador/dispositivo ligado via ADB
./gradlew :app:installDebug
```

## 2. Testing

- **Framework:** JUnit 4 + `kotlinx-coroutines-test` (testes rápidos em JVM, sem emulador para lógica de negócio).
- **Regra de Ouro:** Toda a matemática financeira (`Recebido`, `Gasto`, `% do Rendimento`, `Poupança`, `Média Mensal`, `Gerador de Fixos`) vive em classes Kotlin puras em `domain/` com **100% de cobertura unitária**.
- **Validação de Contratos:** Cada fase exige validação **Tier 3 (Behavioral)** via `./gradlew :app:testDebugUnitTest --tests "..."`.

## 3. Project Structure

Single-module Android app (**Kotlin 2.0 + Jetpack Compose Material 3 + Room SQLite**):

```text
app/src/main/kotlin/com/financas/app/
├── data/
│   ├── db/           # AppDatabase (Room), DAOs (CategoryDao, TransactionDao)
│   ├── entity/       # CategoryEntity (2 níveis), TransactionEntity (4 dimensões)
│   ├── repository/   # FinanceRepository, BackupSerializer (JSON/CSV)
│   └── seed/         # DefaultSeed (Categorias Pai > Subcategoria e Contextos iniciais)
├── domain/
│   ├── model/        # Modelos imutáveis (MoneyCents, MonthlySummary, DimensionBreakdown)
│   └── usecase/      # MonthlyAnalyticsCalculator, RecurringMonthGenerator, SmartPrefillEngine
├── ui/
│   ├── theme/        # Ultra-Dark Theme (Revolut/Copilot palette), Typography
│   ├── components/   # CalculatorNumpad, InteractiveCanvasCharts, EffortBar, FilterChips
│   ├── navigation/   # BottomBar 5 pontos: Início | Movimentos | (+) | Análise | Conta
│   └── screens/      # home/, transactions/, addsheet/, analytics/, account/
└── di/               # AppContainer (injeção manual leve sem overhead de KSP/Hilt)
```

## 4. Code Style & Financial Rules

- **Dinheiro sempre em Cêntimos (`Long`):** Nunca usar `Double` ou `Float` para valores monetários.
- **Formato PT-PT:** Moeda em Euros (`1.234,56 €`), percentagens a 1 casa decimal (`24,5%`).
- **Dark Mode Exclusivo:** Paleta escura de alto contraste (`#0B0E14` background, `#151A24` surface).
- **Zero Comentários Desnecessários:** Não adicionar comentários inline a menos que pedidos.

```kotlin
// ✅ GOOD: Cêntimos em Long + cálculo seguro de % face ao rendimento recebido
val amountCents: Long = 1250L // 12,50 €
fun incomeSharePermille(spentCents: Long, incomeCents: Long): Double =
    if (incomeCents <= 0L) 0.0 else (spentCents.toDouble() / incomeCents.toDouble()) * 100.0

// ❌ BAD: Float/Double para dinheiro ou divisão sem guarda contra receita zero
val amount: Double = 12.50
val pct = (amount / totalIncome) * 100 // Crash/Infinity quando totalIncome == 0.0
```

## 5. Git Workflow

- **Branches:** `main` (estável) ou `sprint/phase-N`.
- **Commits:** Imperativo, minúsculas, conciso: `feat(phase-1): add room schema and analytics calculator`.
- **Ciclo de Sprint:** Apenas fazer commit após o **Validador** aprovar todos os critérios `MUST` do contrato ativo.

## 6. Boundaries

- **Always:**
  - Ler [`PLANO_ARQUITETURA.md`](./PLANO_ARQUITETURA.md) e o contrato ativo em [`contracts/`](./contracts/) antes de alterar código.
  - Seguir o contrato de 3 papéis (**Planeador → Executor → Validador**).
  - Validar alterações com testes file-scoped (`./gradlew :app:testDebugUnitTest --tests "..."`).
- **Ask first:**
  - Antes de adicionar qualquer dependência externa ao `build.gradle.kts`.
  - Antes de alterar o esquema de tabelas (`CategoryEntity`, `TransactionEntity`).
- **Never:**
  - Nunca usar `Float`/`Double` para guardar montantes em base de dados.
  - Nunca adicionar bibliotecas externas de gráficos (usar Compose `Canvas` nativo).
  - Nunca usar `fallbackToDestructiveMigration()` em produção sem backup.
  - Nunca fazer auto-implementação se estiveres no papel de Planeador ou Validador.

## 7. Skills (Activate on Demand)

Quando uma tarefa corresponder à descrição de uma skill, lê `~/.gemini/config/skills/<skill>/SKILL.md`:

| Skill | Quando Usar |
|-------|-------------|
| **sprint-contract** | Qualquer pedido de implementação ou planeamento de fase (Papel: Planeador). |
| **frontend-design** | Criação de ecrãs Compose, calculadora numpad e gráficos em `Canvas` (Papel: Executor). |
| **code-review** | Revisão de diff e verificação independente de critérios T1–T4 (Papel: Validador). |
| **agents-md-writer** | Atualização deste ficheiro `AGENTS.md`. |
