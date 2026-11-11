package app.melova.data.repository

import app.melova.data.local.StudentSession
import app.melova.data.local.TeacherReferenceCache
import app.melova.data.local.TeacherSession
import app.melova.data.model.Catalog
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.model.TeacherOrnament
import app.melova.data.remote.datasource.MelovaRemoteDataSource
import app.melova.data.remote.datasource.MelovaRemoteDataSourceImpl
import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.mapper.toDomain
import app.melova.data.remote.mapper.toOrnaments
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.writeAtomically
import app.melova.platform.concurrentMapOf
import app.melova.audio.io.WavUtils
import kotlinx.io.IOException
import kotlinx.coroutines.withContext
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.ioDispatcher
import kotlin.concurrent.Volatile
import app.melova.platform.filesDir

class RemoteLessonRepository(
    private val remoteDataSource: MelovaRemoteDataSource = MelovaRemoteDataSourceImpl()
) : LessonRepository {
    override suspend fun catalog(context: PlatformContext, forceRefresh: Boolean): Catalog =
        withContext(ioDispatcher) {
            // The catalog says which lessons are the student's, so every copy belongs to one account.
            val studentId = StudentSession(context).studentId.orEmpty()
            memoryCache?.takeIf { !forceRefresh && it.first == studentId }?.let { return@withContext it.second }

            val cacheFile = LocalFile(coursesDir(context), catalogCacheName(studentId))
            val catalog = try {
                val response = remoteDataSource.getCatalog(TeacherSession(context).token)
                val rawCatalog = MelovaNetwork.json.encodeToString(CatalogResponseDto.serializer(), response)
                cacheFile.writeText(rawCatalog)
                response.toDomain()
            } catch (error: Throwable) {
                val cached = cacheFile.takeIf { it.exists() }?.let {
                    runCatching { MelovaNetwork.json.decodeFromString(CatalogResponseDto.serializer(), it.readText()) }.getOrNull()
                } ?: throw IOException(
                    "دریافت فهرست درس‌ها ممکن نشد. اتصال اینترنت را بررسی کنید.",
                    error
                )
                cached.toDomain()
            }
            memoryCache = studentId to catalog
            catalog
        }

    override suspend fun introFile(context: PlatformContext, lesson: Lesson): LocalFile =
        withContext(ioDispatcher) {
            check(lesson.hasIntro) { "این درس آموزش کلی ندارد." }
            cachedDownload(
                dir = lessonDir(context, lesson.id),
                prefix = "intro_",
                version = lesson.introVersion
            ) { target ->
                remoteDataSource.downloadLessonIntro(lesson.id, lesson.introVersion).writeAtomically(target, minBytes = 1_024L)
            }
        }

    override suspend fun sectionAudioFile(
        context: PlatformContext,
        lesson: Lesson,
        section: LessonSection
    ): LocalFile = withContext(ioDispatcher) {
        check(section.isAvailable) { "فایل این بخش هنوز آماده نیست." }
        cachedDownload(
            dir = lessonDir(context, lesson.id),
            prefix = "section_${safeName(section.id)}_",
            version = section.audioVersion,
            extension = ".wav"
        ) { target ->
            remoteDataSource.downloadSectionAudio(lesson.id, section.id, section.audioVersion, TeacherSession(context).token).writeAtomically(target, minBytes = 45L)
            require(TeacherReferenceCache.isValidWav(target)) { "فایل صوتی استاد معتبر نیست." }
            // Playback-only copy: the server scores against its own original file.
            val (samples, sampleRate) = WavUtils.readPcm16Samples(target)
            if (samples.isNotEmpty()) {
                WavUtils.writePcm16Wav(target, WavUtils.normalizePeak(samples), sampleRate)
            }
        }
    }

    override suspend fun sectionSlowAudioFile(
        context: PlatformContext,
        lesson: Lesson,
        section: LessonSection,
        speed: Float
    ): LocalFile = withContext(ioDispatcher) {
        cachedDownload(
            dir = lessonDir(context, lesson.id),
            // Not starting with the section's own prefix, which would clear this copy away.
            prefix = "slow_${safeName(speed.toString())}_${safeName(section.id)}_",
            version = "${section.audioVersion}_$SLOW_RECIPE",
            extension = ".wav"
        ) { target ->
            remoteDataSource.downloadSectionSlowAudio(lesson.id, section.id, section.audioVersion, speed).writeAtomically(target, minBytes = 45L)
            require(TeacherReferenceCache.isValidWav(target)) { "فایل صوتی استاد معتبر نیست." }
            val (samples, sampleRate) = WavUtils.readPcm16Samples(target)
            if (samples.isNotEmpty()) {
                WavUtils.writePcm16Wav(target, WavUtils.normalizePeak(samples), sampleRate)
            }
        }
    }

    override suspend fun sectionOrnaments(
        lesson: Lesson,
        section: LessonSection
    ): List<TeacherOrnament> = withContext(ioDispatcher) {
        val key = "${lesson.id}/${section.id}/${section.audioVersion}"
        ornamentCache[key]?.let { return@withContext it }

        val response = try {
            remoteDataSource.getSectionOrnaments(lesson.id, section.id)
        } catch (error: Throwable) {
            throw IOException("دریافت تحریرهای استاد ممکن نشد. اتصال اینترنت را بررسی کنید.", error)
        }
        response.failureReason?.takeIf { it.isNotBlank() }?.let { throw IOException(it) }
        val ornaments = response.ornaments.toOrnaments()
        ornamentCache[key] = ornaments
        ornaments
    }

    /** Keeps one file per [prefix]; a new server [version] replaces the old download. */
    private suspend fun cachedDownload(
        dir: LocalFile,
        prefix: String,
        version: String,
        extension: String = ".audio",
        download: suspend (LocalFile) -> Unit
    ): LocalFile {
        val target = LocalFile(dir, "$prefix${safeName(version.ifBlank { "v0" })}$extension")
        if (target.exists() && target.length() > 0L) return target

        try {
            download(target)
        } catch (error: Throwable) {
            target.delete()
            throw IOException("دانلود فایل صوتی ممکن نشد. اتصال اینترنت را بررسی کنید.", error)
        }
        dir.listFiles { file -> file.name.startsWith(prefix) && file != target }
            ?.forEach { it.delete() }
        return target
    }

    private fun coursesDir(context: PlatformContext): LocalFile =
        LocalFile(filesDir(context), "courses").apply { mkdirs() }

    private fun lessonDir(context: PlatformContext, lessonId: String): LocalFile =
        LocalFile(coursesDir(context), safeName(lessonId)).apply { mkdirs() }

    private fun safeName(raw: String): String = raw.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private companion object {
        fun catalogCacheName(studentId: String) =
            if (studentId.isEmpty()) "catalog.json" else "catalog_${studentId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.json"

        /**
         * Raised with every change to the server's stretching settings, so a phone
         * that already has a slower copy of a section fetches the new one.
         */
        const val SLOW_RECIPE = "r2"

        /** Shared across screens so opening a lesson or section needs no extra request. */
        @Volatile
        var memoryCache: Pair<String, Catalog>? = null

        /** Teacher ornaments per section audio version; the server's first analysis is slow. */
        val ornamentCache = concurrentMapOf<String, List<TeacherOrnament>>()
    }
}
