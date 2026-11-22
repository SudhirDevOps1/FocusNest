# FocusNest Android Compatibility Audit (API 26–36 / Android 8–16+)

- **Configured SDKs**: `minSdk = 26` (Android 8.0), `targetSdk = 36` (Android 16), `compileSdk = 36.1`.

## 1. Version-by-Version Technical Matrix
| Android Version | API Level | Specific Constraints & Behaviors in FocusNest |
| :--- | :---: | :--- |
| **Android 8.0–8.1** | 26–27 | Requires notification channels (`CHANNEL_TIMER`, `CHANNEL_REMINDERS`, `CHANNEL_VPN`) and `startForegroundService()` -> `startForeground()` within 5s. `UsageStatsManager` works via `AppOpsManager.checkOpNoThrow`. |
| **Android 9** | 28 | Private DNS (DNS-over-TLS) introduced system-wide (`Settings > Network > Private DNS`). **Honest disclosure required**: if Private DNS / DoH is enabled on Android 9+, browser/system queries bypass plain UDP port 53 `FocusNestVpnService`. |
| **Android 10** | 29 | Background activity launches from services/receivers restricted (`SYSTEM_ALERT_WINDOW` or AccessibilityService exemption needed). FocusNest relies cleanly on `AccessibilityService` to launch `BlockedOverlayActivity`. `AppOpsManager.unsafeCheckOpNoThrow` used on API 29+. |
| **Android 11** | 30 | Package visibility filtering (`<queries>` required in `AndroidManifest.xml` for launcher discovery). Foreground service types tightened. Auto-revocation of unused permissions introduced for runtime grants (`CAMERA`, `POST_NOTIFICATIONS` later). Note: `CATEGORY_HOME` resolution requires querying launcher/home or `<queries>` declaration for `CATEGORY_HOME` too so default launcher detection works cleanly on API 30+! |
| **Android 12–12L** | 31–32 | ForegroundService start restrictions from background; exact alarm restrictions (FocusNest uses in-service monotonic `SystemClock.elapsedRealtime()` + WorkManager); `BiometricManager` strong/weak + device credential handling; Dynamic Material You color (`dynamicColorEnabled`). |
| **Android 13** | 33 | Runtime `POST_NOTIFICATIONS` permission required; typed `getParcelableExtra(key, Class)` API; CameraX & Photo rules; `ResolveInfoFlags.of(MATCH_ALL)` in `PackageManager.queryIntentActivities`. |
| **Android 14–15** | 34–35 | Mandatory `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` (declared for `FocusTimerService`, ensure `FocusNestVpnService` and `BlockedOverlayActivity` comply cleanly); Restricted Settings blocks sideloaded APK Accessibility/NotificationListener until user clicks "Allow restricted settings" in App Info (needs clear UI guidance!). |
| **Android 16** | 36 | Edge-to-edge enforcement (`enableEdgeToEdge()`, `WindowInsets.safeDrawing`); adaptive resizing / foldables (`AdaptiveTwoPane`, `NavigationRail`). |

## 2. OEM Background Killing & Specific Limitations
1. **Realme UI / ColorOS / OxygenOS (Android 11–15+)**: Aggressively kills background accessibility services & periodic workers unless "Allow background activity", "Auto-launch", and Recent Apps Lock are toggled by the user.
2. **Xiaomi MIUI / HyperOS**: Requires "Autostart", "No battery restrictions", and "Display pop-up windows while running in background" for `BlockedOverlayActivity` to appear reliably over other apps.
3. **Samsung One UI**: Device Care moves apps into "Sleeping apps" or "Deep sleeping apps" unless Battery is set to "Unrestricted".
4. **Sideloaded GitHub APKs on Android 13+**: Android disables Accessibility & Notification Listener toggles by default ("Restricted setting") for sideloaded APKs until unlocked from App Info (`⋮ -> Allow restricted settings`). Our `PermissionsSubPage` & `docs/` must explain this clearly.
5. **`<queries>` Manifest Gap Discovered**: `AndroidManifest.xml` declares `CATEGORY_LAUNCHER` inside `<queries>`, **but omits `CATEGORY_HOME`**. On Android 11+ (API 30+), `PackageManager.resolveActivity(Intent(ACTION_MAIN).addCategory(CATEGORY_HOME))` needs `<intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.HOME"/></intent>` in `<queries>` to reliably detect custom third-party launchers across all OEMs! We will add this to `AndroidManifest.xml` (no sensitive permission required).
