package com.financas.app.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast

fun shareText(context: Context, text: String, title: String, mimeType: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, title)
            type = mimeType
        }
        val chooser = Intent.createChooser(sendIntent, title)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // Fallback: copy to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(title, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
    }
}

fun saveToDownloads(context: Context, text: String, fileName: String, mimeType: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: return false
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        true
    } catch (e: Exception) {
        false
    }
}

fun exportTextFile(context: Context, text: String, fileName: String, mimeType: String) {
    if (saveToDownloads(context, text, fileName, mimeType)) {
        Toast.makeText(context, "Guardado em Downloads: $fileName", Toast.LENGTH_LONG).show()
    } else {
        shareText(context, text, fileName, mimeType)
    }
}
