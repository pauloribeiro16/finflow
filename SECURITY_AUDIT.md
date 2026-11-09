> **Obsoleto.** Esta auditoria refere-se à v2.2.0 e assume uma app sem permissão `INTERNET`. Ver [`SECURITY_AUDIT_2026-11-09.md`](./SECURITY_AUDIT_2026-11-09.md).

# SECURITY_AUDIT.md — Finflow (AppAndroid, Finanças Pessoais)

**Data:** 2026-11-01 · **Tipo:** Auditoria estática defensiva (read-only), estilo OWASP MASVS
**Alvo:** `com.financas.app` v2.2.0 (versionCode 16), minSdk 26 / targetSdk 35, single-module Kotlin/Compose/Room+SQLCipher
**Threat model assumido (app local, sem backend):** (1) outra app no telemóvel; (2) root/backup do dispositivo; (3) pessoa com o telemóvel desbloqueado; (4) o próprio repositório git.

---

## Sumário executivo

A app tem uma base arquitetural de segurança sólida para o seu propósito: **não declara a permissão `INTERNET`** (não existe qualquer código de rede — o "sync XTB" é parsing offline de CSV), a base de dados é encriptada com SQLCipher 4.6.1 cuja chave de 32 bytes é gerada com `SecureRandom` e envolvida com AES-GCM por uma chave não-exportável do Android Keystore, o listener de notificações tem whitelist de pacotes bancários e **não persiste o texto bruto** das notificações, e os logs estão limpos. Os problemas reais concentram-se na **exposição de dados pessoais fora do cofre cifrado**: (1) o APK embutia em plaintext um asset com histórico financeiro pessoal (já removido) e era lido automaticamente em cada instalação fresca; (2) o histórico do git continha esse asset (num commit anterior) e o código hardcodava valores reais de investimento; (3) o gate biométrico falha aberto em API 26/27 e não tem fallback de credencial; (4) sem `FLAG_SECURE`, o task switcher mostra dados; (5) `allowBackup="true"` sem `dataExtractionRules`. Nenhum achado P0. Ver "Estado das correções" para o que já foi resolvido no commit atual.

---

## Tabela de achados

| ID | Severidade | Título | Localização principal |
|----|-----------|--------|----------------------|
| A1 | **P1** | Dados financeiros reais embarcados em plaintext no APK + seed automática | `app/src/main/assets/` — asset com histórico financeiro pessoal (já removido); `DefaultSeed.kt:149-176` |
| A2 | **P2** | Dados financeiros reais no histórico do git | blob de um commit anterior (histórico reescrito via squash) |
| A3 | **P2** | Dados pessoais hardcoded no código atual (investimentos PPR, nome) | `DefaultSeed.kt:182-221`; `UserProfileStore.kt:39` |
| A4 | **P2** | Gate biométrico: fail-open em API < 28, sem fallback de credencial, BiometricPrompt cru | `BiometricLockGate.kt:57-85`; `MainActivity.kt:60` |
| A5 | **P2** | Ausência de `FLAG_SECURE` — task switcher/screenshots expõem dados | `MainActivity.kt` (sem setFlags) |
| A6 | **P2** | `allowBackup="true"` sem `dataExtractionRules`/`fullBackupContent` | `AndroidManifest.xml:8`; aapt2 confirma `debuggable=true`, `allowBackup=true` |
| A7 | P3 | Migração plaintext→encriptada: DB em claro em disco durante a janela, sem wipe seguro | `DatabaseEncryptionMigrator.kt:52-62,108-148,150-163` |
| A8 | P3 | Exportação JSON/CSV completa via shareText + fallback clipboard global | `AccountScreen.kt:790-815`; `ShareUtils.kt:9-26` |
| A9 | P3 | Whitelist de apps financeiras por substring com tokens genéricos | `FinancialAppFilter.kt:5-34` |
| A10 | P3 | Passphrase SQLCipher e backup JSON de migração nunca são zerados em memória | `DatabaseKeyManager.kt:19-26`; `AppContainer.kt:50-64` |
| A11 | P3 | SQLCipher 4.6.1 desatualizado (4.7.2 disponível, bugfix) | `gradle/libs.versions.toml:13` |
| A12 | P3 | Falha no unwrap da chave Keystore não tem fallback (crash-loop pós-restore D2D) | `AppContainer.kt:50-64`; `DatabaseKeyManager.kt:19-26` |
| A13 | INFO | `scripts/__pycache__/` tracked no git; release sem R8/minify; APK analisado é debug | `git ls-files scripts/`; `app/build.gradle.kts:24` |
| P1..P6 | INFO | Positivos: sem INTERNET, whitelist de notificações correta, rawText não persistido, logs limpos, sem FileProvider, criptografia da BD bem desenhada | ver secção "Boas práticas confirmadas" |

