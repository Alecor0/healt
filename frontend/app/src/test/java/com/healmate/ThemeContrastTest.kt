package com.healmate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.healmate.ui.theme.AuroraColors
import com.healmate.ui.theme.BrandColorScheme
import com.healmate.ui.theme.BrandInk
import com.healmate.ui.theme.BrandMint
import com.healmate.ui.theme.CardTone
import com.healmate.ui.theme.TAKEN_OVERLAY_ALPHA
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private val scheme = BrandColorScheme
    private val accents = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.error)

    @Test
    fun textRolesHaveReadableContrast() {
        val pairs = listOf(
            scheme.onBackground to scheme.background,
            scheme.onSurface to scheme.surface,
            scheme.onSurfaceVariant to scheme.surface,
            scheme.onSurfaceVariant to scheme.surfaceVariant,
            scheme.onPrimary to scheme.primary,
            scheme.onPrimaryContainer to scheme.primaryContainer,
            scheme.onSecondary to scheme.secondary,
            scheme.onSecondaryContainer to scheme.secondaryContainer,
            scheme.onTertiary to scheme.tertiary,
            scheme.onTertiaryContainer to scheme.tertiaryContainer,
            scheme.onError to scheme.error,
            scheme.onErrorContainer to scheme.errorContainer,
            scheme.onSurface to scheme.surfaceContainerHigh,
            scheme.onSurface to scheme.surfaceContainerHighest,
            scheme.onSurfaceVariant to scheme.surfaceBright,
            scheme.inverseOnSurface to scheme.inverseSurface,
            scheme.inversePrimary to scheme.inverseSurface
        )
        for ((foreground, background) in pairs) assertContrast(foreground, background)
    }

    @Test
    fun accentTextIsReadableOnTheBackgrounds() {
        // Enlaces, estados y severidad del dolor se escriben con estos colores.
        for (background in listOf(scheme.background, scheme.surface, scheme.surfaceContainerHighest)) {
            for (accent in accents) assertContrast(accent, background)
        }
    }

    @Test
    fun inputOutlinesAreVisibleOnFormSurfaces() {
        for (background in listOf(scheme.surface, scheme.surfaceVariant, scheme.surfaceContainerHighest)) {
            assertContrast(scheme.outline, background, minimum = 3f)
        }
    }

    @Test
    fun cardTextIsReadableOnEveryToneAtItsBrightestPoint() {
        val takenOverlay = scheme.secondary.copy(alpha = TAKEN_OVERLAY_ALPHA)
        for (tone in CardTone.entries) {
            for (stop in tone.colors) {
                // Peor caso: justo bajo la luz de la esquina, y además con la capa de "tomada".
                val lit = tone.glow.compositeOver(stop)
                for (background in listOf(lit, takenOverlay.compositeOver(lit))) {
                    for (foreground in listOf(scheme.onSurface, scheme.onSurfaceVariant) + accents) {
                        assertContrast(foreground, background, "$tone")
                    }
                }
            }
        }
    }

    @Test
    fun brandTextIsReadableWhereAllAuroraLightsOverlap() {
        val worst = AuroraColors.fold(BrandInk) { base, color -> color.compositeOver(base) }
        for (foreground in listOf(Color.White, scheme.onSurfaceVariant, scheme.primary, BrandMint)) {
            assertContrast(foreground, worst)
        }
    }

    private fun assertContrast(foreground: Color, background: Color, where: String = "", minimum: Float = 4.5f) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val contrast = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("Contraste de $contrast entre $foreground y $background $where", contrast >= minimum)
    }
}
