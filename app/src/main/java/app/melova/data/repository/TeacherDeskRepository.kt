package app.melova.data.repository

import android.content.Context
import android.net.Uri
import app.melova.data.local.TeacherSession
import app.melova.data.model.Review
import app.melova.data.model.ReviewStatus
import app.melova.data.model.StudentCard
import app.melova.data.model.TeacherReport
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.TeacherDeskApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.StudentCardDto
import app.melova.data.remote.writeAtomically
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** The review queue, students and their payments, and the hours report. */
class TeacherDeskRepository(
    context: Context,
    private val api: TeacherDeskApi = MelovaNetwork.teacherDeskApi
) {
    private val appContext = context.applicationContext
    private val session = TeacherSession(appContext)
    private val prefs = appContext.getSharedPreferences("melova_teacher_desk", Context.MODE_PRIVATE)

    /** What an hour of private teaching nets the teacher; the report compares against it. */
    var privateHourlyRateToman: Long?
        get() = prefs.getLong(KEY_RATE, 0L).takeIf { it > 0L }
        set(value) {
            prefs.edit().apply {
                if (value == null || value <= 0L) remove(KEY_RATE) else putLong(KEY_RATE, value)
            }.apply()
        }

    /** [status] null for every review. */
    suspend fun reviews(status: ReviewStatus?): List<Review> = apiCall {
        api.reviews(token(), status?.name?.lowercase().orEmpty())
    }.reviews.orEmpty().mapNotNull { it.toDomain() }

    suspend fun review(reviewId: String): Review =
        apiCall { api.review(token(), reviewId) }.toDomain() ?: throw IOException("این درخواست پیدا نشد.")

    /** The student's take, downloaded once. */
    suspend fun reviewAudio(reviewId: String): File = apiCall {
        val dir = File(appContext.cacheDir, "review_takes").apply { mkdirs() }
        val target = File(dir, "${reviewId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.wav")
        if (!target.exists() || target.length() == 0L) {
            api.reviewAudio(token(), reviewId).use { it.writeAtomically(target, appContext.cacheDir) }
        }
        target
    }

    /** [minutes] null lets the server count the time since the review was opened. */
    suspend fun reply(reviewId: String, text: String, voice: Uri?, minutes: Double?): Review {
        val plain = "text/plain".toMediaType()
        return apiCall {
            val upload = voice?.let { copyToCache(it) }
            try {
                api.reply(
                    token = token(),
                    reviewId = reviewId,
                    text = text.trim().toRequestBody(plain),
                    minutes = minutes?.toString()?.toRequestBody(plain),
                    audio = upload?.let { (file, name) ->
                        MultipartBody.Part.createFormData("audio", name, file.asRequestBody("audio/*".toMediaType()))
                    }
                )
            } finally {
                upload?.first?.delete()
            }
        }.toDomain() ?: throw IOException("پاسخ ثبت نشد.")
    }

    /** Close a take without reviewing it; the student gets the credit back. */
    suspend fun decline(reviewId: String, text: String): Review =
        apiCall { api.decline(token(), reviewId, mapOf("text" to text.trim())) }.toDomain()
            ?: throw IOException("درخواست بسته نشد.")

    suspend fun students(): List<StudentCard> =
        apiCall { api.students(token()) }.students.orEmpty().mapNotNull { it.toDomain() }

    /**
     * A payment taken outside the app. With a [lessonId] and no [reviews], the lesson's own
     * included reviews are given.
     */
    suspend fun recordPayment(studentId: String, lessonId: String?, reviews: Int?, priceToman: Long): StudentCard {
        val body = buildMap<String, Any> {
            if (lessonId != null) put("lesson_id", lessonId)
            if (reviews != null) put("reviews", reviews)
            put("price_toman", priceToman)
        }
        return apiCall { api.recordPayment(token(), studentId, body) }.toDomain()
            ?: throw IOException("پرداخت ثبت نشد.")
    }

    /** [kind] prep, support or admin: time outside reviews, so the report sees all of it. */
    suspend fun logTime(minutes: Double, kind: String, note: String) =
        apiCall { api.logTime(token(), mapOf("minutes" to minutes, "kind" to kind, "note" to note.trim())) }

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

    /** A recorded (file://) or picked (content://) reply copied where it can be streamed. */
    private fun copyToCache(uri: Uri): Pair<File, String> {
        val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { '.' in it } ?: "reply.m4a"
        val file = File(appContext.cacheDir, "reply_${System.nanoTime()}_$name")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        } ?: throw IOException("فایل پاسخ باز نشد.")
        return file to name
    }

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
        ownedLessons = ownedLessons.orEmpty().toSet(),
        reviewCredits = reviewCredits ?: 0
    )
}
