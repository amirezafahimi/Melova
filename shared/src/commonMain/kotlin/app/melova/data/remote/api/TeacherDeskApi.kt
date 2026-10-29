package app.melova.data.remote.api

import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.ReviewDto
import app.melova.data.remote.dto.ReviewsDto
import app.melova.data.remote.dto.StudentCardDto
import app.melova.data.remote.dto.StudentsDto
import app.melova.data.remote.dto.TeacherReportDto
import app.melova.data.remote.multipartBody
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

/** The teacher's desk: takes students sent, the students themselves, and the hours report. */
class TeacherDeskApi(private val client: HttpClient) {
    /** [status] open, answered, refunded, or empty for all; oldest first. */
    suspend fun reviews(token: String, status: String): ReviewsDto =
        client.call(HttpMethod.Get, "api/v1/teacher/reviews") {
            header(TEACHER_TOKEN, token)
            parameter("status", status)
        }.decode()

    /** Opening a review the first time starts its clock on the server. */
    suspend fun review(token: String, reviewId: String): ReviewDto =
        client.call(HttpMethod.Get, "api/v1/teacher/reviews/${segment(reviewId)}") {
            header(TEACHER_TOKEN, token)
        }.decode()

    suspend fun reviewAudio(token: String, reviewId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/teacher/reviews/${segment(reviewId)}/audio") {
            header(TEACHER_TOKEN, token)
        }.bodyAsBytes()

    suspend fun reply(token: String, reviewId: String, text: String, minutes: String?, audio: FilePart?): ReviewDto =
        client.call(HttpMethod.Post, "api/v1/teacher/reviews/${segment(reviewId)}/reply") {
            header(TEACHER_TOKEN, token)
            setBody(multipartBody(mapOf("text" to text, "minutes" to minutes), listOfNotNull(audio)))
        }.decode()

    suspend fun decline(token: String, reviewId: String, body: JsonObject): ReviewDto =
        client.call(HttpMethod.Post, "api/v1/teacher/reviews/${segment(reviewId)}/decline") {
            header(TEACHER_TOKEN, token)
            jsonBody(body)
        }.decode()

    suspend fun students(token: String): StudentsDto =
        client.call(HttpMethod.Get, "api/v1/teacher/students") { header(TEACHER_TOKEN, token) }.decode()

    /** A payment the teacher took directly (card to card): a lesson, reviews, or both. */
    suspend fun recordPayment(token: String, studentId: String, body: JsonObject): StudentCardDto =
        client.call(HttpMethod.Post, "api/v1/teacher/students/${segment(studentId)}/payments") {
            header(TEACHER_TOKEN, token)
            jsonBody(body)
        }.decode()

    suspend fun logTime(token: String, body: JsonObject) {
        client.call(HttpMethod.Post, "api/v1/teacher/time") {
            header(TEACHER_TOKEN, token)
            jsonBody(body)
        }
    }

    suspend fun report(token: String, days: Int, hourlyRateToman: Long?): TeacherReportDto =
        client.call(HttpMethod.Get, "api/v1/teacher/report") {
            header(TEACHER_TOKEN, token)
            parameter("days", days)
            hourlyRateToman?.let { parameter("hourly_rate_toman", it) }
        }.decode()
}
