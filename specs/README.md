# Finflow Android — Spec-of-Specs

Living specifications for the Finflow Android application (`com.financas.app`).
Each spec defines observable requirements (EARS), acceptance criteria (`GIVEN / WHEN / THEN`), formal invariants, and links to code and verification suites.

> General repo conventions and build commands live in [`AGENTS.md`](../AGENTS.md) and are not duplicated here.

## Security Specifications (Priority 1 — Formally Verified)

| Spec | Scope | Key Code | Formal Verification |
|---|---|---|---|
| [01 — Database Encryption & Key Lifecycle](./security/01-database-encryption-and-key-lifecycle.md) | SQLCipher AES-256 DB, Android Keystore master key, plaintext→encrypted migration, quarantine recovery | `DatabaseKeyManager`, `DatabaseEncryptionMigrator`, `DatabaseRecovery`, `DefaultAppContainer` | TLA+ [`DbEncryptionLifecycle.tla`](../formal/tla/DbEncryptionLifecycle.tla) + Kotlin `SecurityFormalPropertiesTest` |
| [02 — Session Lock, Biometrics & Privacy](./security/02-session-lock-and-biometrics.md) | Cold-start lock, configurable background timeout, external-flow grace, export/privacy auth, `FLAG_SECURE`, tapjacking | `SessionLockPolicy`, `BiometricLockGate`, `MainActivity`, `PrivacyModeRequest` | TLA+ [`SessionLockAuth.tla`](../formal/tla/SessionLockAuth.tla) + Kotlin `SecurityFormalPropertiesTest` |
| [03 — Backup Encryption, Restore & Local Files](./security/03-backup-encryption-and-restore.md) | `.finflow` PBKDF2+AES-GCM envelope, password strength policy, atomic restore, `LocalFileCipher` (`FFLF1`) | `BackupCrypto`, `BackupPasswordPolicy`, `BackupSanitizer`, `BackupSerializer`, `LocalFileCipher` | Kotlin `SecurityFormalPropertiesTest` |
| [04 — Network Allowlist & Update Integrity](./security/04-network-and-update-integrity.md) | `SafeHttp` host allowlist, APK redirect/signature/anti-downgrade gate | `SafeHttp`, `UpdateUrlPolicy`, `UpdateSignatureCheck`, `AppUpdateManager` | TLA+ [`UpdateInstallProtocol.tla`](../formal/tla/UpdateInstallProtocol.tla) + Kotlin `SecurityFormalPropertiesTest` |
| [05 — Notification Capture Security](./security/05-notification-capture-security.md) | `NotificationListenerService` isolation, package spoofing, flood guard, parser bounds | `FinancialNotificationListenerService`, `FinancialAppFilter`, `BankNotificationParser`, `PendingFloodGuard` | TLA+ [`NotificationCaptureSecurity.tla`](../formal/tla/NotificationCaptureSecurity.tla) + Kotlin `SecurityFormalPropertiesTest` |

## Core Functional Specifications

| Spec | Scope | Key Code | Verification |
|---|---|---|---|
| [06 — Transactions & Monthly Analytics](./core/06-transactions-and-analytics.md) | 4-dimension ledger, `MoneyCents`, monthly summary, net expenses, trends, calendar, projection, smart prefill, deduplication | `domain/model/*`, `domain/usecase/Monthly*`, `SmartPrefillEngine`, `TransactionDeduplicator` | JVM unit tests (≥95% `domain/` coverage gate) |
| [07 — Budgets, Goals & Investments](./core/07-budgets-goals-and-investments.md) | Category budgets, savings goals, portfolio valuation, XTB report import/sync, opt-in Yahoo Finance quotes | `BudgetProgressCalculator`, `GoalProgressCalculator`, `InvestmentPortfolioCalculator`, `Xtb*`, `PortfolioMarketSyncUseCase` | JVM unit tests (≥95% `domain/` coverage gate) |

## Verification Entry Point

Run all formal checks (TLA+ TLC model checks + Kotlin security property tests):

```bash
./formal/verify.sh
```

See also the project-local skill [`.agents/skills/verify-security/SKILL.md`](../.agents/skills/verify-security/SKILL.md).
