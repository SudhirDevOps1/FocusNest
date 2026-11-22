# Permissions Guide

FocusNest requests permissions contextually only when you enable the corresponding feature:

| Permission | Required? | Purpose | Fallback if Denied |
| --- | --- | --- | --- |
| `PACKAGE_USAGE_STATS` | Optional | Reads real daily app screen time & launch counts via `UsageStatsManager` | Shows clear onboarding banner; never fabricates usage stats |
| `BIND_ACCESSIBILITY_SERVICE` | Optional | Detects foreground apps & configured Reels/Shorts views locally | Focus timer, analytics, and manual profiles continue working |
| `BIND_VPN_SERVICE` | Optional | Local-only DNS domain blocklist/allowlist filter | Website blocking remains inactive |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Optional | Mutes distracting notifications during Focus/Bedtime & logs locally | Standard system notifications remain untouched |
| `POST_NOTIFICATIONS` | Optional (API 33+) | Ongoing Focus timer & break reminders | Timer runs in-app without notification alerts |
| `CAMERA` | Optional | Offline QR code & barcode scanning via CameraX + ZXing | Manual token entry remains available |
| `NFC` | Optional | Reads physical NFC tags for quick focus/unlock actions | All other 10 unlock methods remain available |
| `ACCESS_NOTIFICATION_POLICY` | Optional | Toggles Do Not Disturb during Bedtime mode | Manual DND remains available in Android quick settings |
| `RECEIVE_BOOT_COMPLETED` | Install-time | Restores active Focus timers & schedules after device reboot | User restarts timer manually after reboot |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Install-time | Keeps active Focus timer countdown accurate when screen is off | N/A |
