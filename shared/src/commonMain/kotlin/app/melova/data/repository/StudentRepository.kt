package app.melova.data.repository

import app.melova.audio.io.takePart
import app.melova.data.local.StudentSession
import app.melova.data.local.TeacherSession
import app.melova.data.model.LessonCheckout
import app.melova.data.model.LessonOffer
import app.melova.data.model.LessonTier
import app.melova.data.model.LoginCodeSent
import app.melova.data.model.LoginChannel
import app.melova.data.model.LoginMethods
import app.melova.data.model.JournalSection
import app.melova.data.model.JournalTake
import app.melova.data.model.PracticeProgress
import app.melova.data.model.PracticeWeek
import app.melova.data.model.PracticeTake
import app.melova.data.model.PracticeTakeDetail
import app.melova.data.model.StudentAccount
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.StudentApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.JournalSectionDto
import app.melova.data.remote.dto.JournalTakeDto
import app.melova.data.remote.dto.LoginMethodsDto
import app.melova.data.remote.dto.PracticeWeekDto
import app.melova.data.remote.dto.PracticeTakeDto
import app.melova.data.remote.mapper.toDomain
import app.melova.data.remote.dto.StudentMeDto
import app.melova.data.remote.writeAtomically
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.cacheDir
import app.melova.platform.currentTimeMillis
import app.melova.platform.utcOffsetMinutes
import app.melova.platform.filesDir
import app.melova.platform.supportsGoogleSignIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

/** How a sign-in code reaches the student; [apiKind] is what the server calls it. */
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
        return apiCall { api.loginMethods() }.toDomain(supportsGoogleSignIn)
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

    /** Taking a lesson needs a signed-in account, once there is a way to sign in. */
    suspend fun mayTakeLessons(): Boolean =
        runCatching { account().signedIn || !loginMethods().any }.getOrDefault(false)

    suspend fun claimFreeLesson(lessonId: String): StudentAccount =
        apiCall { api.claimFreeLesson(lessonId) }.toAccount()

    /** Buys [tier] of a lesson; with the plain version owned, the one with the teacher costs the difference. */
    suspend fun checkout(lessonId: String, tier: LessonTier): LessonCheckout {
        val dto = apiCall { api.checkout(lessonId, tier.apiName) }
        return LessonCheckout(
            paid = dto.order?.status == "paid",
            redirectUrl = dto.order?.redirectUrl?.takeIf { it.isNotBlank() },
            offer = dto.lesson?.toDomain() ?: LessonOffer(),
            account = dto.toAccount()
        )
    }

    /** The student's version of a lesson and messages left; it changes as takes are sent and come back. */
    suspend fun lessonOffer(lessonId: String): LessonOffer = apiCall { api.lessonAccess(lessonId) }.toDomain()

    /** Whom the student follows now. Follows are kept on the account, so a sign-in brings them. */
    suspend fun follow(teacherIds: Set<String>): Set<String> {
        ensureAccount()
        return apiCall { api.follow(teacherIds) }.followedTeachers.orEmpty().toSet()
    }

    suspend fun unfollow(teacherId: String): Set<String> =
        apiCall { api.unfollow(teacherId) }.followedTeachers.orEmpty().toSet()

    /**
     * Sends this take to the teacher, with its whole result so the chat can open the same page.
     * The score's referral id says the scorer itself asked. Returns the chat it went into and
     * the lesson's messages left after this one.
     */
    suspend fun sendForReview(
        lessonId: String,
        sectionId: String,
        take: LocalFile,
        note: String,
        score: VocalScoreResult?
    ): SentForReview {
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
        }.let { SentForReview(chatId = it.chatId, offer = it.lesson?.toDomain()) }
    }

    suspend fun practiceProgress(): PracticeProgress {
        val progress = apiCall { api.practice() }
        return PracticeProgress(
            bestScores = progress.bestScores.orEmpty().mapNotNull { best ->
                val score = best.score ?: return@mapNotNull null
                PracticeProgress.key(best.lessonId ?: return@mapNotNull null, best.sectionId ?: return@mapNotNull null) to score
            }.toMap(),
            lastLessonId = progress.last?.lessonId,
            lastSectionId = progress.last?.sectionId,
            lastScore = progress.last?.score,
            stamped = progress.stamped.orEmpty().mapNotNullTo(mutableSetOf()) { section ->
                PracticeProgress.key(section.lessonId ?: return@mapNotNullTo null, section.sectionId ?: return@mapNotNullTo null)
            }
        )
    }

    /** This week's practice days in the device's own time, so a day ends at the student's midnight. */
    suspend fun practiceWeek(): PracticeWeek =
        apiCall { api.practiceWeek(utcOffsetMinutes(currentTimeMillis())) }.toDomain()

    suspend fun setPracticeWeek(goal: Int, reminders: Boolean): PracticeWeek =
        apiCall { api.setPracticeWeek(goal, reminders, utcOffsetMinutes(currentTimeMillis())) }.toDomain()

    /** The weekly reminder's text when it is due, for an app with no push address; null otherwise. */
    suspend fun claimPracticeReminder(): String? =
        apiCall { api.claimPracticeReminder() }.body?.takeIf { it.isNotBlank() }

    suspend fun practiceJournal(): List<JournalSection> =
        apiCall { api.practiceJournal() }.sections.orEmpty().mapNotNull { it.toDomain() }

    /** This section's earlier takes the practice page lists, newest first. */
    suspend fun practiceTakes(lessonId: String, sectionId: String): List<PracticeTake> =
        apiCall { api.practiceTakes(lessonId, sectionId) }.takes.orEmpty().mapNotNull { it.toDomain() }

    suspend fun practiceTake(takeId: String): PracticeTakeDetail {
        val take = apiCall { api.practiceTake(takeId) }
        return PracticeTakeDetail(
            createdAtMs = ((take.createdAt ?: 0.0) * 1000).toLong(),
            result = take.result?.toDomain()
        )
    }

    /** The take's recording, downloaded once: a kept take never changes. */
    suspend fun practiceTakeAudio(takeId: String): LocalFile = apiCall {
        val dir = LocalFile(cacheDir(context), "practice_takes").apply { mkdirs() }
        val target = LocalFile(dir, "${takeId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.wav")
        if (!target.exists() || target.length() == 0L) {
            api.practiceTakeAudio(takeId).writeAtomically(target)
        }
        target
    }

    private companion object {
        /** Two screens asking at once must not create two accounts. */
        val accountLock = Mutex()
    }
}

