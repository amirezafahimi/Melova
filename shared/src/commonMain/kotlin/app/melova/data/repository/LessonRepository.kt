package app.melova.data.repository

import app.melova.data.model.Catalog
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.model.TeacherOrnament
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext

interface LessonRepository {
    /**
     * Teachers and lessons (with a signed-in teacher's drafts). Served from memory unless
     * [forceRefresh]; a refresh that fails offline falls back to the last copy on disk.
     */
    suspend fun catalog(context: PlatformContext, forceRefresh: Boolean = false): Catalog

    suspend fun lessons(context: PlatformContext, forceRefresh: Boolean = false): List<Lesson> =
        catalog(context, forceRefresh).lessons

    /** Local copy of the lesson's overall teaching audio, downloaded on first use. */
    suspend fun introFile(context: PlatformContext, lesson: Lesson): LocalFile

    /** Local copy of the teacher's performance for one section (16-bit PCM WAV). */
    suspend fun sectionAudioFile(context: PlatformContext, lesson: Lesson, section: LessonSection): LocalFile

    /** The teacher's tahrir and vibrato vowels in one section, in time order. */
    suspend fun sectionOrnaments(lesson: Lesson, section: LessonSection): List<TeacherOrnament>
}
