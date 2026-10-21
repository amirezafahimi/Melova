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
import app.melova.presentation.CatalogViewModel
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
    val catalogViewModel: CatalogViewModel = viewModel()
    val teacherViewModel: TeacherViewModel = viewModel()
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val teacherState by teacherViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Teacher edits, sign-in and sign-out change which lessons the student side sees.
    LaunchedEffect(teacherState.revision) {
        if (teacherState.revision > 0) catalogViewModel.refresh(silent = true)
    }
    LaunchedEffect(teacherState.message) {
        teacherState.message?.let {
            snackbarHostState.showSnackbar(it)
            teacherViewModel.consumeMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppScreen.tabFor(backStackEntry?.destination?.route)
    val selectTab: (MainTab) -> Unit = { tab ->
        navController.navigate(AppScreen.tabRoute(tab)) {
            // Home is always the bottom of the stack (the splash removes itself).
            popUpTo(AppScreen.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                    state = catalogState,
                    onRefresh = { catalogViewModel.refresh() },
                    onAppear = {
                        catalogViewModel.refresh(silent = true)
                        catalogViewModel.refreshProgress()
                    },
                    onLessonClick = { navController.navigate(AppScreen.lesson(it)) },
                    onTeacherClick = { navController.navigate(AppScreen.teacherPage(it)) },
                    onSolfegeClick = { navController.navigate(AppScreen.SOLFEGE) },
                    onFollowChange = catalogViewModel::setFollowed,
                    onExploreClick = { selectTab(MainTab.EXPLORE) }
                )
            }

            composable(AppScreen.EXPLORE, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                ExploreScreen(
                    state = catalogState,
                    onTeacherClick = { navController.navigate(AppScreen.teacherPage(it)) },
                    onFollowChange = catalogViewModel::setFollowed,
                    onRetry = { catalogViewModel.refresh() }
                )
            }

            composable(AppScreen.PROFILE, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                ProfileScreen(
                    catalogState = catalogState,
                    teacherState = teacherState,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onFollowChange = catalogViewModel::setFollowed,
                    onTeacherClick = { navController.navigate(AppScreen.teacherPage(it)) },
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
                    catalogState = catalogState,
                    onBack = { navController.popBackStack() },
                    onRetry = { catalogViewModel.refresh() },
                    onSectionClick = { sectionId -> navController.navigate(AppScreen.practice(lessonId, sectionId)) },
                    onResume = catalogViewModel::refreshProgress
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

            composable(
                AppScreen.TEACHER_PAGE,
                arguments = listOf(navArgument(AppScreen.ARG_TEACHER_ID) { type = NavType.StringType })
            ) { entry ->
                val teacherId = entry.arguments?.getString(AppScreen.ARG_TEACHER_ID).orEmpty()
                TeacherPageScreen(
                    teacherId = teacherId,
                    state = catalogState,
                    onFollowChange = { catalogViewModel.setFollowed(teacherId, it) },
                    onLessonClick = { navController.navigate(AppScreen.lesson(it)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.TEACHER_PANEL) {
                val ownId = teacherState.profile?.id
                TeacherPanelScreen(
                    state = teacherState,
                    viewModel = teacherViewModel,
                    isFollowingSelf = ownId != null && catalogState.isFollowed(ownId),
                    onFollowSelfChange = { followed -> ownId?.let { catalogViewModel.setFollowed(it, followed) } },
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
                onSelect = { tab -> if (tab != currentTab) selectTab(tab) }
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
