# Contributing to FocusNest

1. Fork the repository and create a topic branch from `main`.
2. Keep FocusNest 100% offline-first: do not introduce `INTERNET` permission, ad SDKs, or telemetry.
3. Follow Kotlin official code style, Jetpack Compose Material 3 guidelines, and `Modifier.testTag(...)` conventions.
4. Add unit or Robolectric tests for new domain logic, DAOs, or UI flows.
5. Run `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` before opening a pull request.
