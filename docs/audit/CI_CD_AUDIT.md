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
| **Unpinned Third-Party Actions** | High | Uses `@v4`, `@v5`, `@v2` floating major tags across all workflows. | Pin every `uses:` step to exact immutable 40-char commit SHAs with version comments. |
| **Missing Workflows Requested** | Medium | Missing `release-please.yml`, `dependency-review.yml`, `codeql.yml`, `website-ci.yml`, `docs-check.yml`. | Create modular workflows with explicit `permissions:` and `concurrency:` blocks. |
| **Release Signing Ambiguity** | Medium | `release.yml` calls `assembleRelease` + `bundleRelease` even when `KEYSTORE_PATH` doesn't exist (defaulting to missing `my-upload-key.jks` or debug fallback) without clearly separating `*-release-unsigned.apk` when unsigned. | Make `android-release.yml` inspect keystore presence: sign cleanly when secrets are present, otherwise rename unsigned release output `FocusNest-vX.Y.Z-release-unsigned.apk` + include debug APK + `SHA256SUMS.txt`. Never label unsigned APK as signed. |
| **No Website Validation Before Deploy** | Low | `deploy-pages.yml` uploads `website/` without HTML/JS syntax, broken link, or metadata verification. | Consolidate into `website-ci.yml` running HTML/CSS/JS/link checks on PRs & pushes before `deploy-pages` on `main`. |
| **Branch Protection & Pages Permission Gap** | Info | Audited PAT token returns `403 Resource not accessible by personal access token` for `/pages` POST and `/branches/main/protection` PUT/GET (token lacks `Administration: write` / `Pages: write` fine-grained scopes). | Automate everything permitted (`Contents`, `Pull Requests`, `Workflows`), monitor live PR CI checks on GitHub, and document exact GitHub Pages + Branch Protection JSON rules & manual UI setup steps honestly if 403 persists. |
