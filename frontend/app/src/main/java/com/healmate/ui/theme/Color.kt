package com.healmate.ui.theme

import androidx.compose.ui.graphics.Color

internal val BrandNight = Color(0xFF0D1B23)
internal val BrandInk = Color(0xFF142730)
internal val BrandBlue = Color(0xFF204765)
internal val BrandSky = Color(0xFFA9D8F4)
internal val BrandMint = Color(0xFF9DE2B6)
private val BrandTeal = Color(0xFF205762)

/**
 * Variantes de tarjeta: mismo lenguaje del panel de marca con distinta luz.
 * [colors] define el degradado de fondo y [glow] la luz suave de la esquina.
 */
enum class CardTone(val colors: List<Color>, val glow: Color) {
    BLUE(listOf(BrandInk, Color(0xFF1D384B)), BrandBlue.copy(alpha = 0.35f)),
    SLATE(listOf(Color(0xFF1B2D40), Color(0xFF213447)), BrandSky.copy(alpha = 0.08f)),
    GREEN(listOf(Color(0xFF172D28), Color(0xFF1C342C)), Color(0xFF346354).copy(alpha = 0.30f)),
    TEAL(listOf(Color(0xFF142D35), Color(0xFF18353C)), BrandTeal.copy(alpha = 0.35f))
}

/** Tinte verde que toma una tarjeta al marcar la dosis como tomada. */
internal const val TAKEN_OVERLAY_ALPHA = 0.14f

/** Luces del panel de marca, en orden de dibujo. */
internal val AuroraColors = listOf(
    BrandBlue.copy(alpha = 0.75f),
    BrandTeal.copy(alpha = 0.28f),
    BrandSky.copy(alpha = 0.10f)
)
