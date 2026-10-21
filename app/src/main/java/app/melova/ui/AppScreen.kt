package app.melova.ui

import app.melova.presentation.LessonIntroViewModel
import app.melova.presentation.VocalTrainingViewModel

/** Navigation routes; argument names match the keys the ViewModels read from SavedStateHandle. */
object AppScreen {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SOLFEGE = "solfege"
    const val LESSON = "lesson/{${LessonIntroViewModel.ARG_LESSON_ID}}"
    const val PRACTICE =
        "practice/{${VocalTrainingViewModel.ARG_LESSON_ID}}/{${VocalTrainingViewModel.ARG_SECTION_ID}}"

    fun lesson(lessonId: String) = "lesson/${android.net.Uri.encode(lessonId)}"

    fun practice(lessonId: String, sectionId: String) =
        "practice/${android.net.Uri.encode(lessonId)}/${android.net.Uri.encode(sectionId)}"
}
