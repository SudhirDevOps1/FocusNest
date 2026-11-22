# Feature Guides

## App Blocker, Launch Pause Curtain & Profiles
- Configure per-app daily/weekly limits, shared app groups, category daily caps, and priority-ordered profiles (`Study`, `College`, `Work`, `Sleep`, `Reading`, `Weekend`, `Custom`).
- Arm selected apps with the **Mindful Launch Pause Curtain** (`5s–15s` guided breathing delay before launch) under **Shield > Apps & Limits**.
- Emergency-safe apps (Dialer, Contacts, Settings) can be marked safe anytime.

## Focus Timer & Offline Ambient Soundscapes
- Supports **Pomodoro**, **Countdown**, and **Stopwatch** modes across 6 session types (`Study`, `Work`, `Reading`, `Coding`, `Creative`, `Custom`).
- Enable synthesized offline **Ambient Soundscapes** (`Brown Noise`, `Soft Rain`, `Warm Drone`, `Study Wash`) right inside **Focus Sanctuary** with volume adjustment—zero internet or MP3 downloads required.
- Timer state persists via target epoch + monotonic `elapsedRealtime()` timestamps in DataStore + `FocusTimerService` + `BootCompletedReceiver`.

## Website Blocking (Local VPN)
- Starts `FocusNestVpnService` only after explicit Android `VpnService.prepare()` consent.
- Intercepts DNS queries on-device (`10.88.0.1`) against your blocklist/allowlist and offline domain packs with optional per-app scoping. Zero external VPN servers.

## Bedtime Wind-Down, NFC, QR & Barcode Setup
- **Evening Wind-Down Checklist**: Check off bedtime steps under **More > Mindful Usage Tools** with automatic daily reset plus shortcuts to Display Brightness and Night Light settings.
- **NFC**: Tap any NDEF/UID tag under **More > NFC, QR & Barcode Vault** to bind it to Focus Start, Profile Unlock, or Quick Shield.
- **QR & Barcodes**: Generate personal FocusNest QR codes offline or scan existing QR/barcodes with CameraX + ZXing.

## Report Cards, Updates & Encrypted Backup
- Export **Printable/PDF Weekly Wellbeing Report Cards**, CSV/JSON analytics, or AES-256-GCM passphrase-encrypted backups anytime.
- Check for newer releases offline-safely via **More > Settings > Check Releases on GitHub**, which opens `https://github.com/OWNER/FocusNest/releases/latest` in your browser.