Nenhum achado **P0**.

---

## Estado das correções (2026-11-01)

Corrigidos no commit atual (após esta auditoria):

- **A1** — o asset com histórico financeiro pessoal foi removido de `app/src/main/assets/` e o APK recompilado (o artefacto atual já não contém qualquer asset embutido).
- **A2** — o histórico do git foi reescrito (squash total), eliminando o blob com dados pessoais; o repositório recomeça com um histórico limpo.
- **A3** — os investimentos com valores reais e o nome hardcoded foram removidos do código (defaults vazios).
- **A4** — o gate biométrico foi corrigido: falha fechada em API < 28 e tratamento de lockout (ver `BiometricLockGate.kt`).
- **A5** — `FLAG_SECURE` aplicado na `MainActivity` (task switcher/screenshots já não mostram dados).
- **A6** — `android:allowBackup="false"` no `AndroidManifest.xml`.

Os restantes achados (A7–A13) mantêm o estado descrito abaixo.

---

## Detalhe dos achados

### A1 (P1) — Dados financeiros reais embarcados em plaintext no APK

**Localização:** um asset com histórico financeiro pessoal em `app/src/main/assets/` (JSON em plaintext, já removido); lido em `app/src/main/kotlin/com/financas/app/data/seed/DefaultSeed.kt:164-166` e `app/src/main/kotlin/com/financas/app/ui/screens/account/AccountViewModel.kt:328-330`.

**Evidência:**
```
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep assets
    → listava um asset JSON com o histórico financeiro pessoal em plaintext
```
`DefaultSeed.seedHistoricalIfNeeded()` corria no arranque de **toda a instalação fresca** (`FinancasApplication.kt:23-32`):
```kotlin
if (transactionDao.count() == 0 || currentVersion < targetVersion) {
    val json = context.assets.open("<asset histórico pessoal>.json")...
```
Conteúdo real confirmado: merchants como `"Café / Restaurante"`, `"Ginásio"`, `"Educação"`, `"Combustível"`, `"Vencimento Mensal"`, salários e montantes por dia.

**Impacto:** o SQLCipher protege a BD no dispositivo, mas este asset **contornava todo o cofre**: quem tivesse o APK (APK partilhado, enviado por email, no sendspace, ou copiado do telemóvel via `adb pull`) extraía os dados com `unzip`. Além disso contradizia a intenção declarada num commit anterior ("enforce empty default database") — uma instalação fresca nunca ficava vazia: era semeada com o histórico completo. Nota: o ficheiro estava `.gitignore`d (`app/src/main/assets/historical_*.json`), mas continuava a ser empacotado porque existia na pasta local.

**Recomendação concreta:**
1. Remover o asset com histórico financeiro pessoal de `app/src/main/assets/` (ou mover para fora de `src/`).
2. Substituir o seeding automático por importação manual (o botão "Restaurar Backup (JSON)" já existente já cobre este caso).
3. Apagar `app/build/` e recompilar para garantir que o APK novo não arrasta o asset de builds anteriores.
4. Se o import via asset for mantido para conveniência pessoal, aceitar explicitamente o risco e nunca distribuir esse APK.

---

### A2 (P2) — Dados financeiros reais no histórico do git

**Localização:** um commit anterior ("feat(data): historical excel import...") adicionou o asset com histórico financeiro pessoal ao repositório. Commits posteriores removeram o ficheiro do working tree, mas o blob permanecia acessível no histórico do git (agora reescrito via squash — ver "Estado das correções").

