# Security Policy

FocusNest is offline-first, omits `android.permission.INTERNET`, and collects zero user data.

## Reporting a Vulnerability
Please report security issues responsibly via the GitHub Security Advisories tab on `OWNER/FocusNest` without publishing exploit details before a fix is released.

## Security & Supply-Chain Guarantees
- Standard cryptography only (`javax.crypto` AES-256-GCM + `PBKDF2WithHmacSHA256`, Android Keystore, SHA-256).
- Parameterized Room SQLite queries via `@Dao` annotations with non-destructive migrations.
- Zero Dynamic Code Loading (DCL) and zero third-party ad/analytics SDKs.
- Automated CI privacy gate verifying `android.permission.INTERNET` is absent from `app/src/main/AndroidManifest.xml` on every push and pull request.
- Tagged GitHub Releases publish `SHA256SUMS.txt` checksums for independent artifact verification.
