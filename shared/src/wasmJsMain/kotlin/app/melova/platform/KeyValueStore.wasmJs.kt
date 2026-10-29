package app.melova.platform

import kotlinx.browser.localStorage

/** localStorage entries named "<store>/<key>"; sets are kept one per line. */
actual class KeyValueStore actual constructor(context: PlatformContext, name: String) {
    private val prefix = "$name/"

    actual fun getString(key: String): String? = localStorage.getItem(prefix + key)

    actual fun getInt(key: String): Int? = getString(key)?.toIntOrNull()

    actual fun getLong(key: String): Long? = getString(key)?.toLongOrNull()

    actual fun getStringSet(key: String): Set<String> =
        getString(key)?.split('\n')?.filter { it.isNotEmpty() }?.toSet().orEmpty()

    actual fun putString(key: String, value: String) = localStorage.setItem(prefix + key, value)

    actual fun putInt(key: String, value: Int) = putString(key, value.toString())

    actual fun putLong(key: String, value: Long) = putString(key, value.toString())

    actual fun putStringSet(key: String, value: Set<String>) = putString(key, value.joinToString("\n"))

    actual fun remove(key: String) = localStorage.removeItem(prefix + key)

    actual fun intEntries(): Map<String, Int> = buildMap {
        for (index in 0 until localStorage.length) {
            val fullKey = localStorage.key(index) ?: continue
            if (!fullKey.startsWith(prefix)) continue
            localStorage.getItem(fullKey)?.toIntOrNull()?.let { put(fullKey.removePrefix(prefix), it) }
        }
    }
}
