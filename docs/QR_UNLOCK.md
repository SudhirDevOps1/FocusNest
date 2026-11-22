# Offline QR, Barcode & NFC Unlocking

## Physical Token Vault
- **NFC Tag Vault**: Detects hardware support via `NfcAdapter.getDefaultAdapter(context)`. Reads tag UIDs via foreground dispatch and pairs them with `START_FOCUS`, `UNLOCK_PROFILE`, or `TOGGLE_BLOCKER` actions.
- **Signed One-Time & Expiring QR/Barcode Tokens**: Uses local ZXing Core (zero internet calls) and CameraX for completely offline scanning/generation. Tokens store a SHA-256 signature (`signatureHex`), optional `isOneTime` replay lock (`isConsumed`), and optional `expiresAtEpochMs` deadline verified atomically inside `BlockerRepository.verifyAndConsumeQrOrBarcodeToken`.
