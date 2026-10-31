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
    @SerialName("signed_in") val signedIn: Boolean? = null,
    /** The phone or email they signed in with. */
    @SerialName("signed_in_as") val signedInAs: String? = null,
    /** Changes with the photo; null when there is none. */
    @SerialName("photo_version") val photoVersion: Long? = null,
    @SerialName("is_teacher") val isTeacher: Boolean? = null,
    @SerialName("owned_lessons") val ownedLessons: List<String>?,
    /** After sending a take: the chat it went into. */
    @SerialName("chat_id") val chatId: String? = null,
    /** Sign-in calls only: the token that replaces this install's old one. */
    @SerialName("token") val token: String? = null,
    /** Sign-in calls only: this sign-in made the account, so it has no name yet. */
    @SerialName("is_new_account") val isNewAccount: Boolean? = null
)

/** Which ways of signing in this server has turned on. */
@Serializable
data class LoginMethodsDto(
    @SerialName("phone") val phone: Boolean?,
    @SerialName("email") val email: Boolean?,
    @SerialName("google_client_id") val googleClientId: String?,
    @SerialName("mock") val mock: Boolean?
)

@Serializable
data class LoginCodeSentDto(
    /** The address as the student should see it. */
    @SerialName("sent_to") val sentTo: String?,
    @SerialName("resend_after") val resendAfter: Int?
)

@Serializable
data class StudentCardDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("created_at") val createdAt: Double?,
    @SerialName("owned_lessons") val ownedLessons: List<String>?
)

@Serializable
data class StudentsDto(
    @SerialName("students") val students: List<StudentCardDto>?
)

@Serializable
data class LessonRequestDto(
    @SerialName("id") val id: String?,
    @SerialName("lesson_id") val lessonId: String?,
    @SerialName("price_toman") val priceToman: Long?,
    @SerialName("payer") val payer: String?,
    @SerialName("student_name") val studentName: String?,
    @SerialName("created_at") val createdAt: Double?
)

@Serializable
data class FollowsDto(
    @SerialName("followed_teachers") val followedTeachers: List<String>?
)

@Serializable
data class LessonRequestsDto(
    @SerialName("requests") val requests: List<LessonRequestDto>?
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
