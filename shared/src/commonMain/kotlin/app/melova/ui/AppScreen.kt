package app.melova.ui

import io.ktor.http.encodeURLPathPart
import app.melova.presentation.LessonIntroViewModel
import app.melova.presentation.VocalTrainingViewModel
import app.melova.ui.components.MainTab

/** Navigation routes; argument names match the keys the ViewModels read from SavedStateHandle. */
object AppScreen {
    const val SPLASH = "splash"

    // Bottom-bar tabs.
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val PROFILE = "profile"

    const val ARG_LOGIN_REASON = "reason"
    const val LOGIN = "login/{$ARG_LOGIN_REASON}"
    const val EDIT_PROFILE = "profile/edit"
    const val FOLLOWED_TEACHERS = "profile/teachers"

    const val SOLFEGE = "solfege"
    const val LESSON = "lesson/{${LessonIntroViewModel.ARG_LESSON_ID}}"
    const val PRACTICE =
        "practice/{${VocalTrainingViewModel.ARG_LESSON_ID}}/{${VocalTrainingViewModel.ARG_SECTION_ID}}"
    /** A take sent to a teacher, opened from its chat message: the same page, read-only. */
    const val PRACTICE_RESULT =
        "practice/{${VocalTrainingViewModel.ARG_LESSON_ID}}/{${VocalTrainingViewModel.ARG_SECTION_ID}}" +
            "/result/{${VocalTrainingViewModel.ARG_REVIEW_ID}}"
    /** The teacher's performance in detail: waveform with tahrir/vibrato spans. */
    const val TEACHER_PERFORMANCE =
        "practice/{${VocalTrainingViewModel.ARG_LESSON_ID}}/{${VocalTrainingViewModel.ARG_SECTION_ID}}/teacher"

    const val ARG_TEACHER_ID = "teacherId"
    const val ARG_LESSON_ID = "lessonId"
    /** A teacher's public page, as students see it. */
    const val TEACHER_PAGE = "teachers/{$ARG_TEACHER_ID}"
    const val TEACHER_PANEL = "teacher"
    const val LESSON_EDITOR = "teacher/lesson/{$ARG_LESSON_ID}"
    const val TEACHER_STUDENTS = "teacher/students"
    const val TEACHER_REPORT = "teacher/report"

    const val ARG_CHAT_ID = "chatId"
    const val CHATS = "chats"
    const val CHAT = "chats/{$ARG_CHAT_ID}"

    fun tabRoute(tab: MainTab) = when (tab) {
        MainTab.EXPLORE -> EXPLORE
        MainTab.PRACTICE -> HOME
        MainTab.PROFILE -> PROFILE
    }

    fun tabFor(route: String?): MainTab? = when (route) {
        EXPLORE -> MainTab.EXPLORE
        HOME -> MainTab.PRACTICE
        PROFILE -> MainTab.PROFILE
        else -> null
    }

    fun login(reason: LoginReason) = "login/${reason.name}"

    fun lesson(lessonId: String) = "lesson/${lessonId.encodeURLPathPart()}"

    fun practice(lessonId: String, sectionId: String) =
        "practice/${lessonId.encodeURLPathPart()}/${sectionId.encodeURLPathPart()}"

    fun practiceResult(lessonId: String, sectionId: String, reviewId: String) =
        "practice/${lessonId.encodeURLPathPart()}/${sectionId.encodeURLPathPart()}/result/${reviewId.encodeURLPathPart()}"

    fun teacherPerformance(lessonId: String, sectionId: String) =
        "practice/${lessonId.encodeURLPathPart()}/${sectionId.encodeURLPathPart()}/teacher"

    fun teacherPage(teacherId: String) = "teachers/${teacherId.encodeURLPathPart()}"

    fun lessonEditor(lessonId: String) = "teacher/lesson/${lessonId.encodeURLPathPart()}"

    fun chat(chatId: String) = "chats/${chatId.encodeURLPathPart()}"
}
