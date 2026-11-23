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
import androidx.compose.ui.text.font.FontFamily
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

private val Paper = AppColors(
    bg = Color(0xFFFAF9F6),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF2F0EA),
    border = Color(0xFFE3DFD6),
    text = Color(0xFF16161A),
    textSub = Color(0xFF6C6862),
    accent = Color(0xFFE4502A),
    accentSoft = Color(0xFFFDECE5),
    accentText = Color(0xFFAF3717),
    userBubble = Color(0xFFF3F0E9),
    userBubbleText = Color(0xFF201E1A),
    codeBg = Color(0xFF15171B),
    codeText = Color(0xFFE7E4DE),
    ok = Color(0xFF16795D),
    warn = Color(0xFFA66F19),
    danger = Color(0xFFBE3A2B)
)

private val Ink = AppColors(
    bg = Color(0xFF0B0B0D),
    surface = Color(0xFF141418),
    surfaceAlt = Color(0xFF1B1B20),
    border = Color(0xFF27272E),
    text = Color(0xFFEFEDE8),
    textSub = Color(0xFF9A968E),
    accent = Color(0xFFFF7048),
    accentSoft = Color(0xFF2B1A14),
    accentText = Color(0xFFFFA184),
    userBubble = Color(0xFF1F1C19),
    userBubbleText = Color(0xFFF0EAE2),
    codeBg = Color(0xFF0E0F12),
    codeText = Color(0xFFE4E1DB),
    ok = Color(0xFF3FBF95),
    warn = Color(0xFFD9A24A),
    danger = Color(0xFFE8705F)
)

val LocalPalette = staticCompositionLocalOf { Paper }

val LocalDark = staticCompositionLocalOf { false }

val AppGradient = Brush.linearGradient(listOf(Color(0xFFFF7A4D), Color(0xFFD8411D)))

val AppGradientWarm = Brush.linearGradient(listOf(Color(0xFFE4502A), Color(0xFF9E2F14)))

val Serif = FontFamily.Serif
val Mono = FontFamily.Monospace
val Sans = FontFamily.Default

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Serif,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Serif,
        fontSize = 25.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Serif,
        fontSize = 21.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Serif,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 15.5.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontFamily = Mono,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Mono,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.7.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

private data class Accent(val light: Color, val lightSoft: Color, val lightText: Color, val dark: Color, val darkSoft: Color, val darkText: Color)

private val Accents = listOf(
    Accent(Color(0xFFE4502A), Color(0xFFFDECE5), Color(0xFFAF3717), Color(0xFFFF7048), Color(0xFF2B1A14), Color(0xFFFFA184)),
    Accent(Color(0xFF2F5BEA), Color(0xFFE9EEFF), Color(0xFF1E3FB0), Color(0xFF7D9BFF), Color(0xFF141C33), Color(0xFFA9BEFF)),
    Accent(Color(0xFF0E7A5F), Color(0xFFE4F5EF), Color(0xFF0A5A46), Color(0xFF4FD1AC), Color(0xFF0F2A24), Color(0xFF8FE8D0)),
    Accent(Color(0xFF8A3FFC), Color(0xFFF1E9FF), Color(0xFF6527C4), Color(0xFFB78BFF), Color(0xFF241733), Color(0xFFD3B8FF)),
    Accent(Color(0xFFB07A12), Color(0xFFFBF1D9), Color(0xFF7C560C), Color(0xFFE0B154), Color(0xFF2B2313), Color(0xFFF0CC86))
)

private fun AppColors.withAccent(index: Int, dark: Boolean): AppColors {
    val a = Accents.getOrElse(index) { Accents.first() }
    return if (dark) copy(
        accent = a.dark,
        accentSoft = a.darkSoft,
        accentText = a.darkText
    ) else copy(
        accent = a.light,
        accentSoft = a.lightSoft,
        accentText = a.lightText
    )
}

private fun AppColors.glassy(alpha: Float): AppColors = copy(
    bg = Color.Transparent,
    surface = surface.copy(alpha = alpha.coerceIn(0.42f, 0.96f)),
    surfaceAlt = surfaceAlt.copy(alpha = (alpha - 0.08f).coerceIn(0.34f, 0.94f)),
    border = border.copy(alpha = 0.5f),
    userBubble = userBubble.copy(alpha = (alpha + 0.02f).coerceAtMost(0.96f)),
    accentSoft = accentSoft.copy(alpha = 0.86f),
    codeBg = codeBg.copy(alpha = 0.97f)
)

@Composable
fun AIChatTheme(
    themeMode: Int,
    accent: Int = 0,
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
    val base = (if (dark) Ink else Paper).withAccent(accent, dark)
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
