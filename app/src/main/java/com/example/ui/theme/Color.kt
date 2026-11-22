package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AccentPalette

// Brand Core Tokens
val SanctuaryDeep = Color(0xFF0E2D27)
val SanctuaryForest = Color(0xFF16423A)
val SanctuaryMint = Color(0xFF7CE3C4)
val SanctuarySage = Color(0xFF2E7D6B)
val SanctuaryAmber = Color(0xFFF4B860)

fun focusNestLightScheme(palette: AccentPalette): ColorScheme {
  return when (palette) {
    AccentPalette.EMERALD_SAGE ->
      lightColorScheme(
        primary = Color(0xFF155E50),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC5F2E3),
        onPrimaryContainer = Color(0xFF072922),
        secondary = Color(0xFF47635B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD5EAE2),
        onSecondaryContainer = Color(0xFF0E2620),
        tertiary = Color(0xFF8E5B13),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE1B8),
        onTertiaryContainer = Color(0xFF2D1900),
        background = Color(0xFFF5F8F6),
        onBackground = Color(0xFF141E1B),
        surface = Color(0xFFF5F8F6),
        onSurface = Color(0xFF141E1B),
        surfaceVariant = Color(0xFFE1EAE6),
        onSurfaceVariant = Color(0xFF404E49),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEFF4F1),
        surfaceContainer = Color(0xFFE8EFEA),
        surfaceContainerHigh = Color(0xFFE0E9E4),
        outline = Color(0xFF707E79),
        outlineVariant = Color(0xFFC3D0CB),
        error = Color(0xFFB3261E),
      )
    AccentPalette.INDIGO_FOCUS ->
      lightColorScheme(
        primary = Color(0xFF6558E8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5E2FF),
        onPrimaryContainer = Color(0xFF19125C),
        secondary = Color(0xFF56C8C2),
        onSecondary = Color(0xFF002B28),
        secondaryContainer = Color(0xFFD2F5F2),
        tertiary = Color(0xFF28897A),
        tertiaryContainer = Color(0xFFC5F0E4),
        background = Color(0xFFF7F7FC),
        onBackground = Color(0xFF171824),
        surface = Color(0xFFF7F7FC),
        onSurface = Color(0xFF171824),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F1FA),
        surfaceContainer = Color(0xFFE9E9F5),
        surfaceContainerHigh = Color(0xFFE1E1F0),
      )
    AccentPalette.WARM_AMBER ->
      lightColorScheme(
        primary = Color(0xFF8C5308),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDFB8),
        onPrimaryContainer = Color(0xFF2D1600),
        secondary = Color(0xFF5D5346),
        secondaryContainer = Color(0xFFECE2D4),
        tertiary = Color(0xFF1C6B5B),
        tertiaryContainer = Color(0xFFC6F0E4),
        background = Color(0xFFFAF7F2),
        surface = Color(0xFFFAF7F2),
        surfaceContainerLow = Color(0xFFF4EFE6),
        surfaceContainer = Color(0xFFEEE7DC),
        surfaceContainerHigh = Color(0xFFE7DFD2),
      )
    AccentPalette.OCEAN_SLATE ->
      lightColorScheme(
        primary = Color(0xFF125A78),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E9FA),
        onPrimaryContainer = Color(0xFF001E2B),
        secondary = Color(0xFF495E69),
        secondaryContainer = Color(0xFFD7E5ED),
        tertiary = Color(0xFF2E7D6B),
        tertiaryContainer = Color(0xFFC6F0E4),
        background = Color(0xFFF4F7F9),
        surface = Color(0xFFF4F7F9),
        surfaceContainerLow = Color(0xFFEDF2F6),
        surfaceContainer = Color(0xFFE5ECF1),
        surfaceContainerHigh = Color(0xFFDCE5EC),
      )
    AccentPalette.LAVENDER_DUSK ->
      lightColorScheme(
        primary = Color(0xFF514B8A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2DFFF),
        onPrimaryContainer = Color(0xFF120B46),
        secondary = Color(0xFF57566B),
        secondaryContainer = Color(0xFFE4E2F3),
        tertiary = Color(0xFF1E6E5E),
        tertiaryContainer = Color(0xFFC4F0E4),
        background = Color(0xFFF7F6FA),
        surface = Color(0xFFF7F6FA),
        surfaceContainerLow = Color(0xFFF1EFF7),
        surfaceContainer = Color(0xFFE9E7F2),
        surfaceContainerHigh = Color(0xFFE1DEEC),
      )
    AccentPalette.TERRACOTTA_CALM ->
      lightColorScheme(
        primary = Color(0xFF8C4233),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD2),
        onPrimaryContainer = Color(0xFF390B04),
        secondary = Color(0xFF69534E),
        secondaryContainer = Color(0xFFF2DFDA),
        tertiary = Color(0xFF28695B),
        tertiaryContainer = Color(0xFFC5F0E4),
        background = Color(0xFFFAF6F4),
        surface = Color(0xFFFAF6F4),
        surfaceContainerLow = Color(0xFFF5EEEB),
        surfaceContainer = Color(0xFFEDE4E0),
        surfaceContainerHigh = Color(0xFFE6DBD6),
      )
  }
}

