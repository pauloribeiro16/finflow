package com.financas.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException

const val MAX_IMPORT_BYTES = 20L * 1024 * 1024
const val MAX_PHOTO_BYTES = 15L * 1024 * 1024
const val MAX_PHOTO_SIDE_PX = 1024

fun readTextLimited(context: Context, uri: Uri, maxBytes: Long = MAX_IMPORT_BYTES): String {
    val bytes = readBytesLimited(context, uri, maxBytes)
    return String(bytes, Charsets.UTF_8)
}

private fun readBytesLimited(context: Context, uri: Uri, maxBytes: Long): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Não foi possível abrir o ficheiro")
    return input.use { stream ->
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) throw IOException("Ficheiro demasiado grande (máx. ${maxBytes / (1024 * 1024)} MB)")
            out.write(buffer, 0, read)
        }
        out.toByteArray()
    }
}

fun copyImageDownscaled(context: Context, uri: Uri, target: File): Boolean {
    return try {
        val bytes = readBytesLimited(context, uri, MAX_PHOTO_BYTES)
        writeDownscaledJpeg(bytes, target)
    } catch (e: Exception) {
        false
    } catch (e: OutOfMemoryError) {
        false
    }
}

fun writeDownscaledJpeg(bytes: ByteArray, target: File): Boolean {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
    var sample = 1
    while (bounds.outWidth / sample > MAX_PHOTO_SIDE_PX * 2 || bounds.outHeight / sample > MAX_PHOTO_SIDE_PX * 2) {
        sample *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return false
    val scale = MAX_PHOTO_SIDE_PX.toFloat() / maxOf(decoded.width, decoded.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
    } else {
        decoded
    }
    target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return target.length() > 0
}
