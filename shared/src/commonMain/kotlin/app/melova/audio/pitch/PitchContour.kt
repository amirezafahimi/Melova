package app.melova.audio.pitch

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.yield

/** A moment of singing: where it falls in the file, and its note in semitones (A4 is 69). */
data class PitchPoint(val timeMs: Long, val semitone: Float)

/**
 * The line a voice draws over part of a recording. [PitchAnalyzer] reads every 46 ms, too few
 * to show a slide or a vibrato, so this reads every [HOP_MS] over a window of two periods of
 * the lowest voice. Each returned list is one unbroken stretch of singing.
 */
object PitchContour {
    suspend fun of(samples: ShortArray, sampleRate: Int, startMs: Long, endMs: Long): List<List<PitchPoint>> {
        if (samples.isEmpty() || sampleRate <= 0 || endMs <= startMs) return emptyList()
        val step = (sampleRate / ANALYSIS_RATE).coerceAtLeast(1)
        val rate = sampleRate.toFloat() / step
        val windowSize = (2 * rate / LOWEST_HZ).toInt()
        val yin = Yin(rate, windowSize, checkOctaveBelow = true)
        val window = FloatArray(windowSize)
        val frameCount = ((endMs - startMs) / HOP_MS).toInt() + 1
        val notes = FloatArray(frameCount) { Float.NaN }
        val levels = FloatArray(frameCount)

        for (frame in 0 until frameCount) {
            // The browser has one thread, so the page must get to draw between readings.
            if (frame % FRAMES_BETWEEN_YIELDS == 0) yield()
            val centre = (startMs + frame * HOP_MS) * sampleRate / 1000
            levels[frame] = readWindow(samples, centre - windowSize * step / 2, step, window)
            if (levels[frame] < SILENCE_LEVEL) continue
            val (hz, probability) = yin.detect(window)
            if (hz in LOWEST_HZ..HIGHEST_HZ && probability >= MIN_PROBABILITY) notes[frame] = semitoneOf(hz)
        }

        val floor = (levels.maxOrNull() ?: 0f) * MIN_RELATIVE_LEVEL
        for (frame in notes.indices) if (levels[frame] < floor) notes[frame] = Float.NaN
        return linesOf(notes, startMs)
    }

    /**
     * The line the score itself was read from, over [startMs]..[endMs]: the server's readings laid
     * on this object's own grid, so a line that came with the score and one measured here are
     * cleaned and drawn the same way.
     */
    fun of(points: List<PitchPoint>, startMs: Long, endMs: Long): List<List<PitchPoint>> {
        if (points.isEmpty() || endMs <= startMs) return emptyList()
        val frameCount = ((endMs - startMs) / HOP_MS).toInt() + 1
        val notes = FloatArray(frameCount) { Float.NaN }
        for (point in points) {
            val frame = ((point.timeMs - startMs) / HOP_MS).toInt()
            if (frame in 0 until frameCount) notes[frame] = point.semitone
        }
        return linesOf(notes, startMs)
    }

    /** The note of [hz], as the lines carry it. */
    fun semitoneOf(hz: Float): Float = 69f + OCTAVE * ln(hz / 440f) / ln(2f)

    private fun linesOf(notes: FloatArray, startMs: Long): List<List<PitchPoint>> {
        val sound = withoutStrayLeaps(withoutOctaveSlips(inOneRegister(notes)))
        return runsOf(medianFiltered(sound), startMs).map(::softened)
    }

    /** [voice] moved by whole octaves to sit with [reference], as the score compares them. */
    fun inRegisterOf(reference: List<List<PitchPoint>>, voice: List<List<PitchPoint>>): List<List<PitchPoint>> {
        val target = medianNote(reference) ?: return voice
        val own = medianNote(voice) ?: return voice
        val octaves = ((target - own) / OCTAVE).roundToInt()
        if (octaves == 0) return voice
        return voice.map { run -> run.map { it.copy(semitone = it.semitone + octaves * OCTAVE) } }
    }

    /**
     * The teacher's line to lie under the student's: on the student's clock through [toStudentMs],
     * and moved by the student's own key, read from the notes away from the mistake at [markMs]
     * so that the mistake itself still shows, and capped as the score caps it.
     */
    fun guide(
        teacher: List<List<PitchPoint>>,
        student: List<List<PitchPoint>>,
        markMs: LongRange,
        toStudentMs: (Long) -> Long
    ): List<List<PitchPoint>> {
        val onStudentClock = teacher.map { run -> run.map { it.copy(timeMs = toStudentMs(it.timeMs)) } }
        val teacherPoints = onStudentClock.flatten()
        val awayFromMark = (markMs.first - MARK_MARGIN_MS)..(markMs.last + MARK_MARGIN_MS)
        val gaps = student.flatten()
            .filter { it.timeMs !in awayFromMark }
            .mapNotNull { point ->
                noteNear(teacherPoints, point.timeMs)?.let { note ->
                    val gap = point.semitone - note
                    gap - OCTAVE * (gap / OCTAVE).roundToInt()
                }
            }
            .sorted()
        if (gaps.size < MIN_KEY_FRAMES) return onStudentClock
        val key = gaps[gaps.size / 2].coerceIn(-MAX_KEY_OFFSET, MAX_KEY_OFFSET)
        return onStudentClock.map { run -> run.map { it.copy(semitone = it.semitone + key) } }
    }

