package com.kevin233.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Kicker(text: String, color: Color = pal.textSub, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = pal.textSub,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = pal.border) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun NumberLabel(number: Int, active: Boolean = false) {
    val palette = pal
    Box(
        Modifier
            .size(24.dp)
            .background(if (active) palette.accent else palette.surfaceAlt, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) palette.onAccent else palette.textSub
        )
    }
}

@Composable
fun CacheBadge(hit: Int, miss: Int, compact: Boolean = false) {
    val palette = pal
    val total = hit + miss
    if (total <= 0) return
    val rate = hit.toFloat() / total
    val good = rate >= 0.3f
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (good) palette.ok.copy(alpha = 0.12f) else palette.surfaceAlt)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(5.dp)
                .background(if (good) palette.ok else palette.textSub, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "缓存 ${(rate * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = if (good) palette.ok else palette.textSub
        )
        if (!compact) {
            Text(
                " / ${com.kevin233.chat.util.Tokens.format(hit)}",
                style = MaterialTheme.typography.labelSmall,
                color = (if (good) palette.ok else palette.textSub).copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
fun DotsLoading(color: Color = pal.textSub) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(560, delayMillis = i * 140, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(6.dp)
                    .background(color.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = pal
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(18.dp))
            .border(1.dp, palette.border, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(palette.accent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text)
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = pal.accent,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val palette = pal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.text)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = palette.textSub.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AppDivider(verticalPadding: Dp = 10.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding)
            .height(1.dp)
            .background(pal.border)
    )
}

@Composable
fun PillChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val palette = pal
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.accent else palette.surfaceAlt)
            .then(if (selected) Modifier else Modifier.border(1.dp, palette.border, RoundedCornerShape(50)))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.onAccent else palette.text
        )
    }
}

@Composable
fun BulletDot(modifier: Modifier = Modifier, color: Color = pal.accent) {
    Box(
        modifier
            .size(6.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun ToastOverlay(message: String?, onDone: () -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2400)
            onDone()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            val palette = pal
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.text,
                modifier = Modifier.padding(bottom = 118.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = message ?: "",
                    color = palette.bg,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
