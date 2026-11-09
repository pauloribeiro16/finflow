# Auditoria de Segurança — Finflow v2.5.2 (2026-11-09)

**Tipo:** revisão estática (código, manifesto, build, CI/CD, histórico git), estilo OWASP MASVS.
**Alvo:** `com.financas.app` v2.5.2 (versionCode 27), minSdk 26 / targetSdk 35.
**Substitui** o `SECURITY_AUDIT.md` de 2026-11-01 (v2.2.0), que já não é válido em pontos centrais: a app passou a ter permissão `INTERNET`, um auto-updater que descarrega e instala APKs (`REQUEST_INSTALL_PACKAGES`) e o repositório `pauloribeiro16/finflow` é **público**.

**Modelo de ameaça:** (1) outra app instalada no telemóvel; (2) alguém com o telemóvel na mão (desbloqueado ou que sabe o PIN); (3) a cadeia de distribuição (GitHub Releases → updater); (4) o repositório público; (5) cópias do dispositivo (troca de telemóvel, root).

---

## Resumo

A base é sólida: BD cifrada com SQLCipher e chave aleatória protegida pelo Android Keystore, só a activity principal é exportada, `FileProvider` restrito, `PendingIntent` imutável, `allowBackup=false`, `FLAG_SECURE` em release, sem segredos nem dados pessoais no histórico git (verificado).

Os problemas principais estão **à volta** do cofre cifrado:

1. Cada atualização grava automaticamente **um backup completo, sem cifra, em Downloads**.
2. O bloqueio biométrico pode ser **contornado mudando a hora do telemóvel**, e pode ser desligado sem voltar a autenticar.
3. A **chave de assinatura** fica exposta, no workflow de release, a uma GitHub Action de terceiros não fixada. É a única defesa do updater.
4. A **política de privacidade** diz "100% offline / sem servidores externos", mas a app contacta `api.github.com`.

Não há nenhuma vulnerabilidade explorável remotamente sem interação.

| ID | Sev. | Título | Onde |
|----|------|--------|------|
| S1 | **Alta** | Backup completo em claro gravado em Downloads (automático a cada atualização) | `AppUpdateManager.kt:76-85`, `ShareUtils.kt:31-55`, `AccountScreen.kt:809,825` |
| S2 | **Média** | Bloqueio biométrico contornável alterando o relógio do sistema | `MainActivity.kt:112,125` |
| S3 | **Média** | Biometria: desligar não pede autenticação; ligar não verifica se há biometria inscrita (risco de ficar trancado fora dos dados) | `AccountViewModel.kt:177-180`, `BiometricLockGate.kt:45-86` |
| S4 | **Média** | Workflow de release expõe keystore + passwords a action de terceiros não fixada; injeção via `workflow_dispatch` | `.github/workflows/release.yml:31,52,97` |
| S5 | **Média** | Updater: verificação de assinatura "falha aberta", sem verificação de package name nem de host | `AppUpdateManager.kt:90-94,164-177`, `UpdateSignatureCheck.kt:5-8` |
| S6 | **Média** | Transferência entre dispositivos (Android 12+) não bloqueada → app crasha no arranque no telemóvel novo | `AndroidManifest.xml` (sem `dataExtractionRules`), `DatabaseKeyManager.kt:19-22` |
| S7 | **Média** | Política de privacidade e notas de release desatualizadas face ao uso de rede | `docs/PRIVACY_POLICY.md:5,12`, `scripts/publish-release.sh` |
| S8 | Baixa | Filtro de apps financeiras por substring (`bank`, `banco`, `ticket`, `wise`, `ctt`…) permite injetar movimentos pendentes falsos | `FinancialAppFilter.kt:5-54` |
| S9 | Baixa | Imports sem limites: JSON sem limite de tamanho/profundidade (crash por `StackOverflowError`), foto sem limite (OOM) | `AccountScreen.kt:186-270`, `BackupSerializer.kt` (parser) |
| S10 | Baixa | Bloqueio só na UI: não está ligado à chave da BD; flag em `shared_prefs` em claro | `BiometricLockGate.kt:13-25`, `DatabaseKeyManager.kt:56-70` |
| S11 | Baixa | Export CSV sem neutralizar fórmulas (`=`, `+`, `-`, `@`) | `BackupSerializer.kt:453-459` |
| S12 | Baixa | Release sem R8 e fallback silencioso para assinatura debug em builds locais | `app/build.gradle.kts:66,74` |
| S13 | Baixa | Partilha/clipboard do backup sem marca de sensível; Android 8–9 cai sempre na partilha | `ShareUtils.kt:12-32` |
| S14 | Info | SQLCipher 4.6.1 desatualizado; `gradle-wrapper` sem `distributionSha256Sum`; APKs antigos ficam em `cache/updates` | `libs.versions.toml:13`, `gradle-wrapper.properties` |

