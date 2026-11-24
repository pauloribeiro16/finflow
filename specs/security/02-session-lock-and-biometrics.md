# Spec 02 — Session Lock, Biometrics & Privacy

## Objectivo
Proteger o acesso visual e ações sensíveis (exportação de dados, desmascarar valores em modo privacidade, alterar tempo de bloqueio) com autenticação biométrica/PIN do dispositivo e bloqueio automático configurável após tempo em segundo plano, prevenindo capturas de ecrã e tapjacking.

## Ficheiros Cobertos
- `app/src/main/kotlin/com/financas/app/core/security/SessionLockPolicy.kt`
- `app/src/main/kotlin/com/financas/app/ui/screens/account/BiometricLockGate.kt`
- `app/src/main/kotlin/com/financas/app/ui/screens/account/PrivacyModeRequest.kt`
- `app/src/main/kotlin/com/financas/app/MainActivity.kt`

## Requisitos Funcionais e de Segurança (EARS)
1. **Arranque a frio bloqueado:** WHEN `biometricsEnabled == true` e `MainActivity.onCreate` executa, THE SYSTEM SHALL iniciar com `sessionLocked = true` e não renderizar `MainScaffold` até autenticação bem-sucedida.
2. **Bloqueio por inatividade em background:** WHEN `MainActivity.onResume` ocorre após `onPause` com `biometricsEnabled == true`, THE SYSTEM SHALL bloquear a sessão (`sessionLocked = true`) se `elapsed < 0` (recuo de relógio), se `timeoutMs == 0` (imediato, fora de fluxo externo), ou se `elapsed > timeoutMs`.
3. **Janela de graça para fluxos externos:** WHEN um seletor de ficheiros / intent externo é esperado (`externalFlowExpected == true`), THE SYSTEM SHALL aplicar graça de `max(timeoutMs, 120_000 ms)` apenas nesse regresso e repor `externalFlowExpected = false` no `onResume`.
4. **Ações sensíveis autenticadas:** WHEN `biometricsEnabled == true` e o utilizador tenta desativar o modo privacidade, exportar dados (JSON/CSV) ou alterar o tempo de bloqueio, THE SYSTEM SHALL exigir autenticação via `BiometricLockGate.authenticate` antes de executar a ação.
5. **Proteção de janela:** WHEN `MainActivity` é criada em build de release (`!BuildConfig.DEBUG`), THE SYSTEM SHALL aplicar `FLAG_SECURE`; em todos os builds SHALL ativar proteção contra tapjacking (`setHideOverlayWindows(true)` em API ≥ 31, `filterTouchesWhenObscured = true` em API < 31).
6. **Normalização do timeout:** WHEN um valor de timeout é lido ou gravado, THE SYSTEM SHALL restringi-lo a `{0, 30_000, 60_000, 300_000}` ms (omissão `30_000` ms).

## Acceptance Criteria
- [ ] GIVEN `biometricsEnabled = true` e `timeoutMs = 30_000` WHEN a app regressa após `30_001 ms` sem fluxo externo THEN `SessionLockPolicy.shouldLock` devolve `true` e `sessionLocked` passa a `true`.
- [ ] GIVEN `biometricsEnabled = true` e `timeoutMs = 0` WHEN a app regressa após `1 ms` sem fluxo externo THEN `shouldLock` devolve `true`.
- [ ] GIVEN `elapsed < 0` (clock skew) e `biometricsEnabled = true` WHEN `shouldLock` é avaliado THEN devolve `true` (fail-closed).
- [ ] GIVEN `biometricsEnabled = true` e `externalFlowExpected = true` WHEN a app regressa após `60_000 ms` THEN `shouldLock` devolve `false` e `externalFlowExpected` é limpo para `false`.
- [ ] GIVEN `biometricsEnabled = true` e modo privacidade ativo WHEN o utilizador pede para mostrar valores THEN `requestPrivacyModeChange` exige autenticação antes de chamar `apply(false)`.

## Invariantes Formais (`formal/tla/SessionLockAuth.tla`)
- `NoContentWhenLocked`: `MainScaffold` nunca é visível enquanto `sessionLocked = TRUE`.
- `LockedAfterTimeoutExceeded`: Se `biometricsEnabled = TRUE` e o tempo em background excedeu o limite aplicável (ou `elapsed < 0`), ao regressar a foreground a sessão fica bloqueada.
- `ExternalGraceNotReusable`: Um `externalFlowExpected` consumido num regresso não concede graça a um background subsequente sem novo fluxo externo.
- `SensitiveActionRequiresAuth`: Com `biometricsEnabled = TRUE` e dispositivo seguro, nenhuma ação sensível (exportar / desmascarar) executa sem autenticação bem-sucedida.

## Fora de Âmbito
- Encriptação da BD ligada a `CryptoObject` biométrico (ver Spec 01).

## Riscos e Questões Abertas
- **Dispositivo sem PIN/biometria (`!isDeviceSecure`):** Em `BiometricLockGate.authenticate()`, se o utilizador remover o PIN do sistema depois de ativar o bloqueio na app, `!isDeviceSecure` faz fallback para `onSuccess()` (fail-open). Avaliado no modelo TLA+.
- **Flag `externalFlowExpected` órfã:** Se `expectExternalFlow()` for chamado mas o intent externo não pausar a Activity (ou falhar antes de `onPause`), a flag fica `true` até ao próximo background/resume real. Avaliado no modelo TLA+.
