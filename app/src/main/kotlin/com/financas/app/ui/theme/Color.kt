package com.financas.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Finflow Design System: Colors Specification
// ==========================================
data class FinflowColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val border: Color,
    val borderSubtle: Color,
    val outline: Color,
    val primary: Color,
    val primaryContainer: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val secondaryLight: Color,
    val coral: Color,
    val coralLight: Color,
    val amber: Color,
    val amberLight: Color,
    val purple: Color,
    val purpleLight: Color,
    val sky: Color,
    val skyLight: Color,
    val teal: Color,
    val tealLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color
)

// Modo Diurno (Light Mode)
val LightFinflowColors = FinflowColors(
    isDark = false,
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FF),
    surfaceContainer = Color(0xFFEAEDFF),
    surfaceContainerHigh = Color(0xFFE2E7FF),
    surfaceContainerHighest = Color(0xFFDAE2FD),
    border = Color(0xFFE5E7EB),
    borderSubtle = Color(0xFFF0F2F8),
    outline = Color(0xFFC7C4D8),
    primary = Color(0xFF3525CD),
    primaryContainer = Color(0xFF4F46E5),
    primaryLight = Color(0xFFE2DFFF),
    secondary = Color(0xFF006C49),
    secondaryContainer = Color(0xFF6CF8BB),
    secondaryLight = Color(0xFFD1FAE5),
    coral = Color(0xFFEF4444),
    coralLight = Color(0xFFFEE2E2),
    amber = Color(0xFFF59E0B),
    amberLight = Color(0xFFFEF3C7),
    purple = Color(0xFF7C3AED),
    purpleLight = Color(0xFFEDE9FE),
    sky = Color(0xFF0284C7),
    skyLight = Color(0xFFE0F2FE),
    teal = Color(0xFF0D9488),
    tealLight = Color(0xFFCCFBF1),
    textPrimary = Color(0xFF131B2E),
    textSecondary = Color(0xFF464555),
    textMuted = Color(0xFF777587),
    textInverse = Color(0xFFFFFFFF)
)

// Modo Noturno (Dark Mode) - Alto Contraste
val DarkFinflowColors = FinflowColors(
    isDark = true,
    background = Color(0xFF090D14),
    surface = Color(0xFF141923),
    surfaceContainerLow = Color(0xFF1C2230),
    surfaceContainer = Color(0xFF252D3E),
    surfaceContainerHigh = Color(0xFF2F394E),
    surfaceContainerHighest = Color(0xFF3B4760),
    border = Color(0xFF222B3B),
    borderSubtle = Color(0xFF171D28),
    outline = Color(0xFF4B5563),
    primary = Color(0xFF6366F1),
    primaryContainer = Color(0xFF4F46E5),
    primaryLight = Color(0xFF2E2A72),
    secondary = Color(0xFF10B981),
    secondaryContainer = Color(0xFF059669),
    secondaryLight = Color(0xFF064E3B),
    coral = Color(0xFFF87171),
    coralLight = Color(0xFF450A0A),
    amber = Color(0xFFFBBF24),
    amberLight = Color(0xFF451A03),
    purple = Color(0xFFA855F7),
    purpleLight = Color(0xFF3B0764),
    sky = Color(0xFF38BDF8),
    skyLight = Color(0xFF082F49),
    teal = Color(0xFF2DD4BF),
    tealLight = Color(0xFF042F2E),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    textInverse = Color(0xFF090D14)
)

val LocalFinflowColors = staticCompositionLocalOf { DarkFinflowColors }

object FinflowTheme {
    val colors: FinflowColors
        @Composable
        get() = LocalFinflowColors.current
}

