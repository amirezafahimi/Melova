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
    /** After a checkout or a sent take: the lesson's offer as it stands now. */
    @SerialName("lesson") val lesson: LessonOfferDto? = null,
    /** After a checkout. */
    @SerialName("order") val order: OrderDto? = null,
    /** Sign-in calls only: the token that replaces this install's old one. */
    @SerialName("token") val token: String? = null
)

@Serializable
data class OrderDto(
    @SerialName("id") val id: String?,
    /** "paid", or "pending" while the provider waits for the student on [redirectUrl]. */
    @SerialName("status") val status: String?,
    @SerialName("redirect_url") val redirectUrl: String? = null
)

/** Which ways of signing in this server has turned on. */
@Serializable
data class LoginMethodsDto(
    @SerialName("phone") val phone: Boolean?,
    @SerialName("email") val email: Boolean?,
    @SerialName("google_client_id") val googleClientId: String?,
    @SerialName("mock") val mock: Boolean?,
    /** Which of "phone" and "email" sign in with no code. */
    @SerialName("mock_kinds") val mockKinds: List<String>? = null
)

@Serializable
data class LoginCodeSentDto(
    /** The address as the student should see it. */
    @SerialName("sent_to") val sentTo: String?,
    @SerialName("resend_after") val resendAfter: Int?
)

@Serializable
data class FollowsDto(
    @SerialName("followed_teachers") val followedTeachers: List<String>?
)

@Serializable
data class TeacherReportDto(
    @SerialName("days") val days: Int?,
    @SerialName("gross_cents") val grossCents: Long?,
    @SerialName("net_cents") val netCents: Long?,
    @SerialName("sales_count") val salesCount: Int? = null,
    @SerialName("paying_students") val payingStudents: Int?,
    @SerialName("teacher_minutes") val teacherMinutes: Double?,
    @SerialName("minutes_per_paying_student") val minutesPerPayingStudent: Double?,
    @SerialName("reviews_answered") val reviewsAnswered: Int?,
    @SerialName("average_review_minutes") val averageReviewMinutes: Double?,
    @SerialName("reviews_waiting") val reviewsWaiting: Int?,
    @SerialName("net_per_hour_cents") val netPerHourCents: Long?,
    @SerialName("private_hourly_rate_cents") val privateHourlyRateCents: Long?,
    @SerialName("worth_it") val worthIt: Boolean?
)

@Serializable
data class EarningsTotalDto(
    @SerialName("count") val count: Int? = null,
    @SerialName("gross_cents") val grossCents: Long? = null,
    @SerialName("teacher_cents") val teacherCents: Long? = null,
    @SerialName("melova_cents") val melovaCents: Long? = null
)

@Serializable
data class TeacherSaleDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("lesson_id") val lessonId: String? = null,
    @SerialName("tier") val tier: String? = null,
    @SerialName("upgrade") val upgrade: Boolean? = null,
    @SerialName("gross_cents") val grossCents: Long? = null,
    @SerialName("teacher_cents") val teacherCents: Long? = null,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("created_at") val createdAt: Double? = null
)

@Serializable
data class TeacherEarningsDto(
    @SerialName("cut_percent") val cutPercent: Int? = null,
    @SerialName("total") val total: EarningsTotalDto? = null,
    @SerialName("this_month") val thisMonth: EarningsTotalDto? = null,
    @SerialName("sales") val sales: List<TeacherSaleDto>? = null
)

/** A section's best score; the lesson and home pages read these. */
@Serializable
data class SectionBestDto(
    @SerialName("lesson_id") val lessonId: String?,
    @SerialName("section_id") val sectionId: String?,
    @SerialName("score") val score: Int? = null
)

/** The latest counted take, so the app can offer to go on from there. */
@Serializable
data class LastTakeDto(
    @SerialName("lesson_id") val lessonId: String?,
    @SerialName("section_id") val sectionId: String?,
    @SerialName("score") val score: Int? = null
)

@Serializable
data class PracticeProgressDto(
    @SerialName("best_scores") val bestScores: List<SectionBestDto>? = null,
    @SerialName("last") val last: LastTakeDto? = null,
    /** Sections where the teacher said «آفرین» to a take; the score field is unused. */
    @SerialName("stamped") val stamped: List<SectionBestDto>? = null
)

/** This week's practice days, Saturday first, against the weekly goal. */
@Serializable
data class PracticeWeekDto(
    @SerialName("goal") val goal: Int? = null,
    @SerialName("days") val days: List<Boolean>? = null,
    @SerialName("today") val today: Int? = null,
    @SerialName("practised") val practised: Int? = null,
    @SerialName("full_weeks") val fullWeeks: Int? = null,
    @SerialName("reminders") val reminders: Boolean? = null
)

@Serializable
data class PracticeReminderDto(@SerialName("body") val body: String? = null)

@Serializable
data class JournalTakeDto(
    @SerialName("id") val id: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("created_at") val createdAt: Double? = null
)

@Serializable
data class JournalSectionDto(
    @SerialName("lesson_id") val lessonId: String?,
    @SerialName("section_id") val sectionId: String?,
    @SerialName("lesson_title") val lessonTitle: String? = null,
    @SerialName("section_title") val sectionTitle: String? = null,
    @SerialName("first") val first: JournalTakeDto? = null,
    @SerialName("best") val best: JournalTakeDto? = null,
    @SerialName("takes") val takes: Int? = null,
    @SerialName("last_at") val lastAt: Double? = null,
    @SerialName("stamped") val stamped: Boolean? = null
)

@Serializable
data class PracticeJournalDto(
    @SerialName("sections") val sections: List<JournalSectionDto>? = null
)

/** An earlier take of a section the practice page lists; [roles] says why it is kept. */
@Serializable
data class PracticeTakeDto(
    @SerialName("id") val id: String?,
    @SerialName("score") val score: Int? = null,
    @SerialName("created_at") val createdAt: Double? = null,
    @SerialName("roles") val roles: List<String>? = null
)

@Serializable
data class PracticeTakesDto(
    @SerialName("takes") val takes: List<PracticeTakeDto>?
)

/** One of those takes, opened: the result the student saw when it was scored. */
@Serializable
data class PracticeTakeDetailDto(
    @SerialName("id") val id: String?,
    @SerialName("created_at") val createdAt: Double? = null,
    @SerialName("result") val result: ScoringResponseDto? = null
)
