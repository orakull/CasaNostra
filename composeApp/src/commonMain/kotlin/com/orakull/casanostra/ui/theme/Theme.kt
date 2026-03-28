package com.orakull.casanostra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CasaNostraLightColors = lightColorScheme(
    // Primary — sage/olive green (из лого)
    primary = Color(0xFF5B6B3C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAE6C3),
    onPrimaryContainer = Color(0xFF1A2408),

    // Secondary — тёплый коричнево-оливковый
    secondary = Color(0xFF5D5C4D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E1CF),
    onSecondaryContainer = Color(0xFF1A1A0E),

    // Tertiary — приглушённое золото
    tertiary = Color(0xFF7D6B3A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E6BC),
    onTertiaryContainer = Color(0xFF2B2108),

    // Error
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Фоны — тёплый крем
    background = Color(0xFFFDF8F0),
    onBackground = Color(0xFF1C1B16),
    surface = Color(0xFFFDF8F0),
    onSurface = Color(0xFF1C1B16),
    surfaceVariant = Color(0xFFF0EBE0),
    onSurfaceVariant = Color(0xFF4B4739),
    outline = Color(0xFF7C7767),
    outlineVariant = Color(0xFFCEC6B4),
    inverseSurface = Color(0xFF31302A),
    inverseOnSurface = Color(0xFFF4F0E7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F3EB),
    surfaceContainer = Color(0xFFF2EDE5),
    surfaceContainerHigh = Color(0xFFECE7DF),
    surfaceContainerHighest = Color(0xFFE6E1D9),
)

val CasaNostraTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun CasaNostraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CasaNostraLightColors,
        typography = CasaNostraTypography,
        content = content,
    )
}
