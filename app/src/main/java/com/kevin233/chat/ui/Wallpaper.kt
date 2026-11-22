package com.kevin233.chat.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.kevin233.chat.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WallpaperPreset(
    val id: String,
    val name: String,
    val colors: List<Color>
)

object WallpaperPresets {
    val all = listOf(
        WallpaperPreset(
            "aurora", "极光",
            listOf(Color(0xFFE4ECFF), Color(0xFFC8D6FF), Color(0xFFE9DCFF), Color(0xFFF7F2FF))
        ),
        WallpaperPreset(
            "sunset", "暮色",
            listOf(Color(0xFFFFE9D8), Color(0xFFFFC9D9), Color(0xFFF7C6B8), Color(0xFFFFF1E4))
        ),
        WallpaperPreset(
            "mint", "薄荷",
            listOf(Color(0xFFE0F7EE), Color(0xFFC7EBE4), Color(0xFFD7EFFF), Color(0xFFF2FBF7))
        ),
        WallpaperPreset(
            "paper", "纸感",
            listOf(Color(0xFFF8F3E9), Color(0xFFEFE4D2), Color(0xFFF4ECE0), Color(0xFFFBF8F2))
        ),
        WallpaperPreset(
            "midnight", "午夜",
            listOf(Color(0xFF101725), Color(0xFF1A2440), Color(0xFF2B1D3D), Color(0xFF121923))
        ),
        WallpaperPreset(
            "sakura", "樱粉",
            listOf(Color(0xFFFFEAF2), Color(0xFFFAD2E4), Color(0xFFEEDCFF), Color(0xFFFFF4F8))
        )
    )

    fun find(id: String): WallpaperPreset? = all.firstOrNull { it.id == id }

    fun brush(id: String): Brush {
        val colors = find(id)?.colors ?: all.first().colors
        return Brush.linearGradient(colors)
    }
}

@Composable
fun WallpaperLayer(id: String, path: String) {
    if (id == "custom" && path.isNotBlank()) {
        var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(path) {
            bitmap = withContext(Dispatchers.IO) { Images.loadWallpaper(path) }
        }
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(WallpaperPresets.brush("aurora")))
        }
    } else {
        Box(
            Modifier.fillMaxSize().background(
                if (id == "none") Brush.linearGradient(listOf(pal.bg, pal.bg))
                else WallpaperPresets.brush(id)
            )
        )
    }
}

val WallpaperScrim: Brush
    @Composable get() = if (darkTheme) {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.28f),
                Color.Black.copy(alpha = 0.38f),
                Color.Black.copy(alpha = 0.52f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.10f),
                Color.Black.copy(alpha = 0.08f)
            )
        )
    }