**Evidência:** `git log --all --diff-filter=A -- "*gastos*"` identificava o commit que adicionou o asset; o blob continha movimentos financeiros reais (merchants e salários).

**Impacto:** o repo está hoje **apenas local** (`git remote -v` vazio), o que mitiga. Mas o autor tem identidade GitHub configurada e qualquer `git push` futuro (backup no GitHub/GitLab privado incluído) publica tudo. A `.gitignore` não protege dados já commitados.

**Recomendação concreta:** se existir qualquer possibilidade de push futuro, reescrever o histórico (`git filter-repo` sobre o asset, ou squash total do repositório). Nunca fazer push sem verificar com `git log --all --diff-filter=A -- "*gastos*"`.

**Nota (2026-11-01):** o histórico do git foi reescrito (squash total), eliminando o blob com dados pessoais — ver "Estado das correções".

---

### A3 (P2) — Dados pessoais hardcoded no código atual

**Localização:**
- `app/src/main/kotlin/com/financas/app/data/seed/DefaultSeed.kt:182-221` — investimentos reais: `"PPR Moderado"` (Instituição X, investido 1.000,00 €, atual 1.050,00 €), `"PPR Leopardo"` (800,00 €), `"PPR Agressivo"` (700,00 €, reforço 100 €/mês), com comentários no código ("Total 1000+800+700 = 2500 €").
- `app/src/main/kotlin/com/financas/app/data/prefs/UserProfileStore.kt:39` — `prefs.getString("user_name", "Utilizador")`.

**Evidência:** trechos acima; estavam commitados no git (não só no histórico).

**Impacto:** valores patrimoniais reais e o nome do utilizador faziam parte do código-fonte — qualquer leak do repo (ou da pasta do projeto) expunha mais do que o necessário. Contradizia também o espírito de um commit anterior ("remove default user seeding").

**Recomendação concreta:** mover os investimentos default para um JSON local fora do APK/`src/` (ou deixar o separador Investimentos vazio até import manual); substituir o default do nome por `""` (o UI já mostra iniciais derivadas do nome definido pelo utilizador — `AccountScreen.kt:237`).

---

### A4 (P2) — Gate biométrico: fail-open em API < 28, sem fallback de credencial

**Localização:** `app/src/main/kotlin/com/financas/app/ui/screens/account/BiometricLockGate.kt:57-85`; `MainActivity.kt:60,120-122`.

