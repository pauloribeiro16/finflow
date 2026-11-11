<!-- second-brain:begin (gerido por scripts/gen-agents-block.py — não editar) -->
## Ecossistema (second-brain)
**AppAndroid** — aplicação Android nativa (Finflow Android). Programa: *Aplicações Móveis & Quant* (Aplicações móveis, quant e plataformas).
1. **Antes de explorar ficheiros, lê o catálogo:** `MAPA.md` no repo `https://git.pushin.eu/paulo/second-brain.git` — diz o que existe, onde está e por onde começar.
2. **Antes de reinvestigar, procura:** `second-brain search "termos" --project=AppAndroid` e a ficha `distilled/aprendizagens/AppAndroid.md` (erros e gotchas já documentados) nesse repo.
3. **Ao concluir algo que valha a pena guardar** (decisão, causa de um bug, resultado de uma experiência), acrescenta ao fim de `NOTAS.md` na raiz deste repo: `## AAAA-MM-DD — título` + 2-5 linhas (o quê, porquê, fontes) e faz commit. Assim a conclusão sobrevive à conversa e o second-brain indexa-a.
Repos irmãos (mesmo programa): MusicaApp, financial-adviser, macro-forecasting-league, viagens.
<!-- second-brain:end -->

# AGENTS.md — AppAndroid (Finanças Pessoais)

> **Fonte única de verdade operacional para agentes de IA.**
> Arquitetura detalhada em [`PLANO_ARQUITETURA.md`](./PLANO_ARQUITETURA.md) e contratos de execução em [`GOAL_DECOMPOSITION.md`](./GOAL_DECOMPOSITION.md) + [`contracts/`](./contracts/).

## 1. Commands (File-Scoped First)

```bash
# Correr um único ficheiro de teste unitário (PREFERIDO — < 5s)
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.domain.MonthlyAnalyticsCalculatorTest"

# Correr testes de um pacote específico
./gradlew :app:testProdDebugUnitTest --tests "com.financas.app.data.*"

# Suite completa de testes unitários (fallback)
./gradlew :app:testProdDebugUnitTest

# Compilar APK de Debug e verificar erros de compilação Kotlin/Compose
./gradlew :app:assembleProdDebug

# Lint estático
./gradlew :app:lintProdDebug

# Instalar no emulador/dispositivo ligado via ADB
./gradlew :app:installProdDebug
```

## 2. Testing

- **Framework:** JUnit 4 + `kotlinx-coroutines-test` (testes rápidos em JVM, sem emulador para lógica de negócio).
- **Regra de Ouro:** Toda a matemática financeira (`Recebido`, `Gasto`, `% do Rendimento`, `Poupança`, `Média Mensal`, `Gerador de Fixos`) vive em classes Kotlin puras em `domain/`, com cobertura de linhas **≥ 95%** em `domain/model` e `domain/usecase` (verificada no CI por `.github/scripts/check_domain_coverage.py`).
- **Migrações:** o esquema Room é exportado para `app/schemas/`. `RoomMigrationSchemaTest` aplica todas as migrações numa base SQLite real (JVM) e compara com o esquema exportado — qualquer alteração de entidade exige subir a versão, acrescentar a migração e fazer commit do novo `N.json`.
- **Validação de Contratos:** Cada fase exige validação **Tier 3 (Behavioral)** via `./gradlew :app:testProdDebugUnitTest --tests "..."`.

## 3. Project Structure

Single-module Android app (**Kotlin 2.4 + Jetpack Compose Material 3 + Room 2.8 + SQLCipher**, compileSdk 37 / targetSdk 36, AGP 9.5):

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
  - Validar alterações com testes file-scoped (`./gradlew :app:testProdDebugUnitTest --tests "..."`).
- **Ask first:**
  - Antes de adicionar qualquer dependência externa ao `build.gradle.kts`.
  - Antes de alterar o esquema de tabelas (`CategoryEntity`, `TransactionEntity`).
  - Antes de acrescentar um servidor a `SafeHttp.ALLOWED_API_HOSTS` ou qualquer outro pedido de rede.
- **Never:**
  - Nunca usar `Float`/`Double` para guardar montantes em base de dados.
  - Nunca adicionar bibliotecas externas de gráficos (usar Compose `Canvas` nativo).
  - Nunca usar `fallbackToDestructiveMigration()` em produção sem backup.
  - Nunca fazer auto-implementação se estiveres no papel de Planeador ou Validador.
  - Nunca abrir ligações de rede fora de `SafeHttp` (o download do APK usa `UpdateUrlPolicy`).
  - Nunca enviar dados para fora do telemóvel sem opt-in explícito do utilizador.
  - Nunca formatar valores para campos de edição com `formatPt()`/`formatCents()` (são mascarados no modo privacidade) — usar `MoneyCents.formatForInput()`.

## 6.1 Gate de Segurança & Privacidade (obrigatório em cada contrato)

O Validador só aprova uma fase se todas as respostas forem "sim" ou "não se aplica":
1. **Dados para fora:** a fase envia algum dado para fora do telemóvel? Se sim: o servidor está em `SafeHttp`, há opt-in/aviso na UI, e `docs/PRIVACY_POLICY.md` + o cartão "Privacidade: Dados Locais" foram atualizados.
2. **Entrada não confiável:** dados de ficheiros, notificações, backups ou rede são validados (tamanho, formato, valores finitos) antes de chegar à BD ou a URLs?
3. **Esquema:** se mexeu em entidades, a versão subiu, há migração, o `N.json` foi commitado e `RoomMigrationSchemaTest` passa?
4. **Modo privacidade:** os montantes novos passam por `MoneyCents` (e os campos de edição por `formatForInput`)?
5. **Segredos e IDs:** nenhum ID de conta/ordem, token ou texto bruto sensível é gravado ou registado em log sem necessidade?
6. **CI verde:** testes, lint, gate de cobertura e build de `androidTest`.

Modelo de contrato: [`contracts/CONTRACT-TEMPLATE.md`](./contracts/CONTRACT-TEMPLATE.md).

## 7. Skills (Activate on Demand)

Quando uma tarefa corresponder à descrição de uma skill, lê `~/.gemini/config/skills/<skill>/SKILL.md`:

| Skill | Quando Usar |
|-------|-------------|
| **sprint-contract** | Qualquer pedido de implementação ou planeamento de fase (Papel: Planeador). |
| **frontend-design** | Criação de ecrãs Compose, calculadora numpad e gráficos em `Canvas` (Papel: Executor). |
| **code-review** | Revisão de diff e verificação independente de critérios T1–T4 (Papel: Validador). |
| **agents-md-writer** | Atualização deste ficheiro `AGENTS.md`. |
