package app.melova.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.melova.data.local.TeacherSession
import app.melova.data.model.TeacherProfile
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.TeacherApi
import app.melova.data.remote.mapper.toDomain
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

/** Teacher panel calls; each returns the teacher's profile as the server now has it. */
class TeacherRepository(
    context: Context,
    private val api: TeacherApi = MelovaNetwork.teacherApi
) {
    private val appContext = context.applicationContext
    private val session = TeacherSession(appContext)

    val isSignedIn: Boolean
        get() = session.token != null

    /** Checks [code] with the server and keeps it only when it is accepted. */
    suspend fun signIn(code: String): TeacherProfile {
        val profile = call { api.me(code.trim()) }
        session.token = code
        return profile
    }

    fun signOut() {
        session.token = null
    }

    suspend fun profile() = call { api.me(token()) }

    suspend fun updateProfile(name: String, bio: String) =
        call { api.updateMe(token(), mapOf("name" to name, "bio" to bio)) }

    suspend fun createCourse(fields: CourseFields) =
        call { api.createCourse(token(), fields.toMap()) }

    suspend fun updateCourse(courseId: String, fields: CourseFields) =
        call { api.updateCourse(token(), courseId, fields.toMap()) }

    suspend fun setCoursePublished(courseId: String, published: Boolean) =
        call { api.updateCourse(token(), courseId, mapOf("published" to published)) }

    suspend fun deleteCourse(courseId: String) = call { api.deleteCourse(token(), courseId) }

    suspend fun createLesson(courseId: String, fields: LessonFields) =
        call { api.createLesson(token(), courseId, fields.toMap()) }

    suspend fun updateLesson(lessonId: String, fields: LessonFields) =
        call { api.updateLesson(token(), lessonId, fields.toMap()) }

    suspend fun deleteLesson(lessonId: String) = call { api.deleteLesson(token(), lessonId) }

    suspend fun uploadIntro(lessonId: String, audio: Uri) = withUpload(audio) { part ->
        api.uploadIntro(token(), lessonId, part)
    }

    suspend fun addSection(lessonId: String, title: String, description: String, audio: Uri) =
        withUpload(audio) { part ->
            val text = "text/plain".toMediaType()
            api.addSection(
                token(),
                lessonId,
                title.toRequestBody(text),
                description.toRequestBody(text),
                part
            )
        }

    suspend fun deleteSection(lessonId: String, sectionId: String) =
        call { api.deleteSection(token(), lessonId, sectionId) }

    private fun token(): String = session.token ?: throw IOException("ابتدا با کد استاد وارد شوید.")

    /** Copies the picked file into the cache so it can be streamed as a multipart part. */
    private suspend fun withUpload(
        uri: Uri,
        send: suspend (MultipartBody.Part) -> app.melova.data.remote.dto.TeacherProfileDto
    ): TeacherProfile {
        val name = displayName(uri)
        val temp = withContext(Dispatchers.IO) {
            File(appContext.cacheDir, "teacher_upload_${System.nanoTime()}").also { file ->
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } ?: throw IOException("فایل انتخاب‌شده باز نشد.")
            }
        }
        return try {
            val mediaType = (appContext.contentResolver.getType(uri) ?: mediaTypeFor(name)).toMediaType()
            val part = MultipartBody.Part.createFormData("audio", name, temp.asRequestBody(mediaType))
            call { send(part) }
        } finally {
            temp.delete()
        }
    }

    private fun displayName(uri: Uri): String {
        // A take recorded in the app is a plain file; its name carries the extension the server checks.
        if (uri.scheme == "file") uri.lastPathSegment?.let { return it }
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        return "audio"
    }

    private fun mediaTypeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "wav" -> "audio/wav"
        "m4a", "aac" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "ogg" -> "audio/ogg"
        else -> "audio/*"
    }

    /** Runs a call and turns server errors into the Persian message the server sent. */
    private suspend fun call(
        block: suspend () -> app.melova.data.remote.dto.TeacherProfileDto
    ): TeacherProfile = withContext(Dispatchers.IO) {
        try {
            block().toDomain()
        } catch (error: HttpException) {
            val detail = error.response()?.errorBody()?.string()?.let { body ->
                runCatching { JsonParser.parseString(body).asJsonObject.get("detail").asString }.getOrNull()
            }
            throw IOException(detail ?: "خطای سرور (${error.code()})", error)
        } catch (error: java.net.ConnectException) {
            throw IOException("ارتباط با سرور برقرار نشد.", error)
        } catch (error: java.net.SocketTimeoutException) {
            throw IOException("پاسخ سرور طول کشید؛ دوباره تلاش کنید.", error)
        } catch (error: java.net.UnknownHostException) {
            throw IOException("اتصال اینترنت را بررسی کنید.", error)
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("ارتباط با سرور برقرار نشد.", error)
        }
    }
}

data class CourseFields(
    val title: String,
    val subtitle: String,
    val level: String,
    val description: String
) {
    fun toMap() = mapOf(
        "title" to title,
        "subtitle" to subtitle,
        "level" to level,
        "description" to description
    )
}

data class LessonFields(
    val title: String,
    val subtitle: String,
    val description: String
) {
    fun toMap() = mapOf("title" to title, "subtitle" to subtitle, "description" to description)
}
