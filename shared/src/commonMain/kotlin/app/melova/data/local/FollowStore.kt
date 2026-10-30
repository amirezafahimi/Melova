package app.melova.data.local

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext


/** Follows from when they were kept on the device; the account holds them now. */
class FollowStore(context: PlatformContext) {
    private val prefs = KeyValueStore(context, "melova_follows")

    fun followedIds(): Set<String> = prefs.getStringSet(KEY_TEACHERS)

    fun clear() = prefs.remove(KEY_TEACHERS)

    private companion object {
        // Follows used to be per course; those ids are simply left behind under the old key.
        const val KEY_TEACHERS = "teacher_ids"
    }
}
