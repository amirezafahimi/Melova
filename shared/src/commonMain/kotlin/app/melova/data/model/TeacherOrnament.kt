package app.melova.data.model

/**
 * One tahrir, vibrato or glide of the teacher; [startMs] and [endMs] are exactly that vowel's ends.
 */
data class TeacherOrnament(
    val kind: TeacherOrnamentKind,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}

/** A take on the server, not yet a section, and what was found in it. */
data class UploadedTake(
    val id: String,
    val ornaments: List<TeacherOrnament>,
    /** Why the ornaments couldn't be read; the take can still be confirmed. */
    val failureReason: String?
)

/** [legend] is the name in the key under the waveform, where there is room to explain it. */
enum class TeacherOrnamentKind(val letter: String, val title: String, val legend: String = title) {
    TAHRIR("T", "تحریر"),
    VIBRATO("V", "ویبراتو"),
    GLIDE("G", "گذر", "گذر (پورتامنتو)");

    companion object {
        fun fromRaw(raw: String): TeacherOrnamentKind? = when (raw.trim().lowercase()) {
            "tahrir", "sustained" -> TAHRIR
            "vibrato" -> VIBRATO
            "glide" -> GLIDE
            else -> null
        }
    }
}
