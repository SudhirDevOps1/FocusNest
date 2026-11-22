# Build Instructions (`BUILD.md`)

- **Application ID**: `com.aistudio.focusnest.fnwbx`
- **Namespace**: `com.example`
- **Version**: `1.2.0` (`versionCode = 2`)
- **Min SDK**: 26 (Android 8.0) | **Target / Compile SDK**: 36
- **JDK**: **Java 17 or Java 21** (Robolectric suite targets Android SDK 34 for universal JDK 17 & 21 CI compatibility)

## Local Gradle Commands

```bash
# Run full JVM & Robolectric test suite
gradle :app:testDebugUnitTest

# Run Android Lint & translation/permission verification
gradle :app:lintDebug

# Build Debug APK
gradle :app:assembleDebug

# Build Release APK & App Bundle (signed via KEYSTORE_PATH env vars, or unsigned in CI without secrets)
gradle :app:assembleRelease :app:bundleRelease
```

## Public CI/CD Pipelines (`.github/workflows/`)
- `android-ci.yml` — Runs Zero-`INTERNET` manifest privacy gate, unit/Robolectric tests (`31` tests), Android Lint, and Debug + unsigned/signed Release APK/AAB verification on every PR and push.
- `website-ci.yml` — Validates `/website` HTML/JS/assets/links on PRs and deploys `/website` to GitHub Pages cleanly on `main` (zero external CDNs).
- `release-please.yml` — Automates Conventional Commit changelogs, `versionName`/`versionCode` bumps, and release PRs (`release-please-config.json`).
- `android-release.yml` — Triggered on `v*` tags; runs verification, signs APK/AAB if `RELEASE_KEYSTORE_BASE64` secret is configured (or packages `FocusNest-<tag>-release-unsigned.apk` + debug APK when unset), generates `SHA256SUMS.txt`, and publishes a GitHub Release.
- `codeql.yml`, `dependency-review.yml`, & `docs-check.yml` — Run static code scanning, dependency vulnerability/license checks, and documentation/icon/link integrity checks on PRs.
