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
    val onAccent: Color,
    val userBubble: Color,
    val userBubbleText: Color,
    val codeBg: Color,
    val codeText: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color
)

data class AccentPreset(
    val name: String,
    val light: Color,
    val lightSoft: Color,
    val lightText: Color,
    val dark: Color,
    val darkSoft: Color,
    val darkText: Color
)

val AccentPresets = listOf(
    AccentPreset(
        "海蓝",
        Color(0xFF2563EB),
        Color(0xFFEAF0FF),
        Color(0xFF1D46B8),
        Color(0xFF7EA0FF),
        Color(0xFF172033),
        Color(0xFFB7C8FF)
    ),
    AccentPreset(
        "松绿",
        Color(0xFF0F766E),
        Color(0xFFE2F5F1),
        Color(0xFF0B5B55),
        Color(0xFF5BD6C4),
        Color(0xFF102925),
        Color(0xFFA0EDE2)
    ),
    AccentPreset(
        "珊瑚",
        Color(0xFFC94732),
        Color(0xFFFDECE8),
        Color(0xFFA53623),
        Color(0xFFFF8C7A),
        Color(0xFF301A15),
        Color(0xFFFFB4A8)
    ),
    AccentPreset(
        "葡萄",
        Color(0xFF7C3AED),
        Color(0xFFF1EAFF),
        Color(0xFF6127C7),
        Color(0xFFB896FF),
        Color(0xFF231A33),
        Color(0xFFD8C6FF)
    ),
    AccentPreset(
        "琥珀",
        Color(0xFF9A4A05),
        Color(0xFFFBF0DF),
        Color(0xFF7A3A04),
        Color(0xFFF0B35A),
        Color(0xFF2A2114),
        Color(0xFFF6D49B)
    )
)

val AccentLabels: List<String> = AccentPresets.map { it.name }

val AccentSwatches: List<Color> = AccentPresets.map { it.light }

private val Paper = AppColors(
    bg = Color(0xFFF6F3ED),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEFEAE1),
    border = Color(0xFFE5DED2),
    text = Color(0xFF1F1F1D),
    textSub = Color(0xFF6F6A61),
    accent = AccentPresets[0].light,
    accentSoft = AccentPresets[0].lightSoft,
    accentText = AccentPresets[0].lightText,
    onAccent = Color.White,
    userBubble = AccentPresets[0].light,
    userBubbleText = Color.White,
    codeBg = Color(0xFF16171B),
    codeText = Color(0xFFE9E6E0),
    ok = Color(0xFF16805F),
    warn = Color(0xFFB7791F),
    danger = Color(0xFFC2413A)
)

private val Ink = AppColors(
    bg = Color(0xFF121315),
    surface = Color(0xFF1B1C1F),
    surfaceAlt = Color(0xFF24252A),
    border = Color(0xFF313238),
    text = Color(0xFFF2F0EB),
    textSub = Color(0xFFA6A199),
    accent = AccentPresets[0].dark,
    accentSoft = AccentPresets[0].darkSoft,
    accentText = AccentPresets[0].darkText,
    onAccent = Color(0xFF121315),
    userBubble = AccentPresets[0].dark,
    userBubbleText = Color(0xFF121315),
    codeBg = Color(0xFF0E0F12),
    codeText = Color(0xFFE7E4DE),
    ok = Color(0xFF46C89E),
    warn = Color(0xFFE4AB58),
    danger = Color(0xFFF07A6B)
)

val LocalPalette = staticCompositionLocalOf { Paper }

val LocalDark = staticCompositionLocalOf { false }

val AppGradient = Brush.linearGradient(listOf(Color(0xFF4F7CFF), Color(0xFF2348C9)))

val AppGradientWarm = Brush.linearGradient(listOf(Color(0xFFF07A5D), Color(0xFFC9432D)))

val Serif = FontFamily.SansSerif
val Mono = FontFamily.Monospace
val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 18.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 16.sp,
        lineHeight = 23.sp,
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
        fontSize = 15.sp,
        lineHeight = 24.sp,
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
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Normal
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun AppColors.withAccent(index: Int, dark: Boolean): AppColors {
    val a = AccentPresets.getOrElse(index) { AccentPresets.first() }
    return if (dark) copy(
        accent = a.dark,
        accentSoft = a.darkSoft,
        accentText = a.darkText,
        onAccent = Color(0xFF121315),
        userBubble = a.dark,
        userBubbleText = Color(0xFF121315)
    ) else copy(
        accent = a.light,
        accentSoft = a.lightSoft,
        accentText = a.lightText,
        onAccent = Color.White,
        userBubble = a.light,
        userBubbleText = Color.White
    )
}

private fun AppColors.glassy(alpha: Float): AppColors = copy(
    bg = Color.Transparent,
    surface = surface.copy(alpha = alpha.coerceIn(0.62f, 0.97f)),
    surfaceAlt = surfaceAlt.copy(alpha = alpha.coerceIn(0.52f, 0.96f)),
    border = border.copy(alpha = 0.62f),
    userBubble = userBubble.copy(alpha = 0.92f),
    accentSoft = accentSoft.copy(alpha = 0.9f),
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
            onPrimary = palette.onAccent,
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
            onPrimary = palette.onAccent,
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
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalDark provides dark
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

val pal: AppColors
    @Composable get() = LocalPalette.current

val darkTheme: Boolean
    @Composable get() = LocalDark.current
