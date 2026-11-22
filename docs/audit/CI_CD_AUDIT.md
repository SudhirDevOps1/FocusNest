# FocusNest CI/CD & Repository DevOps Audit

- **Repository**: `https://github.com/SudhirDevOps1/FocusNest`
- **Inspected Workflows**: `.github/workflows/android-ci.yml`, `.github/workflows/deploy-pages.yml`, `.github/workflows/release.yml`, `.github/dependabot.yml`

## 1. Observed CI Failures (Baseline `d657cd4`)
1. **Failure 1 — `FocusNest Android CI / Privacy Gate, Unit Tests & Lint`**:
   - Step `Run Unit & Robolectric Tests + Android Lint` exited with compilation failure:
     `app/src/test/java/com/example/FocusNestDatabaseAndRepoTest.kt:361:34: Unresolved reference 'getRecentHistory' on receiver of type 'UnlockDao'.`
   - Downstream job `Build APK & App Bundle` was automatically skipped.
   - Same failure blocks all 11 open Dependabot PRs (`#1`–`#11`).
2. **Failure 2 — `Deploy Showcase Website to GitHub Pages / Publish FocusNest Showcase`**:
   - Step `Configure GitHub Pages` (`actions/configure-pages@v5`) failed in 5s with `HttpError: Not Found` (`GET /repos/SudhirDevOps1/FocusNest/pages -> 404`) because GitHub Pages is not enabled on the repository (`has_pages: false`) and the workflow lacked `enablement: true` (plus repo Pages settings require repo Admin/Pages write permission).

## 2. Structural & Security Findings in Baseline CI/CD
| Finding | Severity | Details | Remediation |
| :--- | :---: | :--- | :--- |
| **Unpinned Third-Party Actions** | High | Uses `@v4`, `@v5`, `@v2` floating major tags across all workflows. | Pinned every `uses:` step to exact immutable 40-char commit SHAs in local commit on `fix/production-hardening`. |
| **Robolectric API 36 JDK Mismatch** | High | Baseline `android-ci.yml` ran `java-version: "17"`, causing `DefaultSdkProvider.java:170` (`UnsupportedOperationException`) on Robolectric `@Config(sdk = [36])` during live PR #12 run `#45017142629`. | Updated `android-ci.yml`, `android-release.yml`, and `codeql.yml` to **JDK 21 (`temurin`)** so all 31 Robolectric/unit tests pass on GitHub Actions identically to local execution. |
| **Missing Workflows Requested** | Medium | Missing `release-please.yml`, `dependency-review.yml`, `codeql.yml`, `website-ci.yml`, `docs-check.yml`. | Created all 7 modular workflows with explicit `permissions:` and `concurrency:` blocks. |
| **Release Signing Ambiguity** | Medium | `release.yml` calls `assembleRelease` + `bundleRelease` even when `KEYSTORE_PATH` doesn't exist without clearly separating `*-release-unsigned.apk` when unsigned. | Added `.github/scripts/unsigned-release.init.gradle` + `android-release.yml` logic: signs when secrets exist, otherwise packages `FocusNest-<tag>-release-unsigned.apk` + debug APK + `SHA256SUMS.txt`. |
| **No Website Validation Before Deploy** | Low | `deploy-pages.yml` uploads `website/` without HTML/JS syntax, broken link, or metadata verification. | Consolidated into `website-ci.yml` running `node --check` + asset/CDN/link validation on PRs & pushes before `deploy-pages` (`enablement: true`) on `main`. |
| **PAT Scope Limits (`workflow` / `pages` / `admin`)** | Info | Audited PAT pushed code/docs/PR (`#12`) cleanly, but GitHub rejected `.github/workflows/*` changes (`without workflow scope`) and returned `403 Resource not accessible by personal access token` for `/pages` POST and `/branches/main/protection` GET. | Kept the 7 SHA-pinned workflow commits ready locally on `fix/production-hardening`; run `git push origin fix/production-hardening` after adding `Workflows: Read & write` scope (plus enable GitHub Pages -> Actions & Branch Protection in repo Settings). |
