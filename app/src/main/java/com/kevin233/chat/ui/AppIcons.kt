package com.kevin233.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CacheStack: ImageVector by lazy {
    ImageVector.Builder(
        name = "CacheStack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5.2f, 3.8f)
            horizontalLineTo(18.8f)
            curveTo(20.1f, 3.8f, 21.2f, 4.8f, 21.2f, 6.1f)
            curveTo(21.2f, 7.4f, 20.1f, 8.4f, 18.8f, 8.4f)
            horizontalLineTo(5.2f)
            curveTo(3.9f, 8.4f, 2.8f, 7.4f, 2.8f, 6.1f)
            curveTo(2.8f, 4.8f, 3.9f, 3.8f, 5.2f, 3.8f)
            close()
            moveTo(7.1f, 9.8f)
            horizontalLineTo(16.9f)
            curveTo(18.2f, 9.8f, 19.3f, 10.8f, 19.3f, 12.1f)
            curveTo(19.3f, 13.4f, 18.2f, 14.4f, 16.9f, 14.4f)
            horizontalLineTo(7.1f)
            curveTo(5.8f, 14.4f, 4.7f, 13.4f, 4.7f, 12.1f)
            curveTo(4.7f, 10.8f, 5.8f, 9.8f, 7.1f, 9.8f)
            close()
            moveTo(9.4f, 15.8f)
            horizontalLineTo(14.6f)
            curveTo(15.9f, 15.8f, 17f, 16.8f, 17f, 18.1f)
            curveTo(17f, 19.4f, 15.9f, 20.4f, 14.6f, 20.4f)
            horizontalLineTo(9.4f)
            curveTo(8.1f, 20.4f, 7f, 19.4f, 7f, 18.1f)
            curveTo(7f, 16.8f, 8.1f, 15.8f, 9.4f, 15.8f)
            close()
        }
    }.build()
}

@Composable
fun InitialBadge(text: String, size: Dp = 44.dp) {
    val label = text.trim().take(1).uppercase().ifBlank { "A" }
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(pal.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = pal.accentText,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
