package com.financas.app.data.prefs

import android.content.Context

interface ContextTagsStore {
    fun getCustomTags(): Set<String>
    fun addTag(tag: String)
}

class SharedPrefsContextTagsStore(
    context: Context
) : ContextTagsStore {
    private val prefs = context.getSharedPreferences("financas_prefs", Context.MODE_PRIVATE)

    override fun getCustomTags(): Set<String> =
        prefs.getStringSet("custom_context_tags", emptySet())?.toSet() ?: emptySet()

    override fun addTag(tag: String) {
        val updated = getCustomTags() + tag
        prefs.edit().putStringSet("custom_context_tags", updated).apply()
    }
}
