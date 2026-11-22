# FocusNest Security & Privacy Audit

- **Auditor Scope**: Static code, `AndroidManifest.xml`, cryptography, exported components, Intent/PendingIntent hygiene, WebView usage, data persistence, and supply chain workflows.

## 1. Network & Telemetry Posture
- **Zero-Internet Verification**: `app/src/main/AndroidManifest.xml` does **NOT** declare `android.permission.INTERNET` or `ACCESS_NETWORK_STATE`.
- **Trackers / Ads**: Zero third-party analytics, crash-reporting, advertising, or remote telemetry SDKs exist in `app/build.gradle.kts` or `gradle/libs.versions.toml`.
- **Browser Handoff**: External release links (`Check Releases on GitHub`) use standard `Intent(Intent.ACTION_VIEW, uri)` handed off to the system browser—no sockets or HTTP clients inside FocusNest.

## 2. Exported Components & IPC Audit
| Component | Exported | Protection / Permission | Risk Assessment & Finding |
| :--- | :---: | :--- | :--- |
| `MainActivity` | `true` | Launcher + `NDEF_DISCOVERED` (`application/vnd.focusnest.token`) | Low risk; singleTop NFC intent validates Tag UID safely against local Room vault. |
| `BlockedOverlayActivity` | `false` | Internal only (`launchMode="singleTop"`, `excludeFromRecents="true"`) | Safe; inaccessible to external third-party apps. |
| `FocusTimerService` | `false` | `foregroundServiceType="specialUse"` | **Medium (`FN-005`)**: Ensure foreground notification starts cleanly for all FGS entry actions (`ACTION_START`, `ACTION_RESUME`, `ACTION_SKIP_BREAK`, `ACTION_RESTART`). |
| `AppBlockerAccessibilityService` | `true` | `android.permission.BIND_ACCESSIBILITY_SERVICE` | Protected by OS signature permission; does not persist screen text, notifications content remotely, or keystrokes. |
| `FocusNestVpnService` | `true` | `android.permission.BIND_VPN_SERVICE` | **High (`FN-004`)**: Protected by OS `BIND_VPN_SERVICE` + `VpnService.prepare()`, **but** baseline calls `runBlocking` before `startForeground()` and lacks `ACTION_START` pre-prepare validation guard if invoked directly. Fix: add foreground notification immediately and strict state machine. |
| `FocusNotificationService` | `true` | `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | Protected by OS signature permission; logs app package + optional truncated title locally into Room (clearable anytime). |
| `FocusQuickTileService` | `true` | `android.permission.BIND_QUICK_SETTINGS_TILE` | Protected by OS permission; toggles quick blocker safely via DataStore. |
| `BootCompletedReceiver` | `true` | Broadcast receiver (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) | Safe; restores active persisted timer state only if running. Fix: filter strict expected actions (`if (action != BOOT_COMPLETED && action != MY_PACKAGE_REPLACED) return`). |
| Home Widgets (3 providers) | `true` | `APPWIDGET_UPDATE` (standard OS contract) | Safe; read-only local summaries. |

## 3. Cryptography & Backup Review
- **Algorithm**: `KeystoreAndBackupCrypto.encryptPortableBackup` uses AES-256-GCM (`AES/GCM/NoPadding`, 128-bit auth tag, random 12-byte IV, random 16-byte salt, PBKDF2WithHmacSHA256 at 60,000 iterations). **Pass** (solid portable encryption).
- **Backup Rules**: `android:allowBackup="false"` in `AndroidManifest.xml` prevents silent cloud ADB/Auto-Backup leakage of sensitive app usage or unlock records. Explicit user-initiated AES-256-GCM export is preferred.
- **QR Token Signing**: Uses SHA-256 over `"focusnest-qr|$tokenValue|$createdAtEpochMs"`. While effective against accidental QR payloads or tampering offline, for local storage it is tamper-evident; single-use & expiry checks enforced atomically before granting unlock.
- **Passcode Storage**: `emergencyPasscodeHash` stores unsalted SHA-256 hex. Recommend keeping backward compatibility with unsalted SHA-256 while noting low-entropy PIN considerations transparently in `SECURITY.md`.

## 4. WebView, PendingIntent & Logging Audit
- **WebView (`InsightsScreen.kt:426`)**: Used strictly offline via `loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)` to feed `PrintManager.print(...)`. Fix needed: ensure all dynamic user-supplied strings interpolated inside `buildPrintableReportHtml` (e.g., app names, session notes, intentions) are strictly **HTML-escaped** to eliminate local HTML/script injection inside the print document!
- **PendingIntent Mutability**:
  - Notification & timer intents correctly use `PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT`.
  - NFC foreground dispatch intentionally uses `PendingIntent.FLAG_MUTABLE` as required by Android NFC dispatch API—must target explicit component (`Intent(this, javaClass)`) and **already does** (`MainActivity` & `BlockedOverlayActivity`), which is Play-compliant.
- **Secrets**: Zero hardcoded tokens, API keys, or keystores in source. Keystore injection happens only at CI release build time via environment variables.

## 5. CI/CD Supply Chain Audit
- **Finding**: Baseline `.github/workflows/*.yml` referenced third-party actions using mutable major tags (`actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`, `actions/upload-artifact@v4`, `actions/configure-pages@v5`, `actions/upload-pages-artifact@v3`, `actions/deploy-pages@v4`, `softprops/action-gh-release@v2`).
- **Remediation**: Pin every third-party action to verified immutable commit SHAs with version comments and enable `dependency-review.yml` + `codeql.yml`.
