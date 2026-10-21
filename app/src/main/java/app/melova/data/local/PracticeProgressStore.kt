package app.melova.data.local

import android.content.Context

/** Best score the student reached in each lesson section, kept on the device. */
class PracticeProgressStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun bestScore(lessonId: String, sectionId: String): Int? =
        prefs.getInt(key(lessonId, sectionId), NO_SCORE).takeIf { it != NO_SCORE }

    /** Stores [score] when it beats the previous best; returns true for a new record. */
    fun recordScore(lessonId: String, sectionId: String, score: Int): Boolean {
        prefs.edit().putString(KEY_LAST_LESSON, lessonId).apply()
        val previous = bestScore(lessonId, sectionId)
        if (previous != null && previous >= score) return false
        prefs.edit().putInt(key(lessonId, sectionId), score).apply()
        return true
    }

    /** The lesson the student last sang in, so the practice tab can offer to continue it. */
    fun lastLessonId(): String? = prefs.getString(KEY_LAST_LESSON, null)

    fun allBestScores(): Map<String, Int> =
        prefs.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.toMap()

    companion object {
        private const val PREFS_NAME = "melova_practice_progress"
        private const val NO_SCORE = -1
        private const val KEY_LAST_LESSON = "last_lesson_id"

        fun key(lessonId: String, sectionId: String) = "$lessonId/$sectionId"
    }
}
