package com.kevin233.chat.ui

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kevin233.chat.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DataImage(
    dataUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (() -> Unit)? = null
) {
    var bitmap by remember(dataUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(dataUrl) {
        bitmap = withContext(Dispatchers.IO) { Images.decode(dataUrl) }
    }
    val image = bitmap
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(pal.surfaceAlt)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            DotsLoading()
        }
    }
}

@Composable
fun RemoteImage(url: String, onClick: (() -> Unit)? = null) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            if (url.startsWith("data:")) Images.decode(url) else Images.loadRemote(url)
        }
        failed = bitmap == null
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        )
    } else {
        Surface(color = pal.surfaceAlt, shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                if (failed) {
                    Text("图片没加载出来", style = MaterialTheme.typography.labelSmall, color = pal.textSub)
                } else {
                    DotsLoading()
                }
            }
        }
    }
}

@Composable
fun CodePreviewDialog(code: String, lang: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val palette = pal
    val isSvg = lang.equals("svg", true) || code.trimStart().startsWith("<svg", true)
    val html = if (isSvg) {
        """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>html,body{margin:0;padding:0;background:#fff}body{min-height:100vh;display:flex;align-items:center;justify-content:center}
        svg{max-width:100%;max-height:calc(100vh - 40px);height:auto;width:auto}</style></head><body>$code</body></html>"""
    } else {
        code
    }
    val webView = remember(context) {
        val view = WebView(context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = false
        view.settings.allowFileAccess = false
        view.settings.allowContentAccess = false
        view.settings.loadsImagesAutomatically = true
        view.setBackgroundColor(android.graphics.Color.WHITE)
        view.webViewClient = WebViewClient()
        view
    }
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().background(palette.surface).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isSvg) "SVG 预览" else "HTML 预览",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "关闭", tint = palette.text, modifier = Modifier.size(22.dp))
                }
            }
            AndroidView(
                factory = { webView },
                update = { it.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ImagePreviewDialog(url: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val palette = pal
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            if (url.startsWith("data:")) Images.decode(url) else Images.loadRemote(url)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f))) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                Icon(Icons.Rounded.Close, "关闭", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 78.dp)
                )
            } else {
                DotsLoading(color = Color.White)
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 34.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable {
                        val bmp = bitmap
                        if (bmp != null) {
                            saved = Images.saveToGallery(ctx, bmp)
                            if (!saved) {
                                android.widget.Toast
                                    .makeText(ctx, "保存失败，可能需要在系统里允许存储权限", android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                android.widget.Toast
                                    .makeText(ctx, "已保存到相册", android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (saved) "已保存" else "保存到相册", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