**Evidência:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)...build()
    prompt.authenticate(...)
} else {
    onSuccess()   // ← autentica SEM autenticar
}
```
O builder não usa `setDeviceCredentialAllowed(true)` nem `setAllowedAuthenticators(...)`: só biometria, sem PIN/padrão como alternativa. `minSdk = 26`, logo em Android 8.0/8.1 (API 26/27) o "bloqueio" com biometria ativada é imediatamente saltado.

**Impacto:** (a) em dispositivos API 26/27, a pessoa com o telemóvel desbloqueado acede sem qualquer autenticação apesar de `biometrics_enabled=true` — fail-open; (b) em qualquer versão, se a biometria ficar bloqueada (tentativas excessivas), o utilizador fica sem forma de entrar (falha fechada sem alternativa — risco de indisponibilidade dos próprios dados); (c) o uso direto do `android.hardware.biometrics.BiometricPrompt` de framework em vez de `androidx.biometric.BiometricPrompt` perde compatibilidade e gestão de lockout. Nota honesta: no dispositivo atual do utilizador (API 34/35) o bypass 26/27 não se aplica — a severidade reflete o contrato declarado pelo `minSdk=26`.

**Recomendação concreta:** migrar para `androidx.biometric:biometric` (já declarada no toml mas **não usada** — nota: a dependência declarada não está no `app/build.gradle.kts`, o código usa o framework diretamente) com `BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL`; no ramo < P, falhar fechado (mostrar o ecrã de bloqueio com botão) ou elevar `minSdk` para 28; tratar `BIOMETRIC_ERROR_LOCKOUT`/`_PERMANENT` com fallback para credencial do dispositivo.

**Nota sobre o re-lock 30s (`SessionLockPolicy.kt`):** a lógica em `MainActivity.onPause/onResume` está correta (marca timestamp só quando desbloqueado, re-lock se elapsed > 30s; estado inicial em cold start = bloqueado se biometria ativa, `MainActivity.kt:60`). O Compose só compõe `MainScaffold` quando `!isLocked` — não há `setContent` com dados antes do gate. Bem feito.

---

### A5 (P2) — Ausência de FLAG_SECURE (task switcher e screenshots)

**Localização:** `MainActivity.kt` (nenhuma chamada a `setFlags(FLAG_SECURE)` em todo o `src/main/kotlin`, confirmado por grep).

**Impacto:** nos primeiros 30 s em background (e sempre, se o lock estiver desativado — estado por defeito: `biometrics_enabled=false`), o thumbnail do task switcher e os screenshots mostram saldos, movimentos e gráficos. É exatamente a superfície do "ecrã em task switcher" do threat model. O ecrã de bloqueio em si não revela nada (bom).

**Recomendação concreta:** em `onCreate` da `MainActivity`, `window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)`; opcionalmente em API 34+, `setRecentsScreenshotEnabled(false)`. Trade-off aceitável para uma app de finanças.

---

### A6 (P2) — allowBackup=true sem dataExtractionRules

**Localização:** `AndroidManifest.xml:8` (`android:allowBackup="true"`); sem `android:fullBackupContent` nem `android:dataExtractionRules`. Confirmado no APK via aapt2: `allowBackup=true`, `debuggable=true` (build debug), minSdk 26 / targetSdk 35.

**Impacto:** o backup automático D2D/nuvem inclui `databases/` (SQLCipher) **e** `files/database.key` (blob da chave envolvida). A mitigação real: a chave mestra Keystore não é exportável nem migra entre dispositivos, pelo que o par (DB cifrado + blob) é **indecifrável num dispositivo novo** — a exposição é sobretudo teórica (ciphertext no cloud do Google) e um problema de **resiliência**: após restauro D2D, `unwrap()` falha e a app entra em crash-loop (ver A12). Em builds debug (debuggable), `adb backup`/pull direto do data dir é possível por USB com o dispositivo desbloqueado — mas aí o atacante teria de quebrar o AES-GCM do Keystore local, o que é equivalente a root.

**Recomendação concreta:** ou `android:allowBackup="false"` (opção mais simples para app de finanças pessoais), ou `dataExtractionRules` (API 31+) + `fullBackupContent` (API 26-30) a excluir `databases/`, `files/database.key` e `files/profile_photo.png` do backup.

---

### A7 (P3) — Migração plaintext→encriptada: janela com DB em claro

**Localização:** `DatabaseEncryptionMigrator.kt:52-62` (`preserveLegacyFiles` renomeia para `*.plaintext` + wal/shm), `:108-148` (import em memória), `:150-163` (`deleteLegacyFiles` só no fim, e lança exceção se o delete falhar).

**Análise:** o export do legacy (`exportLegacyPlaintext`) produz o JSON completo **apenas em memória** (nunca escrito para ficheiro — positivo). Mas o ficheiro `financas.plaintext` (+ `-wal`) fica legível no disco privado da app durante toda a migração e, se a importação falhar (ex.: `IllegalStateException("Importação incompleta...")`), permanece em disco até a próxima tentativa ter sucesso. Não há overwrite antes do delete (recuperação forense até GC do FBE). A migração já foi executada no dispositivo do utilizador (one-shot), pelo que o risco residual é de reinstalações futuras.

**Recomendação concreta:** apagar os ficheiros legacy num `finally` mesmo em falha parcial (com toast/log do erro); antes do `delete()`, sobrescrever com zeros (`RandomAccessFile` write) para reduzir recuperação forense; considerar migrar via SQLCipher `sqlcipher_export` (mais atómico e evita o roundtrip JSON em memória).

---

### A8 (P3) — Exportação completa via shareText + fallback clipboard

**Localização:** `AccountScreen.kt:790-815` (`shareText(context, json, "backup_financas_....json", "application/json")` e idem CSV); `ShareUtils.kt:9-26`.

**Análise:** o backup JSON contém **todos** os movimentos/categorias/orçamentos/metas/investimentos; o CSV idem. Não é escrito nenhum ficheiro em cache/Downloads no dispositivo (positivo — vai direto pelo chooser). Risco esperado e controlado pelo utilizador (escolhe o destino). O **fallback** copia o relatório completo para o clipboard global se o chooser falhar — no Android 12+ outros apps só leem clipboard com foco e o sistema mostra aviso, mas o conteúdo fica em texto claro no sistema. As partilhas de relatório mensal em `AnalyticsScreen.kt:185` são resumo agregado (menos sensível).

**Recomendação concreta:** no fallback, copiar apenas um aviso/instruções em vez do conteúdo completo (ou pedir confirmação extra); documentar no ecrã que o export sai do cofre cifrado (o subtítulo do botão já diz "cópia completa" — suficiente). Severidade P3 porque é ação manual e intencional do utilizador.

---

### A9 (P3) — Whitelist de apps financeiras por substring genérica

**Localização:** `FinancialAppFilter.kt:5-34` — tokens `"caixa"`, `"caixageral"`, `"ctt"`, `"bpi"`, `"bcp"`, `"caixa"`, `"moey"` etc. via `pkg.contains(token)`.

**Análise:** a whitelist está corretamente aplicada **antes** de qualquer processamento (`FinancialNotificationListenerService.kt:43`), e apps não financeiras são ignoradas — o requisito de privacidade central está cumprido. Mas substrings tão genéricas como `"caixa"`, `"ctt"`, `"bpi"` podem capturar notificações de apps não bancárias cujo package as contenha (ex.: apps de correio/arquivo). Mitigação em cascata: `BankNotificationParser` só produz resultado se encontrar um montante em € + direção, e `FLAG_ONGOING_EVENT`/auto-notificações são excluídas. O texto bruto **não é persistido** (ver P2 na secção positiva); apenas `rawMerchant` (parseado) vai para `PendingTransactionEntity` (campo `rawText` existe no parser mas é descartado pelo serviço).

**Recomendação concreta:** substituir `contains` por match de prefixo/igualdade sobre packages concretos (`pt.cgd.caixadirecta`, `net.bicicotacao.ctt.app`, …) ou por whitelist de prefixes; rever periodicamente a lista.

---

### A10 (P3) — Material sensível não zerado em memória

**Localização:** `DatabaseKeyManager.kt:19-26` (o `ByteArray` da passphrase é devolvido e mantido vivo por `SupportOpenHelperFactory(passphrase)` em `AppContainer.kt:54`, sem `fill(0)` possível depois); `DatabaseEncryptionMigrator.kt:70,101` (o `backupJson` completo é uma `String` imutável que vive no heap durante a migração).

**Impacto:** num dispositivo rooted com dump de heap, a passphrase (e o JSON de migração) poderia ser extraída da RAM. No threat model root, o atacante já tem acesso a quase tudo, pelo que é defesa em profundidade. Nenhuma String com dados nas operações normais.

**Recomendação concreta:** manter a passphrase num `ByteArray` retido e limpar cópias temporárias com `Arrays.fill(0)`; no migrador, construir o import por stream em vez de `String` única. Baixa prioridade.

---

### A11 (P3) — SQLCipher 4.6.1 desatualizado

**Localização:** `gradle/libs.versions.toml:13` (`sqlcipher = "4.6.1"`, Agosto 2024).

**Análise:** não existem CVEs atribuídas a `net.zetetic:sqlcipher-android:4.6.1` (os "CVEs de SQLCipher" são tipicamente falsos positivos do SQLite upstream que o Zetetic já atualiza). A 4.7.2 (Março 2025) corrige dois defeitos difíceis de reproduzir da 4.7.0 e traz SQLite upstream mais recente.

**Recomendação concreta:** atualizar para `4.7.2` num commit isolado e correr a suite (`testDebugUnitTest`) + um ciclo de abrir/ler a BD existente (a encriptação é compatível entre 4.6.x→4.7.x). Nota: as dependências `androidx.biometric` e `androidx.security-crypto` estão declaradas no toml mas **não são usadas** no `build.gradle.kts` — a security-crypto (EncryptedSharedPreferences) está deprecada pela Google; não a introduzir.

---

### A12 (P3) — Falha no unwrap da chave sem fallback (resiliência)

**Localização:** `AppContainer.kt:50-64` (`database` lazy → `keyManager.getOrCreatePassphrase()`); `DatabaseKeyManager.kt:19-26` (se `keyFile` existe, `unwrap` lança `AEADBadTagException`/`KeyStoreException` sem recuperação).

**Impacto:** após restauro D2D para dispositivo novo (Keystore não migra), ou invalidação parcial do Keystore, a app crasha no arranque sem caminho de recuperação (o utilizador teria de limpar dados da app — perde o histórico local se não tiver backup exportado). Não é um problema de confidencialidade; é disponibilidade. Interage com A6.

**Recomendação concreta:** apanhar falha de unwrap num try/catch no `DefaultAppContainer`, apagar o blob órfão + BD indecifrável, regenerar chave e abrir BD vazia, com diálogo claro a aconselhar restauro do backup JSON (o botão já existe).

---

### A13 (INFO) — Higiene de build/repo

- `scripts/__pycache__/generate_discriminated_data.cpython-312.pyc` está tracked no git (ruído; `.gitignore` de `__pycache__/`).
- `buildTypes.release` tem `isMinifyEnabled = false` — sem R8: código sem obfuscação e APK maior (app pessoal sideload, impacto baixo; sem secret shrinking relevante porque não há secrets no APK).
- O APK analisado é `app-debug.apk` (42,9 MB, 160 ficheiros, `debuggable=true`, `extractNativeLibs=false`): debuggable só existe no buildType debug — nunca instalar/distribuir este APK; para uso "de produção" no telemóvel, construir release assinado (não há `signingConfig` definido — o APK release sairia unsigned).
- `Abrir_App.command` e `local.properties` (só caminho do SDK) estão untracked — correto.
- `gradle.properties`/scripts python não contêm secrets; `populate_database.py` referencia `/tmp/phone_financas.db` e o JSON pessoal local (que está untracked).

---

## Boas práticas confirmadas (INFO / positivos)

1. **Sem rede, ponto.** `uses-permission` no APK: apenas `USE_BIOMETRIC` (+ permission dinâmica gerada pelo androidx). **Sem `INTERNET`** — impossível exfiltrar dados mesmo que o código o quisesse. `XtbSyncEngine`/`XtbReportParser` são parsing offline de CSV/estado local (zero endpoints, zero credenciais, zero OkHttp/HttpURLConnection/Socket — confirmado por grep). `cleartextTraffic`/`networkSecurityConfig` são N/A por construção.
2. **Gestão da chave da BD bem desenhada** (`DatabaseKeyManager.kt`): passphrase 32B de `SecureRandom`, envolvida com AES-256-GCM (IV 12B, tag 128), chave mestra no AndroidKeystore não-exportável (`PURPOSE_ENCRYPT|DECRYPT`, GCM, NoPadding), blob com validação de formato, ficheiro em `files/` (MODE_PRIVATE do Android). Sem `setUserAuthenticationRequired` na chave Keystore — decisão correta, permite abrir a BD em background (listener de notificações) sem exigir desbloqueio.
3. **Notificações bancárias com privacidade por defeito:** processamento só após whitelist de pacotes (`FinancialNotificationListenerService.kt:43`); ignora auto-notificações (`FLAG_ONGOING_EVENT`), ignora a própria app; **`rawText` da notificação não é persistido** (só `rawMerchant` parseado + montante em `PendingTransactionEntity` — dados bancários brutos ficam fora da BD); deduplicação (`TransactionDeduplicator`) evita gravar duplicados de múltiplas apps; toggle de captura (`bank_notification_capture_prefs`, MODE_PRIVATE); BIND_NOTIFICATION_LISTENER_SERVICE só concedível pelo utilizador nas definições do sistema.
4. **Logs limpos:** apenas 2 `Log.d` de ligação/desligamento do serviço, sem valores nem conteúdo de notificações; zero `println`/`printStackTrace` em produção; clipboard usado só no fallback do share.
5. **Componentes mínimos:** sem ContentProviders próprios, sem FileProvider, sem receivers exportados próprios, sem WebView; `MainActivity` exported=true é o launcher (normal); o serviço de notificações exported=true é obrigatório pelo framework e protegido por permissão de sistema. Sem notificações emitidas pela própria app (conteúdo financeiro nunca aparece na status bar própria).
6. **Preferências moderadas** (`UserProfileStore`, `ContextTagsStore`, prefs de segurança): nome/foto/tema/método em SharedPreferences MODE_PRIVATE, sem dados financeiros; foto copiada para `filesDir/profile_photo.png` (privado) e URIs `content://` efémeros são descartados.
7. **Stubs de segurança coerentes:** `SessionLockPolicy` testado (testes unitários dedicados), estado inicial bloqueado em cold start, gate antes da composição de conteúdo.

