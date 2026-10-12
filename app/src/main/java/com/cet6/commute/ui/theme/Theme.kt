package com.cet6.commute.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 全局字号缩放(设置页可调,供试卷等自定义排版读取) */
val LocalFontScale = staticCompositionLocalOf { 1f }

/** 字号档位:0小 1标准 2大 3特大 */
val FONT_SCALES = listOf(0.88f, 1.0f, 1.14f, 1.3f)
val FONT_SCALE_LABELS = listOf("小", "标准", "大", "特大")

/** 考试卷面配色(仿照真实试卷:纸白底面、墨黑正文、灰色分隔线) */
data class ExamPaperColors(
    val paper: Color,
    val ink: Color,
    val line: Color,
    val faint: Color
)

val LocalExamPaperColors = staticCompositionLocalOf {
    ExamPaperColors(Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFFB0B4BA), Color(0xFF6B7078))
}

// 清晨蓝绿配色,字号整体偏大(通勤场景)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF0D3A75),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F1EC),
    onSecondaryContainer = Color(0xFF00443C),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE3E8F0),
    onSurfaceVariant = Color(0xFF44474E),
    error = Color(0xFFD32F2F),
    outline = Color(0xFF90A4AE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D2540),
    primaryContainer = Color(0xFF1E4A7A),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00332D),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFC8F1EC),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE2E4E8),
    surface = Color(0xFF181C22),
    onSurface = Color(0xFFE2E4E8),
    surfaceVariant = Color(0xFF252B34),
    onSurfaceVariant = Color(0xFFB9C0CA),
    error = Color(0xFFEF9A9A),
    outline = Color(0xFF607D8B)
)

private fun TextStyle.scaled(s: Float): TextStyle {
    val f: TextUnit = fontSize * s
    val l: TextUnit = if (lineHeight.isSpecified) lineHeight * s else lineHeight
    return copy(fontSize = f, lineHeight = l)
}

private val BaseType = Typography(
    displayMedium = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 15.sp),
    labelSmall = TextStyle(fontSize = 12.sp)
)

/** 按 fontScale 缩放后的排版 */
fun scaledTypography(s: Float): Typography {
    if (s == 1f) return BaseType
    return Typography(
        displayLarge = BaseType.displayLarge.scaled(s),
        displayMedium = BaseType.displayMedium.scaled(s),
        displaySmall = BaseType.displaySmall.scaled(s),
        headlineLarge = BaseType.headlineLarge.scaled(s),
        headlineMedium = BaseType.headlineMedium.scaled(s),
        headlineSmall = BaseType.headlineSmall.scaled(s),
        titleLarge = BaseType.titleLarge.scaled(s),
        titleMedium = BaseType.titleMedium.scaled(s),
        titleSmall = BaseType.titleSmall.scaled(s),
        bodyLarge = BaseType.bodyLarge.scaled(s),
        bodyMedium = BaseType.bodyMedium.scaled(s),
        bodySmall = BaseType.bodySmall.scaled(s),
        labelLarge = BaseType.labelLarge.scaled(s),
        labelMedium = BaseType.labelMedium.scaled(s),
        labelSmall = BaseType.labelSmall.scaled(s)
    )
}

@Composable
fun AppTheme(mode: ThemeMode, fontScale: Float = 1f, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val examColors = if (dark) {
        ExamPaperColors(
            paper = Color(0xFFF5F2EA),      // 深色模式下的"纸面"用暖白,保持试卷质感
            ink = Color(0xFF1A1A1A),
            line = Color(0xFF8A8F96),
            faint = Color(0xFF5C6068)
        )
    } else {
        ExamPaperColors(
            paper = Color(0xFFFFFFFE),
            ink = Color(0xFF1A1A1A),
            line = Color(0xFFB0B4BA),
            faint = Color(0xFF6B7078)
        )
    }
    CompositionLocalProvider(
        LocalFontScale provides fontScale,
        LocalExamPaperColors provides examColors
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = scaledTypography(fontScale),
            content = content
        )
    }
}
