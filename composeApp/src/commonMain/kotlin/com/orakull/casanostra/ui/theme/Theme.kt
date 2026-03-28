package com.orakull.casanostra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

val CasaNostraDarkColors = darkColorScheme(
    // Primary — осветлённый sage green для тёмного фона
    primary = Color(0xFF8FA86B),
    onPrimary = Color(0xFF1A2408),
    primaryContainer = Color(0xFF3D4A28),
    onPrimaryContainer = Color(0xFFDAE6C3),

    // Secondary — тёплый бежево-серый
    secondary = Color(0xFFC8C6B4),
    onSecondary = Color(0xFF1A1A0E),
    secondaryContainer = Color(0xFF46453A),
    onSecondaryContainer = Color(0xFFE2E1CF),

    // Tertiary — тёплое золото, эффект свечи
    tertiary = Color(0xFFD4B46A),
    onTertiary = Color(0xFF2B2108),
    tertiaryContainer = Color(0xFF5A4B24),
    onTertiaryContainer = Color(0xFFF5E6BC),

    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB),

    // Фоны — тёплый тёмно-оливковый (не холодный чёрный)
    background = Color(0xFF1A1914),
    onBackground = Color(0xFFE8E2D6),
    surface = Color(0xFF1A1914),
    onSurface = Color(0xFFE8E2D6),
    surfaceVariant = Color(0xFF4B4739),
    onSurfaceVariant = Color(0xFFCEC6B4),
    outline = Color(0xFF989080),
    outlineVariant = Color(0xFF4B4739),
    inverseSurface = Color(0xFFE8E2D6),
    inverseOnSurface = Color(0xFF31302A),
    surfaceContainerLowest = Color(0xFF141310),
    surfaceContainerLow = Color(0xFF1F1E18),
    surfaceContainer = Color(0xFF242319),
    surfaceContainerHigh = Color(0xFF2F2E25),
    surfaceContainerHighest = Color(0xFF3A3930),
)

// Кастомные цвета приложения (Mute/Solo и т.п.), не входящие в Material 3 схему
data class CasaNostraExtraColors(
    val muteActive: Color,
    val soloActive: Color,
    val onMuteActive: Color,
    val onSoloActive: Color,
)

val LightExtraColors = CasaNostraExtraColors(
    muteActive = Color(0xFF5B7AA5),
    soloActive = Color(0xFFC49A3C),
    onMuteActive = Color.White,
    onSoloActive = Color.White,
)

val DarkExtraColors = CasaNostraExtraColors(
    muteActive = Color(0xFF7BA0CC),
    soloActive = Color(0xFFD4B46A),
    onMuteActive = Color(0xFF1A2A3A),
    onSoloActive = Color(0xFF2B2108),
)

val LocalCasaNostraColors = staticCompositionLocalOf { LightExtraColors }

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
fun CasaNostraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CasaNostraDarkColors else CasaNostraLightColors
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalCasaNostraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CasaNostraTypography,
            content = content,
        )
    }
}
