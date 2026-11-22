package com.kevin233.chat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val text: Color,
    val textSub: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val userBubble: Color,
    val userBubbleText: Color,
    val codeBg: Color,
    val codeText: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color
)

private val LightPalette = AppColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF6F7FA),
    border = Color(0xFFE8EBF2),
    text = Color(0xFF14161B),
    textSub = Color(0xFF6E7686),
    accent = Color(0xFF4D6BFE),
    accentSoft = Color(0xFFEEF2FF),
    accentText = Color(0xFF3B55D9),
    userBubble = Color(0xFFEFF3FF),
    userBubbleText = Color(0xFF1B2440),
    codeBg = Color(0xFF14171E),
    codeText = Color(0xFFE7EAF0),
    ok = Color(0xFF17A673),
    warn = Color(0xFFB7791F),
    danger = Color(0xFFD64545)
)

private val DarkPalette = AppColors(
    bg = Color(0xFF0E1014),
    surface = Color(0xFF15181E),
    surfaceAlt = Color(0xFF1A1E26),
    border = Color(0xFF262B35),
    text = Color(0xFFEDEFF3),
    textSub = Color(0xFF98A0AE),
    accent = Color(0xFF7C93FF),
    accentSoft = Color(0xFF1B2136),
    accentText = Color(0xFFA9B8FF),
    userBubble = Color(0xFF1D2740),
    userBubbleText = Color(0xFFE7ECFF),
    codeBg = Color(0xFF0B0D11),
    codeText = Color(0xFFE2E6ED),
    ok = Color(0xFF3DDC97),
    warn = Color(0xFFE7B65A),
    danger = Color(0xFFFF6B6B)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val LocalDark = staticCompositionLocalOf { false }

val AppGradient = Brush.linearGradient(listOf(Color(0xFF4D6BFE), Color(0xFF7A8CFF)))

val AppGradientWarm = Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF4D6BFE)))

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun AppColors.glassy(alpha: Float): AppColors = copy(
    bg = Color.Transparent,
    surface = surface.copy(alpha = alpha.coerceIn(0.4f, 0.96f)),
    surfaceAlt = surfaceAlt.copy(alpha = (alpha - 0.06f).coerceIn(0.36f, 0.94f)),
    border = border.copy(alpha = 0.55f),
    userBubble = userBubble.copy(alpha = (alpha + 0.04f).coerceAtMost(0.96f)),
    accentSoft = accentSoft.copy(alpha = 0.82f),
    codeBg = codeBg.copy(alpha = 0.96f)
)

@Composable
fun AIChatTheme(
    themeMode: Int,
    glassAlpha: Float = 0.82f,
    wallpaper: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val base = if (dark) DarkPalette else LightPalette
    val palette = if (wallpaper) base.glassy(glassAlpha) else base
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.accentText,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textSub,
            outline = palette.border,
            outlineVariant = palette.border,
            error = palette.danger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.accentText,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textSub,
            outline = palette.border,
            outlineVariant = palette.border,
            error = palette.danger,
            onError = Color.White
        )
    }
    CompositionLocalProvider(LocalPalette provides palette, LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

val pal: AppColors
    @Composable get() = LocalPalette.current

val darkTheme: Boolean
    @Composable get() = LocalDark.current
