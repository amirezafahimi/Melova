package app.melova.data.remote.api

import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.TeacherProfileDto
import app.melova.data.remote.dto.UploadedTakeDto
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.sendWithProgress
import app.melova.data.remote.multipartBody
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

/** Every call returns the teacher's whole profile with their lessons, already updated. */
class TeacherApi(private val client: HttpClient) {
    suspend fun me(token: String): TeacherProfileDto =
        client.call(HttpMethod.Get, "api/v1/teacher/me") { header(TEACHER_TOKEN, token) }.decode()

    suspend fun updateMe(token: String, body: Map<String, String>): TeacherProfileDto =
        client.call(HttpMethod.Put, "api/v1/teacher/me") {
            header(TEACHER_TOKEN, token)
            jsonBody(body.toJson())
        }.decode()

    suspend fun setPhoto(token: String, photo: FilePart): TeacherProfileDto =
        client.call(HttpMethod.Put, "api/v1/teacher/me/photo") {
            header(TEACHER_TOKEN, token)
            setBody(multipartBody(emptyMap(), listOf(photo)))
        }.decode()

    suspend fun deletePhoto(token: String): TeacherProfileDto =
        client.call(HttpMethod.Delete, "api/v1/teacher/me/photo") { header(TEACHER_TOKEN, token) }.decode()

    /** Anyone's to see, like the name beside it. */
    suspend fun photo(teacherId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/teacher/${segment(teacherId)}/photo").bodyAsBytes()

    suspend fun createLesson(token: String, body: Map<String, String>): TeacherProfileDto =
        client.call(HttpMethod.Post, "api/v1/teacher/lessons") {
            header(TEACHER_TOKEN, token)
            jsonBody(body.toJson())
        }.decode()

    suspend fun updateLesson(token: String, lessonId: String, body: JsonObject): TeacherProfileDto =
        client.call(HttpMethod.Patch, "api/v1/teacher/lessons/${segment(lessonId)}") {
            header(TEACHER_TOKEN, token)
            jsonBody(body)
        }.decode()

    suspend fun deleteLesson(token: String, lessonId: String): TeacherProfileDto =
        client.call(HttpMethod.Delete, "api/v1/teacher/lessons/${segment(lessonId)}") {
            header(TEACHER_TOKEN, token)
        }.decode()

    suspend fun uploadIntro(token: String, lessonId: String, audio: FilePart, onProgress: (Float) -> Unit): TeacherProfileDto =
        MelovaNetwork.json.decodeFromString(
            sendWithProgress(
                HttpMethod.Put,
                "api/v1/teacher/lessons/${segment(lessonId)}/intro",
                mapOf(TEACHER_TOKEN to token),
                emptyMap(),
                audio,
                onProgress
            )
        )

    /** Saves a take and reads its ornaments; [sectionId] makes it a new take of that section. */
    suspend fun uploadTake(
        token: String,
        lessonId: String,
        sectionId: String?,
        audio: FilePart,
        onProgress: (Float) -> Unit
    ): UploadedTakeDto =
        MelovaNetwork.json.decodeFromString(
            sendWithProgress(
                HttpMethod.Post,
                "api/v1/teacher/lessons/${segment(lessonId)}/takes",
                mapOf(TEACHER_TOKEN to token),
                mapOf("section_id" to sectionId),
                audio,
                onProgress
            )
        )

    suspend fun takeAudio(token: String, lessonId: String, takeId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/teacher/lessons/${segment(lessonId)}/takes/${segment(takeId)}/audio") {
            header(TEACHER_TOKEN, token)
        }.bodyAsBytes()

    suspend fun discardTake(token: String, lessonId: String, takeId: String) {
        client.call(HttpMethod.Delete, "api/v1/teacher/lessons/${segment(lessonId)}/takes/${segment(takeId)}") {
            header(TEACHER_TOKEN, token)
        }
    }

    suspend fun addSection(
        token: String,
        lessonId: String,
        title: String,
        description: String,
        takeId: String
    ): TeacherProfileDto =
        client.call(HttpMethod.Post, "api/v1/teacher/lessons/${segment(lessonId)}/sections") {
            header(TEACHER_TOKEN, token)
            setBody(multipartBody(mapOf("title" to title, "description" to description, "take_id" to takeId)))
        }.decode()

    /** Without [takeId] the take stays as it is. */
    suspend fun updateSection(
        token: String,
        lessonId: String,
        sectionId: String,
        title: String,
        description: String,
        takeId: String?
    ): TeacherProfileDto =
        client.call(HttpMethod.Patch, "api/v1/teacher/lessons/${segment(lessonId)}/sections/${segment(sectionId)}") {
            header(TEACHER_TOKEN, token)
            setBody(multipartBody(mapOf("title" to title, "description" to description, "take_id" to takeId)))
        }.decode()

    suspend fun deleteSection(token: String, lessonId: String, sectionId: String): TeacherProfileDto =
        client.call(HttpMethod.Delete, "api/v1/teacher/lessons/${segment(lessonId)}/sections/${segment(sectionId)}") {
            header(TEACHER_TOKEN, token)
        }.decode()
}
