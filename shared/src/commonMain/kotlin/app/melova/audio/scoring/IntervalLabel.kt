package app.melova.audio.scoring

import kotlin.math.abs
import kotlin.math.roundToInt

/** Musician-facing pitch-gap wording (mirrors backend `pitch_utils.interval_label`). */
object IntervalLabel {
    private val counts = listOf("", "یک", "دو", "سه", "چهار", "پنج", "شش")

    /** 100 cents = نیم‌پرده; under 75 cents reads as ربع‌پرده. */
    fun of(cents: Float): String {
        val size = abs(cents)
        if (size < 75f) return if (size >= 35f) "تقریباً ربع پرده" else "کمتر از ربع پرده"
        val semitones = (size / 100f).roundToInt()
        if (semitones == 1) return "تقریباً نیم‌پرده"
        val whole = semitones / 2
        val half = semitones % 2
        if (whole >= counts.size) return "تقریباً $semitones نیم‌پرده"
        val label = if (half == 1) "${counts[whole]} و نیم پرده" else "${counts[whole]} پرده"
        return "تقریباً $label"
    }

    /** cents > 0 means sung sharp. */
    fun directionMessage(timeRange: String, cents: Float): String {
        val softener = if (abs(cents) < 150f) "کمی " else ""
        val verb = if (cents > 0f) "پایین‌تر" else "بالاتر"
        return "در $timeRange — $softener$verb بخوانید (${of(cents)})"
    }
}