    /** The note sounding at [timeMs], if the voice sings then. */
    fun noteNear(points: List<PitchPoint>, timeMs: Long): Float? {
        val nearest = points.minByOrNull { abs(it.timeMs - timeMs) } ?: return null
        return nearest.semitone.takeIf { abs(nearest.timeMs - timeMs) <= NEAR_MS }
    }

    /**
     * The unbroken stretch around [markMs] that sits off [guide], each point with the note it
     * should have been: the mistake as the eye should find it.
     */
    fun offStretch(
        line: List<List<PitchPoint>>,
        guide: List<PitchPoint>,
        markMs: Long
    ): List<Pair<PitchPoint, Float>> {
        val run = line.firstOrNull { markMs in it.first().timeMs..it.last().timeMs } ?: return emptyList()
        val mark = run.indices.minByOrNull { abs(run[it].timeMs - markMs) } ?: return emptyList()
        fun offAt(index: Int): Pair<PitchPoint, Float>? {
            val point = run.getOrNull(index) ?: return null
            val note = noteNear(guide, point.timeMs) ?: return null
            return (point to note).takeIf { abs(point.semitone - note) >= OFF_SEMITONES }
        }
        val before = generateSequence(mark) { it - 1 }.map(::offAt).takeWhile { it != null }.filterNotNull()
        val after = generateSequence(mark + 1) { it + 1 }.map(::offAt).takeWhile { it != null }.filterNotNull()
        return before.toList().asReversed() + after.toList()
    }

    /** The notes all [lines] fit in, never so narrow that a small wobble fills the card. */
    fun rangeOf(lines: List<List<List<PitchPoint>>>): ClosedFloatingPointRange<Float> {
        val notes = lines.flatten().flatten().map { it.semitone }
        val low = notes.minOrNull() ?: return DEFAULT_RANGE
        val high = notes.maxOrNull() ?: return DEFAULT_RANGE
        val centre = (low + high) / 2f
        val half = maxOf((high - low) / 2f + RANGE_MARGIN, MIN_RANGE / 2f)
        return (centre - half)..(centre + half)
    }

    val DEFAULT_RANGE: ClosedFloatingPointRange<Float> = 0f..MIN_RANGE

    /** Fills [window] with every [step]th-sample average from [from]; returns its loudness. */
    private fun readWindow(samples: ShortArray, from: Long, step: Int, window: FloatArray): Float {
        var energy = 0f
        for (index in window.indices) {
            var sum = 0f
            val first = from + index * step
            for (offset in 0 until step) {
                val sample = first + offset
                if (sample >= 0 && sample < samples.size) sum += samples[sample.toInt()] / 32768f
            }
            val value = sum / step
            window[index] = value
            energy += value * value
        }
        return sqrt(energy / window.size)
    }

    /**
     * Whole octaves back onto the stretch's own register. [withoutOctaveSlips] reads each frame
     * against its neighbours, which says nothing where the slip opens the stretch and those
     * neighbours are the slip itself; the median of the whole stretch is the voice either way.
     */
    private fun inOneRegister(notes: FloatArray): FloatArray {
        val voiced = notes.filterNot { it.isNaN() }.sorted()
        if (voiced.isEmpty()) return notes
        val register = voiced[voiced.size / 2]
        return FloatArray(notes.size) { frame ->
            val note = notes[frame]
            if (note.isNaN()) return@FloatArray note
            val octaves = ((register - note) / OCTAVE).roundToInt()
            val folded = note + octaves * OCTAVE
            if (octaves != 0 && abs(register - folded) < REGISTER_TOLERANCE) folded else note
        }
    }

    /**
     * A short reading still a leap from its neighbours after both octave passes belongs to no
     * register of this voice: the tracker caught a harmonic that is not a whole octave away.
     * Drawing a note there invents one, so the line breaks over it instead.
     */
    private fun withoutStrayLeaps(notes: FloatArray): FloatArray {
        val stray = BooleanArray(notes.size) { isStray(notes, it) }
        val kept = notes.copyOf()
        var frame = 0
        while (frame < notes.size) {
            if (!stray[frame]) {
                frame++
                continue
            }
            var last = frame
            while (last + 1 < notes.size && stray[last + 1]) last++
            if ((last - frame + 1) * HOP_MS <= STRAY_MAX_MS) kept.fill(Float.NaN, frame, last + 1)
            frame = last + 1
        }
        return kept
    }

