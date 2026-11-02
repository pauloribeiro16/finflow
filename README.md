# Finflow

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Security-AES--256_SQLCipher-00C853?style=for-the-badge&logo=shield" alt="Security" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <b>Finflow</b> is an ultra-private, offline-first personal finance manager and wealth analytics application for Android.
  <br />
  Built from scratch with modern <b>Jetpack Compose</b>, <b>Room SQLite</b>, and military-grade <b>AES-256 SQLCipher encryption</b>.
</p>

<p align="center">
  <a href="#-quick-download-latest-apk"><b>Download Latest APK</b></a> •
  <a href="#-key-features"><b>Features</b></a> •
  <a href="#-architecture--tech-stack"><b>Architecture</b></a> •
  <a href="#-release-control--automation"><b>Release Control</b></a> •
  <a href="#-building-from-source"><b>Build Instructions</b></a>
</p>

---

## ⚡ Why Finflow?

Most personal finance apps require you to upload your sensitive bank credentials, transactions, and net worth to unknown cloud servers, subject you to targeted advertising, or charge monthly subscriptions.

**Finflow takes the opposite approach:**
- **Zero Cloud Tracking:** All data stays 100% on your device.
- **No Account Required:** Open the app and start immediately.
- **SQLCipher AES-256 Encryption:** Your database is encrypted at rest using industry-standard cryptography.
- **Biometric Security:** Fast biometric authentication (Fingerprint / Face Unlock).
- **Exact Financial Math:** Zero floating-point rounding errors. Monetary amounts are strictly handled in integer cents (`Long`).

---

## 📥 Quick Download (Latest APK)

You can download and sideload the verified, signed Android APK directly from GitHub Releases:

