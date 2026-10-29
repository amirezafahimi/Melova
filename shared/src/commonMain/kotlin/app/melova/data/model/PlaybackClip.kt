package app.melova.data.model

import kotlin.math.PI
import kotlin.math.cos

/**
 * بازه‌ی پخش یک خطا که سرور اعلام می‌کند: از [startMs] تا [endMs] پخش می‌شود و
 * محدوده‌ی خطا ([slowStartMs] تا [slowEndMs]) با سرعت [slowSpeed] پخش می‌شود.
 * سرعت در [rampMs] قبل از خطا آرام‌آرام کم می‌شود و بعد از آن آرام‌آرام برمی‌گردد.
 */
data class PlaybackClip(
    val startMs: Long,
    val endMs: Long,
    val slowStartMs: Long,
    val slowEndMs: Long,
    val slowSpeed: Float,
    val rampMs: Long
) {
    /** سرعت پخش در موقعیت [positionMs] از فایل. */
    fun speedAt(positionMs: Long): Float {
        val ramp = rampMs.coerceAtLeast(1L)
        val fade = when {
            positionMs >= slowStartMs && positionMs <= slowEndMs -> 1f
            positionMs < slowStartMs -> 1f - (slowStartMs - positionMs).toFloat() / ramp
            else -> 1f - (positionMs - slowEndMs).toFloat() / ramp
        }.coerceIn(0f, 1f)
        val eased = ((1.0 - cos(PI * fade)) / 2.0).toFloat()
        return 1f + (slowSpeed - 1f) * eased
    }
}
