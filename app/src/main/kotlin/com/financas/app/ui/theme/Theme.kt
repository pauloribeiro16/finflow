package com.financas.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createDarkColorScheme(colors: FinflowColors) = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.textInverse,
    primaryContainer = colors.primaryContainer,
    onPrimaryContainer = Color.White,
    secondary = colors.secondary,
    onSecondary = colors.textInverse,
    secondaryContainer = colors.secondaryLight,
    onSecondaryContainer = colors.secondary,
    error = colors.coral,
    onError = colors.textInverse,
    errorContainer = colors.coralLight,
    onErrorContainer = colors.coral,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceContainerLow,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.border
)

private fun createLightColorScheme(colors: FinflowColors) = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.textInverse,
    primaryContainer = colors.primaryLight,
    onPrimaryContainer = colors.primary,
    secondary = colors.secondary,
    onSecondary = colors.textInverse,
    secondaryContainer = colors.secondaryLight,
    onSecondaryContainer = colors.secondary,
    error = colors.coral,
    onError = colors.textInverse,
    errorContainer = colors.coralLight,
    onErrorContainer = colors.coral,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceContainerLow,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.border
)

@Composable
fun FinancasTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkFinflowColors else LightFinflowColors
    val colorScheme = if (darkTheme) createDarkColorScheme(colors) else createLightColorScheme(colors)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.surface.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalFinflowColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
