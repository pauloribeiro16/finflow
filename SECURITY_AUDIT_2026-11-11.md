# Auditoria de Segurança — alterações v2.6.0 → main (2026-11-11)

**Tipo:** revisão estática do diff `v2.6.0..4ec5691` (5 commits, 47 ficheiros).
**Âmbito:**
- novo movimento em 3 passos e deteção de comerciante (`0df5351`);
- modo privacidade (`0850b5d`, `0ecd155`);
- cotações Yahoo Finance, catalogação e sanitização XTB (`4ec5691`).

**Complementa** `SECURITY_AUDIT_2026-11-09.md`. Os achados S1–S14 desse relatório não foram reavaliados aqui.

---

## Resumo

| # | Gravidade | Achado | Ficheiro |
|---|-----------|--------|----------|
| N1 | **Média** | A app envia as posições ao Yahoo sem consentimento e contradiz a política de privacidade | `BudgetsViewModel.kt`, `docs/PRIVACY_POLICY.md`, `AccountScreen.kt` |
| N2 | Média | Valores de património errados gravados na BD (câmbio fixo e moedas não tratadas) | `PortfolioMarketSyncUseCase.kt`, `YahooFinanceQuoteService.kt` |
| N3 | Baixa | Ticker não validado nem codificado no URL; resposta sem limite de tamanho | `YahooFinanceQuoteService.kt` |
| N4 | Baixa | Chave de deduplicação XTB sem ticker: perde dividendos legítimos | `XtbTickerMapper.kt`, `XtbSyncEngine.kt` |
| N5 | Baixa | `units` importado do backup sem validação (`Infinity`, valores enormes) | `BackupSerializer.kt` |
| N6 | Baixa | API não oficial do Yahoo com User-Agent de browser falsificado | `YahooFinanceQuoteService.kt` |
| N7 | Info | O modo privacidade é só cosmético e não é um controlo de segurança | `MoneyCents.kt`, `MainScaffold.kt` |
| N8 | Info | O texto bruto da notificação fica guardado nos pendentes sem prazo | `FinancialNotificationListenerService.kt` |

Sem achados de gravidade alta. O ecrã em 3 passos não tem impacto de segurança. A migração 7→8 é aditiva, sem perda de dados.

---

## Estado das correções (2026-11-11)

| # | Estado | O que mudou |
|---|--------|-------------|
| N1 | ✅ Corrigido | Cotações automáticas desligadas por omissão: interruptor em Conta → Versão e aviso antes do primeiro pedido manual. Política de privacidade e cartão "Dados Locais" atualizados. |
| N2 | ✅ Corrigido | Câmbio por moeda (`EUR{CUR}=X`). GBp/GBX, ILA e ZAc convertidos de cêntimos. Se o câmbio falhar, o ativo não é atualizado (sem 1.10 fixo). Variação >3× ou <⅓ não é aplicada e é reportada. |
| N3 | ✅ Corrigido | Novo `SafeHttp`: lista de hosts, HTTPS obrigatório, sem redirects, limite de 512 KB, `disconnect()`. Usado pelo Yahoo e pela verificação de atualizações. `TickerPolicy` valida tickers e o URL usa `URLEncoder`. |
| N4 | ✅ Corrigido | A chave inclui o instrumento e o número de ocorrência (formato v2, 16 hex). As chaves antigas continuam a deduplicar, mas cada uma só cobre uma operação. |
| N5 | ✅ Corrigido | O restauro valida o ticker (`TickerPolicy`) e `units` (finito, 0 < u ≤ 1e9). |
| N6 | ✅ Corrigido | User-Agent honesto `Finflow-App-Android`. A política refere que as cotações são estimativas. |
| N7 | ✅ Melhorado | Com o bloqueio ativo, mostrar valores exige biometria/PIN. |
| N8 | ✅ Corrigido | Os pendentes com mais de 30 dias são apagados ao abrir a app e ao receber notificações. As notas já eram apagadas com o pendente ao aprovar ou descartar. |

### Recomendações gerais

| # | Estado | Nota |
|---|--------|------|
| 2. Cliente HTTP único | ✅ | `SafeHttp` para as APIs. O download do APK mantém a sua política própria (`UpdateUrlPolicy`). |
| 3. Backup cifrado | ✅ | Exportação `.finflow`: PBKDF2-HMAC-SHA256 (310k iterações) + AES-256-GCM, com parâmetros autenticados. O restauro deteta o formato e pede a palavra-passe. JSON em claro só por opção explícita. |
| 4. Bloqueio ligado à chave (S10) | ⏸ Aceite | Com `setUserAuthenticationRequired`, a BD só abre depois de desbloquear. A captura de notificações em segundo plano deixaria de funcionar. Fica aceite, como no S10. |
| 5. Cadeia de dependências | ◑ Parcial | Dependabot (Gradle + Actions) e CodeQL `security-extended` adicionados. *Secret scanning* e *push protection* já estavam ativos no GitHub; faltam *Dependabot security updates* e proteção do `main` (definições do repositório). O `verification-metadata.xml` não foi adicionado: cada atualização de dependência obrigaria a regenerá-lo, e o Dependabot não o faz. |
| 6. Testes de migração | ✅ | Esquema exportado (`app/schemas/…/8.json`); `RoomMigrationSchemaTest` aplica as migrações 1→8 numa base SQLite real (JVM, `sqlite-jdbc` só em testes) e compara colunas e índices com o esquema do Room. Verificado que deteta uma migração errada. |
| 7. Tapjacking | ✅ | Android 12+: `setHideOverlayWindows(true)` (permissão normal `HIDE_OVERLAY_WINDOWS`). Android 8–11: `filterTouchesWhenObscured` em toda a janela. |
| 8. Retenção mínima | ✅ | Ver N8. |

