---
name: verify-security
description: "Formally verify Finflow Android's security invariants (SQLCipher DB encryption/key lifecycle, biometric session lock, OTA APK update integrity, notification capture attack surface, and backup/local-file cryptography) using TLA+ (TLC) and executable Kotlin property tests. Use whenever editing files in core/security/, BiometricLockGate, PrivacyModeRequest, MainActivity, BackupCrypto, LocalFileCipher, SafeHttp, AppUpdateManager, UpdateSignatureCheck, FinancialNotificationListenerService, FinancialAppFilter, BankNotificationParser, PendingFloodGuard, or AppContainer.database."
---

# Verify Finflow Security (Formal Verification Skill)

This project-local skill checks Finflow Android's security guarantees across every reachable state in bounded TLA+ models (via TLC) and across randomized/adversarial inputs on the real Kotlin crypto and policy code.

## Sync Record

- **Sync commit:** `eb8dc12` (`feat(security): configurable lock delay, atomic restore and export auth`)
- **Tool versions:** TLC 2.19 (TLA+ model checker), JUnit 4 via `./gradlew :app:testProdDebugUnitTest`
- **Advance the sync commit ONLY after confirming that every covered file and function still matches its TLA+ model and Kotlin property test at the new commit.**

## Coverage Record

| Model / Suite | Covered Files & Functions | Invariants Checked |
|---|---|---|
| `formal/tla/DbEncryptionLifecycle.tla` | `di/AppContainer.kt` (`DefaultAppContainer.database`), `core/security/DatabaseKeyManager.kt`, `core/security/DatabaseEncryptionMigrator.kt`, `core/security/DatabaseRecovery.kt` | `NoPlaintextLeftAfterReady`, `KeyMatchesEncryptedDb`, `RecoveryNoticeOnQuarantine`, `NoSilentDataLoss` |
| `formal/tla/SessionLockAuth.tla` | `MainActivity.kt` (`onCreate`, `onPause`, `onResume`), `core/security/SessionLockPolicy.kt`, `ui/screens/account/BiometricLockGate.kt`, `ui/screens/account/PrivacyModeRequest.kt` | `ClockSkewAlwaysLocks`, `NormalTimeoutAlwaysLocks`, `NoFailOpenWhenDeviceUnsecured` |
| `formal/tla/UpdateInstallProtocol.tla` | `data/updater/AppUpdateManager.kt`, `domain/usecase/UpdateSignatureCheck.kt` (`UpdateSignatureCheck`, `UpdateUrlPolicy`) | `OnlyAllowlistedNetworkTargets`, `NoUntrustedOrDowngradeInstall`, `RejectedApkDeleted`, `RedirectLimitRespected` |
| `formal/tla/NotificationCaptureSecurity.tla` | `AndroidManifest.xml`, `data/service/FinancialNotificationListenerService.kt`, `domain/usecase/FinancialAppFilter.kt`, `domain/usecase/PendingFloodGuard.kt` | `NoSessionUnlockFromNotification`, `NoAutoConfirmWithoutUser`, `QueueBounded`, `NoSpoofedPackageAccepted`, `LegitBankNotStarvedByAttacker` |
| `formal/tla/BackupAtomicRestore.tla` | `ui/screens/account/AccountViewModel.kt` (`importJson`), `data/repository/BackupSanitizer.kt`, `data/repository/BackupSerializer.kt`, `data/repository/SettingsBackupManager.kt` | `NoPartialDbState`, `NoSettingsWithoutDbCommit`, `IdempotentTransactionCount` |
| `formal/tla/PortfolioMarketSync.tla` | `domain/usecase/PortfolioMarketSyncUseCase.kt`, `data/network/YahooFinanceQuoteService.kt`, `data/network/SafeHttp.kt` | `NoNetworkWithoutOptInAndValidTicker`, `NoCorruptedValueOnBadQuoteFxOrAnomaly` |
| `app/src/test/kotlin/com/financas/app/security/SecurityFormalPropertiesTest.kt` | `BackupCrypto`, `BackupPasswordPolicy`, `LocalFileCipher`, `DatabaseKeyManager.isValidBlobFormat`, `SessionLockPolicy`, `SessionBackgroundClock`, `SafeHttp`, `UpdateUrlPolicy`, `UpdateSignatureCheck`, `PendingFloodGuard`, `BankNotificationParser`, `FinancialAppFilter` | Round-trip, non-determinism, AAD/ciphertext tamper rejection, exact 60-byte key blob boundary, exhaustive timeout & arm-window matrix, adversarial URL/signature matrix, spoofing & overflow guards |
| `app/src/test/kotlin/com/financas/app/security/DomainAndSecurityInvariantsTest.kt` | `BackupSanitizer`, `MonthlyAnalyticsCalculator`, `NetExpenseCalculator`, `TransactionDeduplicator`, `XtbTickerMapper`, `PortfolioMarketSyncUseCase`, `TickerPolicy`, `MoneyCents`, `BackupSerializer.exportToCsv` | Sanitization, accounting conservation, linked refunds, multi-app/XTB deduplication, FX/anomaly rejection, privacy mode masking, CSV formula neutralization |

## When to Run

Before changing or after changing any file in the Coverage Record above, check whether the diff touches covered code:

```bash
git diff --name-only eb8dc12..HEAD
```

## How to Run

1. Run the combined verification script from the repo root:

```bash
chmod +x formal/verify.sh
./formal/verify.sh
```

2. Read every TLA+ result using the **`/statechart`** skill (passing the matching view in `formal/views/<Model>.view.mjs` and a fresh scratch `--out` directory). `/statechart` shows every invariant violation as its shortest path in plain words and draws the Mermaid statechart when the state space fits.

## Distinguishing a Real Bug from a Modeling Error

1. **Replay the counterexample against the real Kotlin code** step by step (by reading the referenced lines or writing a JUnit test).
2. **If the real code exhibits the trace:** it is a **real bug / finding**. Do NOT weaken the invariant or the model to make TLC pass. Report the finding or fix the product code, then re-verify.
3. **If the real code prevents one of the steps in the trace:** the TLA+ model is missing a guard from the code. Tighten the action guard in `formal/tla/<Model>.tla` with a comment pointing to the exact file and line, then re-run.

## Updating the Verification After Intentional Changes

1. Update the corresponding spec in `specs/security/` and TLA+ model in `formal/tla/` (plus `formal/views/` if states/actions changed).
2. Run `./formal/verify.sh` and inspect with `/statechart`.
3. Confirm each new or changed invariant can fail on a broken mutation of the code/model before trusting a pass.
4. Update the **Sync commit** at the top of this file to the new commit hash.

## Baseline Verification Status

All 4 TLA+ models (`DbEncryptionLifecycle`, `SessionLockAuth`, `UpdateInstallProtocol`, `NotificationCaptureSecurity`) and `SecurityFormalPropertiesTest` pass with zero invariant violations:
- `DbEncryptionLifecycle`: quarantines existing encrypted DB if `database.key` is missing before creating a new key.
- `SessionLockAuth`: fails closed when `!isDeviceSecure` and expires `SessionBackgroundClock.armExternalFlow` after `EXTERNAL_FLOW_ARM_WINDOW_MS` (10s) on pause.
- `NotificationCaptureSecurity`: `FinancialAppFilter` enforces exact `ALLOWED_PACKAGES` allowlist; `BankNotificationParser` truncates untrusted inputs and guards against `Long` overflow.
- `UpdateInstallProtocol`: all 5 invariants hold.
