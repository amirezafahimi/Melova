package app.melova.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.melova.presentation.CourseViewModel
import app.melova.presentation.LessonIntroViewModel
import app.melova.presentation.TeacherViewModel
import app.melova.presentation.VocalTrainingViewModel
import app.melova.ui.components.MainTab
import app.melova.ui.components.MelovaBottomBar
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.ThemeMode

@Composable
fun MelovaApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    // Activity-scoped: shared by the tabs, lesson screens and the teacher panel.
    val courseViewModel: CourseViewModel = viewModel()
    val teacherViewModel: TeacherViewModel = viewModel()
    val courseState by courseViewModel.uiState.collectAsStateWithLifecycle()
    val teacherState by teacherViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Teacher edits, sign-in and sign-out change which courses the student side sees.
    LaunchedEffect(teacherState.revision) {
        if (teacherState.revision > 0) courseViewModel.refresh(silent = true)
    }
    LaunchedEffect(teacherState.message) {
        teacherState.message?.let {
            snackbarHostState.showSnackbar(it)
            teacherViewModel.consumeMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppScreen.tabFor(backStackEntry?.destination?.route)

    Box(modifier = Modifier.fillMaxSize()) {
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

            composable(AppScreen.HOME, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                HomeScreen(
                    state = courseState,
                    onRefresh = { courseViewModel.refresh() },
                    onAppear = {
                        courseViewModel.refresh(silent = true)
                        courseViewModel.refreshProgress()
                    },
                    onLessonClick = { navController.navigate(AppScreen.lesson(it)) },
                    onSolfegeClick = { navController.navigate(AppScreen.SOLFEGE) },
                    onFollowChange = courseViewModel::setFollowed
                )
            }

            composable(AppScreen.EXPLORE, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                ExploreScreen()
            }

            composable(AppScreen.PROFILE, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                ProfileScreen(
                    courseState = courseState,
                    teacherState = teacherState,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onFollowChange = courseViewModel::setFollowed,
                    onTeacherSignIn = teacherViewModel::signIn,
                    onTeacherSignOut = teacherViewModel::signOut,
                    onOpenTeacherPanel = { navController.navigate(AppScreen.TEACHER_PANEL) }
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
                    onRetry = { courseViewModel.refresh() },
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

            composable(AppScreen.TEACHER_PANEL) {
                TeacherPanelScreen(
                    state = teacherState,
                    viewModel = teacherViewModel,
                    followedIds = courseState.followedIds,
                    onFollowChange = courseViewModel::setFollowed,
                    onOpenCourse = { navController.navigate(AppScreen.courseEditor(it)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.COURSE_EDITOR,
                arguments = listOf(navArgument(AppScreen.ARG_COURSE_ID) { type = NavType.StringType })
            ) { entry ->
                val courseId = entry.arguments?.getString(AppScreen.ARG_COURSE_ID).orEmpty()
                CourseEditorScreen(
                    courseId = courseId,
                    state = teacherState,
                    viewModel = teacherViewModel,
                    isFollowed = courseState.isFollowed(courseId),
                    onFollowChange = { courseViewModel.setFollowed(courseId, it) },
                    onOpenLesson = { navController.navigate(AppScreen.lessonEditor(it)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.LESSON_EDITOR,
                arguments = listOf(navArgument(AppScreen.ARG_LESSON_ID) { type = NavType.StringType })
            ) { entry ->
                val lessonId = entry.arguments?.getString(AppScreen.ARG_LESSON_ID).orEmpty()
                LessonEditorScreen(
                    lessonId = lessonId,
                    state = teacherState,
                    viewModel = teacherViewModel,
                    onPreview = { navController.navigate(AppScreen.lesson(lessonId)) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        AnimatedVisibility(
            visible = currentTab != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MelovaBottomBar(
                selected = currentTab ?: MainTab.PRACTICE,
                onSelect = { tab ->
                    if (tab == currentTab) return@MelovaBottomBar
                    navController.navigate(AppScreen.tabRoute(tab)) {
                        // Home is always the bottom of the stack (the splash removes itself).
                        popUpTo(AppScreen.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (currentTab != null) 110.dp else 16.dp)
        ) { data ->
            RtlContent {
                Snackbar(containerColor = MelovaCardElevated, contentColor = MelovaText) {
                    Text(data.visuals.message)
                }
            }
        }
    }
}
