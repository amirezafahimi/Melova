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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import app.melova.platform.BrowserBackButton
import app.melova.platform.ChatLinks
import app.melova.platform.rememberNotificationPermission
import app.melova.presentation.CatalogViewModel
import app.melova.presentation.ChatViewModel
import app.melova.presentation.LessonIntroViewModel
import app.melova.presentation.StudentViewModel
import app.melova.presentation.TeacherDeskViewModel
import app.melova.presentation.TeacherViewModel
import app.melova.presentation.TeacherPerformanceViewModel
import app.melova.presentation.VocalTrainingViewModel
import app.melova.ui.components.MainTab
import app.melova.ui.components.MelovaBottomBar
import app.melova.ui.layout.PageBottomGap
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.ThemeMode

private const val CHAT_LIST_REFRESH_MS = 20_000L

@Composable
fun MelovaApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    // Activity-scoped: shared by the tabs, lesson screens and the teacher panel.
    val catalogViewModel: CatalogViewModel = viewModel { CatalogViewModel() }
    val teacherViewModel: TeacherViewModel = viewModel { TeacherViewModel() }
    val studentViewModel: StudentViewModel = viewModel { StudentViewModel() }
    val teacherDeskViewModel: TeacherDeskViewModel = viewModel { TeacherDeskViewModel() }
    val chatViewModel: ChatViewModel = viewModel { ChatViewModel() }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val deskState by teacherDeskViewModel.uiState.collectAsStateWithLifecycle()
    val studentState by studentViewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val teacherState by teacherViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val canChat = (studentState.account != null && !studentState.needsLogin) || teacherState.isSignedIn

    // Teacher edits, sign-in and sign-out change which lessons the student side sees.
    LaunchedEffect(teacherState.revision) {
        if (teacherState.revision > 0) catalogViewModel.refresh(silent = true)
    }
    // A new student account or a sign-in: reload so the catalog says which lessons are theirs.
    LaunchedEffect(studentState.accountRevision) {
        if (studentState.accountRevision > 0) catalogViewModel.refresh(silent = true)
    }
    LaunchedEffect(studentState.account?.id, studentState.account?.signedIn) {
        catalogViewModel.onAccountChanged(studentState.account?.signedIn == true)
    }
    // A teacher is an ordinary account, so the panel opens and closes with it.
    LaunchedEffect(studentState.account?.id, studentState.account?.isTeacher) {
        teacherViewModel.onAccountChanged()
        teacherDeskViewModel.onAccountChanged()
    }
    LaunchedEffect(studentState.account?.id, canChat) {
        chatViewModel.clear()
        if (canChat) chatViewModel.syncNotifications()
    }
    if (canChat) StayConnected(studentState.account?.id to teacherState.isSignedIn, chatViewModel)
    LaunchedEffect(chatState.message) {
        chatState.message?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.consumeMessage()
        }
    }
    // A payment provider that finishes on its own page; the lesson opens when it reports back.
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(studentState.paymentUrl) {
        studentState.paymentUrl?.let { url ->
            runCatching { uriHandler.openUri(url) }
            studentViewModel.consumePaymentUrl()
        }
    }
    LaunchedEffect(studentState.message) {
        studentState.message?.let {
            snackbarHostState.showSnackbar(it)
            studentViewModel.consumeMessage()
        }
    }
    LaunchedEffect(catalogState.message) {
        catalogState.message?.let {
            snackbarHostState.showSnackbar(it)
            catalogViewModel.consumeMessage()
        }
    }
    LaunchedEffect(deskState.message) {
        deskState.message?.let {
            snackbarHostState.showSnackbar(it)
            teacherDeskViewModel.consumeMessage()
        }
    }
    LaunchedEffect(teacherState.message) {
        teacherState.message?.let {
            snackbarHostState.showSnackbar(it)
            teacherViewModel.consumeMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppScreen.tabFor(backStackEntry?.destination?.route)

    // Waits for the splash, which would otherwise land on Home over the chat.
    val tappedChat by ChatLinks.opening.collectAsStateWithLifecycle()
    val isPastSplash = backStackEntry != null && backStackEntry?.destination?.route != AppScreen.SPLASH
    val latestCanChat by rememberUpdatedState(canChat)
    val askNotifications = rememberNotificationPermission { granted ->
        if (granted && latestCanChat) chatViewModel.syncNotifications()
    }
    LaunchedEffect(isPastSplash) {
        if (isPastSplash) chatViewModel.askAtStart(askNotifications)
    }
    LaunchedEffect(tappedChat, canChat, isPastSplash) {
        val chatId = tappedChat ?: return@LaunchedEffect
        if (canChat && isPastSplash) {
            navController.navigate(AppScreen.chat(chatId))
            ChatLinks.opening.value = null
        }
    }
    BrowserBackButton(navController)
    val selectTab: (MainTab) -> Unit = { tab ->
        navController.navigate(AppScreen.tabRoute(tab)) {
            // Home is always the bottom of the stack (the splash removes itself).
            popUpTo(AppScreen.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val hazeState = rememberHazeState()
    Box(modifier = Modifier.fillMaxSize()) {
        // The UI is RTL, so "forward" slides in from the left.
        NavHost(
            navController = navController,
            modifier = Modifier.hazeSource(hazeState),
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
                    onSectionClick = { lessonId, sectionId ->
                        // Through the lesson page, so back from the practice lands there.
                        navController.navigate(AppScreen.lesson(lessonId))
                        navController.navigate(AppScreen.practice(lessonId, sectionId))
                    },
                    onSolfegeClick = { navController.navigate(AppScreen.SOLFEGE) },
                    onExploreClick = { selectTab(MainTab.EXPLORE) },
                    unreadChats = chatState.unread,
                    onChatsClick = if (canChat) {
                        { navController.navigate(AppScreen.CHATS) }
                    } else {
                        null
                    }
                )
            }

            composable(AppScreen.EXPLORE, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                ExploreScreen(
                    state = catalogState,
                    onLessonClick = { navController.navigate(AppScreen.lesson(it)) },
                    onTeacherClick = { navController.navigate(AppScreen.teacherPage(it)) },
                    onFollowChange = catalogViewModel::setFollowed,
                    onRetry = { catalogViewModel.refresh() }
                )
            }

            composable(AppScreen.PROFILE, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                // Waits on needsLogin as the server's answer may arrive after the tap.
                LaunchedEffect(studentState.needsLogin) {
                    if (studentState.needsLogin && studentViewModel.takeLoginOffer()) {
                        navController.navigate(AppScreen.login(LoginReason.PROFILE))
                    }
                }
                ProfileScreen(
                    catalogState = catalogState,
                    teacherState = teacherState,
                    onTeacherSignIn = { code ->
                        if (studentState.account?.signedIn == true) {
                            studentViewModel.sayITeach(code)
                        } else {
                            teacherViewModel.signIn(code)
                        }
                    },
                    onOpenTeacherPanel = { navController.navigate(AppScreen.TEACHER_PANEL) },
                    studentState = studentState,
                    onLogin = { navController.navigate(AppScreen.login(LoginReason.PROFILE)) },
                    onEditProfile = { navController.navigate(AppScreen.EDIT_PROFILE) },
                    onOpenFollowedTeachers = { navController.navigate(AppScreen.FOLLOWED_TEACHERS) },
                    onOpenSettings = { navController.navigate(AppScreen.SETTINGS) },
                    onOpenFaq = { navController.navigate(AppScreen.FAQ) },
                    canChat = canChat,
                    unreadChats = chatState.unread,
                    onOpenChats = { navController.navigate(AppScreen.CHATS) },
                    onStudentAppear = studentViewModel::refresh
                )
            }

            composable(AppScreen.SETTINGS) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    canChat = canChat,
                    chatState = chatState,
                    chatViewModel = chatViewModel,
                    isSignedIn = studentState.account?.signedIn == true,
                    onSignOut = {
                        chatViewModel.forgetThisDevice()
                        studentViewModel.signOut()
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.FAQ) {
                FaqScreen(onBack = { navController.popBackStack() })
            }

            composable(AppScreen.FOLLOWED_TEACHERS) {
                FollowedTeachersScreen(
                    state = catalogState,
                    onTeacherClick = { navController.navigate(AppScreen.teacherPage(it)) },
                    onFollowChange = catalogViewModel::setFollowed,
                    onExploreClick = { selectTab(MainTab.EXPLORE) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.EDIT_PROFILE) {
                EditProfileScreen(
                    state = studentState,
                    onSaveName = studentViewModel::setName,
                    onPhotoPicked = studentViewModel::setPhoto,
                    onRemovePhoto = studentViewModel::removePhoto,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.LOGIN,
                arguments = listOf(navArgument(AppScreen.ARG_LOGIN_REASON) { type = NavType.StringType })
            ) { entry ->
                val reason = entry.arguments?.read { getStringOrNull(AppScreen.ARG_LOGIN_REASON) }
                LoginScreen(
                    reason = LoginReason.entries.firstOrNull { it.name == reason } ?: LoginReason.PROFILE,
                    methods = studentState.loginMethods,
                    onBack = { navController.popBackStack() },
                    onSignedIn = { account ->
                        studentViewModel.onSignedIn(account)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                AppScreen.LESSON,
                arguments = listOf(navArgument(LessonIntroViewModel.ARG_LESSON_ID) { type = NavType.StringType })
            ) { entry ->
                val lessonId = entry.arguments?.read { getStringOrNull(LessonIntroViewModel.ARG_LESSON_ID) }.orEmpty()
                LessonScreen(
                    lessonId = lessonId,
                    catalogState = catalogState,
                    onBack = { navController.popBackStack() },
                    onRetry = { catalogViewModel.refresh() },
                    onSectionClick = { sectionId -> navController.navigate(AppScreen.practice(lessonId, sectionId)) },
                    onResume = catalogViewModel::refreshProgress,
                    isOwnTeacher = teacherState.isSignedIn &&
                        teacherState.profile?.id == catalogState.lesson(lessonId)?.teacherId,
                    loginGate = studentState.loginGate,
                    isChangingLessons = studentState.isChangingLessons,
                    onLogin = { navController.navigate(AppScreen.login(LoginReason.PURCHASE)) },
                    onLoginToAdd = { navController.navigate(AppScreen.login(LoginReason.ADD_LESSON)) },
                    onBuy = { tier -> studentViewModel.buyLesson(lessonId, tier) },
                    onAddFree = { studentViewModel.addLesson(lessonId) },
                    onTeacherClick = { navController.navigate(AppScreen.teacherPage(it)) }
                )
            }

            composable(
                AppScreen.PRACTICE,
                arguments = listOf(
                    navArgument(VocalTrainingViewModel.ARG_LESSON_ID) { type = NavType.StringType },
                    navArgument(VocalTrainingViewModel.ARG_SECTION_ID) { type = NavType.StringType }
                )
            ) { entry ->
                val lessonId = entry.arguments?.read { getStringOrNull(VocalTrainingViewModel.ARG_LESSON_ID) }.orEmpty()
                val sectionId = entry.arguments?.read { getStringOrNull(VocalTrainingViewModel.ARG_SECTION_ID) }.orEmpty()
                val nextSection = catalogState.lesson(lessonId)?.let { catalogState.sectionAfter(it, sectionId) }
                VocalTrainingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTeacherDetails = {
                        navController.navigate(AppScreen.teacherPerformance(lessonId, sectionId))
                    },
                    nextSectionTitle = nextSection?.title,
                    onNextSection = nextSection?.let { next ->
                        {
                            // Replaces this practice, so back still goes to the lesson page.
                            navController.navigate(AppScreen.practice(lessonId, next.id)) {
                                popUpTo(AppScreen.PRACTICE) { inclusive = true }
                            }
                        }
                    },
                    loginGate = studentState.loginGate,
                    onLogin = { navController.navigate(AppScreen.login(LoginReason.TEACHER)) },
                    onOpenChat = { navController.navigate(AppScreen.chat(it)) },
                    onOpenPastTake = { navController.navigate(AppScreen.practicePastTake(lessonId, sectionId, it)) },
                    onSeeVersions = { navController.navigate(AppScreen.lesson(lessonId)) },
                    accountRevision = studentState.accountRevision
                )
            }

            composable(
                AppScreen.PRACTICE_PAST_TAKE,
                arguments = listOf(
                    navArgument(VocalTrainingViewModel.ARG_LESSON_ID) { type = NavType.StringType },
                    navArgument(VocalTrainingViewModel.ARG_SECTION_ID) { type = NavType.StringType },
                    navArgument(VocalTrainingViewModel.ARG_TAKE_ID) { type = NavType.StringType }
                )
            ) { entry ->
                val lessonId = entry.arguments?.read { getStringOrNull(VocalTrainingViewModel.ARG_LESSON_ID) }.orEmpty()
                val sectionId = entry.arguments?.read { getStringOrNull(VocalTrainingViewModel.ARG_SECTION_ID) }.orEmpty()
                VocalTrainingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTeacherDetails = {
                        navController.navigate(AppScreen.teacherPerformance(lessonId, sectionId))
                    }
                )
            }

            composable(
                AppScreen.PRACTICE_RESULT,
                arguments = listOf(
                    navArgument(VocalTrainingViewModel.ARG_LESSON_ID) { type = NavType.StringType },
                    navArgument(VocalTrainingViewModel.ARG_SECTION_ID) { type = NavType.StringType },
                    navArgument(VocalTrainingViewModel.ARG_REVIEW_ID) { type = NavType.StringType }
                )
            ) { entry ->
                val lessonId = entry.arguments?.read { getStringOrNull(VocalTrainingViewModel.ARG_LESSON_ID) }.orEmpty()
                val sectionId = entry.arguments?.read { getStringOrNull(VocalTrainingViewModel.ARG_SECTION_ID) }.orEmpty()
                VocalTrainingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTeacherDetails = {
                        navController.navigate(AppScreen.teacherPerformance(lessonId, sectionId))
                    }
                )
            }

            composable(AppScreen.CHATS) {
                ChatListScreen(
                    state = chatState,
                    onAppear = chatViewModel::refreshChats,
                    onChatClick = { navController.navigate(AppScreen.chat(it)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.CHAT,
                arguments = listOf(navArgument(AppScreen.ARG_CHAT_ID) { type = NavType.StringType })
            ) { entry ->
                // Messages left change as takes are sent and come back, so the banner reads them fresh.
                LaunchedEffect(Unit) { catalogViewModel.refresh(silent = true) }
                ChatScreen(
                    chatId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_CHAT_ID) }.orEmpty(),
                    state = chatState,
                    viewModel = chatViewModel,
                    onOpenPractice = {
                        navController.navigate(AppScreen.practiceResult(it.lessonId, it.sectionId, it.reviewId))
                    },
                    onBack = { navController.popBackStack() },
                    lessonsOf = { teacherId -> catalogState.lessonsOf(teacherId).filter { it.isOwned == true } },
                    onOpenLesson = { navController.navigate(AppScreen.lesson(it)) }
                )
            }

            composable(
                AppScreen.TEACHER_PERFORMANCE,
                arguments = listOf(
                    navArgument(TeacherPerformanceViewModel.ARG_LESSON_ID) { type = NavType.StringType },
                    navArgument(TeacherPerformanceViewModel.ARG_SECTION_ID) { type = NavType.StringType }
                )
            ) {
                TeacherPerformanceScreen(onBack = { navController.popBackStack() })
            }

            composable(AppScreen.SOLFEGE) {
                MelovaScreen(onBack = { navController.popBackStack() })
            }

            composable(
                AppScreen.TEACHER_PAGE,
                arguments = listOf(navArgument(AppScreen.ARG_TEACHER_ID) { type = NavType.StringType })
            ) { entry ->
                val teacherId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_TEACHER_ID) }.orEmpty()
                val messageTeacher = rememberLoginGated(
                    studentState.loginGate,
                    onLogin = { navController.navigate(AppScreen.login(LoginReason.MESSAGE)) },
                    action = { chatViewModel.startChat(teacherId) { navController.navigate(AppScreen.chat(it)) } }
                )
                TeacherPageScreen(
                    teacherId = teacherId,
                    state = catalogState,
                    onFollowChange = { catalogViewModel.setFollowed(teacherId, it) },
                    onLessonClick = { navController.navigate(AppScreen.lesson(it)) },
                    isOpeningChat = chatState.isStartingChat,
                    // A teacher has nothing to say to their own page.
                    onMessage = if (teacherState.profile?.id == teacherId) null else messageTeacher,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.TEACHER_PANEL) {
                LaunchedEffect(Unit) { teacherDeskViewModel.loadEarnings() }
                TeacherPanelScreen(
                    state = teacherState,
                    viewModel = teacherViewModel,
                    earnings = deskState.earnings,
                    onOpenLesson = { navController.navigate(AppScreen.lessonEditor(it)) },
                    onNewLesson = { navController.navigate(AppScreen.NEW_LESSON) },
                    onEditProfile = { navController.navigate(AppScreen.TEACHER_PROFILE) },
                    onBack = { navController.popBackStack() },
                    // An account stays a teacher for good; only a bare teacher code can leave.
                    onSignOut = if (studentState.account?.signedIn == true) null else {
                        {
                            teacherViewModel.signOut()
                            navController.popBackStack()
                        }
                    },
                    unreadChats = chatState.unread,
                    onOpenChats = { navController.navigate(AppScreen.CHATS) },
                    onOpenEarnings = { navController.navigate(AppScreen.TEACHER_EARNINGS) },
                    onOpenReport = { navController.navigate(AppScreen.TEACHER_REPORT) }
                )
            }

            composable(AppScreen.TEACHER_EARNINGS) {
                TeacherEarningsScreen(
                    state = deskState,
                    viewModel = teacherDeskViewModel,
                    lessons = teacherState.profile?.lessons.orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.TEACHER_REPORT) {
                TeacherReportScreen(
                    state = deskState,
                    viewModel = teacherDeskViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.LESSON_EDITOR,
                arguments = listOf(navArgument(AppScreen.ARG_LESSON_ID) { type = NavType.StringType })
            ) { entry ->
                val lessonId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_LESSON_ID) }.orEmpty()
                LessonEditorScreen(
                    lessonId = lessonId,
                    state = teacherState,
                    viewModel = teacherViewModel,
                    onPreview = { navController.navigate(AppScreen.lesson(lessonId)) },
                    onEdit = { navController.navigate(AppScreen.lessonForm(lessonId)) },
                    onAddSection = { navController.navigate(AppScreen.newSection(lessonId)) },
                    onEditSection = { sectionId -> navController.navigate(AppScreen.editSection(lessonId, sectionId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.TEACHER_PROFILE) {
                TeacherProfileScreen(
                    state = teacherState,
                    viewModel = teacherViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.NEW_LESSON) {
                LessonFormScreen(
                    lessonId = null,
                    state = teacherState,
                    viewModel = teacherViewModel,
                    // The form gives way to the new lesson, so back from there returns to the panel.
                    onCreated = { id ->
                        navController.navigate(AppScreen.lessonEditor(id)) {
                            popUpTo(AppScreen.NEW_LESSON) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.LESSON_FORM,
                arguments = listOf(navArgument(AppScreen.ARG_LESSON_ID) { type = NavType.StringType })
            ) { entry ->
                LessonFormScreen(
                    lessonId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_LESSON_ID) }.orEmpty(),
                    state = teacherState,
                    viewModel = teacherViewModel,
                    onCreated = {},
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.NEW_SECTION,
                arguments = listOf(navArgument(AppScreen.ARG_LESSON_ID) { type = NavType.StringType })
            ) { entry ->
                SectionEditorScreen(
                    lessonId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_LESSON_ID) }.orEmpty(),
                    sectionId = null,
                    state = teacherState,
                    viewModel = teacherViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                AppScreen.EDIT_SECTION,
                arguments = listOf(
                    navArgument(AppScreen.ARG_LESSON_ID) { type = NavType.StringType },
                    navArgument(AppScreen.ARG_SECTION_ID) { type = NavType.StringType }
                )
            ) { entry ->
                SectionEditorScreen(
                    lessonId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_LESSON_ID) }.orEmpty(),
                    sectionId = entry.arguments?.read { getStringOrNull(AppScreen.ARG_SECTION_ID) }.orEmpty(),
                    state = teacherState,
                    viewModel = teacherViewModel,
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
                onSelect = { tab -> if (tab != currentTab) selectTab(tab) },
                backdrop = hazeState
            )
        }

        // Over everything, wherever sign-in left off: an account goes nowhere until it has a name.
        if (studentState.needsName && backStackEntry?.destination?.route != AppScreen.SPLASH) {
            ChooseNameScreen(
                isSaving = studentState.isSavingProfile,
                onSave = studentViewModel::setName,
                onSignOut = studentViewModel::signOut
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (currentTab != null) 122.dp else PageBottomGap)
        ) { data ->
            RtlContent {
                Snackbar(containerColor = MelovaCardElevated, contentColor = MelovaText) {
                    Text(data.visuals.message)
                }
            }
        }
    }
}
