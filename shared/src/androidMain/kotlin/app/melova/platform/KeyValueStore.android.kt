package app.melova.platform

import android.content.Context

actual class KeyValueStore actual constructor(context: PlatformContext, name: String) {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun getInt(key: String): Int? = if (prefs.contains(key)) prefs.getInt(key, 0) else null

    actual fun getLong(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0L) else null

    actual fun getStringSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().toSet()

    actual fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    actual fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    actual fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()

    actual fun putStringSet(key: String, value: Set<String>) = prefs.edit().putStringSet(key, value).apply()

    actual fun remove(key: String) = prefs.edit().remove(key).apply()

    actual fun intEntries(): Map<String, Int> =
        prefs.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.toMap()
}
