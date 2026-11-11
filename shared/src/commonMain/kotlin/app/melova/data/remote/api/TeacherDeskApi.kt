package app.melova.data.remote.api

import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.dto.TeacherEarningsDto
import app.melova.data.remote.dto.TeacherReportDto
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

/** The teacher's desk: sales and earnings, and the hours report. */
class TeacherDeskApi(private val client: HttpClient) {
    suspend fun earnings(token: String): TeacherEarningsDto =
        client.call(HttpMethod.Get, "api/v1/teacher/earnings") { header(TEACHER_TOKEN, token) }.decode()

    suspend fun logTime(token: String, body: JsonObject) {
        client.call(HttpMethod.Post, "api/v1/teacher/time") {
            header(TEACHER_TOKEN, token)
            jsonBody(body)
        }
    }

    suspend fun report(token: String, days: Int, hourlyRateCents: Long?): TeacherReportDto =
        client.call(HttpMethod.Get, "api/v1/teacher/report") {
            header(TEACHER_TOKEN, token)
            parameter("days", days)
            hourlyRateCents?.let { parameter("hourly_rate_cents", it) }
        }.decode()
}
