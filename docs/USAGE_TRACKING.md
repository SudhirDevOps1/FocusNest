# Usage Tracking & Android Stats

FocusNest queries real on-device foreground app duration and launch events via `android.app.usage.UsageStatsManager` (`queryUsageStats` and `queryEvents`).

- **Requires `PACKAGE_USAGE_STATS`**: Users grant Usage Access manually in Android System Settings.
- **Launcher & Infrastructure Filtering**: `SystemPackageClassifier` dynamically identifies the active `CATEGORY_HOME` launcher, `com.android.systemui`, enabled IME keyboards, permission controllers, and FocusNest itself, excluding them from analytics totals and distraction lists by default. Users can inspect or include them anytime via **Show system & launcher apps** in Settings, Insights, or Blocker.
- **No Fabricated Telemetry**: When permission is denied or data is reset by Android, FocusNest displays `0m` / an explicit empty state rather than sample numbers.
- **Note**: `UsageStatsManager` measures usage—it does NOT block apps. Real-time blocking relies on the user-enabled `AppBlockerAccessibilityService` paired with `EnforcementMonitor`'s foreground `UsageEvents` fallback while shields/timers are active.
