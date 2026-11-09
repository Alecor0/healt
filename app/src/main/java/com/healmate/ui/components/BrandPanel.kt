package com.healmate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.ui.theme.BrandIndigo
import com.healmate.ui.theme.BrandInk
import com.healmate.ui.theme.BrandMint

@Composable
internal fun BrandPanel(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.horizontalGradient(listOf(BrandInk, BrandIndigo)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.91f, size.height * 0.12f)
            val ring = BrandMint.copy(alpha = 0.12f)
            drawCircle(ring, 46.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            drawCircle(ring, 70.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            drawCircle(ring, 94.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        }
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun BrandLabel(text: String) {
    Text(
        text = text,
        color = BrandMint,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
}
