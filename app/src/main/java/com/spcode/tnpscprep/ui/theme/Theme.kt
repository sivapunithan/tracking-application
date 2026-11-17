package com.spcode.tnpscprep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calm teal palette. Follows the system light/dark setting.
private val Light = lightColorScheme(primary = Color(0xFF3F6E6A), background = Color(0xFFF7FAF9), surface = Color(0xFFF7FAF9))
private val Dark = darkColorScheme(primary = Color(0xFF8CC5BF), background = Color(0xFF101414), surface = Color(0xFF101414))

@Composable
fun TnpscTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
