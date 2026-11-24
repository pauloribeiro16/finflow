# Spec 04 — Network Allowlist, Updates & Notifications

## Objectivo
Garantir que a aplicação só comunica por HTTPS com domínios estritamente autorizados (zero envio silencioso de dados), que as atualizações OTA de APK só são instaladas após verificação de URL, assinatura oficial afixada e anti-downgrade, e que notificações bancárias pendentes têm retenção limitada a 30 dias.

## Ficheiros Cobertos
- `app/src/main/kotlin/com/financas/app/data/network/SafeHttp.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/UpdateSignatureCheck.kt` (`UpdateSignatureCheck`, `UpdateUrlPolicy`)
- `app/src/main/kotlin/com/financas/app/data/updater/AppUpdateManager.kt`
- `app/src/main/kotlin/com/financas/app/data/service/FinancialNotificationListenerService.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/PendingRetention.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/PendingFloodGuard.kt`

## Requisitos Funcionais e de Segurança (EARS)
1. **Allowlist HTTP única (`SafeHttp`):** WHEN qualquer pedido de API é feito via `SafeHttp.getText`, THE SYSTEM SHALL exigir `scheme == "https"`, `port == -1`, `rawUserInfo == null`, host pertencente a `ALLOWED_API_HOSTS` (`api.github.com`, `query1.finance.yahoo.com`), desativar redirects (`instanceFollowRedirects = false`), limitar resposta a `maxBytes` (512 KB por omissão) e fechar a ligação em `finally`.
2. **Política de URLs de atualização (`UpdateUrlPolicy`):** WHEN `AppUpdateManager.downloadUpdate` inicia, THE SYSTEM SHALL exigir URL inicial `https://github.com/pauloribeiro16/finflow/releases/download/...` sem porta explícita nem `..`; em cada redirect (máximo 5 hops) SHALL exigir HTTPS sem porta para `{github.com, objects.githubusercontent.com, release-assets.githubusercontent.com}`.
3. **Limite de tamanho de APK:** WHEN o APK é descarregado, THE SYSTEM SHALL abortar se `Content-Length` ou bytes lidos excederem `MAX_APK_BYTES`.
4. **Verificação criptográfica antes da instalação:** WHEN `installApk` é invocado, THE SYSTEM SHALL verificar: (a) mesmo `packageName` e `candidateVersionCode > installedVersionCode` (anti-downgrade), (b) digest SHA-256 presente em `RELEASE_CERT_SHA256`, e (c) interseção não vazia com os signatários da app instalada. Se qualquer verificação falhar, THE SYSTEM SHALL apagar o ficheiro APK e transitar para `UpdateUiState.Error`.
5. **Retenção e proteção de notificações:** WHEN notificações bancárias são capturadas, THE SYSTEM SHALL aplicar `PendingFloodGuard` e apagar pendentes com idade > 30 dias (`PendingRetention`).

## Acceptance Criteria
- [ ] GIVEN um URL `http://api.github.com/...`, `https://api.github.com:443/...`, `https://user@api.github.com/...` ou `https://api.github.com.evil.com/...` WHEN `SafeHttp.isAllowedUrl` é chamado THEN devolve `false`.
- [ ] GIVEN um URL de release com `..` no path ou redirect para host fora de `REDIRECT_HOSTS` WHEN `UpdateUrlPolicy` é avaliado THEN devolve `false` e o download é rejeitado.
- [ ] GIVEN um APK com `versionCode <= installedVersionCode`, pacote diferente, ou assinatura fora de `RELEASE_CERT_SHA256` WHEN `installApk` corre THEN o ficheiro APK é apagado e o intent de instalação não é lançado.

## Invariantes Formais (`formal/tla/UpdateInstallProtocol.tla`)
- `OnlyAllowlistedNetworkTargets`: Nenhum pedido inicial ou redirect acede a host não autorizado.
- `NoUntrustedOrDowngradeInstall`: O estado `Installed` só é atingível se `samePackage /\ higherVersion /\ officialCert /\ compatibleCert`.
- `RejectedApkDeleted`: Sempre que a verificação falha (`Error`), o ficheiro APK não permanece no cache.
- `RedirectLimitRespected`: O número de redirects seguidos nunca excede `MAX_REDIRECTS`.

## Fora de Âmbito
- Integridade do servidor GitHub caso a chave de assinatura de release seja comprometida externamente.

## Riscos e Questões Abertas
- `UpdateUrlPolicy.isAllowedReleaseUrl` e `isAllowedRedirect` não verificam `uri.rawUserInfo == null` (ao contrário de `SafeHttp.isAllowedUrl`). Documentado para avaliação/hardening futuro.
