package app.melova.data.repository

import app.melova.audio.io.takePart
import app.melova.data.local.StudentSession
import app.melova.data.local.TeacherSession
import app.melova.data.model.LoginCodeSent
import app.melova.data.model.LoginMethods
import app.melova.data.model.StudentAccount
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.StudentApi
import app.melova.data.remote.apiCall
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

    suspend fun account(): StudentAccount = followTeacherRole(apiCall { api.me() }.toAccount())

    /** Every answer carries the account, so each one has to keep the name question open. */
    private fun StudentMeDto.toAccount(): StudentAccount {
        if (!name.isNullOrBlank()) session.awaitingName = false
        return toDomain(awaitingName = session.awaitingName)
    }

    /** The signed-in student says they teach, which [code] proves. The account stays a teacher for good. */
    suspend fun sayITeach(code: String): StudentAccount =
        followTeacherRole(apiCall { api.sayITeach(code.trim()) }.toAccount())

    /** A signed-in account decides whether this install has the teacher panel. */
    private fun followTeacherRole(account: StudentAccount): StudentAccount {
        if (account.signedIn) teacherSession.token = session.token.takeIf { account.isTeacher }
        return account
    }

    suspend fun setName(name: String): StudentAccount =
        apiCall { api.updateMe(mapOf("name" to name.trim())) }.toAccount()

    /** The server crops it to a square; read now, as the picked file is only temporary. */
    suspend fun setPhoto(photo: LocalFile): StudentAccount {
        val part = FilePart("photo", "photo", "application/octet-stream", photo.readBytes())
        return apiCall { api.setPhoto(part) }.toAccount()
    }

    suspend fun removePhoto(): StudentAccount = apiCall { api.deletePhoto() }.toAccount()

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
        // Whoever signs in without a name is asked; toAccount closes this if they have one.
        session.awaitingName = true
        restoreSession()
        return followTeacherRole(account.toAccount())
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
        apiCall { api.claimFreeLesson(lessonId) }.toAccount()

    /** The student paid for a lesson by card, under the name [payer]. */
    suspend fun requestLesson(lessonId: String, payer: String): StudentAccount =
        apiCall { api.requestLesson(lessonId, payer.trim()) }.toAccount()

    /** Whom the student follows now. Follows are kept on the account, so a sign-in brings them. */
    suspend fun follow(teacherIds: Set<String>): Set<String> {
        ensureAccount()
        return apiCall { api.follow(teacherIds) }.followedTeachers.orEmpty().toSet()
    }

    suspend fun unfollow(teacherId: String): Set<String> =
        apiCall { api.unfollow(teacherId) }.followedTeachers.orEmpty().toSet()

    /**
     * Sends this take to the teacher, with its whole result so the chat can open the same page.
     * The score's referral id says the scorer itself asked. Returns the chat it went into.
     */
    suspend fun sendForReview(
        lessonId: String,
        sectionId: String,
        take: LocalFile,
        note: String,
        score: VocalScoreResult?
    ): String? {
        // Read now, so the next recording can't overwrite the file mid-upload.
        val audio = takePart("audio", take.readBytes())
        return apiCall {
            api.sendForReview(
                lessonId = lessonId,
                sectionId = sectionId,
                note = note.trim(),
                referralId = score?.teacherReferral?.referralId,
                scoreJson = score?.sourceJson,
                audio = audio
            )
        }.chatId
    }

    private companion object {
        /** Two screens asking at once must not create two accounts. */
        val accountLock = Mutex()
    }
}

internal fun StudentMeDto.toDomain(awaitingName: Boolean) = StudentAccount(
    id = studentId.orEmpty(),
    name = name.orEmpty(),
    signedIn = signedIn == true,
    signedInAs = signedInAs?.takeIf { it.isNotBlank() },
    photoVersion = photoVersion,
    isTeacher = isTeacher == true,
    ownedLessons = ownedLessons.orEmpty().toSet(),
    needsName = awaitingName && signedIn == true && name.isNullOrBlank()
)
