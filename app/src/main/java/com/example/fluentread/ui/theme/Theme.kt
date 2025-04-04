package com.example.fluentread.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FluentPrimary,
    secondary = FluentSecondary,
    background = FluentBackground,
    surface = FluentSurface,
    onPrimary = FluentOnPrimary,
    onSecondary = FluentOnSecondary,
    onBackground = FluentOnBackground,
    onSurface = FluentOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = FluentPrimaryDark,
    secondary = FluentSecondaryDark,
    background = FluentBackgroundDark,
    surface = FluentSurfaceDark,
    onPrimary = FluentOnPrimaryDark,
    onSecondary = FluentOnSecondaryDark,
    onBackground = FluentOnBackgroundDark,
    onSurface = FluentOnSurfaceDark
)

@Composable
fun FluentReadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FluentTypography,
        shapes = FluentShapes,
        content = content
    )
}