// Dynamic properties with @Composable getter for seamless theme reactivity
val FinflowBackground: Color @Composable get() = FinflowTheme.colors.background
val FinflowSurface: Color @Composable get() = FinflowTheme.colors.surface
val FinflowSurfaceContainerLow: Color @Composable get() = FinflowTheme.colors.surfaceContainerLow
val FinflowSurfaceContainer: Color @Composable get() = FinflowTheme.colors.surfaceContainer
val FinflowSurfaceContainerHigh: Color @Composable get() = FinflowTheme.colors.surfaceContainerHigh
val FinflowSurfaceContainerHighest: Color @Composable get() = FinflowTheme.colors.surfaceContainerHighest
val FinflowBorder: Color @Composable get() = FinflowTheme.colors.border
val FinflowBorderSubtle: Color @Composable get() = FinflowTheme.colors.borderSubtle
val FinflowOutline: Color @Composable get() = FinflowTheme.colors.outline

val FinflowPrimary: Color @Composable get() = FinflowTheme.colors.primary
val FinflowPrimaryContainer: Color @Composable get() = FinflowTheme.colors.primaryContainer
val FinflowPrimaryLight: Color @Composable get() = FinflowTheme.colors.primaryLight
val FinflowPrimaryFixed: Color @Composable get() = FinflowTheme.colors.primaryLight
val FinflowSecondary: Color @Composable get() = FinflowTheme.colors.secondary
val FinflowSecondaryContainer: Color @Composable get() = FinflowTheme.colors.secondaryContainer
val FinflowSecondaryLight: Color @Composable get() = FinflowTheme.colors.secondaryLight

val FinflowCoral: Color @Composable get() = FinflowTheme.colors.coral
val FinflowCoralLight: Color @Composable get() = FinflowTheme.colors.coralLight
val FinflowAmber: Color @Composable get() = FinflowTheme.colors.amber
val FinflowAmberLight: Color @Composable get() = FinflowTheme.colors.amberLight
val FinflowPurple: Color @Composable get() = FinflowTheme.colors.purple
val FinflowPurpleLight: Color @Composable get() = FinflowTheme.colors.purpleLight
val FinflowSky: Color @Composable get() = FinflowTheme.colors.sky
val FinflowSkyLight: Color @Composable get() = FinflowTheme.colors.skyLight
val FinflowTeal: Color @Composable get() = FinflowTheme.colors.teal
val FinflowTealLight: Color @Composable get() = FinflowTheme.colors.tealLight

val FinflowTextPrimary: Color @Composable get() = FinflowTheme.colors.textPrimary
val FinflowTextSecondary: Color @Composable get() = FinflowTheme.colors.textSecondary
val FinflowTextMuted: Color @Composable get() = FinflowTheme.colors.textMuted
val FinflowTextInverse: Color @Composable get() = FinflowTheme.colors.textInverse

// Legacy compatibility aliases
val DarkBackground: Color @Composable get() = FinflowTheme.colors.background
val DarkSurface: Color @Composable get() = FinflowTheme.colors.surface
val DarkSurfaceVariant: Color @Composable get() = FinflowTheme.colors.surfaceContainerLow
val DarkSurfaceHighlight: Color @Composable get() = FinflowTheme.colors.surfaceContainer
val DarkBorder: Color @Composable get() = FinflowTheme.colors.border

val AccentBlue: Color @Composable get() = FinflowTheme.colors.primaryContainer
val AccentBlueContainer: Color @Composable get() = FinflowTheme.colors.primaryLight
val AccentEmerald: Color @Composable get() = FinflowTheme.colors.secondary
val AccentEmeraldContainer: Color @Composable get() = FinflowTheme.colors.secondaryLight
val AccentCoral: Color @Composable get() = FinflowTheme.colors.coral
val AccentCoralContainer: Color @Composable get() = FinflowTheme.colors.coralLight
val AccentAmber: Color @Composable get() = FinflowTheme.colors.amber
val AccentAmberContainer: Color @Composable get() = FinflowTheme.colors.amberLight
val AccentPurple: Color @Composable get() = FinflowTheme.colors.purple

val TextPrimary: Color @Composable get() = FinflowTheme.colors.textPrimary
val TextSecondary: Color @Composable get() = FinflowTheme.colors.textSecondary
val TextMuted: Color @Composable get() = FinflowTheme.colors.textMuted
