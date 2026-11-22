# Build Instructions (`BUILD.md`)

- **Application ID**: `com.aistudio.focusnest.fnwbx`
- **Namespace**: `com.example`
- **Version**: `1.2.0` (`versionCode = 2`)
- **Min SDK**: 26 (Android 8.0) | **Target / Compile SDK**: 36

## Local Gradle Commands

```bash
# Run full JVM & Robolectric test suite
gradle :app:testDebugUnitTest

# Run Android Lint & translation/permission verification
gradle :app:lintDebug

# Build Debug APK
gradle :app:assembleDebug

# Build Release APK & App Bundle (unsigned or signed via KEYSTORE_PATH env vars)
gradle :app:assembleRelease :app:bundleRelease
```

## Public CI/CD Pipelines (`.github/workflows/`)
- `android-ci.yml` — Runs unit/Robolectric tests, Android Lint, Zero-`INTERNET` manifest privacy check, Debug/Release APK & AAB builds on every PR and push.
- `release.yml` — Triggered on `v*` tags; runs verification, signs APK/AAB if `RELEASE_KEYSTORE_BASE64` secret is configured, generates `SHA256SUMS.txt`, and creates a GitHub Release.
- `deploy-pages.yml` — Publishes `/website` to GitHub Pages cleanly with zero external CDNs.
