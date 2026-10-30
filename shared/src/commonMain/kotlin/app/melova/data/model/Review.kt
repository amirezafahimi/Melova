package app.melova.data.model

/** The student's account: what they own and how many teacher reviews they have left. */
data class StudentAccount(
    val id: String,
    val name: String,
    /** False for a guest: the account this install made for itself on first launch. */
    val signedIn: Boolean,
    /** The phone or email they signed in with. */
    val signedInAs: String?,
    /** Changes with the photo; null when there is none. */
    val photoVersion: Long?,
    val ownedLessons: Set<String>,
    /** Review credits left, by teacher id. */
    val reviewCredits: Map<String, Int>
) {
    fun credits(teacherId: String): Int = reviewCredits[teacherId] ?: 0
}

/** Which ways of signing in the server offers, narrowed to what this platform can do. */
data class LoginMethods(
    val phone: Boolean,
    val email: Boolean,
    val googleClientId: String?
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

    /** The teacher closed it without a review; the credit came back. */
    REFUNDED;

    companion object {
        fun fromRaw(raw: String?): ReviewStatus = when (raw) {
            "answered" -> ANSWERED
            "refunded" -> REFUNDED
            else -> OPEN
        }
    }
}

/** A take a student sent the teacher, with the teacher's answer once there is one. */
data class Review(
    val id: String,
    val lessonId: String,
    val sectionId: String,
    /** True when the scorer itself asked for the teacher. */
    val isReferral: Boolean,
    val referralReasons: List<String>,
    val charged: Boolean,
    val note: String,
    val status: ReviewStatus,
    val createdAtMs: Long,
    val answeredAtMs: Long?,
    val replyText: String,
    val hasReplyAudio: Boolean,
    // Teacher's view only.
    val studentId: String = "",
    val studentName: String = "",
    val teacherMinutes: Double? = null,
    val analysis: ReviewAnalysis? = null
)

/** The score summary sent with a take, so the teacher sees what the student was told. */
data class ReviewAnalysis(
    val overall: Int,
    val summary: String,
    val weakPoints: List<String>
)

/** A student as the teacher sees them, to find who paid. */
data class StudentCard(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val ownedLessons: Set<String>,
    val reviewCredits: Int
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
