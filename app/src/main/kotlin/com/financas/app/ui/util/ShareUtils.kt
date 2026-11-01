package com.financas.app.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
