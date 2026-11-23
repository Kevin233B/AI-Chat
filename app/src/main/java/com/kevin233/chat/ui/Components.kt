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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun Kicker(text: String, color: Color = pal.textSub, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = pal.border) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun NumberLabel(number: Int, active: Boolean = false) {
    val palette = pal
    Text(
        text = number.toString().padStart(2, '0'),
        style = MaterialTheme.typography.labelMedium,
        color = if (active) palette.accent else palette.textSub,
        modifier = Modifier.width(26.dp)
    )
}

@Composable
fun CacheBadge(hit: Int, miss: Int, compact: Boolean = false) {
    val palette = pal
    val total = hit + miss
    if (total <= 0) return
    val rate = hit.toFloat() / total
    val good = rate >= 0.3f
    val tint = if (good) palette.ok else palette.textSub
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (good) palette.ok.copy(alpha = 0.09f) else palette.surfaceAlt)
            .border(1.dp, if (good) palette.ok.copy(alpha = 0.24f) else palette.border, RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp).background(tint))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "CACHE ${(rate * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
        if (!compact) {
            Text(
                text = " / ${com.kevin233.chat.util.Tokens.format(hit)}",
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.7f)
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
                initialValue = 0.2f,
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
                    .size(5.dp)
                    .background(color.copy(alpha = alpha))
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
            .background(palette.surface, RoundedCornerShape(4.dp))
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp).background(palette.accent))
                Spacer(Modifier.width(7.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = palette.text)
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
            Spacer(Modifier.height(10.dp))
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
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = palette.text)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
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
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) palette.accent else Color.Transparent)
            .border(
                1.dp,
                if (selected) palette.accent else palette.border,
                RoundedCornerShape(3.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else palette.text
        )
    }
}

@Composable
fun BulletDot(modifier: Modifier = Modifier, color: Color = pal.accent) {
    Box(
        modifier
            .size(4.dp)
            .background(color)
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
                shape = RoundedCornerShape(3.dp),
                color = palette.text,
                modifier = Modifier.padding(bottom = 108.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = message ?: "",
                    color = palette.bg,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
