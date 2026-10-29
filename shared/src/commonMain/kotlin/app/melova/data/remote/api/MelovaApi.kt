package app.melova.data.remote.api

import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.dto.HealthResponseDto
import app.melova.data.remote.dto.ScoringResponseDto
import app.melova.data.remote.dto.TeacherOrnamentsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent

class MelovaApi(private val client: HttpClient) {
    suspend fun health(): HealthResponseDto = client.call(HttpMethod.Get, "health").decode()

    suspend fun downloadTeacherWav(): ByteArray =
        client.call(HttpMethod.Get, "api/v1/assets/teacher.wav").bodyAsBytes()

    suspend fun downloadTeacherSyllables(): ByteArray =
        client.call(HttpMethod.Get, "api/v1/assets/teacher_syllables.json").bodyAsBytes()

    /** Teachers and their published lessons; with a teacher code that teacher's drafts too. */
    suspend fun catalog(teacherToken: String?): CatalogResponseDto =
        client.call(HttpMethod.Get, "api/v1/catalog") {
            teacherToken?.let { header(TEACHER_TOKEN, it) }
        }.decode()

    suspend fun downloadLessonIntro(lessonId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/lessons/${segment(lessonId)}/intro").bodyAsBytes()

    suspend fun downloadSectionAudio(lessonId: String, sectionId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/lessons/${segment(lessonId)}/sections/${segment(sectionId)}/audio")
            .bodyAsBytes()

    /** The teacher's tahrir and vibrato vowels in one section. */
    suspend fun sectionOrnaments(lessonId: String, sectionId: String): TeacherOrnamentsResponseDto =
        client.call(HttpMethod.Get, "api/v1/lessons/${segment(lessonId)}/sections/${segment(sectionId)}/ornaments")
            .decode()

    suspend fun score(body: OutgoingContent): ScoringResponseDto =
        client.call(HttpMethod.Post, "api/v1/score") { setBody(body) }.decode()
}

internal const val TEACHER_TOKEN = "X-Teacher-Token"
