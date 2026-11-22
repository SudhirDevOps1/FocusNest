# Frequently Asked Questions

**Does FocusNest require an internet connection or account?**
No. FocusNest has no `INTERNET` permission and requires no account.

**Why does Home show "Usage Access Required" on first launch?**
Android protects app screen-time statistics behind the `PACKAGE_USAGE_STATS` special permission. FocusNest never fabricates fake usage numbers when access is off.

**Why doesn't UsageStatsManager block apps by itself?**
`UsageStatsManager` only reads historical usage. Real-time app blocking requires Android's optional Accessibility Service (`AppBlockerAccessibilityService`) so FocusNest knows when a restricted app enters the foreground.

**Does FocusNest's Website Blocker send traffic to a remote VPN server?**
No. `FocusNestVpnService` runs a local on-device DNS sinkhole only. Note that Android allows only one active VPN app at a time.

**Can Invincible Mode permanently lock me out of my phone?**
No. FocusNest avoids Device Admin lock abuse and always provides owner-controlled emergency recovery and cooldown unlocks.
