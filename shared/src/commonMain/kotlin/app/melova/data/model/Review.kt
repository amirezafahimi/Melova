package app.melova.data.model

/** The student's account and the lessons they have. */
data class StudentAccount(
    val id: String,
    val name: String,
    /** False for a guest: the account this install made for itself on first launch. */
    val signedIn: Boolean,
    /** The phone or email they signed in with. */
    val signedInAs: String?,
    /** Changes with the photo; null when there is none. */
    val photoVersion: Long?,
    /** A teacher is an ordinary account that also has the teacher panel. */
    val isTeacher: Boolean,
    val ownedLessons: Set<String>,
    /** Whoever signs in without a name is asked for one, until they give it. */
    val needsName: Boolean = false
)

enum class LoginChannel(val apiKind: String) { PHONE("phone"), EMAIL("email") }

/** Which ways of signing in the server offers, narrowed to what this platform can do. */
data class LoginMethods(
    val phone: Boolean,
    val email: Boolean,
    val googleClientId: String?,
    /** These take any number or email with no code, until real sign-in is set up for them. */
    val mockChannels: Set<LoginChannel> = emptySet()
) {
    val any: Boolean
        get() = phone || email || googleClientId != null

    fun isMock(channel: LoginChannel): Boolean = channel in mockChannels
}

/** A sign-in code on its way. */
data class LoginCodeSent(val sentTo: String, val resendAfterSeconds: Int)

enum class ReviewStatus {
    /** Waiting for the teacher. */
    OPEN,
    ANSWERED,

    /** The teacher closed it without a review; the message came back. */
    REFUNDED,

    /** The teacher didn't answer in time; the message came back. */
    RETURNED;

    companion object {
        fun fromRaw(raw: String?): ReviewStatus = when (raw) {
            "answered" -> ANSWERED
            "refunded" -> REFUNDED
            "returned" -> RETURNED
            else -> OPEN
        }
    }
}

/** Is the app worth the teacher's time? Net income per hour of their work. */
data class TeacherReport(
    val days: Int,
    /** Euro cents, before and after Melova's cut. */
    val grossCents: Long,
    val netCents: Long,
    val payingStudents: Int,
    val teacherMinutes: Double,
    val minutesPerPayingStudent: Double?,
    val reviewsAnswered: Int,
    val averageReviewMinutes: Double,
    val reviewsWaiting: Int,
    val netPerHourCents: Long?,
    val privateHourlyRateCents: Long?,
    /** Null until there is logged time and a private rate to compare with. */
    val worthIt: Boolean?
)
