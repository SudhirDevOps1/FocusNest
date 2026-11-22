# Accessibility & Inclusive Design (`ACCESSIBILITY.md`)

- All interactive targets enforce Material 3 48dp minimum touch target dimensions.
- Icons, charts, and generated QR bitmaps include descriptive `contentDescription` attributes.
- Supports system font scaling (`sp`), Light/Dark themes, 5 contrast-verified accent palettes, and Material You Dynamic Color on Android 12+.
- `AppBlockerAccessibilityService` is strictly opt-in, narrowly scoped to `TYPE_WINDOW_STATE_CHANGED` and optional short-form video view IDs, and never records text inputs or screen contents.
