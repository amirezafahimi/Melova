package app.melova.data.repository

import android.content.Context
import app.melova.data.local.StudentSession
import app.melova.data.model.Review
import app.melova.data.model.ReviewAnalysis
import app.melova.data.model.ReviewStatus
import app.melova.data.model.StudentAccount
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.StudentApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.ReviewAnalysisDto
import app.melova.data.remote.dto.ReviewDto
import app.melova.data.remote.dto.StudentMeDto
import app.melova.data.remote.writeAtomically
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** The student's own account, the takes they send the teacher, and the teacher's replies. */
class StudentRepository(
    context: Context,
    private val api: StudentApi = MelovaNetwork.studentApi
) {
    private val appContext = context.applicationContext
    private val session = StudentSession(appContext)

    val studentId: String?
        get() = session.studentId

    /** Puts the saved token on every request; call once at start, before any network call. */
    fun restoreSession() {
        MelovaNetwork.studentToken = session.token
    }

    /** The account, creating one on the first launch. True when it was just created. */
    suspend fun ensureAccount(): Boolean = accountLock.withLock {
        if (session.token != null) {
            restoreSession()
            return false
        }
        val created = apiCall { api.create(emptyMap()) }
        val id = created.studentId ?: throw IOException("ساخت حساب هنرجو ممکن نشد.")
        val token = created.token ?: throw IOException("ساخت حساب هنرجو ممکن نشد.")
        session.save(id, token)
        restoreSession()
        true
    }

    suspend fun account(): StudentAccount = apiCall { api.me() }.toDomain()

    suspend fun setName(name: String): StudentAccount =
        apiCall { api.updateMe(mapOf("name" to name.trim())) }.toDomain()

    suspend fun claimFreeLesson(lessonId: String): StudentAccount =
        apiCall { api.claimFreeLesson(lessonId) }.toDomain()

    /**
     * Sends this take to the teacher. With the score's referral id it is a free referral
     * (the scorer asked); otherwise it costs one review credit.
     */
    suspend fun sendForReview(
        lessonId: String,
        sectionId: String,
        take: File,
        note: String,
        score: VocalScoreResult?
    ): StudentAccount {
        val text = "text/plain".toMediaType()
        // A copy, so the next recording can't overwrite the file mid-upload.
        val upload = File(appContext.cacheDir, "review_take_${System.nanoTime()}.wav")
        take.copyTo(upload, overwrite = true)
        return try {
            val summary = score?.let {
                gson.toJson(ReviewAnalysisDto(it.overall, it.summary, it.weakPoints))
            }
            apiCall {
                api.sendForReview(
                    lessonId = lessonId.toRequestBody(text),
                    sectionId = sectionId.toRequestBody(text),
                    note = note.trim().toRequestBody(text),
                    referralId = score?.teacherReferral?.referralId?.toRequestBody(text),
                    scoreJson = summary?.toRequestBody("application/json".toMediaType()),
                    audio = MultipartBody.Part.createFormData(
                        "audio", "take.wav", upload.asRequestBody("audio/wav".toMediaType())
                    )
                )
            }.toDomain()
        } finally {
            upload.delete()
        }
    }

    /** Newest first. */
    suspend fun reviews(): List<Review> = apiCall { api.reviews() }.reviews.orEmpty().mapNotNull { it.toDomain() }

    /** The teacher's voice reply, downloaded once and kept. */
    suspend fun replyAudio(review: Review): File = apiCall {
        val dir = File(appContext.filesDir, "review_replies").apply { mkdirs() }
        val target = File(dir, "${review.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.audio")
        if (!target.exists() || target.length() == 0L) {
            api.replyAudio(review.id).use { it.writeAtomically(target, appContext.cacheDir) }
        }
        target
    }

    private companion object {
        val gson = Gson()

        /** Two screens asking at once must not create two accounts. */
        val accountLock = Mutex()
    }
}

internal fun StudentMeDto.toDomain() = StudentAccount(
    id = studentId.orEmpty(),
    name = name.orEmpty(),
    ownedLessons = ownedLessons.orEmpty().toSet(),
    reviewCredits = reviewCredits.orEmpty()
)

internal fun ReviewDto.toDomain(): Review? {
    val reviewId = id?.takeIf { it.isNotBlank() } ?: return null
    return Review(
        id = reviewId,
        lessonId = lessonId.orEmpty(),
        sectionId = sectionId.orEmpty(),
        isReferral = reason == "auto",
        referralReasons = autoReasons.orEmpty(),
        charged = charged != false,
        note = note.orEmpty(),
        status = ReviewStatus.fromRaw(status),
        createdAtMs = ((createdAt ?: 0.0) * 1000).toLong(),
        answeredAtMs = answeredAt?.let { (it * 1000).toLong() },
        replyText = replyText.orEmpty(),
        hasReplyAudio = hasReplyAudio == true,
        studentId = studentId.orEmpty(),
        studentName = studentName.orEmpty(),
        teacherMinutes = teacherMinutes,
        analysis = analysis?.let {
            ReviewAnalysis(it.overall ?: 0, it.summary.orEmpty(), it.weakPoints.orEmpty())
        }
    )
}
