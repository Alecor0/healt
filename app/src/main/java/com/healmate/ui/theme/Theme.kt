package com.healmate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4E419E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E3FF),
    onPrimaryContainer = Color(0xFF2E2366),
    secondary = Color(0xFF00685D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F4EB),
    onSecondaryContainer = Color(0xFF00483E),
    tertiary = Color(0xFF794B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF542F00),
    background = Color(0xFFF5F3FA),
    onBackground = Color(0xFF221D35),
    surface = Color.White,
    onSurface = Color(0xFF221D35),
    surfaceVariant = Color(0xFFEDE9F5),
    onSurfaceVariant = Color(0xFF57516A),
    outline = Color(0xFF77718A),
    outlineVariant = Color(0xFFDCD7EA),
    error = Color(0xFFAA2544),
    onError = Color.White,
    errorContainer = Color(0xFFFFDDE5),
    onErrorContainer = Color(0xFF65142B)
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFCDC4FF),
    onPrimary = Color(0xFF291B58),
    primaryContainer = Color(0xFF423571),
    onPrimaryContainer = Color(0xFFF0EAFF),
    secondary = Color(0xFF87DFC9),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF1A4B42),
    onSecondaryContainer = Color(0xFFB9F4E2),
    tertiary = Color(0xFFFFCC80),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF5C3C10),
    onTertiaryContainer = Color(0xFFFFE8C2),
    background = Color(0xFF141121),
    onBackground = Color(0xFFF2EDF9),
    surface = Color(0xFF211D32),
    onSurface = Color(0xFFF2EDF9),
    surfaceVariant = Color(0xFF332C46),
    onSurfaceVariant = Color(0xFFC9C0DC),
    outline = Color(0xFF9990AE),
    outlineVariant = Color(0xFF48405D),
    error = Color(0xFFFFB3C2),
    onError = Color(0xFF5D1027),
    errorContainer = Color(0xFF73243B),
    onErrorContainer = Color(0xFFFFDDE5)
)

private val defaultType = Typography()
private val HealthMateTypography = Typography(
    displaySmall = defaultType.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineMedium = defaultType.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = defaultType.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = defaultType.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = defaultType.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = defaultType.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = defaultType.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = defaultType.labelMedium.copy(fontWeight = FontWeight.SemiBold)
)
private val HealthMateShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HealthMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = HealthMateTypography,
        shapes = HealthMateShapes,
        content = content
    )
}
