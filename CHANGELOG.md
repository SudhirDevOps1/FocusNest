# Changelog

## [1.2.0] - 2026-11-22
- **Adaptive Foldable & Tablet Two-Pane Layouts**: Added `AdaptiveTwoPane` responsive layouts across Home Dashboard, Focus Sanctuary, Blocker (`Apps & Limits`), Insights & Reports, and Habits plus a canonical List-Detail two-pane controller on More & Settings (`720.dp+`).
- **Offline Ambient Focus Soundscapes**: Added pure-Kotlin synthesized `AmbientSoundEngine` (`Brown Noise`, `Soft Rain`, `Warm Drone`, `Study Wash`) wired to `FocusTimerService` and `FocusScreen` with volume control and zero network permissions or bundled MP3 bloat.
- **Mindful App-Launch Pause Curtain**: Added `LaunchPauseCoordinator` + breathing orb curtain mode in `BlockedOverlayActivity` with configurable `5s–15s` delay, per-app toggles, instant preview, and 5-minute proceed cooldown.
- **Weekly Printable / PDF Wellbeing Report Card**: Upgraded `InsightsAndBackupRepository.buildPrintableReportHtml` and Insights export panel with KPI tiles, focus goal attainment badge, category table, top apps table, and reflection logs.
- **Bedtime Wind-Down Checklist & Night Display Shortcuts**: Added a daily auto-resetting evening wind-down checklist card with linear progress tracking alongside quick shortcuts to Android Display Brightness and Night Light settings.
- **Zero-Internet App Updates**: Added an App Version & Offline Updates card (`Settings` and `Privacy & About`) that opens `https://github.com/OWNER/FocusNest/releases/latest` in the user's browser while preserving zero `INTERNET` permission.
- **Android Lint & Resource Hygiene**: Resolved all Android Lint errors (`StaticFieldLeak`, `SwitchIntDef`, `ProtectedPermissions`, `MissingPermission`, `MissingTranslation`, `UnusedResources`, `IconLocation`, `ExperimentalMaterial3Api` opt-ins, and `PrimaryTabRow` migrations).
- **Interactive Showcase Website & Public CI/CD**: Upgraded `/website` with a live 5-tab interactive phone preview and Light/Dark + Palette switcher; expanded GitHub Actions CI, tagged release packaging (`SHA256SUMS.txt`), GitHub Pages deployment, and Dependabot.

## [1.1.0] - 2026-11-22
- **Habits & Routines Tracker**: Offline Room-backed `HabitEntity` & `HabitCompletionEntity` with streaks, 7-day completion strips, daily/weekly completion bar charts, and starter habits.
- **Breathing Unlock Challenge (12th Method)**: Guided 4-2-4 inhale-hold-exhale orb challenge added to `UnlockChallengeType.BREATHING` alongside predefined intention chips and adaptive division math.
- **Category Limits, Grace Periods & Progressive Warnings**: `CategoryLimitEntity` daily caps, per-app grace windows (0–15 min), and 80%/90%/100% WorkManager threshold alerts.
- **Focus Controls & Monotonic Timer**: Quick duration presets, `+5m` Extend, `Restart`, notification `Pause`, Break screen `Skip`, and `SystemClock.elapsedRealtime()` clock-tamper resilience.
- **Signed One-Time & Expiring QR/Barcode Tokens**: Replay-protected SHA-256 verification and automatic expiry enforcement in NFC & QR Vault.
- **Per-App VPN Network Scope**: Selectively route chosen packages through `FocusNestVpnService`.
- **Android Quick Settings Tile & Localization**: `FocusQuickTileService` toggle, full Hindi (`values-hi`) localization + runtime language switcher, `INDIGO_FOCUS` 6th accent palette, and installed app icons across lists.
- **Granular Privacy Controls**: Separate cleanup actions for usage history, notification logs, and habits without wiping everything.
- **Room Database v2**: Real SQL `MIGRATION_1_2` preserving all v1 user records.

## [1.0.0] - 2026-11-21
- Initial release of FocusNest (`com.aistudio.focusnest.fnwbx`).
- 10-step contextual onboarding flow with skip-friendly optional permissions.
- Home Dashboard with animated screen-time goal ring, quick shield, daily intention, and weekly chart.
- Focus Sanctuary timer (Pomodoro, Countdown, Stopwatch) with foreground service & reboot recovery.
- Configurable App Blocker, 6+ preset profiles, schedules, shared app groups, and 11 unlock challenges.
- Local-only `VpnService` DNS website filter, Reels & Shorts reduction, Mindful Tools, and Invincible Mode.
- Insights & Reports with CSV, JSON, and printable HTML exports, plus AES-256-GCM encrypted backups.
- 3 Home Screen widgets, CameraX + ZXing offline QR/Barcode scanner/generator, and NFC Tag Vault.
