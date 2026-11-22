# FocusNest — Semantic Versioning Strategy

## 1. Principles
FocusNest adheres strictly to [Semantic Versioning 2.0.0](https://semver.org/):
`MAJOR.MINOR.PATCH-PRERELEASE`

### 1.1 Development Phase (Current Track)
- `0.1.0-alpha.1`: Initial development release with Android 11 compatibility, design system, and core stability.
- `0.1.0-alpha.2`, `0.1.0-alpha.3`: Subsequent feature and bugfix alpha increments.

### 1.2 Beta Phase
- `0.9.0-beta.1`: Feature-complete candidate ready for broad physical device testing.

### 1.3 Stable Production Phase
- `1.0.0`: The first production-grade stable release. Requires explicit maintainer sign-off. Never automatically triggered from alpha.

---

## 2. Android `versionCode` vs `versionName`

| Property | Rule | Current Setting |
| :--- | :--- | :--- |
| **versionName** | Public human-readable SemVer string | `0.1.0-alpha.1` |
| **versionCode** | Monotonically increasing positive integer | `4` |
| **minSdk** | Minimum supported Android API | `26` (Android 8.0) |
| **targetSdk** | Target Android API | `36` (Android 16) |

---

## 3. Single Source of Truth
`version.json` in the project root is the sole authoritative source of truth. Gradle build dynamically parses `version.json` at build time, and `node scripts/sync-version.js` validates all project metadata files.
