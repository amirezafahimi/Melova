package app.melova.data.repository

import app.melova.data.local.TeacherSession
import app.melova.data.model.LessonPurchaseRequest
import app.melova.data.model.StudentCard
import app.melova.data.model.TeacherReport
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.TeacherDeskApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.LessonRequestDto
import app.melova.data.remote.dto.StudentCardDto
import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext
import kotlinx.io.IOException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Students and their payments, and the hours report. */
class TeacherDeskRepository(
    context: PlatformContext,
    private val api: TeacherDeskApi = MelovaNetwork.teacherDeskApi
) {
    private val session = TeacherSession(context)
    private val prefs = KeyValueStore(context, "melova_teacher_desk")

    /** What an hour of private teaching nets the teacher; the report compares against it. */
    var privateHourlyRateToman: Long?
        get() = prefs.getLong(KEY_RATE)?.takeIf { it > 0L }
        set(value) {
            if (value == null || value <= 0L) prefs.remove(KEY_RATE) else prefs.putLong(KEY_RATE, value)
        }

    suspend fun students(): List<StudentCard> =
        apiCall { api.students(token()) }.students.orEmpty().mapNotNull { it.toDomain() }

    /** Gives a student a lesson they paid for outside the app. */
    suspend fun recordPayment(studentId: String, lessonId: String, priceToman: Long): StudentCard {
        val body = buildJsonObject {
            put("lesson_id", lessonId)
            put("price_toman", priceToman)
        }
        return apiCall { api.recordPayment(token(), studentId, body) }.toDomain()
            ?: throw IOException("پرداخت ثبت نشد.")
    }

    suspend fun purchaseRequests(): List<LessonPurchaseRequest> =
        apiCall { api.purchaseRequests(token()) }.requests.orEmpty().mapNotNull { it.toDomain() }

    /** Confirms or refuses a student's card payment; returns the requests still waiting. */
    suspend fun decidePurchase(requestId: String, approve: Boolean): List<LessonPurchaseRequest> = apiCall {
        if (approve) api.approvePurchase(token(), requestId) else api.rejectPurchase(token(), requestId)
    }.requests.orEmpty().mapNotNull { it.toDomain() }

    /** [kind] prep, support or admin: time outside reviews, so the report sees all of it. */
    suspend fun logTime(minutes: Double, kind: String, note: String) = apiCall {
        api.logTime(
            token(),
            buildJsonObject {
                put("minutes", minutes)
                put("kind", kind)
                put("note", note.trim())
            }
        )
    }

    suspend fun report(days: Int = 30): TeacherReport = apiCall {
        api.report(token(), days, privateHourlyRateToman)
    }.let { dto ->
        TeacherReport(
            days = dto.days ?: days,
            grossToman = dto.grossToman ?: 0L,
            netToman = dto.netToman ?: 0L,
            payingStudents = dto.payingStudents ?: 0,
            teacherMinutes = dto.teacherMinutes ?: 0.0,
            minutesPerPayingStudent = dto.minutesPerPayingStudent,
            reviewsAnswered = dto.reviewsAnswered ?: 0,
            averageReviewMinutes = dto.averageReviewMinutes ?: 0.0,
            reviewsWaiting = dto.reviewsWaiting ?: 0,
            netPerHourToman = dto.netPerHourToman,
            privateHourlyRateToman = dto.privateHourlyRateToman,
            worthIt = dto.worthIt
        )
    }

    private fun token(): String = session.token ?: throw IOException("ابتدا با کد استاد وارد شوید.")

    private companion object {
        const val KEY_RATE = "private_hourly_rate_toman"
    }
}

private fun StudentCardDto.toDomain(): StudentCard? {
    val studentId = id?.takeIf { it.isNotBlank() } ?: return null
    return StudentCard(
        id = studentId,
        name = name.orEmpty(),
        createdAtMs = ((createdAt ?: 0.0) * 1000).toLong(),
        ownedLessons = ownedLessons.orEmpty().toSet()
    )
}

private fun LessonRequestDto.toDomain(): LessonPurchaseRequest? {
    val requestId = id?.takeIf { it.isNotBlank() } ?: return null
    return LessonPurchaseRequest(
        id = requestId,
        lessonId = lessonId.orEmpty(),
        priceToman = priceToman ?: 0L,
        payer = payer.orEmpty(),
        studentName = studentName.orEmpty(),
        createdAtMs = ((createdAt ?: 0.0) * 1000).toLong()
    )
}
