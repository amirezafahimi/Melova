package app.melova.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.melova.presentation.CourseViewModel
import app.melova.presentation.LessonIntroViewModel
import app.melova.presentation.VocalTrainingViewModel
import app.melova.ui.theme.ThemeMode

@Composable
fun MelovaApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    // Activity-scoped: the catalog and progress are shared by the home and lesson screens.
    val courseViewModel: CourseViewModel = viewModel()
    val courseState by courseViewModel.uiState.collectAsStateWithLifecycle()

    // The UI is RTL, so "forward" slides in from the left.
    NavHost(
        navController = navController,
        startDestination = AppScreen.SPLASH,
        enterTransition = { slideInHorizontally { -it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() }
    ) {
        composable(
            AppScreen.SPLASH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SplashScreen(
                onFinished = {
                    navController.navigate(AppScreen.HOME) {
                        popUpTo(AppScreen.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AppScreen.HOME) {
            HomeScreen(
                state = courseState,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onRefresh = courseViewModel::refresh,
                onLessonClick = { navController.navigate(AppScreen.lesson(it)) },
                onSolfegeClick = { navController.navigate(AppScreen.SOLFEGE) }
            )
        }

        composable(
            AppScreen.LESSON,
            arguments = listOf(navArgument(LessonIntroViewModel.ARG_LESSON_ID) { type = NavType.StringType })
        ) { entry ->
            val lessonId = entry.arguments?.getString(LessonIntroViewModel.ARG_LESSON_ID).orEmpty()
            LessonScreen(
                lessonId = lessonId,
                courseState = courseState,
                onBack = { navController.popBackStack() },
                onRetry = courseViewModel::refresh,
                onSectionClick = { sectionId -> navController.navigate(AppScreen.practice(lessonId, sectionId)) },
                onResume = courseViewModel::refreshProgress
            )
        }

        composable(
            AppScreen.PRACTICE,
            arguments = listOf(
                navArgument(VocalTrainingViewModel.ARG_LESSON_ID) { type = NavType.StringType },
                navArgument(VocalTrainingViewModel.ARG_SECTION_ID) { type = NavType.StringType }
            )
        ) {
            VocalTrainingScreen(onBack = { navController.popBackStack() })
        }

        composable(AppScreen.SOLFEGE) {
            MelovaScreen(onBack = { navController.popBackStack() })
        }
    }
}
