# Contributing to FocusNest

Thank you for helping improve **FocusNest** (`SudhirDevOps1/FocusNest`, maintained by **Sudhir Kumar / SudhirDevOps1**).

## Core Non-Negotiables
1. **Zero-Internet Architecture**: Never add `android.permission.INTERNET`, analytics SDKs, crash telemetry uploaders, ad libraries, or remote tracking endpoints.
2. **100% Free Forever**: Do not gate features behind paywalls, trials, subscriptions, or accounts.
3. **Offline-First Storage**: Persist user state cleanly via Room (`Migration` required for schema updates) and DataStore.

## Development Workflow
1. Fork [`SudhirDevOps1/FocusNest`](https://github.com/SudhirDevOps1/FocusNest) and create a descriptive topic branch from `main`.
2. Follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `ci:`, `refactor:`, `test:`) so Release Please generates accurate changelogs and semver bumps.
3. Follow Kotlin coding conventions, Material 3 adaptive design (`AdaptiveTwoPane`, `NavigationRail`), and `Modifier.testTag(...)` on interactive Compose elements.
4. Keep unit/Robolectric tests passing and add regression coverage for new domain logic, DAOs, or UI screens.

## Local Pre-PR Verification
```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

## Website & Documentation Changes
- Any asset in `website/` must stay self-contained (zero external CDNs, tracking scripts, or third-party fonts).
- Update `docs/` and `CHANGELOG.md` whenever user-facing capabilities or architecture modules change.
