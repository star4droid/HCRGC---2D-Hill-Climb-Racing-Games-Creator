package com.star4droid.hcrgc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = StudioAccentBlue,
    onPrimary = StudioSurface,
    primaryContainer = StudioSurfaceElevated,
    onPrimaryContainer = StudioTextPrimary,
    secondary = StudioAccentIndigo,
    onSecondary = StudioSurface,
    background = StudioBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioSurfaceBorder,
    outlineVariant = CanvasGridLine
)

private val DarkColorScheme = darkColorScheme(
    primary = StudioAccentBlue,
    onPrimary = StudioSurface,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = StudioSurface,
    secondary = StudioAccentIndigo,
    onSecondary = StudioSurface,
    background = DarkSurface,
    onBackground = StudioSurface,
    surface = DarkSurfaceVariant,
    onSurface = StudioSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = StudioTextTertiary,
    outline = DarkSurfaceVariant
)

@Composable
fun HCRGCTheme(
    darkTheme: Boolean = false, // Clean professional light theme by default as per spec
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
