package app.melova.data.local

import android.content.Context

/** Courses the student follows; they make up the practice tab. Kept on the device. */
class FollowStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("melova_follows", Context.MODE_PRIVATE)

    fun followedIds(): Set<String> = prefs.getStringSet(KEY_COURSES, emptySet()).orEmpty().toSet()

    fun setFollowed(courseId: String, followed: Boolean) {
        val updated = followedIds().toMutableSet().apply {
            if (followed) add(courseId) else remove(courseId)
        }
        prefs.edit().putStringSet(KEY_COURSES, updated).apply()
    }

    private companion object {
        const val KEY_COURSES = "course_ids"
    }
}
