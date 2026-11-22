# Threat Model

## Assets Protected
- Screen-time history, app launch records, and focus session notes
- Restriction profiles, emergency recovery passcode hashes, and backup files
- Local DNS queries and optional notification metadata

## Trust Boundaries & Mitigations
1. **Network Exfiltration**: Mitigated structurally by omitting `android.permission.INTERNET` from `AndroidManifest.xml`.
2. **Local DNS VPN Spoofing / Loops**: `FocusNestVpnService` routes only a virtual local DNS interface (`10.88.0.1`), excludes itself via `addDisallowedApplication`, and never forwards packet contents to third-party servers.
3. **Accessibility Abuse Prevention**: `AppBlockerAccessibilityService` inspects only `TYPE_WINDOW_STATE_CHANGED` package names and configured short-form view IDs when enabled, never storing text input or screen contents.
4. **Backup Confidentiality**: Portable backups use standard `PBKDF2WithHmacSHA256` (60,000 iterations) + `AES/GCM/NoPadding` (256-bit key, 12-byte random IV, 128-bit auth tag).
5. **Anti-Lockout Safeguards**: FocusNest never uses Device Administrator locks; owner emergency recovery remains available so users cannot brick device access.