fun focusNestDarkScheme(palette: AccentPalette): ColorScheme {
  return when (palette) {
    AccentPalette.EMERALD_SAGE ->
      darkColorScheme(
        primary = Color(0xFF7CE3C4),
        onPrimary = Color(0xFF06382E),
        primaryContainer = Color(0xFF134D41),
        onPrimaryContainer = Color(0xFFC5F2E3),
        secondary = Color(0xFFA9CFC4),
        onSecondary = Color(0xFF13362D),
        secondaryContainer = Color(0xFF22473E),
        onSecondaryContainer = Color(0xFFD5EAE2),
        tertiary = Color(0xFFF4B860),
        onTertiary = Color(0xFF3D2500),
        tertiaryContainer = Color(0xFF5C3B08),
        onTertiaryContainer = Color(0xFFFFE1B8),
        background = Color(0xFF0B1412),
        onBackground = Color(0xFFE2ECE8),
        surface = Color(0xFF0B1412),
        onSurface = Color(0xFFE2ECE8),
        surfaceVariant = Color(0xFF1A2824),
        onSurfaceVariant = Color(0xFFB3C5BE),
        surfaceContainerLowest = Color(0xFF070E0C),
        surfaceContainerLow = Color(0xFF111D1A),
        surfaceContainer = Color(0xFF162420),
        surfaceContainerHigh = Color(0xFF1E2F2A),
        outline = Color(0xFF586B64),
        outlineVariant = Color(0xFF2B3D37),
      )
    AccentPalette.INDIGO_FOCUS ->
      darkColorScheme(
        primary = Color(0xFF8B7CF6),
        onPrimary = Color(0xFF1C135E),
        primaryContainer = Color(0xFF342A8E),
        onPrimaryContainer = Color(0xFFE5E2FF),
        secondary = Color(0xFF56C8C2),
        onSecondary = Color(0xFF002B28),
        secondaryContainer = Color(0xFF154946),
        tertiary = Color(0xFFF4B860),
        tertiaryContainer = Color(0xFF5C3B08),
        background = Color(0xFF10111A),
        onBackground = Color(0xFFEAECFA),
        surface = Color(0xFF10111A),
        onSurface = Color(0xFFEAECFA),
        surfaceContainerLowest = Color(0xFF0B0C14),
        surfaceContainerLow = Color(0xFF1B1C28),
        surfaceContainer = Color(0xFF222434),
        surfaceContainerHigh = Color(0xFF2A2C3E),
      )
    AccentPalette.WARM_AMBER ->
      darkColorScheme(
        primary = Color(0xFFF4B860),
        onPrimary = Color(0xFF3A2200),
        primaryContainer = Color(0xFF573600),
        onPrimaryContainer = Color(0xFFFFDFB8),
        secondary = Color(0xFFD4C4B0),
        secondaryContainer = Color(0xFF3A3227),
        tertiary = Color(0xFF7CE3C4),
        tertiaryContainer = Color(0xFF134D41),
        background = Color(0xFF13110D),
        surface = Color(0xFF13110D),
        surfaceContainerLow = Color(0xFF1B1813),
        surfaceContainer = Color(0xFF221E18),
        surfaceContainerHigh = Color(0xFF2B261F),
      )
    AccentPalette.OCEAN_SLATE ->
      darkColorScheme(
        primary = Color(0xFF7CCBF0),
        onPrimary = Color(0xFF00344A),
        primaryContainer = Color(0xFF0A4B67),
        onPrimaryContainer = Color(0xFFC8E9FA),
        secondary = Color(0xFFB3C7D1),
        secondaryContainer = Color(0xFF243640),
        tertiary = Color(0xFF7CE3C4),
        tertiaryContainer = Color(0xFF134D41),
        background = Color(0xFF0B1216),
        surface = Color(0xFF0B1216),
        surfaceContainerLow = Color(0xFF111A20),
        surfaceContainer = Color(0xFF162229),
        surfaceContainerHigh = Color(0xFF1E2C35),
      )
    AccentPalette.LAVENDER_DUSK ->
      darkColorScheme(
        primary = Color(0xFFC1BDFF),
        onPrimary = Color(0xFF241E5A),
        primaryContainer = Color(0xFF393372),
        onPrimaryContainer = Color(0xFFE2DFFF),
        secondary = Color(0xFFC4C2D8),
        secondaryContainer = Color(0xFF2A293C),
        tertiary = Color(0xFF7CE3C4),
        tertiaryContainer = Color(0xFF134D41),
        background = Color(0xFF0F0F17),
        surface = Color(0xFF0F0F17),
        surfaceContainerLow = Color(0xFF161622),
        surfaceContainer = Color(0xFF1C1C2B),
        surfaceContainerHigh = Color(0xFF242436),
      )
    AccentPalette.TERRACOTTA_CALM ->
      darkColorScheme(
        primary = Color(0xFFF2A494),
        onPrimary = Color(0xFF4A180F),
        primaryContainer = Color(0xFF6B2A1E),
        onPrimaryContainer = Color(0xFFFFDAD2),
        secondary = Color(0xFFD6BEBA),
        secondaryContainer = Color(0xFF392B28),
        tertiary = Color(0xFF7CE3C4),
        tertiaryContainer = Color(0xFF134D41),
        background = Color(0xFF15100E),
        surface = Color(0xFF15100E),
        surfaceContainerLow = Color(0xFF1D1614),
        surfaceContainer = Color(0xFF241C1A),
        surfaceContainerHigh = Color(0xFF2D2421),
      )
  }
}
