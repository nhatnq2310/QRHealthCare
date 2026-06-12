package com.qrhealthcare.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = Red500,
    onPrimary        = White,
    primaryContainer = Red50,
    onPrimaryContainer = Red600,
    secondary        = Gray50,
    onSecondary      = Black,
    background       = White,
    onBackground     = Black,
    surface          = White,
    onSurface        = Black,
    surfaceVariant   = Gray50,
    onSurfaceVariant = Gray400,
    outline          = Gray100,
    error            = Red500,
    onError          = White,
)

private val DarkColorScheme = darkColorScheme(
    primary          = Red500,
    onPrimary        = White,
    primaryContainer = Color(0xFF8B0000),  // Dark red container
    background       = Black,
    onBackground     = White,
    surface          = Color(0xFF262626),
    onSurface        = White,
    surfaceVariant   = Color(0xFF333333),
    onSurfaceVariant = Gray400,
    outline          = Color(0xFF404040),
    error            = Red500,
    onError          = White,
)

@Composable
fun QrHealthcareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
