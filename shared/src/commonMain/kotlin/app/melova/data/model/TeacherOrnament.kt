package app.melova.data.model

/**
 * یک تحریر، ویبراتو یا گذر استاد؛ [startMs] و [endMs] دقیقاً ابتدا و انتهای همان مصوت‌اند.
 */
data class TeacherOrnament(
    val kind: TeacherOrnamentKind,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}

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
