package com.healmate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Tema de marca con superficies azul petróleo, azul claro para acciones,
 * verde para confirmaciones, ámbar para avisos y coral para errores.
 */
internal val BrandColorScheme = darkColorScheme(
    primary = BrandSky,
    onPrimary = BrandInk,
    primaryContainer = BrandBlue,
    onPrimaryContainer = Color(0xFFDAEEFC),
    inversePrimary = Color(0xFF245A7C),
    secondary = BrandMint,
    onSecondary = Color(0xFF123724),
    secondaryContainer = Color(0xFF254C38),
    onSecondaryContainer = Color(0xFFC5F4D4),
    tertiary = Color(0xFFFFD28F),
    onTertiary = Color(0xFF442B08),
    tertiaryContainer = Color(0xFF59401B),
    onTertiaryContainer = Color(0xFFFFE8C4),
    background = BrandNight,
    onBackground = Color(0xFFEBF5F8),
    surface = BrandInk,
    onSurface = Color(0xFFEBF5F8),
    surfaceVariant = Color(0xFF263E49),
    onSurfaceVariant = Color(0xFFBBD0D8),
    surfaceTint = BrandSky,
    surfaceContainerLowest = BrandNight,
    surfaceContainerLow = Color(0xFF10212A),
    surfaceContainer = BrandInk,
    surfaceContainerHigh = Color(0xFF1D323D),
    surfaceContainerHighest = Color(0xFF28414D),
    surfaceBright = Color(0xFF304A56),
    surfaceDim = BrandNight,
    inverseSurface = Color(0xFFDDEBF0),
    inverseOnSurface = Color(0xFF20353F),
    outline = Color(0xFF8BA9B3),
    outlineVariant = Color(0xFF405A62),
    error = Color(0xFFFFBBB2),
    onError = Color(0xFF571D19),
    errorContainer = Color(0xFF6D302A),
    onErrorContainer = Color(0xFFFFDDD8)
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
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HealthMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandColorScheme,
        typography = HealthMateTypography,
        shapes = HealthMateShapes,
        content = content
    )
}
