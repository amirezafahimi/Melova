package app.melova.data.remote.api

import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.LessonRequestsDto
import app.melova.data.remote.dto.StudentCardDto
import app.melova.data.remote.dto.StudentsDto
import app.melova.data.remote.dto.TeacherReportDto
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

/** The teacher's desk: the students and their payments, and the hours report. */
class TeacherDeskApi(private val client: HttpClient) {
    suspend fun students(token: String): StudentsDto =
        client.call(HttpMethod.Get, "api/v1/teacher/students") { header(TEACHER_TOKEN, token) }.decode()

    /** Gives a student a lesson they paid for outside the app. */
    suspend fun recordPayment(token: String, studentId: String, body: JsonObject): StudentCardDto =
        client.call(HttpMethod.Post, "api/v1/teacher/students/${segment(studentId)}/payments") {
            header(TEACHER_TOKEN, token)
            jsonBody(body)
        }.decode()

    /** Students who say they paid by card; the two calls below return what is still waiting. */
    suspend fun purchaseRequests(token: String): LessonRequestsDto =
        client.call(HttpMethod.Get, "api/v1/teacher/purchase-requests") { header(TEACHER_TOKEN, token) }.decode()

    suspend fun approvePurchase(token: String, requestId: String): LessonRequestsDto =
        client.call(HttpMethod.Post, "api/v1/teacher/purchase-requests/${segment(requestId)}/approve") {
            header(TEACHER_TOKEN, token)
        }.decode()

    suspend fun rejectPurchase(token: String, requestId: String): LessonRequestsDto =
        client.call(HttpMethod.Post, "api/v1/teacher/purchase-requests/${segment(requestId)}/reject") {
            header(TEACHER_TOKEN, token)
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
