package app.melova.data.repository

import app.melova.data.local.StudentSession
import app.melova.data.local.TeacherSession
import app.melova.data.model.EarningsTotal
import app.melova.data.model.LessonTier
import app.melova.data.model.TeacherEarnings
import app.melova.data.model.TeacherReport
import app.melova.data.model.TeacherSale
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.TeacherDeskApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.EarningsTotalDto
import app.melova.data.remote.dto.TeacherSaleDto
import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext
import kotlinx.io.IOException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Sales and earnings, and the hours report. */
class TeacherDeskRepository(
    private val context: PlatformContext,
    private val api: TeacherDeskApi = MelovaNetwork.teacherDeskApi
) {
    private val session = TeacherSession(context)
    private val account = StudentSession(context)

    // One teacher's rate is theirs alone, so a second teacher on this device starts without it.
    private val prefs: KeyValueStore?
        get() = account.studentId?.let { KeyValueStore(context, "melova_teacher_desk_$it") }

    /** What an hour of private teaching nets the teacher; the report compares against it. */
    var privateHourlyRateCents: Long?
        get() = prefs?.getLong(KEY_RATE)?.takeIf { it > 0L }
        set(value) {
            val prefs = prefs ?: return
            if (value == null || value <= 0L) prefs.remove(KEY_RATE) else prefs.putLong(KEY_RATE, value)
        }

    /** Every sale of this teacher's lessons and their share of it; recorded, not paid out. */
    suspend fun earnings(): TeacherEarnings = apiCall { api.earnings(token()) }.let { dto ->
        TeacherEarnings(
            cutPercent = dto.cutPercent ?: 30,
            total = dto.total.toDomain(),
            thisMonth = dto.thisMonth.toDomain(),
            sales = dto.sales.orEmpty().mapNotNull { it.toDomain() }
        )
    }

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
        api.report(token(), days, privateHourlyRateCents)
    }.let { dto ->
        TeacherReport(
            days = dto.days ?: days,
            grossCents = dto.grossCents ?: 0L,
            netCents = dto.netCents ?: 0L,
            payingStudents = dto.payingStudents ?: 0,
            teacherMinutes = dto.teacherMinutes ?: 0.0,
            minutesPerPayingStudent = dto.minutesPerPayingStudent,
            reviewsAnswered = dto.reviewsAnswered ?: 0,
            averageReviewMinutes = dto.averageReviewMinutes ?: 0.0,
            reviewsWaiting = dto.reviewsWaiting ?: 0,
            netPerHourCents = dto.netPerHourCents,
            privateHourlyRateCents = dto.privateHourlyRateCents,
            worthIt = dto.worthIt
        )
    }

    private fun token(): String = session.token ?: throw IOException("ابتدا با کد استاد وارد شوید.")

    private companion object {
        // A new key: the old one held toman, which read as cents would be nonsense.
        const val KEY_RATE = "private_hourly_rate_cents"
    }
}

private fun EarningsTotalDto?.toDomain(): EarningsTotal = EarningsTotal(
    count = this?.count ?: 0,
    grossCents = this?.grossCents ?: 0L,
    teacherCents = this?.teacherCents ?: 0L,
    melovaCents = this?.melovaCents ?: 0L
)

private fun TeacherSaleDto.toDomain(): TeacherSale? {
    val saleId = id ?: return null
    return TeacherSale(
        id = saleId.toString(),
        lessonId = lessonId.orEmpty(),
        tier = LessonTier.fromRaw(tier) ?: LessonTier.LESSON,
        upgrade = upgrade == true,
        grossCents = grossCents ?: 0L,
        teacherCents = teacherCents ?: 0L,
        studentName = studentName.orEmpty(),
        createdAtMs = ((createdAt ?: 0.0) * 1000).toLong()
    )
}
