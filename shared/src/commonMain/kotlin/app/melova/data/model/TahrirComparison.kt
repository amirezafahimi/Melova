package app.melova.data.model

/**
 * یک ناحیه تحریر استاد و مقایسه‌اش با اجرای هنرجو.
 * بازه‌های زمانی برای پخش گزیده‌ی صدای استاد و هنرجو استفاده می‌شوند.
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
    val teacherClip: PlaybackClip? = null
) {
    /** شناسه‌ی پایدار برای تشخیص اینکه کدام ردیف در حال پخش است. */
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
