package com.paopao.englearn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue600,
    secondary = Green500,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Green100,
    tertiary = Orange500,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = Orange100,
    error = Red500,
    errorContainer = Red100,
    background = Gray50,
    onBackground = Gray900,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray200
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue500,
    onPrimary = DarkBackground,
    primaryContainer = DarkBlue600,
    onPrimaryContainer = Blue100,
    secondary = Green500,
    onSecondary = DarkBackground,
    secondaryContainer = Green500.copy(alpha = 0.3f),
    tertiary = Orange500,
    onTertiary = DarkBackground,
    error = Red500,
    errorContainer = Red500.copy(alpha = 0.3f),
    background = DarkBackground,
    onBackground = Gray100,
    surface = DarkSurface,
    onSurface = Gray100,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Gray500,
    outline = Gray700
)

@Composable
fun EnglishLearnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