    private fun isStray(notes: FloatArray, frame: Int): Boolean {
        val note = notes[frame]
        if (note.isNaN()) return false
        val around = medianAround(notes, frame, OCTAVE_CHECK_FRAMES)
        return !around.isNaN() && abs(note - around) > MAX_STEP_SEMITONES
    }

    /** A reading an octave from the notes around it is the tracker's slip, not a leap. */
    private fun withoutOctaveSlips(notes: FloatArray): FloatArray = FloatArray(notes.size) { frame ->
        val note = notes[frame]
        val around = medianAround(notes, frame, OCTAVE_CHECK_FRAMES)
        if (note.isNaN() || around.isNaN()) return@FloatArray note
        val octaves = ((around - note) / OCTAVE).roundToInt()
        val folded = note + octaves * OCTAVE
        if (octaves != 0 && abs(around - folded) < OCTAVE_TOLERANCE) folded else note
    }

    private fun medianFiltered(notes: FloatArray): FloatArray = FloatArray(notes.size) { frame ->
        if (notes[frame].isNaN()) Float.NaN else medianAround(notes, frame, MEDIAN_REACH_FRAMES)
    }

    private fun medianAround(notes: FloatArray, frame: Int, reach: Int): Float {
        val first = (frame - reach).coerceAtLeast(0)
        val last = (frame + reach).coerceAtMost(notes.lastIndex)
        val voiced = (first..last).map { notes[it] }.filterNot { it.isNaN() }.sorted()
        return if (voiced.isEmpty()) Float.NaN else voiced[voiced.size / 2]
    }

    private fun runsOf(notes: FloatArray, startMs: Long): List<List<PitchPoint>> {
        val runs = mutableListOf<List<PitchPoint>>()
        var run = mutableListOf<PitchPoint>()
        var silentFrames = 0
        fun close() {
            if (run.size >= MIN_RUN_FRAMES) runs += run
            run = mutableListOf()
        }
        notes.forEachIndexed { frame, note ->
            if (note.isNaN()) {
                silentFrames++
                if (silentFrames > MAX_GAP_FRAMES) close()
            } else {
                if (run.isNotEmpty() && abs(note - run.last().semitone) > MAX_STEP_SEMITONES) close()
                silentFrames = 0
                run += PitchPoint(startMs + frame * HOP_MS, note)
            }
        }
        close()
        return runs
    }

    /** Takes the tracker's jitter off the line; a vibrato spans many frames and stays. */
    private fun softened(run: List<PitchPoint>): List<PitchPoint> = run.indices.map { index ->
        val before = run[(index - 1).coerceAtLeast(0)].semitone
        val after = run[(index + 1).coerceAtMost(run.lastIndex)].semitone
        run[index].copy(semitone = (before + 2f * run[index].semitone + after) / 4f)
    }

    private fun medianNote(line: List<List<PitchPoint>>): Float? {
        val notes = line.flatten().map { it.semitone }.sorted()
        return notes.getOrNull(notes.size / 2)
    }

    private const val ANALYSIS_RATE = 14_000
    private const val LOWEST_HZ = 70f
    private const val HIGHEST_HZ = 1_400f
    private const val MIN_PROBABILITY = 0.75f
    private const val SILENCE_LEVEL = 0.002f

    /** Quieter than this share of the stretch's loudest reading is breath or room. */
    private const val MIN_RELATIVE_LEVEL = 0.08f
    private const val FRAMES_BETWEEN_YIELDS = 12
    private const val OCTAVE = 12f
    private const val OCTAVE_CHECK_FRAMES = 25
    private const val OCTAVE_TOLERANCE = 2.5f

    /** Tighter than the neighbours' check: a note this near the register once folded was a slip,
     * while a real leap of nearly an octave lands further off and is left where it was sung. */
    private const val REGISTER_TOLERANCE = 1.5f
    private const val MEDIAN_REACH_FRAMES = 2
    private const val MIN_RUN_FRAMES = 6
    private const val MAX_GAP_FRAMES = 3
    private const val MAX_STEP_SEMITONES = 5f

    /** Longer than this and a reading away from every register is singing, not a slip. */
    private const val STRAY_MAX_MS = 100L
    private const val RANGE_MARGIN = 1f
    private const val MARK_MARGIN_MS = 150L
    private const val MIN_KEY_FRAMES = 30
    private const val NEAR_MS = 15L

    /** `OFF_PITCH_SEMITONES` on the server: from here a note counts as off. */
    private const val OFF_SEMITONES = 0.5f

    /** `MAX_GLOBAL_MICRO_SHIFT` on the server: past this a key counts as wrong notes. */
    private const val MAX_KEY_OFFSET = 1f
    private const val MIN_RANGE = 8f

    const val HOP_MS = 10L
}
