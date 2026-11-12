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

    /** Separación entre elementos que entran en cascada. */
    const val STAGGER_STEP = 45

    /** Máximo de pasos de cascada: a partir de ahí todos entran a la vez para no hacer esperar. */
    const val STAGGER_MAX_STEPS = 8

    /** Retraso de entrada del elemento [index] de una lista. */
    fun staggerDelay(index: Int): Int = index.coerceIn(0, STAGGER_MAX_STEPS) * STAGGER_STEP

    /** Resorte con un ligero rebote para selecciones. */
    fun <T> playful() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
}
