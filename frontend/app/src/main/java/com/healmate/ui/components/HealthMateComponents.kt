package com.healmate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.healmate.ui.theme.CardTone
import com.healmate.ui.theme.Motion

/**
 * Tarjeta de marca: degradado diagonal del [tone] con una luz suave en la esquina
 * superior derecha. [overlay] tiñe la tarjeta según su estado (tomada, pausada,
 * cancelada) y el cambio se anima.
 */
@Composable
fun HealthCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.BLUE,
    overlay: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    val overlayColor by animateColorAsState(overlay, Motion.standard(), label = "cardOverlay")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Box(
            Modifier.drawBehind {
                drawRect(Brush.linearGradient(tone.colors, Offset.Zero, Offset(size.width, size.height)))
                val corner = Offset(size.width, 0f)
                val radius = size.maxDimension * 0.75f
                drawCircle(Brush.radialGradient(listOf(tone.glow, Color.Transparent), corner, radius), radius, corner)
                drawRect(overlayColor)
            }
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

/** Icono sobre un círculo tonal muy suave. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    val animatedTint by animateColorAsState(tint, Motion.standard(), label = "badge")
    Box(
        modifier = Modifier
            .size(size)
            .background(animatedTint.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = animatedTint, modifier = Modifier.size(20.dp))
    }
}

/** Estado compacto: punto de color + texto. El texto cambia con un fundido. */
@Composable
fun StatusPill(text: String, color: Color) {
    val animatedColor by animateColorAsState(color, Motion.standard(), label = "pill")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .background(animatedColor, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(Motion.standard(Motion.SHORT)) togetherWith fadeOut(Motion.standard(Motion.SHORT)) },
            label = "pillText"
        ) { value ->
            Text(value, style = MaterialTheme.typography.labelMedium, color = animatedColor)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BreathingIcon(icon, MaterialTheme.colorScheme.primary, Modifier.size(32.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Confirmación para acciones destructivas (eliminar, cancelar). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}