---

## Mapa MASVS (v2, resumo)

| Domínio | Controlo | Estado |
|---|---|---|
| MASVS-STORAGE-1/2 (dados em repouso) | SQLCipher + chave Keystore AES-GCM; prefs sem dados financeiros; rawText não persistido | **Pass** (excepto asset A1 fora do cofre) |
| MASVS-CRYPTO-1/2 | AES-256-GCM, IV 12B aleatório, tag 128, SecureRandom, sem modos deprecados | **Pass** |
| MASVS-AUTH-1/2/3 (autenticação local) | Lock biométrico com re-lock 30s; mas fail-open API<28, sem credential fallback (A4) | **Partial** |
| MASVS-PLATFORM-1/2/3 (plataforma) | Permissões mínimas (1), zero providers/receivers exportados, sem WebView | **Pass** |
| MASVS-CODE-1/2 (qualidade/defesa) | Sem R8 no release; debug APK nunca distribuir; sem secrets no APK (excepto dados A1) | **Partial** |
| MASVS-RESILIENCE-1/2 (integridade) | N/A sem backend; validação de blob de chave presente; crash-loop pós-D2D (A12) | **Partial** |
| MASVS-PRIVACY-1/2 (privacidade) | Whitelist bancária, sem telemetria, sem rede; dados pessoais no APK/git (A1-A3) | **Partial** |
| MASVS-NETWORK-1..6 | N/A — sem INTERNET (mais forte que qualquer config TLS) | **Pass (N/A)** |

