package app.melova.data.model

/**
 * One tahrir of the teacher compared with the student's take.
 * The time spans play the excerpts of both voices.
 */
data class TahrirComparison(
    val kind: TahrirKind,
    val refStartMs: Long,
    val refEndMs: Long,
    val studentStartMs: Long?,
    val studentEndMs: Long?,
    val score: Int,
    val vowelCount: Int,
    val message: String,
    val studentClip: PlaybackClip? = null,
    val teacherClip: PlaybackClip? = null,
    /** The word of the poem this falls on, when the server knew it. */
    val word: String? = null
) {
    /** A stable id, to tell which row is playing. */
    val key: Long get() = refStartMs

    val hasStudentRegion: Boolean
        get() = studentStartMs != null && studentEndMs != null && studentEndMs > studentStartMs
}

enum class TahrirKind {
    SUSTAINED,
    ALTERNATING,
    VIBRATO,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String): TahrirKind = when (raw.trim().lowercase()) {
            "sustained" -> SUSTAINED
            "alternating" -> ALTERNATING
            "vibrato" -> VIBRATO
            else -> UNKNOWN
        }
    }
}