/** A take on its way to the teacher. */
data class SentForReview(val chatId: String?, val offer: LessonOffer?)

internal fun LoginMethodsDto.toDomain(canUseGoogle: Boolean) = LoginMethods(
    phone = phone == true,
    email = email == true,
    googleClientId = googleClientId?.takeIf { it.isNotBlank() && canUseGoogle },
    mockChannels = LoginChannel.entries.filterTo(mutableSetOf()) { channel ->
        // A server from before "mock_kinds" mocks every channel or none.
        mockKinds?.contains(channel.apiKind) ?: (mock == true)
    }
)

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

private fun PracticeTakeDto.toDomain(): PracticeTake? {
    val takeId = id ?: return null
    val takeScore = score ?: return null
    val why = roles.orEmpty()
    return PracticeTake(
        id = takeId,
        score = takeScore,
        createdAtMs = ((createdAt ?: 0.0) * 1000).toLong(),
        isFirst = "first" in why,
        isBest = "best" in why,
        isRecent = "recent" in why
    )
}

internal fun PracticeWeekDto.toDomain(): PracticeWeek {
    val week = days.orEmpty()
    return PracticeWeek(
        goal = (goal ?: PracticeWeek.DEFAULT_GOAL).coerceIn(PracticeWeek.MIN_GOAL, PracticeWeek.MAX_GOAL),
        days = List(7) { week.getOrNull(it) == true },
        today = (today ?: 0).coerceIn(0, 6),
        practised = practised ?: 0,
        fullWeeks = fullWeeks ?: 0,
        reminders = reminders == true
    )
}

private fun JournalTakeDto.toDomain(): JournalTake? =
    JournalTake(id = id, score = score ?: return null, createdAtMs = ((createdAt ?: 0.0) * 1000).toLong())

internal fun JournalSectionDto.toDomain(): JournalSection? = JournalSection(
    lessonId = lessonId ?: return null,
    sectionId = sectionId ?: return null,
    lessonTitle = lessonTitle.orEmpty(),
    sectionTitle = sectionTitle.orEmpty(),
    first = first?.toDomain() ?: return null,
    best = best?.toDomain() ?: return null,
    takes = takes ?: 0,
    lastAtMs = ((lastAt ?: 0.0) * 1000).toLong(),
    stamped = stamped == true
)
