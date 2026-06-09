package app.melova.audio.playback

import android.content.Context
import app.melova.audio.io.WavUtils
import app.melova.data.ScaleExercise
import app.melova.data.ScalePattern
import java.io.File
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

object ReferenceAudioGenerator {
    private const val ROOT_MIDI = 60 // C4
    private const val NOTE_DURATION_SECONDS = 1.0
    private const val LAST_NOTE_DURATION_SECONDS = 1.35

    private val majorSemitones = listOf(0, 2, 4, 5, 7, 9, 11, 12)
    private val minorSemitones = listOf(0, 2, 3, 5, 7, 8, 10, 12)
    private val majorLabels = listOf("دو", "ر", "می", "فا", "سل", "لا", "سی", "دو")
    private val minorLabels = listOf("دو", "ر", "می♭", "فا", "سل", "لا♭", "سی♭", "دو")

    data class NoteSegment(
        val frequencyHz: Double,
        val durationSeconds: Double,
        val label: String
    )

    data class GeneratedExercise(
        val exercise: ScaleExercise,
        val referenceFile: File,
        val segments: List<ExpectedSegment>,
        val durationMs: Long,
        val noteCount: Int
    )

    fun prepareExercise(context: Context, exercise: ScaleExercise): GeneratedExercise {
        val melody = buildMelody(exercise)
        val file = File(context.filesDir, cacheFileName(exercise))
        if (!file.exists() || file.length() == 0L) {
            writeReferenceWav(file, melody)
        }
        val segments = melodyToSegments(melody)
        return GeneratedExercise(
            exercise = exercise,
            referenceFile = file,
            segments = segments,
            durationMs = segments.lastOrNull()?.endMs ?: 0L,
            noteCount = melody.size
        )
    }

    fun buildMelody(exercise: ScaleExercise): List<NoteSegment> {
        val semitones = if (exercise.isMinor) minorSemitones else majorSemitones
        val labels = if (exercise.isMinor) minorLabels else majorLabels
        val ascendingNotes = semitones.mapIndexed { index, semitone ->
            NoteSegment(
                frequencyHz = midiToHz(ROOT_MIDI + semitone),
                durationSeconds = NOTE_DURATION_SECONDS,
                label = labels[index]
            )
        }

        val orderedNotes = when (exercise.pattern) {
            ScalePattern.ASCENDING -> ascendingNotes
            ScalePattern.DESCENDING -> ascendingNotes.reversed()
            ScalePattern.ASCENDING_DESCENDING -> {
                ascendingNotes + ascendingNotes.dropLast(1).reversed()
            }
        }

        return orderedNotes.mapIndexed { index, note ->
            val isLast = index == orderedNotes.lastIndex
            note.copy(
                durationSeconds = if (isLast) LAST_NOTE_DURATION_SECONDS else NOTE_DURATION_SECONDS
            )
        }
    }

    private fun melodyToSegments(melody: List<NoteSegment>): List<ExpectedSegment> {
        var cursorMs = 0L
        return melody.map { note ->
            val segment = ExpectedSegment(
                startMs = cursorMs,
                endMs = cursorMs + (note.durationSeconds * 1000).toLong(),
                frequencyHz = note.frequencyHz.toFloat(),
                label = note.label
            )
            cursorMs = segment.endMs
            segment
        }
    }

    private fun writeReferenceWav(file: File, melody: List<NoteSegment>) {
        val totalDurationSeconds = melody.sumOf { it.durationSeconds }
        val totalSamples = (WavUtils.SAMPLE_RATE * totalDurationSeconds).toInt()
        val samples = ShortArray(totalSamples)
        var sampleIndex = 0

        melody.forEach { note ->
            val noteSamples = (note.durationSeconds * WavUtils.SAMPLE_RATE).toInt()
            repeat(noteSamples) { i ->
                if (sampleIndex >= totalSamples) return@repeat
                val t = i.toDouble() / WavUtils.SAMPLE_RATE
                val envelope = noteEnvelope(i, noteSamples)
                val vibrato = 1.0 + 0.004 * sin(2 * PI * 5.5 * t)
                val fundamental = sin(2 * PI * note.frequencyHz * vibrato * t)
                val harmonic2 = 0.35 * sin(2 * PI * note.frequencyHz * 2 * vibrato * t)
                val harmonic3 = 0.15 * sin(2 * PI * note.frequencyHz * 3 * vibrato * t)
                val value = (fundamental + harmonic2 + harmonic3) * envelope * Short.MAX_VALUE * 0.55
                samples[sampleIndex++] = value.toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }

        WavUtils.writePcm16Wav(file, samples)
    }

    private fun noteEnvelope(sampleIndex: Int, totalSamples: Int): Double {
        val attack = (totalSamples * 0.08).coerceAtLeast(1.0)
        val release = (totalSamples * 0.18).coerceAtLeast(1.0)
        return when {
            sampleIndex < attack -> sampleIndex / attack
            sampleIndex > totalSamples - release -> (totalSamples - sampleIndex) / release
            else -> 1.0
        }
    }

    private fun cacheFileName(exercise: ScaleExercise): String {
        val tempoTag = (NOTE_DURATION_SECONDS * 100).toInt()
        return "reference_${exercise.id}_t$tempoTag.wav"
    }

    private fun midiToHz(midi: Int): Double {
        return 440.0 * 2.0.pow((midi - 69) / 12.0)
    }
}

data class ExpectedSegment(
    val startMs: Long,
    val endMs: Long,
    val frequencyHz: Float,
    val label: String
)
