package app.melova.data.local

import android.content.Context

/** Teachers the student follows; their lessons make up the practice tab. Kept on the device. */
class FollowStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("melova_follows", Context.MODE_PRIVATE)

    fun followedIds(): Set<String> = prefs.getStringSet(KEY_TEACHERS, emptySet()).orEmpty().toSet()

    fun setFollowed(teacherId: String, followed: Boolean) {
        val updated = followedIds().toMutableSet().apply {
            if (followed) add(teacherId) else remove(teacherId)
        }
        prefs.edit().putStringSet(KEY_TEACHERS, updated).apply()
    }

    private companion object {
        // Follows used to be per course; those ids are simply left behind under the old key.
        const val KEY_TEACHERS = "teacher_ids"
    }
}
