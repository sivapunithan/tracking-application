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

// Crimson + maroon on warm white (Light is the default). Gold is the small accent.
private val Light = lightColorScheme(
    primary = Color(0xFFC8102E), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9), onPrimaryContainer = Color(0xFF5A0012),
    secondary = Color(0xFF800020), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6D6DB), onSecondaryContainer = Color(0xFF4A0010),
    tertiary = Color(0xFFB8860B), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9B8), onTertiaryContainer = Color(0xFF3B2600),
    background = Color(0xFFFBF6F4), onBackground = Color(0xFF22181A),
    surface = Color.White, onSurface = Color(0xFF22181A),
    surfaceVariant = Color(0xFFF5EAEA), onSurfaceVariant = Color(0xFF6B5A5D),
    outline = Color(0xFFC4B1B4), outlineVariant = Color(0xFFEADCDD),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFFE1DE), onErrorContainer = Color(0xFF5C0A07),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAF3F2),
    surfaceContainer = Color(0xFFF6ECEB), surfaceContainerHigh = Color(0xFFF0E4E3), surfaceContainerHighest = Color(0xFFEADDDC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF8A9B), onPrimary = Color(0xFF5A0012),
    primaryContainer = Color(0xFF8E1228), onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE8A0AB), onSecondary = Color(0xFF4A0010),
    secondaryContainer = Color(0xFF6B1224), onSecondaryContainer = Color(0xFFF6D6DB),
    tertiary = Color(0xFFE9C46A), onTertiary = Color(0xFF3B2600),
    tertiaryContainer = Color(0xFF5C3F00), onTertiaryContainer = Color(0xFFFFE9B8),
    background = Color(0xFF150D0F), onBackground = Color(0xFFF2E6E7),
    surface = Color(0xFF1E1416), onSurface = Color(0xFFF2E6E7),
    surfaceVariant = Color(0xFF2E2124), onSurfaceVariant = Color(0xFFC9B4B7),
    outline = Color(0xFF8A7478), outlineVariant = Color(0xFF3D2D30),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF110A0C), surfaceContainerLow = Color(0xFF191113),
    surfaceContainer = Color(0xFF22181A), surfaceContainerHigh = Color(0xFF2A1E21), surfaceContainerHighest = Color(0xFF33262A),
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
