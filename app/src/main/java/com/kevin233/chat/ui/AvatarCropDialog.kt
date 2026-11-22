package com.kevin233.chat.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kevin233.chat.util.Images
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AvatarCropDialog(bitmap: Bitmap, onCancel: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val palette = pal
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun baseScale(): Float {
        if (viewport.width <= 0 || viewport.height <= 0) return 1f
        return max(viewport.width / bitmap.width.toFloat(), viewport.height / bitmap.height.toFloat())
    }

    fun clamp(candidate: Offset, currentScale: Float): Offset {
        if (viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
        val base = baseScale() * currentScale
        val maxX = ((bitmap.width * base - viewport.width) / 2f).coerceAtLeast(0f)
        val maxY = ((bitmap.height * base - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(palette.bg).statusBarsPadding().navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, "取消", tint = palette.text, modifier = Modifier.size(22.dp))
                }
                Text(
                    "调整头像",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.text,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(300.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1D22))
                        .onSizeChanged {
                            viewport = it
                            offset = clamp(offset, scale)
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val next = (scale * zoom).coerceIn(1f, 4f)
                                scale = next
                                offset = clamp(offset + pan, next)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                    )
                }
            }
            Text(
                "双指缩放，拖动调整位置",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = palette.textSub)
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    val vw = viewport.width.toFloat()
                    val vh = viewport.height.toFloat()
                    if (vw <= 0f || vh <= 0f) return@Button
                    val finalScale = baseScale() * scale
                    val side = (min(vw, vh) / finalScale).roundToInt()
                        .coerceIn(1, min(bitmap.width, bitmap.height))
                    val left = ((bitmap.width - side) / 2f - offset.x / finalScale)
                        .roundToInt().coerceIn(0, bitmap.width - side)
                    val top = ((bitmap.height - side) / 2f - offset.y / finalScale)
                        .roundToInt().coerceIn(0, bitmap.height - side)
                    val cropped = Bitmap.createBitmap(bitmap, left, top, side, side)
                    val resized = Bitmap.createScaledBitmap(cropped, 512, 512, true)
                    val path = Images.saveAvatar(context, resized)
                    if (path != null) {
                        onDone(path)
                    } else {
                        android.widget.Toast
                            .makeText(context, "保存头像失败", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    }
                }) {
                    Text("保存头像", fontSize = 14.sp)
                }
            }
        }
    }
}
