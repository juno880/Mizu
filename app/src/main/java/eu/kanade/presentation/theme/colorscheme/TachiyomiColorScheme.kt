package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Default theme
 * Based on Mizu colour palette
 *
 * Key colors:
 * Primary    #4d608c  (slate blue)
 * Surface    #f8f7fc  (near white with blue tint)
 * Dark base  #1a1b20  (near black blue-grey)
 * Accent     #777488  (muted purple-grey)
 */
internal object TachiyomiColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFAAB3CD),
        onPrimary = Color(0xFF1A2540),
        primaryContainer = Color(0xFF2E3A54),
        onPrimaryContainer = Color(0xFFD8E2FF),
        inversePrimary = Color(0xFF4D608C),
        secondary = Color(0xFFAAB3CD), // Unread badge
        onSecondary = Color(0xFF1A2540), // Unread badge text
        secondaryContainer = Color(0xFF2E3A54), // Navigation bar selector pill & progress indicator
        onSecondaryContainer = Color(0xFFD8E2FF), // Navigation bar selector icon
        tertiary = Color(0xFFC5C6D0), // Downloaded badge
        onTertiary = Color(0xFF2A2B35), // Downloaded badge text
        tertiaryContainer = Color(0xFF3D3E4A),
        onTertiaryContainer = Color(0xFFE2E3EE),
        background = Color(0xFF0A0B10),
        onBackground = Color(0xFFE4E3E9),
        surface = Color(0xFF0A0B10),
        onSurface = Color(0xFFE4E3E9),
        surfaceVariant = Color(0xFF1A1B20), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFC5C6D0),
        surfaceTint = Color(0xFFAAB3CD),
        inverseSurface = Color(0xFFE4E3E9),
        inverseOnSurface = Color(0xFF1A1B20),
        outline = Color(0xFF777488),
        surfaceContainerLowest = Color(0xFF08090E),
        surfaceContainerLow = Color(0xFF0E0F14),
        surfaceContainer = Color(0xFF131419), // Navigation bar background
        surfaceContainerHigh = Color(0xFF18191E),
        surfaceContainerHighest = Color(0xFF1E1F24),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF4D608C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD8E2FF),
        onPrimaryContainer = Color(0xFF0A1940),
        inversePrimary = Color(0xFFAAB3CD),
        secondary = Color(0xFF4D608C), // Unread badge
        onSecondary = Color(0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(0xFFDCE2FF), // Navigation bar selector pill & progress indicator
        onSecondaryContainer = Color(0xFF0A1940), // Navigation bar selector icon
        tertiary = Color(0xFF777488), // Downloaded badge
        onTertiary = Color(0xFFFFFFFF), // Downloaded badge text
        tertiaryContainer = Color(0xFFEDECF2),
        onTertiaryContainer = Color(0xFF1A1820),
        background = Color(0xFFF8F7FC),
        onBackground = Color(0xFF1A1B20),
        surface = Color(0xFFF8F7FC),
        onSurface = Color(0xFF1A1B20),
        surfaceVariant = Color(0xFFE4E3E9), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF3D3E4A),
        surfaceTint = Color(0xFF4D608C),
        inverseSurface = Color(0xFF2E2F35),
        inverseOnSurface = Color(0xFFF0EFF5),
        outline = Color(0xFF777488),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F4FA),
        surfaceContainer = Color(0xFFEFEEF4), // Navigation bar background
        surfaceContainerHigh = Color(0xFFE9E8EE),
        surfaceContainerHighest = Color(0xFFE4E3E9),
    )
}
