package com.kevin233.chat.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object Images {

    private const val MAX_SIDE = 1400
    private const val JPEG_QUALITY = 82
    private const val MAX_BYTES = 1_400_000

    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun toDataUrl(context: Context, uri: Uri): String? {
        val bitmap = try {
            if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val scale = (MAX_SIDE.toFloat() / maxOf(w, h)).coerceAtMost(1f)
                    decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val input = context.contentResolver.openInputStream(uri) ?: return null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)
                input.close()
                val scale = (MAX_SIDE.toFloat() / maxOf(bounds.outWidth, bounds.outHeight)).coerceAtMost(1f)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = if (scale <= 0.5f) 2 else 1
                }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        } catch (e: Exception) {
            null
        } ?: return null

        val bytes = try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            if (out.size() > MAX_BYTES) {
                out.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 62, out)
            }
            out.toByteArray()
        } catch (e: Exception) {
            return null
        }
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun saveWallpaper(context: Context, uri: Uri): String? {
        val bitmap = decodeWallpaper(context, uri) ?: return null
        return try {
            val file = File(context.filesDir, "wallpaper.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun loadWallpaper(path: String): Bitmap? {
        if (path.isBlank()) return null
        return try {
            if (!File(path).exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            val side = maxOf(bounds.outWidth, bounds.outHeight)
            while (side / sample > 1600) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeWallpaper(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val scale = (2200f / maxOf(w, h)).coerceAtMost(1f)
                    decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val scale = (2200f / maxOf(bounds.outWidth, bounds.outHeight)).coerceAtMost(1f)
                val options = BitmapFactory.Options().apply { inSampleSize = if (scale <= 0.5f) 2 else 1 }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decode(dataUrl: String): Bitmap? {
        if (!dataUrl.startsWith("data:")) return null
        cache.get(dataUrl)?.let { return it }
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return null
        val bytes = try {
            Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
        } catch (e: Exception) {
            return null
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        cache.put(dataUrl, bitmap)
        return bitmap
    }

    fun loadRemote(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        val bitmap = try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 25000
            connection.setRequestProperty("User-Agent", "AIChat/1.0 Android")
            connection.inputStream.use { stream ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val bytes = stream.readBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val scale = (1600f / maxOf(bounds.outWidth, bounds.outHeight)).coerceAtMost(1f)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = if (scale <= 0.5f) 2 else 1
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } catch (e: Exception) {
            null
        }
        if (bitmap != null) cache.put(url, bitmap)
        return bitmap
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        val name = "aichat-" + System.currentTimeMillis() + ".jpg"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI Chat")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    bitmap,
                    name,
                    "Saved by AI Chat"
                ) != null
            }
        } catch (e: Exception) {
            false
        }
    }
}
