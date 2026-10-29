package app.melova.data.repository

import app.melova.data.local.StudentSession
import app.melova.data.model.Review
import app.melova.data.model.ReviewAnalysis
import app.melova.data.model.ReviewStatus
import app.melova.data.model.StudentAccount
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.StudentApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.ReviewAnalysisDto
import app.melova.data.remote.dto.ReviewDto
import app.melova.data.remote.dto.StudentMeDto
import app.melova.data.remote.writeAtomically
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.filesDir
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

/** The student's own account, the takes they send the teacher, and the teacher's replies. */
class StudentRepository(
    private val context: PlatformContext,
    private val api: StudentApi = MelovaNetwork.studentApi
) {
    private val session = StudentSession(context)

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
        take: LocalFile,
        note: String,
        score: VocalScoreResult?
    ): StudentAccount {
        // Read now, so the next recording can't overwrite the file mid-upload.
        val audio = FilePart("audio", "take.wav", "audio/wav", take.readBytes())
        val summary = score?.let {
            MelovaNetwork.json.encodeToString(
                ReviewAnalysisDto.serializer(),
                ReviewAnalysisDto(it.overall, it.summary, it.weakPoints)
            )
        }
        return apiCall {
            api.sendForReview(
                lessonId = lessonId,
                sectionId = sectionId,
                note = note.trim(),
                referralId = score?.teacherReferral?.referralId,
                scoreJson = summary,
                audio = audio
            )
        }.toDomain()
    }

    /** Newest first. */
    suspend fun reviews(): List<Review> = apiCall { api.reviews() }.reviews.orEmpty().mapNotNull { it.toDomain() }

    /** The teacher's voice reply, downloaded once and kept. */
    suspend fun replyAudio(review: Review): LocalFile = apiCall {
        val dir = LocalFile(filesDir(context), "review_replies").apply { mkdirs() }
        val target = LocalFile(dir, "${review.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.audio")
        if (!target.exists() || target.length() == 0L) {
            api.replyAudio(review.id).writeAtomically(target)
        }
        target
    }

    private companion object {
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