---

## Estado das correções (2026-11-09)

| ID | Estado | O que mudou |
|----|--------|-------------|
| S1 | Corrigido | Backup pré-atualização vai para `filesDir/backups` (privado, últimos 3). Exports manuais usam `ACTION_CREATE_DOCUMENT`; nada é gravado em Downloads sem escolha do utilizador. |
| S2 | Corrigido | `SystemClock.elapsedRealtime()`; tempo negativo bloqueia. Testado no emulador com a hora recuada. |
| S3 | Corrigido | Ligar/desligar exige autenticação; ligar exige bloqueio de ecrã; fallback para PIN/padrão do telemóvel (API 28–35). |
| S4 | Corrigido | Actions fixadas por SHA, `softprops` substituída por `gh release create`, keystore apagado após o build, input validado por regex, verificação do certificado antes de publicar, validação do Gradle wrapper. |
| S5 | Corrigido | Certificado de release fixado (`8104…92a5`), verificação falha fechada, mesmo package + versionCode maior, URL só do repo no GitHub e redirects só para hosts do GitHub, limite de 200 MB, APKs antigos limpos. |
| S6 | Corrigido | `dataExtractionRules` + `fullBackupContent` excluem tudo; se a chave do Keystore faltar, os ficheiros ilegíveis são isolados e a app arranca vazia com aviso para restaurar backup. |
| S7 | Corrigido | Política de privacidade e textos da app deixam de dizer "100% offline"; verificação automática de atualizações pode ser desligada (Conta → Versão). |
| S8 | Corrigido | Correspondência por segmento do package e só marcas específicas; removidos `bank`, `banco`, `caixa`, `ctt`, `ticket`. |
| S9 | Corrigido | Limite de 20 MB nos imports, profundidade JSON máx. 32, foto reduzida (máx. 1024 px, 15 MB). |
| S10 | Risco aceite | Ligar a chave da BD à biometria obrigaria a autenticar para qualquer acesso (incluindo a captura de notificações em segundo plano). Fica documentado. |
| S11 | Corrigido | Valores começados por `= + - @ \t \r` são prefixados com `'` (números negativos mantêm-se). |
| S12 | Corrigido | R8 + shrinkResources ativos (APK 34,6 → 9,7 MB); build de release falha sem chave em vez de usar a debug. |
| S13 | Corrigido | Exports já não caem na partilha/clipboard; clipboard restante marcado como sensível. |
| S14 | Parcial | SQLCipher 4.6.1 → 4.17.0 (versões ≥ 4.18 exigem compileSdk 36/37); checksum do Gradle wrapper adicionado. |

Verificação: 509 testes unitários (prod e demo), lint sem erros, e no emulador: v2.5.2 publicada → nova build por cima com dados preservados, export via seletor, ativar bloqueio com PIN, desbloqueio com PIN, bloqueio com hora recuada.

---

## Detalhe

### S1 (Alta) — Backup completo em claro em Downloads

