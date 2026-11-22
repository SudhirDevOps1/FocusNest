# Android Compatibility (`ANDROID_COMPATIBILITY.md`)

Supported versions: **Android 8.0 (API 26) through Android 16 (API 36)**, including Android 11, 12, 13, 14, and 15+.

- **Android 11 (API 30+)**: Uses `<queries>` `ACTION_MAIN` + `CATEGORY_LAUNCHER` for Play-compliant app discovery without `QUERY_ALL_PACKAGES`.
- **Android 12 (API 31+)**: Supports Material You `dynamicLightColorScheme`/`dynamicDarkColorScheme` and `PendingIntent.FLAG_IMMUTABLE` / `FLAG_MUTABLE` (for NFC foreground dispatch).
- **Android 13 (API 33+)**: Contextual `POST_NOTIFICATIONS` runtime request, monochrome adaptive icon support, and typed `getParcelableExtra` for NFC tags.
- **Android 14–15+ (API 34+)**: Explicit `foregroundServiceType="specialUse"` declaration with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- **Realme UI / ColorOS / Xiaomi / Samsung**: Built-in OEM guide under **More > Permissions & Device Compatibility** explains background activity, auto-launch, and recent-apps lock steps.
