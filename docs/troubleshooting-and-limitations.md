# Troubleshooting & Known Limitations

## Realme UI / ColorOS / MIUI / Samsung Background Killing
Aggressive OEM battery managers may pause background workers or accessibility listeners. Follow the tailored checklist inside **More > Permissions & Device Compatibility**:
- Allow Background Activity & Auto-Launch (Realme/Oppo/OnePlus).
- Exclude FocusNest from Sleeping Apps (Samsung) and Battery Restrictions (Xiaomi).
- Lock FocusNest in Recents on Android 11+ if needed.

## Known Limitations
- Android allows only **one active VPN app** at a time; enabling FocusNest's local DNS website filter replaces any external VPN while active.
- `Reels & Shorts Reduction` relies on configurable UI view-ID hints; if a third-party app updates its layout, FocusNest fails safely without blocking the app.
- Devices without NFC hardware automatically disable NFC registration while keeping all other 10 unlock challenges available.
