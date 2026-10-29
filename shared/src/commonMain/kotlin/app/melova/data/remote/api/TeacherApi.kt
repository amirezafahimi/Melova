package app.melova.data.remote.api

import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.TeacherProfileDto
import app.melova.data.remote.multipartBody
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
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

    suspend fun uploadIntro(token: String, lessonId: String, audio: FilePart): TeacherProfileDto =
        client.call(HttpMethod.Put, "api/v1/teacher/lessons/${segment(lessonId)}/intro") {
            header(TEACHER_TOKEN, token)
            setBody(multipartBody(emptyMap(), listOf(audio)))
        }.decode()

    suspend fun addSection(
        token: String,
        lessonId: String,
        title: String,
        description: String,
        audio: FilePart
    ): TeacherProfileDto =
        client.call(HttpMethod.Post, "api/v1/teacher/lessons/${segment(lessonId)}/sections") {
            header(TEACHER_TOKEN, token)
            setBody(multipartBody(mapOf("title" to title, "description" to description), listOf(audio)))
        }.decode()

    suspend fun deleteSection(token: String, lessonId: String, sectionId: String): TeacherProfileDto =
        client.call(HttpMethod.Delete, "api/v1/teacher/lessons/${segment(lessonId)}/sections/${segment(sectionId)}") {
            header(TEACHER_TOKEN, token)
        }.decode()
}
