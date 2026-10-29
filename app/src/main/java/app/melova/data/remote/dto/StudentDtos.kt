package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreatedStudentDto(
    @SerializedName("student_id") val studentId: String?,
    @SerializedName("token") val token: String?
)

/** The student's account as the server has it; returned by every student call. */
data class StudentMeDto(
    @SerializedName("student_id") val studentId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("owned_lessons") val ownedLessons: List<String>?,
    /** Review credits left, by teacher id. */
    @SerializedName("review_credits") val reviewCredits: Map<String, Int>?,
    @SerializedName("review") val review: ReviewDto? = null
)

data class ReviewsDto(
    @SerializedName("reviews") val reviews: List<ReviewDto>?
)

data class ReviewDto(
    @SerializedName("id") val id: String?,
    @SerializedName("lesson_id") val lessonId: String?,
    @SerializedName("section_id") val sectionId: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("auto_reasons") val autoReasons: List<String>?,
    @SerializedName("charged") val charged: Boolean?,
    @SerializedName("note") val note: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: Double?,
    @SerializedName("answered_at") val answeredAt: Double?,
    @SerializedName("reply_text") val replyText: String?,
    @SerializedName("has_reply_audio") val hasReplyAudio: Boolean?,
    // Teacher's view only.
    @SerializedName("student_id") val studentId: String? = null,
    @SerializedName("student_name") val studentName: String? = null,
    @SerializedName("teacher_minutes") val teacherMinutes: Double? = null,
    /** The detail call only: the score summary the app sent with the take. */
    @SerializedName("analysis") val analysis: ReviewAnalysisDto? = null
)

/** What the app sends with a take for the teacher to read beside it. */
data class ReviewAnalysisDto(
    @SerializedName("overall") val overall: Int?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("weak_points") val weakPoints: List<String>?
)

data class StudentCardDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("created_at") val createdAt: Double?,
    @SerializedName("owned_lessons") val ownedLessons: List<String>?,
    @SerializedName("review_credits") val reviewCredits: Int?
)

data class StudentsDto(
    @SerializedName("students") val students: List<StudentCardDto>?
)

data class TeacherReportDto(
    @SerializedName("days") val days: Int?,
    @SerializedName("gross_toman") val grossToman: Long?,
    @SerializedName("net_toman") val netToman: Long?,
    @SerializedName("paying_students") val payingStudents: Int?,
    @SerializedName("teacher_minutes") val teacherMinutes: Double?,
    @SerializedName("minutes_per_paying_student") val minutesPerPayingStudent: Double?,
    @SerializedName("reviews_answered") val reviewsAnswered: Int?,
    @SerializedName("average_review_minutes") val averageReviewMinutes: Double?,
    @SerializedName("reviews_waiting") val reviewsWaiting: Int?,
    @SerializedName("net_per_hour_toman") val netPerHourToman: Long?,
    @SerializedName("private_hourly_rate_toman") val privateHourlyRateToman: Long?,
    @SerializedName("worth_it") val worthIt: Boolean?
)
