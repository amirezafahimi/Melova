package app.melova.data.repository

import android.content.Context
import app.melova.data.local.TeacherReferenceCache
import app.melova.data.local.TeacherSession
import app.melova.data.model.Catalog
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.remote.datasource.MelovaRemoteDataSource
import app.melova.data.remote.datasource.MelovaRemoteDataSourceImpl
import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.mapper.toDomain
import app.melova.data.remote.writeAtomically
import app.melova.audio.io.WavUtils
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteLessonRepository(
    private val remoteDataSource: MelovaRemoteDataSource = MelovaRemoteDataSourceImpl()
) : LessonRepository {
    private val gson = Gson()

    override suspend fun catalog(context: Context, forceRefresh: Boolean): Catalog =
        withContext(Dispatchers.IO) {
            memoryCache?.takeIf { !forceRefresh }?.let { return@withContext it }

            val cacheFile = File(coursesDir(context), CATALOG_CACHE_NAME)
            val catalog = try {
                val response = remoteDataSource.getCatalog(TeacherSession(context).token)
                cacheFile.writeText(gson.toJson(response))
                response.toDomain()
            } catch (error: Exception) {
                val cached = cacheFile.takeIf { it.exists() }?.let {
                    runCatching { gson.fromJson(it.readText(), CatalogResponseDto::class.java) }.getOrNull()
                } ?: throw IOException(
                    "دریافت فهرست درس‌ها ممکن نشد. اتصال اینترنت را بررسی کنید.",
                    error
                )
                cached.toDomain()
            }
            memoryCache = catalog
            catalog
        }

    override suspend fun introFile(context: Context, lesson: Lesson): File =
        withContext(Dispatchers.IO) {
            check(lesson.hasIntro) { "این درس آموزش کلی ندارد." }
            cachedDownload(
                dir = lessonDir(context, lesson.id),
                prefix = "intro_",
                version = lesson.introVersion
            ) { target ->
                remoteDataSource.downloadLessonIntro(lesson.id).use { body ->
                    body.writeAtomically(target, context.cacheDir, minBytes = 1_024L)
                }
            }
        }

    override suspend fun sectionAudioFile(
        context: Context,
        lesson: Lesson,
        section: LessonSection
    ): File = withContext(Dispatchers.IO) {
        check(section.isAvailable) { "فایل این بخش هنوز آماده نیست." }
        cachedDownload(
            dir = lessonDir(context, lesson.id),
            prefix = "section_${safeName(section.id)}_",
            version = section.audioVersion,
            extension = ".wav"
        ) { target ->
            remoteDataSource.downloadSectionAudio(lesson.id, section.id).use { body ->
                body.writeAtomically(target, context.cacheDir, minBytes = 45L)
            }
            require(TeacherReferenceCache.isValidWav(target)) { "فایل صوتی استاد معتبر نیست." }
            // Playback-only copy: the server scores against its own original file.
            val (samples, sampleRate) = WavUtils.readPcm16Samples(target)
            if (samples.isNotEmpty()) {
                WavUtils.writePcm16Wav(target, WavUtils.normalizePeak(samples), sampleRate)
            }
        }
    }

    /** Keeps one file per [prefix]; a new server [version] replaces the old download. */
    private suspend fun cachedDownload(
        dir: File,
        prefix: String,
        version: String,
        extension: String = ".audio",
        download: suspend (File) -> Unit
    ): File {
        val target = File(dir, "$prefix${safeName(version.ifBlank { "v0" })}$extension")
        if (target.exists() && target.length() > 0L) return target

        try {
            download(target)
        } catch (error: Exception) {
            target.delete()
            throw IOException("دانلود فایل صوتی ممکن نشد. اتصال اینترنت را بررسی کنید.", error)
        }
        dir.listFiles { file -> file.name.startsWith(prefix) && file != target }
            ?.forEach { it.delete() }
        return target
    }

    private fun coursesDir(context: Context): File =
        File(context.filesDir, "courses").apply { mkdirs() }

    private fun lessonDir(context: Context, lessonId: String): File =
        File(coursesDir(context), safeName(lessonId)).apply { mkdirs() }

    private fun safeName(raw: String): String = raw.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private companion object {
        const val CATALOG_CACHE_NAME = "catalog.json"

        /** Shared across screens so opening a lesson or section needs no extra request. */
        @Volatile
        var memoryCache: Catalog? = null
    }
}