---

## Detalhe

### N1 (Média) — Envio ao Yahoo sem consentimento e em contradição com a política

**O que acontece:**
- Ao abrir o separador de Investimentos, o `BudgetsViewModel` (no `init`) chama `syncAllActiveAssets` se algum ativo tiver ticker e unidades.
- Não há opção para desligar este pedido nem aviso ao utilizador.
- O Yahoo recebe a lista de tickers detidos, o IP e a hora, uma vez por dia. Isso revela a composição da carteira (por exemplo, o LQQ.PA alavancado).
- O interruptor "Verificação automática" só cobre o GitHub.

**O que a app afirma:**
- A política de privacidade (secção 0 e secção 3) diz: *"A única ligação à internet serve para verificar e descarregar atualizações"*.
- O cartão "Privacidade: Dados Locais" em Conta diz o mesmo.

Com o código do `main` (ainda não lançado), as duas afirmações passariam a ser falsas. É o mesmo tipo de problema que o S7 da auditoria anterior.

**Correção:**
- Criar o interruptor "Cotações automáticas (Yahoo Finance)" em Conta → Segurança, **desligado por omissão**. Sem ele, só se atualiza com um toque explícito em "Atualizar cotações".
- Atualizar `PRIVACY_POLICY.md` (secção 3: o que é enviado, a quem, como desligar) e o texto do cartão.
- Considerar pedir sempre todos os tickers do mesmo lote, ou juntar alguns índices de "ruído". É opcional.

### N2 (Média) — Integridade dos valores calculados

- `fetchEurUsdRate()` devolve **1.10 fixo** se o pedido falhar. O valor errado é gravado em `currentValueCents` com a data de hoje. O utilizador não tem forma de saber.
- Só `USD` é convertido. Qualquer outra moeda é tratada como EUR, incluindo `GBP`, `CHF` e `GBp`:
  - o Yahoo devolve os ETFs `.L` em **pence (`GBp`)**;
  - o `uppercase()` transforma isso em `GBP`;
  - o valor fica sobrevalorizado ~100× (ou mal convertido, no caso de GBP/CHF).
- A sincronização sobrescreve o `currentValueCents` introduzido à mão, sem histórico.

**Correção:**
- Se o câmbio falhar, não atualizar os ativos USD (marcá-los como falhados).
- Usar uma lista de moedas suportadas (`EUR`, `USD`, `GBP`, `GBp`÷100, …). Moeda desconhecida conta como falha.
- Validar que o preço é finito e que a variação face ao valor anterior é plausível, por exemplo <50% num dia. Caso contrário, pedir confirmação.

### N3 (Baixa) — Entrada não validada no pedido HTTP

**Problema:**
- O `ticker` vem do formulário, do relatório XTB ou do backup JSON e é concatenado diretamente no path. Um `?`, `#` ou `/..` altera o pedido. O host continua fixo, por isso não há SSRF, mas a entrada não é controlada.
- `readText()` lê a resposta inteira para memória, sem limite.
- O redirect segue automaticamente e a ligação não é fechada (`disconnect`).

**Correção:**
- Validar o ticker com `^[A-Z0-9.^=-]{1,20}$` e usar `URLEncoder`.
- Limitar a resposta a ~256 KB.
- Usar `instanceFollowRedirects = false` e chamar `disconnect()` num `finally`.
- Reutilizar a política de hosts do updater (`UpdateUrlPolicy`) para ter uma única lista de domínios permitidos.

### N4 (Baixa) — Colisões na deduplicação XTB

**Problema:**
- `generateAnonymousDeduplicationKey(epochDay, amountCents, type)` não inclui o instrumento.
- Dois dividendos do mesmo valor, no mesmo dia, de ativos diferentes geram a mesma chave. O segundo é descartado como "duplicado" sem aviso.
- O hash curto (32 bits) não é o problema; a falta do ticker é.

**Correção:** incluir o ticker e o instrumento no material do hash.

### N5 (Baixa) — Validação de `units` no restauro

**Problema:**
- `asDoubleOrNull()` aceita `"Infinity"` e números enormes.
- Com `units = Infinity`, `roundToLong()` dá `Long.MAX_VALUE` e as somas do património sofrem overflow.
- O ticker do backup também entra sem validação (ver N3).

