# FocusNest Feature Status Matrix

This matrix evaluates every user-facing feature in **FocusNest** (`v1.2.0`) truthfully based on source inspection and test execution.

| Category | Feature | Baseline Status | Verified Hardened Status & Honest Scope |
| :--- | :--- | :--- | :--- |
| **Onboarding** | 10-step Privacy Onboarding | **Broken on Repeat Launch** (`P0`) | **Fixed & Verified**: `StartupPhase` state machine + versioned completion + Settings replay mode. |
| **Focus Timer** | Pomodoro / Countdown / Stopwatch + Reboot Restore | **Working** | **Hardened & Verified**: Monotonic `elapsedRealtime()`, reboot restoration, wired with `BlockingScheduler` & `EnforcementMonitor`. |
| **Focus Audio** | Offline Synthesized Ambient Soundscapes | **Working** | **Verified**: Procedural PCM `AudioTrack` (`Brown Noise`, `Soft Rain`, `Warm Drone`, `Study Wash`), 0 network usage. |
| **App Blocker** | App / Group / Category Limits & Profiles | **Partial** (`P1`) | **Hardened & Verified**: Modular engines (`RuleEvaluator`, `BlockingScheduler`, `BlockingStateManager`, `EmergencyAccessManager`, `EnforcementMonitor`) + `UsageEvents` fallback + deduplicated overlay + OEM setup card. |
| **Mindful Pause** | Launch Pause Curtain (5–15s Breathing Buffer) | **Working** | **Hardened & Verified**: Interactive breathing orb + 5-min proceed cooldown (`LaunchPauseCoordinator`); depends on optional Accessibility service when live. |
| **Reels & Shorts** | Short-Form View-ID Interception | **Best-Effort Optional** | **Documented Honestly**: View-ID matching on Instagram/YouTube/Facebook/TikTok depends on 3rd-party app layout stability; fails safely if layout changes. |
| **Website Blocker** | Local On-Device DNS Sinkhole (`VpnService`) | **Partial** (`P1`) | **Hardened & Verified**: Immediate `startForeground()`, IO settings load, finite `VpnConnectionState` + `onRevoke()` cleanup + live Domain Verification screen + DoH/IPv6 disclosures. |
| **QR / Barcode** | Offline CameraX + ZXing Scanner & Token Vault | **Crashing on Hardware** (`P0`) | **Fixed & Verified**: Stride-safe `YuvLuminanceExtractor`, lifecycle bind/unbind, flashlight toggle, error/retry UI + manual entry. |
| **NFC Vault** | NFC Tag Pairing & Tap Triggers | **Hardware-Dependent** | **Verified**: Works on NFC-capable devices when NFC is enabled; displays clean disabled/unsupported notice otherwise. |
| **Screen Time** | Real `UsageStatsManager` Analytics & Charts | **Polluted by Launcher** (`P2`) | **Fixed & Verified**: Dynamic `SystemPackageClassifier` excluding launcher/SystemUI/IME infra by default + **Show system & launcher apps** toggle. |
| **Habits & Mindful** | Offline Habits + Bedtime Wind-Down Checklist | **Working** | **Verified**: Tracks daily streaks, 7-day completion strips, evening checklist & Display/Night Light shortcuts (system grayscale remains OS-settings shortcut). |
| **Reports & Backup** | Printable/PDF Report Card, CSV/JSON & AES-256-GCM Backup | **Working** | **Fixed & Verified**: Offline HTML print adapter + PBKDF2 AES-256-GCM portable archive; unit test DAO query (`getRecentHistory` + `getAllHistory`) fixed & passing. |
| **Adaptive UI** | Compact + Tablet/Foldable Two-Pane (`720.dp+`) | **Working** | **Polished & Verified**: `NavigationRail` + `AdaptiveTwoPane` + List-Detail More hub; viewport-safe layouts verified in Robolectric tests. |
| **Updates** | Zero-Internet GitHub Releases Handoff | **Misconfigured** (`P3`) | **Fixed & Verified**: Points to `SudhirDevOps1/FocusNest/releases/latest` via browser intent; zero `INTERNET` permission. |