---

## Quick wins (top 5 ações, por esforço/impacto)

1. **Remover o asset com histórico financeiro pessoal e recompilar** — tira os movimentos financeiros reais do artefacto distribuível (A1). 5 minutos, maior redução de risco da lista.
2. **Limpar o histórico do git** (`git filter-repo` no ficheiro dos gastos) **antes de qualquer push**; nunca push sem verificação (A2).
3. **`window.setFlags(FLAG_SECURE)` na `MainActivity`** — task switcher/screenshots deixam de mostrar dados (A5). 2 linhas.
4. **Corrigir `BiometricLockGate`**: usar `androidx.biometric.BiometricPrompt` com `DEVICE_CREDENTIAL` e falhar fechado em API<28 (A4).
5. **`android:allowBackup="false"`** (ou `dataExtractionRules` a excluir `databases/` e `files/database.key`) — backups deixam de carregar a BD cifrada + blob, e some a interação com o crash-loop pós-D2D (A6/A12).

Bónus (10 min cada): trocar o fallback do `shareText` por clipboard com aviso (A8); apagar `scripts/__pycache__` do git (A13); agendar update SQLCipher 4.6.1→4.7.2 (A11).

---

*Auditoria estática read-only; nenhum ficheiro de código foi modificado. Análise de APK via `aapt2` (build-tools 9.3.1) sobre `app/build/outputs/apk/debug/app-debug.apk` (v2.2.0, versionCode 16, compilado 2026-11-01).*
