# Android Platform Limitations & Release Process

## Known Android Constraints
- **Background Execution (OEMs / Realme UI / MIUI / OnePlus)**: Aggressive battery savers can suspend accessibility/foreground services; FocusNest surfaces `OemCompatibilityHelper` battery-unrestricted instructions directly inside Permissions.
- **Short-Form Reels/Shorts Detection**: Uses narrow accessibility view-ID checks while enabled and fails safely without blocking host apps if an upstream social app changes its layout IDs.
- **System Grayscale**: Third-party apps cannot toggle system-wide display grayscale without `WRITE_SECURE_SETTINGS` via ADB, so FocusNest directs users honestly to Android Digital Wellbeing Bedtime mode.
- **Local VpnService**: Does not inspect HTTPS TLS payloads; encrypted DNS-over-HTTPS (DoH) bypasses UDP/53 filtering.

## Build & Release Commands
- Unit & Robolectric Verification: `gradle :app:testDebugUnitTest`
- Debug APK Build: `gradle :app:assembleDebug`
- Release APK / Bundle: `gradle :app:assembleRelease` / `gradle :app:bundleRelease`
