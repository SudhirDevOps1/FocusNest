# FocusNest — Release System Audit & Historical Inventory

## 1. Inventory of Pre-Existing Releases & Tags
Before performing any tag cleanup or version reset, a complete immutable record of all pre-existing project releases and assets has been recorded:

### Historical Release Inventory
- **Tag `v1.3.0`**:
  - *Published*: 2026-11-22T13:16:07Z
  - *Title*: `v1.3.0`
  - *Associated Assets*:
    - `FocusNest-v1.3.0-release.apk` (18.42 MB)
    - `FocusNest-v1.3.0-debug.apk` (18.84 MB)
    - `FocusNest-v1.3.0-bundle.aab` (17.20 MB)
    - `SHA256SUMS.txt`
  - *Release Type*: Simple / Merged Release Please PR #14

---

## 2. Release Please Configuration & Automation
- **Action**: `googleapis/release-please-action@v4`
- **Config**: `release-please-config.json`
- **Manifest**: `.release-please-manifest.json`
- **Default Branch**: `main`
- **Versioning Strategy**: Semantic Versioning with Alpha/Beta prerelease tracking.

---

## 3. Version Progression Rules
1. **Initial Development Target**: `0.1.0-alpha.1`
2. **VersionCode Safety**: Because versions up to `v1.3.0` (with `versionCode: 3`) have been built, the `versionCode` for `0.1.0-alpha.1` is set to `4` to prevent `INSTALL_FAILED_VERSION_DOWNGRADE` for existing physical installs.
3. **Automated Synchronization**: `scripts/sync-version.js` updates and verifies:
   - `version.json`
   - `app/build.gradle.kts`
   - `.release-please-manifest.json`
   - `metadata.json`
   - `BUILD.md`
   - `README.md`
   - `website/index.html`

---

## 4. Safe Release Cleanup & Backup Verification
Following the Section 10.2 protocol, obsolete release `v1.3.0` was formally inventoried and backed up before removal:
- **Backup File**: `docs/releases-backup/v1.3.0.json` (SHA-256 digests, asset download URLs, release body).
- **Cleanup Action**: Executed `gh release delete v1.3.0 --cleanup-tag -y`.
- **Verification**: Confirmed `gh release list` and `git ls-remote --tags origin` report 0 releases and 0 tags.
- **Next Release Target**: `v0.1.0-alpha.1` with `versionCode: 4`.
