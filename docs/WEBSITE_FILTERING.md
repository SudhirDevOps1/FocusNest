# Local On-Device Website Filtering (VpnService)

FocusNest implements an offline local DNS sinkhole (`FocusNestVpnService`):
- Captures UDP DNS queries on port 53 locally and responds with `NXDOMAIN` for domains matched against `WebsiteRuleEntity` (respecting allowlist overrides).
- Supports **Per-App VPN Network Scope**: when `blockedNetworkPackages` is non-empty, `builder.addAllowedApplication(pkg)` scopes the tunnel strictly to chosen apps; when empty, `addDisallowedApplication(packageName)` routes all other apps.
- **Limitation**: Encrypted DNS (`Private DNS` / DNS-over-TLS / DNS-over-HTTPS in Chrome or Android Network settings) can bypass standard UDP/53 inspection cleanly without breaking connectivity.
- Only one `VpnService` can be active at a time on Android.