`AppUpdateManager.downloadUpdate()` chama `saveToDownloads()` com o export JSON completo (todos os movimentos, investimentos, metas, nome, foto de perfil em base64, definições) **antes de cada atualização**, sem pedir nada ao utilizador. O ficheiro chama-se `Finflow_backup_antes_de_atualizar_<tag>.json`. Os exports manuais (JSON e CSV) fazem o mesmo. Os ficheiros acumulam-se e nunca são apagados.

**Impacto:** anula a cifra SQLCipher. `Downloads` é armazenamento partilhado: fica acessível a gestores de ficheiros e a qualquer app com "Acesso a todos os ficheiros" (`MANAGE_EXTERNAL_STORAGE`), via USB/PC, a apps de sincronização que fazem upload de pastas (Drive, OneDrive, Syncthing) e a quem pegar no telemóvel desbloqueado.

**Correção:**
- Guardar o backup pré-atualização em `filesDir` (privado). A atualização por cima mantém os dados, por isso o backup público não é necessário.
- Se for preciso um backup exportável, cifrá-lo com uma password escolhida pelo utilizador (PBKDF2/Argon2 + AES-GCM) e usar `ACTION_CREATE_DOCUMENT` para o utilizador escolher o destino.
- Apagar os backups automáticos antigos.

### S2 (Média) — Bypass do bloqueio pelo relógio

`onPause` guarda `System.currentTimeMillis()` e `onResume` calcula `elapsed = agora − então`. Quem tiver o telemóvel (desbloqueado, ou sabendo o PIN, que não dá acesso à impressão digital) pode desligar a "hora automática" e recuar o relógio para logo depois do momento em que a app foi minimizada. `elapsed` fica < 30 s (ou negativo) e `shouldLock` devolve `false`, por isso a app abre sem biometria.

**Correção:** usar `SystemClock.elapsedRealtime()` (monotónico, não se altera nas definições) e tratar `elapsed < 0` como "bloquear".

### S3 (Média) — Gestão da biometria

- `toggleBiometrics(false)` desliga o bloqueio sem pedir biometria. Se alguém apanhar a app aberta (janela de 30 s, ou S2), desliga-o de vez.
- `toggleBiometrics(true)` não verifica `BiometricManager.canAuthenticate()`. Se a biometria for ligada sem impressão digital inscrita, ou se as impressões forem removidas mais tarde, o `BiometricPrompt` dá erro e `onError = { }` não faz nada. Não existe fallback para o PIN do telemóvel, e a única saída é "Limpar dados", que **destrói a BD cifrada**.

**Correção:** pedir autenticação antes de desligar. Validar `canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` antes de ligar. Usar `androidx.biometric` com `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` para permitir o PIN como alternativa.

### S4 (Média) — Cadeia de release

- O keystore e as passwords são escritos em disco (`finflow-release.keystore`, `keystore.properties`) no job que depois corre `softprops/action-gh-release@v2`. É uma action de terceiros referenciada por **tag móvel** e corre com `contents: write`. Se essa action (ou a tag) for comprometida, pode ler os dois ficheiros e roubar a chave. Com a chave, um atacante publica "atualizações" que passam a verificação da app **e** a do Android.
- `TAG="${{ github.event.inputs.tag_name }}"` interpola um input diretamente na shell (script injection). Só quem tem permissão de escrita no repo consegue disparar o workflow, por isso o risco é baixo.

**Correção:** fixar todas as actions por SHA de commit (ou substituir a action pelo `gh release create`, que já vem no runner). Separar o build assinado e a publicação em jobs diferentes, apagando keystore e properties no fim do passo de build. Passar o input por `env:` (`TAG: ${{ inputs.tag_name }}`) e validá-lo com regex `^v[0-9]+\.[0-9]+\.[0-9]+$`. Ativar a proteção de tags `v*` e 2FA na conta GitHub. Guardar uma cópia offline da chave: se se perder, deixa de ser possível atualizar por cima.

### S5 (Média) — Robustez do updater

