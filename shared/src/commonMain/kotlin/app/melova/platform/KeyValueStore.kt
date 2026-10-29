package app.melova.platform

/** Small settings kept on the device: SharedPreferences on Android, localStorage on the web. */
expect class KeyValueStore(context: PlatformContext, name: String) {
    fun getString(key: String): String?
    fun getInt(key: String): Int?
    fun getLong(key: String): Long?
    fun getStringSet(key: String): Set<String>
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putStringSet(key: String, value: Set<String>)
    fun remove(key: String)

    /** Every whole-number entry, by key. */
    fun intEntries(): Map<String, Int>
}
