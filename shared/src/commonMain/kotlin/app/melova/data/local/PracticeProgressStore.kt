package app.melova.data.local

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext


/** Best score the student reached in each lesson section, kept on the device. */
class PracticeProgressStore(context: PlatformContext) {
    private val prefs = KeyValueStore(context, PREFS_NAME)

    fun bestScore(lessonId: String, sectionId: String): Int? =
        prefs.getInt(key(lessonId, sectionId))?.takeIf { it != NO_SCORE }

    /** Stores [score] when it beats the previous best; returns true for a new record. */
    fun recordScore(lessonId: String, sectionId: String, score: Int): Boolean {
        prefs.putString(KEY_LAST_LESSON, lessonId)
        val previous = bestScore(lessonId, sectionId)
        if (previous != null && previous >= score) return false
        prefs.putInt(key(lessonId, sectionId), score)
        return true
    }

    /** The lesson the student last sang in, so the practice tab can offer to continue it. */
    fun lastLessonId(): String? = prefs.getString(KEY_LAST_LESSON)

    fun allBestScores(): Map<String, Int> =
        prefs.intEntries()

    companion object {
        private const val PREFS_NAME = "melova_practice_progress"
        private const val NO_SCORE = -1
        private const val KEY_LAST_LESSON = "last_lesson_id"

        fun key(lessonId: String, sectionId: String) = "$lessonId/$sectionId"
    }
}
