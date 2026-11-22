# Testing Guide (`TESTING.md`)

FocusNest uses pure JVM unit tests + Robolectric + Jetpack Compose UI tests:
- `FocusNestDomainUnitTest`: `UnlockChallengeEngine` (12 challenges including `BREATHING`, adaptive division math, 80%/90%/100% thresholds), `RulePriorityResolver` (grace periods & category daily caps), `LaunchPauseCoordinator` (enabled/disabled/cooldown checks), `BedtimeChecklistDefaults`, `AmbientSoundType`, `UpdateReleaseConfig`, 6 `AccentPalette`s, local DNS packet parser/NXDOMAIN builder, AES-256-GCM backup crypto, and duration formatters.
- `FocusNestDatabaseAndRepoTest`: Room in-memory database CRUD, `MIGRATION_1_2` SQL schema upgrade verification, `HabitRepository` streaks & 7-day completion rates, signed/one-time/expiring QR token replay & expiry rejection, `BlockerRepository` group/category/daily limits & mindful proceed records, `InsightsAndBackupRepository` printable HTML Wellbeing Report Card & encrypted backup/restore round-trips, `FocusNestPreferences` soundscape/launch-pause/wind-down rollover, and `FocusRepository` multi-day streaks.
- `ExampleRobolectricTest`: Compose UI onboarding-to-dashboard flow, 5-tab navigation + Habits seeding & verification, Mindful Launch Pause Curtain controls in Blocker, Weekly Report Card print CTA in Insights, Wind-Down Checklist & GitHub Releases update checker in More/Settings, wide tablet width (`720.dp+`) two-pane layout rendering, Hindi (`values-hi`) localization resolution, and `BootCompletedReceiver`.

Run locally or in CI:
```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```