👉 **[Download the Latest Finflow APK](https://github.com/pauloribeiro16/finflow/releases/latest)**

Each release includes:
- `Finflow-vX.Y.Z.apk`: Standalone signed APK ready for installation on any Android phone (Android 8.0+ / API 26+).
- `Finflow-vX.Y.Z.aab`: Production Android App Bundle for Google Play Store.
- `SHA256SUMS.txt`: Cryptographic checksums to verify package integrity.

---

## 🚀 Key Features

### 1. 4-Dimensional Transaction Tracking
Every income and expense is categorized across four distinct dimensions:
1. **Category & Subcategory:** Where did the funds go? (e.g. *Food > Supermarket*, *Housing > Electricity*).
2. **Context / Entity:** Who was it for? (*Personal*, *Work / Business*, *Family*, *Vacation*).
3. **Payment Method:** How was it paid? (*Debit Card*, *Credit Card*, *Cash*, *Bank Transfer*, *MB WAY*).
4. **Classification:** Fixed recurrent obligations vs. variable everyday expenses.

### 2. Global Multi-Language (English & Portuguese)
- **English by Default:** Ready for global distribution out-of-the-box.
- **Instant Language Switcher:** Seamlessly switch between **English** and **Portuguese (PT)** anytime from the Account settings.
- Automatically localizes navigation, category seed lists, charts, and transaction dialogs.

### 3. Native Canvas Analytics (Zero Bloat)
- Interactive, GPU-accelerated charts rendered entirely with native Jetpack Compose `Canvas`.
- **Expense Breakdown:** Multi-colored radial and bar distributions with percentage of income.
- **Daily Spending Rhythm:** Visualize spending trends across the month.
- **Multi-Month Comparative:** Compare your current month against previous cycles.

### 4. Wealth & Portfolio Management
- Track asset allocations across **Emergency Funds**, **PPRs (Retirement Plans)**, **ETFs & Stocks**, and **Crypto**.
- Built-in tax optimization calculator for retirement accounts (e.g. Portuguese PPR Art. 21 EBF benefits).

### 5. Smart Automation & Prefill Engine
- Automatic merchant and category prefill based on your historical habits.
- **Monthly Recurring Generator:** Clone recurring rent, subscriptions, and salaries with a single tap.

### 6. Data Sovereignty & Portability
- Export your entire financial history anytime in standardized **JSON** and **CSV** formats.
- Offline encrypted backups ensure you are never locked into any ecosystem.

---

## 🏗️ Architecture & Tech Stack

Finflow adheres strictly to clean single-module Android architecture with lightweight manual dependency injection:

```text
app/src/main/kotlin/com/financas/app/
├── data/
│   ├── db/           # Room Database with SQLCipher AES-256, DAOs
│   ├── entity/       # TransactionEntity, CategoryEntity, InvestmentAssetEntity
│   ├── prefs/        # UserProfileStore, BiometricLockGate
│   ├── repository/   # FinanceRepository, BackupSerializer (JSON/CSV)
│   └── seed/         # Initial categories & subcategories seed
├── domain/
│   ├── model/        # Immutable models (MoneyCents, MonthlySummary, DimensionBreakdown)
│   └── usecase/      # MonthlyAnalyticsCalculator, RecurringMonthGenerator, SmartPrefillEngine
├── ui/
│   ├── i18n/         # Multi-language string catalogs (AppStrings, CategoryTranslator)
│   ├── theme/        # Ultra-Dark Theme palette, Typography, Elevation
│   ├── components/   # InteractiveCanvasCharts, CalculatorNumpad, MonthSelector
│   ├── navigation/   # Bottom Navigation: Home | Transactions | (+) | Analytics | Wealth | Account
│   └── screens/      # Jetpack Compose Screens & ViewModels
└── di/               # AppContainer (Lightweight manual DI with zero reflection overhead)
```

### Core Technologies
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3.
- **Language:** Kotlin 2.0 with Coroutines and StateFlow.
- **Database:** [Android Room](https://developer.android.com/training/data-storage/room) powered by [SQLCipher](https://github.com/sqlcipher/android-database-sqlcipher) (AES-256).
- **Navigation:** Multi-screen scaffold with sticky header and bottom bar navigation.

---

## 🛠️ Release Control & Automation

Finflow includes automated release scripts following senior DevOps practices:

### Local Release Build & Signature Check
```bash
# Builds signed Release AAB and APK, verifies crypto signatures, and prints SHA-256
./scripts/release.sh
```

### One-Command GitHub Release Publication
Publishes a new release directly to GitHub Releases with automatic tagging, release notes, and artifact uploads:
```bash
# Automatically detects version from build.gradle.kts or takes explicit argument:
./scripts/publish-release.sh 2.2.0 "Initial Global Release"
```

### GitHub Actions CI/CD
- **`ci.yml`**: Runs on every pull request and push to `main` to execute all unit tests and verify build compilation.
- **`release.yml`**: Triggers automatically on git tags (e.g., `v2.2.0`), builds the signed release APK and AAB, computes `SHA256SUMS.txt`, and creates the GitHub Release with direct download links.

---

## 💻 Building from Source

### Prerequisites
- **JDK 17** (Temurin 17 recommended)
- **Android SDK Platform 35** (`build-tools 35.0.0`)
- **Android Gradle Plugin 8.11+**

### Build Commands
```bash
# Clone the repository
git clone https://github.com/pauloribeiro16/finflow.git
cd finflow

# Run Unit Tests (< 5 seconds)
./gradlew :app:testDebugUnitTest

# Build Debug APK
./gradlew :app:assembleDebug

# Build Release APK
./gradlew :app:assembleRelease

# Install directly to a connected device via ADB
./gradlew :app:installDebug
```

---

## 📜 Privacy & Compliance

- **Privacy Policy:** See [`docs/PRIVACY_POLICY.md`](./docs/PRIVACY_POLICY.md).
- **Google Play Compliance:** See [`docs/PLAY_STORE_COMPLIANCE.md`](./docs/PLAY_STORE_COMPLIANCE.md).
- **Store Listing Copy:** See [`docs/STORE_LISTING.md`](./docs/STORE_LISTING.md).

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
