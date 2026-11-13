package com.healmate.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.healmate.ui.theme.Motion
import kotlin.math.cos
import kotlin.math.sin

/**
 * Entrada en cascada: el elemento [index] aparece subiendo y desvaneciéndose
 * un poco después del anterior. Solo se anima la primera vez que se muestra.
 */
fun Modifier.staggeredEntrance(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(Motion.MEDIUM + 80, delayMillis = Motion.staggerDelay(index), easing = FastOutSlowInEasing)
        )
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 28.dp.toPx()
        val scale = 0.96f + 0.04f * p
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Se encoge un poco mientras se mantiene presionado y vuelve con un rebote.
 * No consume el toque: el botón o la tarjeta siguen recibiendo su clic.
 */
fun Modifier.pressScale(pressedScale: Float = 0.95f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.playful(), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            pressed = true
            waitForUpOrCancellation(PointerEventPass.Initial)
            pressed = false
        }
    }
}

/** Fondo de "aurora": manchas de luz que se desplazan lentamente. */
@Composable
fun AuroraBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "auroraAngle"
    )
    Canvas(modifier) {
        colors.forEachIndexed { i, color ->
            val phase = angle + i * 2.1f
            val center = Offset(
                x = size.width * (0.5f + 0.38f * cos(phase)),
                y = size.height * (0.5f + 0.42f * sin(phase * 0.8f))
            )
            val radius = size.maxDimension * (0.55f + 0.08f * i)
            drawCircle(
                brush = Brush.radialGradient(listOf(color, Color.Transparent), center, radius),
                radius = radius,
                center = center
            )
        }
    }
}

/** Icono que crece y se encoge suavemente, como si respirara. */
@Composable
fun BreathingIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale"
    )
    Icon(
        icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = 0.6f + 0.4f * (scale - 0.9f) / 0.18f
        }
    )
}

/** El botón flotante se expande arriba de la lista y se encoge a solo icono al desplazarse. */
@Composable
fun rememberFabExpanded(listState: LazyListState): State<Boolean> =
    remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

/** Sacudida horizontal (para errores de validación). Llama a `shake()` para dispararla. */
class ShakeController {
    internal val offset = Animatable(0f)

    suspend fun shake() {
        offset.snapTo(0f)
        offset.animateTo(
            0f,
            keyframes {
                durationMillis = 420
                -14f at 60
                12f at 130
                -9f at 200
                6f at 270
                -3f at 340
            }
        )
    }
}

fun Modifier.shake(controller: ShakeController): Modifier =
    graphicsLayer { translationX = controller.offset.value * density }

/** Valor 0..1 que va y vuelve sin parar; útil para pulsos y halos. */
@Composable
fun rememberPulse(durationMillis: Int = 1_400): State<Float> =
    rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearOutSlowInEasing)),
        label = "pulseValue"
    )
