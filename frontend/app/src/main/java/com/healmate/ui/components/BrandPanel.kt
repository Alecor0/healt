package com.healmate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import com.healmate.ui.theme.AuroraColors
import com.healmate.ui.theme.BrandInk
import com.healmate.ui.theme.BrandSky

/** Panel de marca: fondo oscuro con una aurora animada detrás del contenido. */
@Composable
internal fun BrandPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = BrandInk
    ) {
        Box {
            // Luces suaves en movimiento; con alfa bajo para que el texto blanco siga legible.
            AuroraBackground(
                colors = AuroraColors,
                modifier = Modifier.matchParentSize()
            )
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
internal fun BrandLabel(text: String) {
    Text(
        text = text,
        color = BrandSky,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.4.sp
    )
}
