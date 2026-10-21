package app.melova.audio.playback

import android.content.Context
import app.melova.audio.io.WavUtils
import app.melova.data.local.TeacherReferenceCache
import java.io.File

/**
 * Builds a [TeacherLesson] from the teacher reference WAV cached by [TeacherReferenceRepository].
 */
object TeacherVocalGenerator {
    data class TeacherLesson(
        val title: String,
        val teacherName: String,
        val description: String,
        val referenceFile: File,
        val durationMs: Long,
        val sourceLabel: String,
        val audioFormatLabel: String,
        val sampleRate: Int,
        /** Known note windows for synthetic lessons; null → extract from reference audio at score time. */
        val expectedSegments: List<ExpectedSegment>? = null
    )

    data class PhraseNote(
        val midi: Int,
        val durationSeconds: Double,
        val label: String,
        val vowel: Vowel = Vowel.A
    )

    enum class Vowel(val f1Ratio: Double, val f2Ratio: Double, val f3Ratio: Double) {
        A(1.0, 1.8, 3.2),
        E(0.8, 2.2, 3.0),
        O(0.7, 1.2, 2.5)
    }

    fun prepareLesson(context: Context): TeacherLesson {
        require(TeacherReferenceCache.isReady(context)) {
            "Teacher reference is not cached. Download it via TeacherReferenceRepository first."
        }

        val referenceFile = TeacherReferenceCache.wavFile(context)
        val sourceLabel = readSourceLabel(context)

        return buildLessonFromReference(referenceFile, sourceLabel)
    }

    /** Lesson for one course section whose teacher WAV is already on disk. */
    fun lessonFromFile(
        referenceFile: File,
        title: String,
        teacherName: String,
        description: String
    ): TeacherLesson {
        val (sampleRate, formatLabel) = readWavMetadata(referenceFile)
        return TeacherLesson(
            title = title,
            teacherName = teacherName,
            description = description,
            referenceFile = referenceFile,
            durationMs = readDurationMs(referenceFile, sampleRate),
            sourceLabel = "",
            audioFormatLabel = formatLabel,
            sampleRate = sampleRate
        )
    }

    private fun readSourceLabel(context: Context): String {
        val meta = TeacherReferenceCache.metaFile(context)
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            .orEmpty()
        return TeacherReferenceCache.displaySourceLabel(meta)
    }

    private fun buildLessonFromReference(
        referenceFile: File,
        sourceLabel: String
    ): TeacherLesson {
        val (sampleRate, formatLabel) = readWavMetadata(referenceFile)
        val durationMs = readDurationMs(referenceFile, sampleRate)

        return TeacherLesson(
            title = "تمرین آزادسازی صدا",
            teacherName = "استاد",
            description = buildDescription(),
            referenceFile = referenceFile,
            durationMs = durationMs,
            sourceLabel = sourceLabel,
            audioFormatLabel = formatLabel,
            sampleRate = sampleRate,
            expectedSegments = null
        )
    }

    private fun buildDescription(): String {
        return "نمونه صدای استاد از سرور بارگذاری شد. " +
            "این اجرا را گوش دهید، سپس همان ملودی را بخوانید (اکتاو بالاتر یا پایین‌تر هم قبول است). " +
            "سیستم کل ضبط را می‌شنود و نمره می‌دهد — سرعت مهم نیست؛ " +
            "اختلاف تا نصف ربع‌پرده در هر نت قابل قبول است."
    }

    fun buildPhrase(): List<PhraseNote> {
        return listOf(
            PhraseNote(64, 1.0, "می", Vowel.E),
            PhraseNote(67, 0.9, "سل", Vowel.E),
            PhraseNote(69, 1.3, "لا", Vowel.A),
            PhraseNote(67, 0.9, "سل", Vowel.O),
            PhraseNote(64, 1.1, "می", Vowel.E),
            PhraseNote(62, 0.4, "—", Vowel.A),
            PhraseNote(60, 1.0, "دو", Vowel.O),
            PhraseNote(64, 1.0, "می", Vowel.E),
            PhraseNote(67, 1.4, "سل", Vowel.A),
            PhraseNote(64, 1.0, "می", Vowel.E),
            PhraseNote(60, 1.4, "دو", Vowel.O),
            PhraseNote(62, 0.4, "—", Vowel.A),
            PhraseNote(67, 0.9, "سل", Vowel.O),
            PhraseNote(69, 0.9, "لا", Vowel.A),
            PhraseNote(71, 1.5, "سی", Vowel.E),
            PhraseNote(69, 1.0, "لا", Vowel.A),
            PhraseNote(67, 2.0, "سل", Vowel.O)
        )
    }

    private fun readWavMetadata(file: File): Pair<Int, String> {
        val (_, sampleRate) = WavUtils.readPcm16Samples(file)
        val rate = if (sampleRate > 0) sampleRate else WavUtils.SAMPLE_RATE
        val rateLabel = if (rate % 1000 == 0) "${rate / 1000} kHz" else "$rate Hz"
        val label = "WAV PCM · $rateLabel · mono · 16-bit"
        return rate to label
    }

    private fun readDurationMs(file: File, sampleRate: Int): Long {
        val (pcm, _) = WavUtils.readPcm16Le(file)
        val sampleCount = pcm.size / 2
        val rate = if (sampleRate > 0) sampleRate else WavUtils.SAMPLE_RATE
        return (sampleCount.toLong() * 1000L) / rate
    }
}
