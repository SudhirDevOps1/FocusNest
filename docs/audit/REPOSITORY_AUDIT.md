# FocusNest Repository Audit

- **Repository**: `https://github.com/SudhirDevOps1/FocusNest`
- **Owner / Developer**: Sudhir Kumar (`SudhirDevOps1`)
- **Audit Baseline**: Commit `d657cd4` on default branch `main` (local package `com.example`, `applicationId = "com.aistudio.focusnest.fnwbx"`, `versionCode = 2`, `versionName = "1.2.0"`)
- **Stabilization Branch**: `fix/production-hardening`

## 1. Git & GitHub State Summary
- **Default Branch**: `main` (2 commits: `4313331 Initial commit`, `d657cd4 feat: scaffold FocusNest application`).
- **Active Remote Branches**: `main` plus 11 automated `dependabot/*` branches (`#1`–`#11`).
- **Open Pull Requests**: PRs `#1`–`#11` created by Dependabot for Gradle & GitHub Actions bumps.
- **Releases & Tags**: None published yet (`0` releases, `0` tags).
- **GitHub Pages**: `has_pages: false` on baseline, causing `Deploy Showcase Website to GitHub Pages` (`45010337716`) to fail at `Configure GitHub Pages`.
- **Android CI**: `FocusNest Android CI` (`45010337701`) failed at `Run Unit & Robolectric Tests + Android Lint` because `FocusNestDatabaseAndRepoTest.kt:361` invoked non-existent `UnlockDao.getRecentHistory()` instead of `UnlockDao.getAllHistory()`.

## 2. Project & Build Architecture Inventory
- **Build System**: Gradle 9.3.1 (Kotlin DSL), AGP + Compose compiler plugin + KSP (`2.x`) + Secrets Gradle Plugin (`minSdk = 26`, `targetSdk = 36`, `compileSdk = 36.1`). Note: `gradle/wrapper/` stores `gradle-wrapper.properties`; environment executes `gradle` directly.
- **Manifest (`app/src/main/AndroidManifest.xml`)**:
  - Intentionally omits `android.permission.INTERNET` (zero-network guarantee).
  - Declares `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `NFC`, `CAMERA`, `ACCESS_NOTIFICATION_POLICY`.
  - Uses `<queries>` for `ACTION_MAIN` / `CATEGORY_LAUNCHER` instead of broad `QUERY_ALL_PACKAGES`.
  - Exposes exported system-bound services with mandatory system permissions (`BIND_ACCESSIBILITY_SERVICE`, `BIND_VPN_SERVICE`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `BIND_QUICK_SETTINGS_TILE`), 3 `AppWidgetProvider` receivers, `BootCompletedReceiver`, and unexported `BlockedOverlayActivity` + `FocusTimerService`.
- **Persistence Layer**:
  - Room database `focusnest_local.db` (v2) with 21 entities, 18 DAOs, and non-destructive `MIGRATION_1_2`.
  - Jetpack DataStore (`focusnest_preferences`) storing theme, accent palettes, Pomodoro config, Invincible Mode, ambient audio, launch-pause config, bedside wind-down state, and monotonic timer state.
- **Domain & Repositories**:
  - `UsageStatsRepository`, `BlockerRepository`, `FocusRepository`, `HabitRepository`, `InsightsAndBackupRepository`.
  - Cryptography in `KeystoreAndBackupCrypto` (AES-256-GCM + PBKDF2WithHmacSHA256 + SHA-256 token verification).
- **UI & Services**:
  - Jetpack Compose M3 single-activity shell (`MainActivity` extending `FragmentActivity` for `BiometricPrompt`) with 5 tabs (`Home`, `Focus`, `Blocker`, `Insights`, `More`) + `Habits` sub-route + `BlockedOverlayActivity` + `OnboardingScreen`.
  - Adaptive two-pane layouts (`AdaptiveTwoPane` and `MoreScreen` List-Detail at `720.dp+` with `NavigationRail`).
  - Background execution via `FocusTimerService` (+ `AmbientSoundEngine`), `AppBlockerAccessibilityService`, `FocusNestVpnService`, `FocusNotificationService`, `FocusQuickTileService`, and `FocusUsageMonitorWorker` (15-min WorkManager).
- **Website & Docs**:
  - Static showcase in `website/` (`index.html`, `app.js`, `styles.css`, `favicon.svg`) using placeholder `OWNER/FocusNest` links.
  - Root Markdown docs (`README.md`, `CHANGELOG.md`, `ARCHITECTURE.md`, etc.) and `docs/*.md` using placeholder `OWNER/FocusNest` links.

## 3. High-Priority Systemic Gaps Identified
1. Startup initialization race causing the 10-step Onboarding flow to appear on every cold launch before DataStore finishes loading.
2. Decentralized permission snapshots missing explicit runtime vs. special-access states, revocation alerts, and `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` / refresh feedback.
3. `OfflineQrBarcodeScannerDialog` Frame YUV stride bug + non-lifecycle-cleaned `ProcessCameraProvider` + missing flashlight toggle and hardware error UI.
4. `FocusNestVpnService` calling `runBlocking` before `startForeground()`, lacking a finite connection state machine, and missing a live Domain Verification & Diagnostics screen.
5. App blocker lacking dedicated modular engines (`RuleEvaluator`, `BlockingScheduler`, `BlockingStateManager`, `EnforcementMonitor`, `EmergencyAccessManager`) and `UsageEvents` fallback monitoring during focus sessions.
6. `UsageStatsRepository` failing to dynamically detect/exclude the active `CATEGORY_HOME` launcher, `com.android.systemui`, and active IME keyboards from default distracting-app rankings and totals.
7. Repository identity & CI hardening gaps (`OWNER/FocusNest` placeholders, unpinned workflow action tags, missing Release Please, missing `website-ci`, `codeql`, `dependency-review`, `docs-check`).
