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
import com.healmate.ui.theme.BrandInk
import com.healmate.ui.theme.BrandMint

/** Panel de marca: un solo color plano, sin degradado ni decoraciones. */
@Composable
internal fun BrandPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = BrandInk
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
        letterSpacing = 1.4.sp
    )
}
