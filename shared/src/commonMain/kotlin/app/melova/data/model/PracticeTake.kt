package app.melova.data.model

/**
 * An earlier take of a section that the practice page lists: the first, the one with the best
 * score, or one of the last three. A take can be several of these at once.
 */
data class PracticeTake(
    val id: String,
    val score: Int,
    val createdAtMs: Long,
    val isFirst: Boolean,
    val isBest: Boolean,
    /** One of the last three. */
    val isRecent: Boolean
)

/** Where the student stands, as the server counts it: nothing about practice is kept on the phone. */
data class PracticeProgress(
    /** Keyed by [key]: the best score of each section sung in. */
    val bestScores: Map<String, Int> = emptyMap(),
    val lastLessonId: String? = null,
    val lastSectionId: String? = null,
    /** What the latest take scored (not the section's best). */
    val lastScore: Int? = null
) {
    companion object {
        fun key(lessonId: String, sectionId: String) = "$lessonId/$sectionId"
    }
}

/** One of those takes opened again, with the result the student saw then. */
data class PracticeTakeDetail(
    val createdAtMs: Long,
    /** Null when the result can't be read any more. */
    val result: VocalScoreResult?
)
