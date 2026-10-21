package app.melova.data.repository

import android.content.Context
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import java.io.File

interface CourseRepository {
    /**
     * Lesson catalog. Served from memory unless [forceRefresh]; a refresh that fails
     * offline falls back to the last copy saved on disk.
     */
    suspend fun lessons(context: Context, forceRefresh: Boolean = false): List<Lesson>

    /** Local copy of the lesson's overall teaching audio, downloaded on first use. */
    suspend fun introFile(context: Context, lesson: Lesson): File

    /** Local copy of the teacher's performance for one section (16-bit PCM WAV). */
    suspend fun sectionAudioFile(context: Context, lesson: Lesson, section: LessonSection): File
}