**Correção:**
- Aceitar só `units.isFinite() && units in 0.0..1e9`.
- Aplicar a regex do ticker no import.

### N6 (Baixa) — API não oficial do Yahoo

**Problema:**
- `query1.finance.yahoo.com/v8/finance/chart` não é uma API pública licenciada.
- O User-Agent `Mozilla/5.0 (Macintosh…)` faz-se passar por um browser.

**Riscos:**
- Pode deixar de funcionar ou ser bloqueada a qualquer momento.
- Distribuir a app publicamente com isto pode violar os termos do Yahoo.

**Recomendação:**
- Usar um User-Agent honesto (`Finflow-App-Android`).
- Tratar a funcionalidade como "melhor esforço", com indicação visível de "estimativa".

### N7 (Info) — Modo privacidade

**Limitações:**
- Esconde montantes no ecrã, mas não protege os dados em si: exportações, partilhas e a BD continuam completas.
- Desliga-se com um toque, sem autenticação.
- É um estado global estático (`@Volatile var`).

Está bem para "mostrar a app a alguém". A UI não deve sugerir mais do que isso.

**Melhorias opcionais:**
- Exigir biometria para **revelar** valores, quando o bloqueio estiver ativo.
- Ligar o modo automaticamente ao voltar do segundo plano.

### N8 (Info) — Texto bruto das notificações

**Situação:**
- Quando o comerciante não é identificado, ficam guardados até 300 caracteres da notificação, incluindo `EXTRA_MESSAGES`.
- Está documentado na política de privacidade.
- Fica na BD cifrada e não entra nos backups.

**Recomendação:**
- Apagar o campo `notes` quando o pendente é aprovado ou descartado.
- Purgar pendentes com mais de 30 dias.

---

## Recomendações gerais para a app

Por ordem de retorno face ao esforço:

1. **Corrigir N1 antes da próxima versão.** O código do Yahoo (`4ec5691`) está no `main` mas não entrou na v2.7.0 publicada; lançado tal como estava, a política de privacidade ficaria errada.
2. **Um único cliente HTTP** (`SafeHttp`) para o GitHub e o Yahoo, com:
   - lista de hosts permitidos;
   - HTTPS obrigatório;
   - sem redirects cross-host;
   - limite de tamanho;
   - timeouts.

   Assim nenhuma funcionalidade nova abre rede "por fora".
3. **Exportação cifrada:** opção de backup `.finflow` cifrado com palavra-passe (AES-GCM + PBKDF2/Argon2). Hoje o JSON/CSV exportado sai em claro para onde o utilizador escolher.
4. **Ligar o bloqueio à chave (S10):** chave do Keystore com `setUserAuthenticationRequired` e `CryptoObject` na biometria. Assim o bloqueio deixa de ser só uma flag em `shared_prefs`.
5. **Cadeia de dependências:**
   - `gradle/verification-metadata.xml` (verificação de checksums das dependências);
   - Dependabot/Renovate para atualizações;
   - CodeQL e secret scanning no CI, porque o repositório é público.
6. **Testes de migração reais:**
   - `exportSchema = true`;
   - `MigrationTestHelper`;
   - reparar o `androidTest`, que não compila.

   Uma migração errada numa BD cifrada sem backup é perda de dados.
7. **Tapjacking:** `filterTouchesWhenObscured` nos interruptores de segurança (bloqueio, captura de notificações, instalação de atualização), porque o minSdk 26 ainda permite overlays.
8. **Retenção mínima:** apagar notas brutas e pendentes antigos (N8) e limpar backups internos antigos. O updater já guarda só os 3 mais recentes.

## Segunda ronda (2026-11-11) — manutenção

- **Modo privacidade:** os campos de edição eram preenchidos com `•••• €` com o modo ligado; guardar podia gravar 0. Corrigido com `MoneyCents.formatForInput()` (nunca mascarado).
- **Tradução:** ~580 textos passaram a `tr("pt", "en")` (`core/i18n/AppLocale.kt`); datas, dias da semana e meios de pagamento seguem o idioma; mudar o idioma recria a UI (`key(appLanguage)`).
- **Ecrãs divididos:** Conta, Análise e Património passaram de ~3.200–3.800 linhas para ficheiros de ≤ 1.000 (diálogos e secções em funções próprias).
- **CI:** lint, gate de cobertura do domínio (≥ 95%), build do `androidTest` e relatórios como artefacto.
- **Testes de UI:** voltaram a compilar e foram atualizados para a UI atual (português forçado, fluxo em 3 passos), mas **não foram validados até ao fim** — a execução no telemóvel do utilizador foi interrompida a pedido (não alterar dados). Validar num emulador/CI dedicado.
- **Processo:** gate de segurança & privacidade obrigatório em `AGENTS.md` §6.1 e `contracts/CONTRACT-TEMPLATE.md`.
