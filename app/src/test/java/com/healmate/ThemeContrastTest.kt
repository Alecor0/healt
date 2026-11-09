package com.healmate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.healmate.ui.theme.BrandIndigo
import com.healmate.ui.theme.BrandInk
import com.healmate.ui.theme.BrandMint
import com.healmate.ui.theme.DarkColorScheme
import com.healmate.ui.theme.LightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test
    fun textRolesHaveReadableContrastInBothThemes() {
        for (scheme in listOf(LightColorScheme, DarkColorScheme)) {
            val pairs = listOf(
                scheme.onBackground to scheme.background,
                scheme.onSurface to scheme.surface,
                scheme.onSurfaceVariant to scheme.surface,
                scheme.onSurfaceVariant to scheme.surfaceVariant,
                scheme.onPrimary to scheme.primary,
                scheme.primary to scheme.surface,
                scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondary to scheme.secondary,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onTertiaryContainer to scheme.tertiaryContainer,
                scheme.onError to scheme.error,
                scheme.onErrorContainer to scheme.errorContainer
            )
            for ((foreground, background) in pairs) assertReadable(foreground, background)
        }
    }

    @Test
    fun brandTextIsReadableAtBothEndsOfTheGradient() {
        for (background in listOf(BrandInk, BrandIndigo)) {
            assertReadable(Color.White, background)
            assertReadable(BrandMint, background)
        }
    }

    private fun assertReadable(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val contrast = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("Contraste de $contrast entre $foreground y $background", contrast >= 4.5f)
    }
}
