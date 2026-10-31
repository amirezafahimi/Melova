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
    /** Only an account just made is asked its name; one that signed in before never is. */
    val needsName: Boolean = false
)

/** Which ways of signing in the server offers, narrowed to what this platform can do. */
data class LoginMethods(
    val phone: Boolean,
    val email: Boolean,
    val googleClientId: String?,
    /** The server takes any number or email with no code, until real sign-in is set up. */
    val isMock: Boolean = false
) {
    val any: Boolean
        get() = phone || email || googleClientId != null
}

/** A sign-in code on its way. */
data class LoginCodeSent(val sentTo: String, val resendAfterSeconds: Int)

enum class ReviewStatus {
    /** Waiting for the teacher. */
    OPEN,
    ANSWERED,

    /** The teacher closed it without a review. */
    REFUNDED;

    companion object {
        fun fromRaw(raw: String?): ReviewStatus = when (raw) {
            "answered" -> ANSWERED
            "refunded" -> REFUNDED
            else -> OPEN
        }
    }
}

/** A student as the teacher sees them, to find who paid. */
data class StudentCard(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val ownedLessons: Set<String>
)

/** A student who says they paid for a lesson by card, waiting for the teacher to find it. */
data class LessonPurchaseRequest(
    val id: String,
    val lessonId: String,
    val priceToman: Long,
    /** The name on the payment. */
    val payer: String,
    val studentName: String,
    val createdAtMs: Long
)

/** Is the app worth the teacher's time? Net income per hour of their work. */
data class TeacherReport(
    val days: Int,
    val grossToman: Long,
    val netToman: Long,
    val payingStudents: Int,
    val teacherMinutes: Double,
    val minutesPerPayingStudent: Double?,
    val reviewsAnswered: Int,
    val averageReviewMinutes: Double,
    val reviewsWaiting: Int,
    val netPerHourToman: Long?,
    val privateHourlyRateToman: Long?,
    /** Null until there is logged time and a private rate to compare with. */
    val worthIt: Boolean?
)
