package com.healmate.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Especificaciones de movimiento compartidas para que toda la app se sienta igual. */
internal object Motion {
    const val SHORT = 180
    const val MEDIUM = 320
    const val LONG = 700

    fun <T> standard(duration: Int = MEDIUM) = tween<T>(duration, easing = FastOutSlowInEasing)

    /** Resorte suave sin rebote para cambios de color y tamaño. */
    fun <T> gentle() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Resorte con un ligero rebote para selecciones. */
    fun <T> playful() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
}