- `isSignatureCompatible` devolve `true` em API < 28, em qualquer exceção e quando a lista de assinaturas está vazia (APK que não se consegue ler). É uma verificação que "falha aberta".
- O `packageName` do APK descarregado não é comparado com o da app. Um APK de outro pacote passaria para o instalador do sistema como uma app nova.
- O `browser_download_url` vem do JSON e é usado sem verificar o host; `instanceFollowRedirects = true`.
- Não há verificação de hash, apesar de o release publicar `SHA256SUMS.txt`.

**Mitigação existente:** HTTPS e, no Android, uma app instalada só pode ser substituída por um APK assinado com a mesma chave. O risco real depende sobretudo de S4.

**Correção:** falhar fechado (`false` em exceção ou lista vazia, e em API < 28 usar `GET_SIGNATURES`). Exigir `candidate.packageName == context.packageName` e `versionCode` maior. Aceitar só `https://github.com/pauloribeiro16/finflow/releases/download/…` (os redirects vão para `objects.githubusercontent.com`). Melhor ainda: fixar na app o SHA-256 do certificado de release e compará-lo diretamente. Apagar `cache/updates` depois de instalar.

### S6 (Média) — Transferência entre dispositivos

Com targetSdk ≥ 31, `allowBackup="false"` desativa o backup na cloud **mas não** a transferência direta entre telemóveis (D2D). A BD cifrada e o `database.key` são copiados para o telemóvel novo, mas a chave do Keystore não é (não sai do hardware). `getOrCreateMasterKey()` cria então uma chave nova, `unwrap` falha com `AEADBadTagException` e a app crasha a cada arranque. `shared_prefs` e a foto (em claro) também são copiados.

**Correção:** acrescentar `android:dataExtractionRules` que exclua `database`, `sharedpref`, `file` de `cloud-backup` e de `device-transfer`. Se o `unwrap` falhar, mostrar um ecrã de recuperação ("restaurar backup / começar de novo") em vez de crashar.

### S7 (Média) — Conformidade / transparência

`PRIVACY_POLICY.md` afirma "100% offline-first" e "Sem Servidores Externos". As notas de release dizem "no third-party servers". A app contacta `api.github.com` e `objects.githubusercontent.com`, o que expõe o IP, o User-Agent `Finflow-App-Android` e a frequência de utilização. Isto é relevante para o RGPD e para o formulário Data Safety da Play Store.

**Correção:** atualizar a política (quem é contactado, quando, que dados), tornar a verificação automática opcional e corrigir o `SECURITY_AUDIT.md` antigo, que diz "sem INTERNET".

### S8 (Baixa) — Filtro de notificações por substring

`isFinancialApp` aceita qualquer pacote que contenha `bank`, `banco`, `caixa`, `ticket`, `wise`, `ctt`, `curve`, `bpi`… Por exemplo, uma app maliciosa `com.x.bankfake` ou outras legítimas (`…ticketmaster`, `…otherwise…`). Qualquer uma pode publicar notificações que entram como movimentos pendentes (spam, ou movimentos falsos que o utilizador pode aceitar por engano no "Confirmar Todos").

**Correção:** lista explícita de package names exatos (`pt.cgd.caixadirecta`, `com.revolut.revolut`, `pt.sibs.android.mbway`…) e substring só como opção manual.

### S9 (Baixa) — Imports sem limites

O `readText()` de JSON/CSV não tem limite de tamanho. O parser JSON próprio é recursivo e não tem limite de profundidade: um ficheiro com milhares de `[` provoca `StackOverflowError`, que o `catch (e: Exception)` não apanha, e a app crasha. A foto de perfil é copiada e descodificada sem limite (OOM). O risco vem de um ficheiro enviado por terceiros.

**Correção:** limitar o tamanho (por exemplo 20 MB), limitar a profundidade (por exemplo 32), usar `inSampleSize` no decode da foto e apanhar `Throwable` na fronteira de import.

### S10 (Baixa) — Bloqueio desligado da cifra

