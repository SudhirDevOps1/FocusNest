# Installation & Build Guide

## Option 1: Prebuilt GitHub Release
1. Visit [`https://github.com/OWNER/FocusNest/releases/latest`](https://github.com/OWNER/FocusNest/releases/latest) (or tap **More > Settings > Check Releases on GitHub** inside FocusNest).
2. Download the `.apk` artifact and optionally verify its SHA-256 hash against `SHA256SUMS.txt`.

## Option 2: Build from Source
### Prerequisites
- JDK 17+ and Android SDK (Compile SDK 36, Min SDK 26 / Android 8.0+)
- Gradle 9+

### Build Commands
```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

## First Launch
Install the generated APK from `app/build/outputs/apk/debug/` and walk through the 10-step onboarding flow to enable only the optional permissions you choose.
