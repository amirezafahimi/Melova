package app.melova.data.remote.api

import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.CreatedStudentDto
import app.melova.data.remote.dto.FollowsDto
import app.melova.data.remote.dto.LessonOfferDto
import app.melova.data.remote.dto.LoginCodeSentDto
import app.melova.data.remote.dto.LoginMethodsDto
import app.melova.data.remote.dto.PracticeProgressDto
import app.melova.data.remote.dto.PracticeTakeDetailDto
import app.melova.data.remote.dto.PracticeTakesDto
import app.melova.data.remote.dto.StudentMeDto
import app.melova.data.remote.multipartBody
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The student's own account; the token header is added by [app.melova.data.remote.MelovaNetwork]. */
class StudentApi(private val client: HttpClient) {
    suspend fun create(body: Map<String, String>): CreatedStudentDto =
        client.call(HttpMethod.Post, "api/v1/students") { jsonBody(body.toJson()) }.decode()

    suspend fun me(): StudentMeDto = client.call(HttpMethod.Get, "api/v1/me").decode()

    suspend fun updateMe(body: Map<String, String>): StudentMeDto =
        client.call(HttpMethod.Put, "api/v1/me") { jsonBody(body.toJson()) }.decode()

    suspend fun sayITeach(code: String): StudentMeDto =
        client.call(HttpMethod.Put, "api/v1/me/teacher") { jsonBody(mapOf("code" to code).toJson()) }.decode()

    suspend fun setPhoto(photo: FilePart): StudentMeDto =
        client.call(HttpMethod.Put, "api/v1/me/photo") { setBody(multipartBody(emptyMap(), listOf(photo))) }.decode()

    suspend fun deletePhoto(): StudentMeDto = client.call(HttpMethod.Delete, "api/v1/me/photo").decode()

    suspend fun photo(): ByteArray = client.call(HttpMethod.Get, "api/v1/me/photo").bodyAsBytes()

    suspend fun loginMethods(): LoginMethodsDto = client.call(HttpMethod.Get, "api/v1/auth/methods").decode()

    /** [body] holds the kind ("phone" or "email") and the value the student typed. */
    suspend fun requestLoginCode(body: Map<String, String>): LoginCodeSentDto =
        client.call(HttpMethod.Post, "api/v1/auth/code") { jsonBody(body.toJson()) }.decode()

    suspend fun verifyLoginCode(body: Map<String, String>): StudentMeDto =
        client.call(HttpMethod.Post, "api/v1/auth/verify") { jsonBody(body.toJson()) }.decode()

    suspend fun signInWithGoogle(idToken: String): StudentMeDto =
        client.call(HttpMethod.Post, "api/v1/auth/google") { jsonBody(mapOf("id_token" to idToken).toJson()) }.decode()

    suspend fun logout() {
        client.call(HttpMethod.Post, "api/v1/auth/logout")
    }

    /** Add a free lesson to the student's lessons. */
    suspend fun claimFreeLesson(lessonId: String): StudentMeDto =
        client.call(HttpMethod.Post, "api/v1/me/lessons/${segment(lessonId)}/claim").decode()

    /** Buy one version ("lesson" or "teacher") of a lesson; the mock provider pays at once. */
    suspend fun checkout(lessonId: String, tier: String): StudentMeDto =
        client.call(HttpMethod.Post, "api/v1/me/lessons/${segment(lessonId)}/checkout") {
            jsonBody(mapOf("tier" to tier).toJson())
        }.decode()

    /** The student's version of a lesson and their messages to the teacher. */
    suspend fun lessonAccess(lessonId: String): LessonOfferDto =
        client.call(HttpMethod.Get, "api/v1/me/lessons/${segment(lessonId)}/access").decode()

    suspend fun follow(teacherIds: Set<String>): FollowsDto =
        client.call(HttpMethod.Post, "api/v1/me/follows") {
            jsonBody(JsonObject(mapOf("teacher_ids" to JsonArray(teacherIds.map { JsonPrimitive(it) }))))
        }.decode()

    suspend fun unfollow(teacherId: String): FollowsDto =
        client.call(HttpMethod.Delete, "api/v1/me/follows/${segment(teacherId)}").decode()

    suspend fun sendForReview(
        lessonId: String,
        sectionId: String,
        note: String,
        referralId: String?,
        scoreJson: String?,
        audio: FilePart
    ): StudentMeDto = client.call(HttpMethod.Post, "api/v1/me/reviews") {
        setBody(
            multipartBody(
                mapOf(
                    "lesson_id" to lessonId,
                    "section_id" to sectionId,
                    "note" to note,
                    "referral_id" to referralId,
                    "score_json" to scoreJson
                ),
                listOf(audio)
            )
        )
    }.decode()

    suspend fun practice(): PracticeProgressDto = client.call(HttpMethod.Get, "api/v1/me/practice").decode()

    suspend fun practiceTakes(lessonId: String, sectionId: String): PracticeTakesDto =
        client.call(HttpMethod.Get, "api/v1/me/lessons/${segment(lessonId)}/sections/${segment(sectionId)}/takes").decode()

    suspend fun practiceTake(takeId: String): PracticeTakeDetailDto =
        client.call(HttpMethod.Get, "api/v1/me/takes/${segment(takeId)}").decode()

    suspend fun practiceTakeAudio(takeId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/me/takes/${segment(takeId)}/audio").bodyAsBytes()
}

internal fun Map<String, String>.toJson() = JsonObject(mapValues { JsonPrimitive(it.value) })
