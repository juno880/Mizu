package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Mizu theme (water-inspired blue)
 */
internal object MizuColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF4FC3F7),
        onPrimary = Color(0xFF00344D),
        primaryContainer = Color(0xFF4FC3F7),
        onPrimaryContainer = Color(0xFF00344D),
        inversePrimary = Color(0xFF0277BD),
        secondary = Color(0xFF4FC3F7), // Unread badge
        onSecondary = Color(0xFF00344D), // Unread badge text
        secondaryContainer = Color(0xFF184E64), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF4FC3F7), // Navigation bar selector icon
        tertiary = Color(0xFFBF1F2F), // Downloaded badge
        onTertiary = Color(0xFFFFFFFF), // Downloaded badge text
        tertiaryContainer = Color(0xFF200508),
        onTertiaryContainer = Color(0xFFBF1F2F),
        background = Color(0xFF1A2226),
        onBackground = Color(0xFFDEE3E5),
        surface = Color(0xFF1A2226),
        onSurface = Color(0xFFDEE3E5),
        surfaceVariant = Color(0xFF1F323B), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFDEE3E5),
        surfaceTint = Color(0xFF4FC3F7),
        inverseSurface = Color(0xFFDEE3E5),
        inverseOnSurface = Color(0xFF1A2226),
        outline = Color(0xFF859298),
        surfaceContainerLowest = Color(0xFF19282D),
        surfaceContainerLow = Color(0xFF1C2C32),
        surfaceContainer = Color(0xFF1F323B), // Navigation bar background
        surfaceContainerHigh = Color(0xFF243B44),
        surfaceContainerHighest = Color(0xFF2B4753),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF0277BD),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF0277BD),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF4FC3F7),
        secondary = Color(0xFF0277BD), // Unread badge
        onSecondary = Color(0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(0xFFCDE8F7), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF0277BD), // Navigation bar selector icon
        tertiary = Color(0xFFFF7F7F), // Downloaded badge
        onTertiary = Color(0xFF000000), // Downloaded badge text
        tertiaryContainer = Color(0xFF2A1616),
        onTertiaryContainer = Color(0xFFFF7F7F),
        background = Color(0xFFF9FCFD),
        onBackground = Color(0xFF04090B),
        surface = Color(0xFFF9FCFD),
        onSurface = Color(0xFF04090B),
        surfaceVariant = Color(0xFFE7F1F5), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF04090B),
        surfaceTint = Color(0xFFBFE0F0),
        inverseSurface = Color(0xFF04090B),
        inverseOnSurface = Color(0xFFF9FCFD),
        outline = Color(0xFF6C7A80),
        surfaceContainerLowest = Color(0xFFDDEAEF),
        surfaceContainerLow = Color(0xFFE2EDF1),
        surfaceContainer = Color(0xFFE7F1F5), // Navigation bar background
        surfaceContainerHigh = Color(0xFFECF5F8),
        surfaceContainerHighest = Color(0xFFF3FAFC),
    )
}
