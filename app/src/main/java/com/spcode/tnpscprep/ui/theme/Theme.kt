package com.spcode.tnpscprep.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: LIGHT
    }
}

/** True while the dark palette is active (used by the few places that need to pick between two hand-made looks). */
val LocalIsDark = compositionLocalOf { false }

// Calm indigo + teal on warm white. Light is the default.
private val Light = lightColorScheme(
    primary = Color(0xFF4F46E5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E4FF), onPrimaryContainer = Color(0xFF1E1B6B),
    secondary = Color(0xFF0D9488), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF3EE), onSecondaryContainer = Color(0xFF053B36),
    tertiary = Color(0xFFF59E0B), onTertiary = Color(0xFF3B2600),
    tertiaryContainer = Color(0xFFFFE9B8), onTertiaryContainer = Color(0xFF3B2600),
    background = Color(0xFFFBF8F3), onBackground = Color(0xFF1B1B22),
    surface = Color.White, onSurface = Color(0xFF1B1B22),
    surfaceVariant = Color(0xFFF1EEF7), onSurfaceVariant = Color(0xFF5E5B6B),
    outline = Color(0xFFB5B1C4), outlineVariant = Color(0xFFE3DFEE),
    error = Color(0xFFDC2626), onError = Color.White,
    errorContainer = Color(0xFFFFE1DE), onErrorContainer = Color(0xFF5C0A07),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F4FB),
    surfaceContainer = Color(0xFFF3F0F9), surfaceContainerHigh = Color(0xFFEDE9F5), surfaceContainerHighest = Color(0xFFE7E2F1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA5A0FF), onPrimary = Color(0xFF1A1673),
    primaryContainer = Color(0xFF332F9C), onPrimaryContainer = Color(0xFFE6E4FF),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0F5750), onSecondaryContainer = Color(0xFFCDF3EE),
    tertiary = Color(0xFFFBBF24), onTertiary = Color(0xFF3B2600),
    tertiaryContainer = Color(0xFF5C3F00), onTertiaryContainer = Color(0xFFFFE9B8),
    background = Color(0xFF0F1115), onBackground = Color(0xFFE8E6F0),
    surface = Color(0xFF171A21), onSurface = Color(0xFFE8E6F0),
    surfaceVariant = Color(0xFF242833), onSurfaceVariant = Color(0xFFB4B0C4),
    outline = Color(0xFF6E6A80), outlineVariant = Color(0xFF2F3340),
    error = Color(0xFFFF8A80), onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5C1511), onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0B0D11), surfaceContainerLow = Color(0xFF14171D),
    surfaceContainer = Color(0xFF1B1E26), surfaceContainerHigh = Color(0xFF22262F), surfaceContainerHighest = Color(0xFF2A2E39),
)

// Bigger, softer corners everywhere.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TnpscTheme(mode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tamil = LocalConfiguration.current.locales[0].language == "ta"

    // Status/navigation bar icons must follow OUR theme, not the phone's (e.g. dark icons on the light background).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = remember(tamil) { tnpscTypography(tamil) },
            shapes = AppShapes,
            content = content,
        )
    }
}
