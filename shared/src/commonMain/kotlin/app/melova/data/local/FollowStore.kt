package app.melova.data.local

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext


/** Teachers the student follows; their lessons make up the practice tab. Kept on the device. */
class FollowStore(context: PlatformContext) {
    private val prefs = KeyValueStore(context, "melova_follows")

    fun followedIds(): Set<String> = prefs.getStringSet(KEY_TEACHERS)

    fun setFollowed(teacherId: String, followed: Boolean) {
        val updated = followedIds().toMutableSet().apply {
            if (followed) add(teacherId) else remove(teacherId)
        }
        prefs.putStringSet(KEY_TEACHERS, updated)
    }

    private companion object {
        // Follows used to be per course; those ids are simply left behind under the old key.
        const val KEY_TEACHERS = "teacher_ids"
    }
}
