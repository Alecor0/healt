package com.healmate.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.healmate.ui.components.BrandLabel
import com.healmate.ui.components.BrandPanel
import com.healmate.ui.theme.BrandMint
import com.healmate.ui.theme.Motion

@Composable
internal fun AuthLayout(
    title: String,
    description: String,
    formTitle: String,
    form: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // El formulario sube y aparece suavemente al abrir la pantalla.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, Motion.standard(Motion.LONG)) }
    Box(
        modifier = Modifier.fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandPanel {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp)
                            .background(BrandMint.copy(alpha = 0.16f), MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = BrandMint)
                    }
                    BrandLabel("HEALTHMATE")
                }
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(description, color = Color(0xFFE8E1FA), style = MaterialTheme.typography.bodyMedium)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * 24.dp.toPx()
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = {
                        Text(formTitle, style = MaterialTheme.typography.titleLarge)
                        form()
                    }
                )
            }
        }
    }
}

@Composable
internal fun AuthMessage(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isError) colors.errorContainer else colors.surfaceVariant,
        contentColor = if (isError) colors.onErrorContainer else colors.onSurfaceVariant
    ) {
        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
