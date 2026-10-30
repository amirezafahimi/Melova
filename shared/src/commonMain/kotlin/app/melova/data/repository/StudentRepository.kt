package app.melova.data.repository

import app.melova.data.local.StudentSession
import app.melova.data.local.TeacherSession
import app.melova.data.model.LoginCodeSent
import app.melova.data.model.LoginMethods
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
import app.melova.platform.supportsGoogleSignIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

/** How a sign-in code reaches the student; [apiKind] is what the server calls it. */
enum class LoginChannel(val apiKind: String) { PHONE("phone"), EMAIL("email") }

/** The student's own account, the takes they send the teacher, and the teacher's replies. */
class StudentRepository(
    private val context: PlatformContext,
    private val api: StudentApi = MelovaNetwork.studentApi
) {
    private val session = StudentSession(context)
    private val teacherSession = TeacherSession(context)

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

    suspend fun account(): StudentAccount = followTeacherRole(apiCall { api.me() }.toDomain())

    /** The signed-in student says they teach; [code] proves it. */
    suspend fun sayITeach(code: String): StudentAccount =
        followTeacherRole(apiCall { api.sayITeach(code.trim()) }.toDomain())

    suspend fun sayIDontTeach(): StudentAccount = followTeacherRole(apiCall { api.sayIDontTeach() }.toDomain())

    /** A signed-in account decides whether this install has the teacher panel. */
    private fun followTeacherRole(account: StudentAccount): StudentAccount {
        if (account.signedIn) teacherSession.token = session.token.takeIf { account.isTeacher }
        return account
    }

    suspend fun setName(name: String): StudentAccount =
        apiCall { api.updateMe(mapOf("name" to name.trim())) }.toDomain()

    /** The server crops it to a square; read now, as the picked file is only temporary. */
    suspend fun setPhoto(photo: LocalFile): StudentAccount {
        val part = FilePart("photo", "photo", "application/octet-stream", photo.readBytes())
        return apiCall { api.setPhoto(part) }.toDomain()
    }

    suspend fun removePhoto(): StudentAccount = apiCall { api.deletePhoto() }.toDomain()

    /** The student's photo, downloaded once per version and kept. */
    suspend fun photo(account: StudentAccount): ByteArray? {
        val version = account.photoVersion ?: return null
        return apiCall {
            val dir = LocalFile(filesDir(context), "profile_photo").apply { mkdirs() }
            val target = LocalFile(dir, "${account.id}-$version.jpg")
            if (!target.exists() || target.length() == 0L) {
                dir.listFiles()?.forEach { it.delete() }
                api.photo().writeAtomically(target)
            }
            target.readBytes()
        }
    }

    suspend fun loginMethods(): LoginMethods {
        val methods = apiCall { api.loginMethods() }
        return LoginMethods(
            phone = methods.phone == true,
            email = methods.email == true,
            googleClientId = methods.googleClientId?.takeIf { it.isNotBlank() && supportsGoogleSignIn },
            isMock = methods.mock == true
        )
    }

    /** True the first time it's asked, and never after signing in. */
    fun takeLoginOffer(): Boolean {
        if (session.loginOffered) return false
        session.loginOffered = true
        return true
    }

    suspend fun requestLoginCode(channel: LoginChannel, address: String): LoginCodeSent {
        val sent = apiCall { api.requestLoginCode(mapOf("kind" to channel.apiKind, "value" to address.trim())) }
        return LoginCodeSent(sent.sentTo ?: address.trim(), sent.resendAfter ?: 60)
    }

    suspend fun verifyLoginCode(channel: LoginChannel, address: String, code: String): StudentAccount =
        signedIn(apiCall {
            api.verifyLoginCode(mapOf("kind" to channel.apiKind, "value" to address.trim(), "code" to code))
        })

    suspend fun signInWithGoogle(idToken: String): StudentAccount = signedIn(apiCall { api.signInWithGoogle(idToken) })

    /** The server moved this install's guest data to the account; from now on this token opens it. */
    private fun signedIn(account: StudentMeDto): StudentAccount {
        val id = account.studentId ?: throw IOException("ورود ممکن نشد.")
        val token = account.token ?: throw IOException("ورود ممکن نشد.")
        session.save(id, token)
        session.loginOffered = true
        restoreSession()
        return followTeacherRole(account.toDomain())
    }

    /** Back to being a guest, with a new guest account. */
    suspend fun signOut() {
        // Forgetting the token is what signs this install out, so a failed call changes nothing.
        runCatching { apiCall { api.logout() } }
        session.clear()
        teacherSession.token = null
        restoreSession()
        ensureAccount()
    }

    suspend fun claimFreeLesson(lessonId: String): StudentAccount =
        apiCall { api.claimFreeLesson(lessonId) }.toDomain()

    /** The student paid for a lesson by card, under the name [payer]. */
    suspend fun requestLesson(lessonId: String, payer: String): StudentAccount =
        apiCall { api.requestLesson(lessonId, payer.trim()) }.toDomain()

    /** Whom the student follows now. Follows are kept on the account, so a sign-in brings them. */
    suspend fun follow(teacherIds: Set<String>): Set<String> {
        ensureAccount()
        return apiCall { api.follow(teacherIds) }.followedTeachers.orEmpty().toSet()
    }

    suspend fun unfollow(teacherId: String): Set<String> =
        apiCall { api.unfollow(teacherId) }.followedTeachers.orEmpty().toSet()

    /** Sends this take to the teacher; the score's referral id says the scorer itself asked. */
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
    signedIn = signedIn == true,
    signedInAs = signedInAs?.takeIf { it.isNotBlank() },
    photoVersion = photoVersion,
    isTeacher = isTeacher == true,
    ownedLessons = ownedLessons.orEmpty().toSet()
)

internal fun ReviewDto.toDomain(): Review? {
    val reviewId = id?.takeIf { it.isNotBlank() } ?: return null
    return Review(
        id = reviewId,
        lessonId = lessonId.orEmpty(),
        sectionId = sectionId.orEmpty(),
        isReferral = reason == "auto",
        referralReasons = autoReasons.orEmpty(),
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
