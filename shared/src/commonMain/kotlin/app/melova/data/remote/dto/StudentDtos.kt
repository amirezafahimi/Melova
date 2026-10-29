package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatedStudentDto(
    @SerialName("student_id") val studentId: String?,
    @SerialName("token") val token: String?
)

/** The student's account as the server has it; returned by every student call. */
@Serializable
data class StudentMeDto(
    @SerialName("student_id") val studentId: String?,
    @SerialName("name") val name: String?,
    @SerialName("owned_lessons") val ownedLessons: List<String>?,
    /** Review credits left, by teacher id. */
    @SerialName("review_credits") val reviewCredits: Map<String, Int>?,
    @SerialName("review") val review: ReviewDto? = null
)

@Serializable
data class ReviewsDto(
    @SerialName("reviews") val reviews: List<ReviewDto>?
)

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String?,
    @SerialName("lesson_id") val lessonId: String?,
    @SerialName("section_id") val sectionId: String?,
    @SerialName("reason") val reason: String?,
    @SerialName("auto_reasons") val autoReasons: List<String>?,
    @SerialName("charged") val charged: Boolean?,
    @SerialName("note") val note: String?,
    @SerialName("status") val status: String?,
    @SerialName("created_at") val createdAt: Double?,
    @SerialName("answered_at") val answeredAt: Double?,
    @SerialName("reply_text") val replyText: String?,
    @SerialName("has_reply_audio") val hasReplyAudio: Boolean?,
    // Teacher's view only.
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("teacher_minutes") val teacherMinutes: Double? = null,
    /** The detail call only: the score summary the app sent with the take. */
    @SerialName("analysis") val analysis: ReviewAnalysisDto? = null
)

/** What the app sends with a take for the teacher to read beside it. */
@Serializable
data class ReviewAnalysisDto(
    @SerialName("overall") val overall: Int?,
    @SerialName("summary") val summary: String?,
    @SerialName("weak_points") val weakPoints: List<String>?
)

@Serializable
data class StudentCardDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("created_at") val createdAt: Double?,
    @SerialName("owned_lessons") val ownedLessons: List<String>?,
    @SerialName("review_credits") val reviewCredits: Int?
)

@Serializable
data class StudentsDto(
    @SerialName("students") val students: List<StudentCardDto>?
)

@Serializable
data class TeacherReportDto(
    @SerialName("days") val days: Int?,
    @SerialName("gross_toman") val grossToman: Long?,
    @SerialName("net_toman") val netToman: Long?,
    @SerialName("paying_students") val payingStudents: Int?,
    @SerialName("teacher_minutes") val teacherMinutes: Double?,
    @SerialName("minutes_per_paying_student") val minutesPerPayingStudent: Double?,
    @SerialName("reviews_answered") val reviewsAnswered: Int?,
    @SerialName("average_review_minutes") val averageReviewMinutes: Double?,
    @SerialName("reviews_waiting") val reviewsWaiting: Int?,
    @SerialName("net_per_hour_toman") val netPerHourToman: Long?,
    @SerialName("private_hourly_rate_toman") val privateHourlyRateToman: Long?,
    @SerialName("worth_it") val worthIt: Boolean?
)