A chave do Keystore não usa `setUserAuthenticationRequired`, e a biometria não usa `CryptoObject`. Com root/Frida, ou mudando `biometrics_enabled` em `shared_prefs`, o bloqueio desaparece. O nome, a foto e as definições também ficam fora da BD cifrada. Para uma app pessoal é aceitável, mas a limitação deve ficar documentada.

### S11 (Baixa) — Injeção de fórmulas no CSV

`escapeCsv` só trata `;`, `"` e quebras de linha. Um comerciante `=HYPERLINK(...)` introduzido manualmente é executado ao abrir o ficheiro no Excel. Nos comerciantes vindos de notificações, a regex já limita os caracteres a letras, dígitos e `& ' . -`, o que reduz o risco.

**Correção:** prefixar com `'` os valores que começam por `= + - @ \t \r`.

### S12 (Baixa) — Build de release

- `isMinifyEnabled = false` e não existe `proguard-rules.pro`.
- Sem `keystore.properties` nem variáveis de ambiente, o `release` é assinado **silenciosamente com a chave debug** (`initWith(debug)`). O CI protege-se disso, mas `scripts/publish-release.sh` não: o `jarsigner -verify` aceita a assinatura debug.

**Correção:** fazer o build falhar se faltar a chave em tarefas `*Release`. Ativar R8 com regras para Room/SQLCipher.

### S13 (Baixa) — Partilha / clipboard

Em Android 8–9, `saveToDownloads` devolve sempre `false` e o backup vai para a folha de partilha como texto (qualquer app recetora fica com os dados). Se a partilha falhar, o backup vai para o clipboard sem `ClipDescription.EXTRA_IS_SENSITIVE`.

**Correção:** marcar o clipboard como sensível ou remover esse fallback, e usar `ACTION_CREATE_DOCUMENT`.

### S14 (Info)

- SQLCipher 4.6.1 → atualizar para a versão 4.x mais recente.
- Acrescentar `distributionSha256Sum` ao `gradle-wrapper.properties` e a action `gradle/actions/wrapper-validation` ao CI.
- Higiene local: o keystore, as passwords (`keystore.properties`) e os ficheiros `Gastos*.xlsx/.json` estão na pasta do projeto no Desktop. O `.gitignore` cobre-os (verificado com `git check-ignore`), mas convém confirmar que o Desktop não sincroniza com o iCloud e manter uma cópia offline cifrada do keystore.

---

## Boas práticas confirmadas

- **Cifra da BD:** SQLCipher, passphrase de 32 bytes `SecureRandom`, protegida com AES-256-GCM numa chave não exportável do Keystore. Migração plaintext→cifrado verificada por contagem.
- **Superfície IPC mínima:** só `MainActivity` (launcher) exportada. O listener de notificações está protegido por `BIND_NOTIFICATION_LISTENER_SERVICE`. O `FileProvider` não está exportado e só expõe `updates/`.
- `PendingIntent.FLAG_IMMUTABLE`. Sem deep links nem `WebView`.
- Rede só por HTTPS (targetSdk 35 bloqueia cleartext por omissão).
- Queries Room parametrizadas e migrações com SQL estático, sem risco de SQL injection.
- O texto bruto das notificações não é persistido e os logs não têm dados.
- `allowBackup=false` e `FLAG_SECURE` em release.
- O histórico git do repo público está limpo: sem keystore, passwords, tokens nem ficheiros de dados pessoais (`git log --all` verificado).
- O CI recusa publicar um release sem os segredos de assinatura.

## Prioridade sugerida

1. **S1** — tirar o backup automático de Downloads (pequena mudança, maior ganho).
2. **S2 + S3** — `elapsedRealtime`, autenticar antes de desligar a biometria, fallback para o PIN.
3. **S4 + S5** — fixar as actions por SHA, isolar o keystore, fixar na app o certificado de release e falhar fechado.
4. **S6 + S7** — `dataExtractionRules`, ecrã de recuperação, política de privacidade.
5. Restantes (S8–S14).
