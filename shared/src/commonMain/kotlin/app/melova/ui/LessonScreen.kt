package app.melova.ui

import app.melova.data.model.SectionLevel
import app.melova.ui.components.SectionLevelChip
import app.melova.ui.components.ForwardArrow
import app.melova.ui.theme.MelovaAccentTint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import app.melova.ui.components.CenteredLine
import androidx.compose.material.icons.filled.Mic
import app.melova.ui.components.TeacherAvatar
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.width
import app.melova.ui.theme.MelovaPillShape
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.model.LessonTier
import app.melova.platform.persianDigits
import app.melova.presentation.CatalogUiState
import app.melova.presentation.IntroStatus
import app.melova.presentation.LessonIntroUiState
import app.melova.presentation.LessonIntroViewModel
import app.melova.presentation.LoginGate
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.SeekLine
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.ScoreBadge
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.formatClock
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.LocalMelovaPalette

@Composable
fun LessonScreen(
    lessonId: String,
    catalogState: CatalogUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSectionClick: (sectionId: String) -> Unit,
    onResume: () -> Unit,
    /** Signed in as this lesson's teacher: a paid lesson is open to them. */
    isOwnTeacher: Boolean = false,
    /** Buying a lesson takes a signed-in account. */
    loginGate: LoginGate = LoginGate.OPEN,
    /** A purchase or an added lesson is on its way to the server. */
    isChangingLessons: Boolean = false,
    onLogin: () -> Unit = {},
    onLoginToAdd: () -> Unit = {},
    onBuy: (LessonTier) -> Unit = {},
    onAddFree: () -> Unit = {},
    onTeacherClick: (String) -> Unit = {},
    introViewModel: LessonIntroViewModel = viewModel { LessonIntroViewModel(createSavedStateHandle()) }
) {
    val introState by introViewModel.uiState.collectAsStateWithLifecycle()
    val lesson = catalogState.lesson(lessonId)
    val addFree = rememberLoginGated(loginGate, onLoginToAdd, onAddFree)
    var offerOpen by rememberSaveable(lessonId) { mutableStateOf(false) }

    // Refresh best scores when the student comes back from a practice section.
    LaunchedEffect(Unit) { onResume() }
    DisposableEffect(Unit) { onDispose { introViewModel.pause() } }

    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { MelovaTopBar(title = lesson?.title ?: "درس", onBack = onBack) }

                if (lesson == null) {
                    item {
                        if (catalogState.isLoading) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MelovaAccent)
                            }
                        } else {
                            MessageCard(
                                icon = Icons.Default.CloudOff,
                                message = catalogState.errorMessage ?: "این درس در دسترس نیست.",
                                actionLabel = "تلاش دوباره",
                                onAction = onRetry
                            )
                        }
                    }
                    return@LazyColumn
                }

                item { LessonHero(lesson, onTeacherClick = { onTeacherClick(lesson.teacherId) }) }
                val locked = !lesson.isOpen && !isOwnTeacher
                if (locked) {
                    item { LessonOfferCard(lesson, isChangingLessons, onOpen = { offerOpen = true }) }
                } else if (lesson.isFree && lesson.isOwned != true && !isOwnTeacher) {
                    item {
                        FreeLessonCard(isBusy = isChangingLessons, onAdd = addFree)
                    }
                }
                item {
                    IntroPlayerCard(
                        state = introState,
                        teacher = lesson.teacher,
                        onToggle = introViewModel::togglePlayback,
                        onSeek = introViewModel::seekTo,
                        onSkip = introViewModel::skipBy
                    )
                }
                // Hidden while the page asks to add a free lesson, so it keeps one main action.
                // A locked lesson still starts here: its first section is free.
                val offersAdd = !locked && lesson.isFree && lesson.isOwned != true && !isOwnTeacher
                // Before buying, only the free first section can be next.
                val next = catalogState.nextSection(lesson)?.takeIf { !locked || lesson.isSectionOpen(it.id) }
                if (next != null && !offersAdd) {
                    item {
                        StartPracticeButton(
                            number = lesson.sections.indexOf(next) + 1,
                            isFirst = catalogState.practicedSections(lesson) == 0,
                            onClick = { onSectionClick(next.id) }
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "بخش‌های تمرین",
                        trailing = persianDigits("${catalogState.practicedSections(lesson)} از ${lesson.sections.size}")
                    )
                }
                itemsIndexed(lesson.sections, key = { _, section -> section.id }) { index, section ->
                    SectionCard(
                        number = index + 1,
                        section = section,
                        bestScore = catalogState.bestScore(lesson.id, section.id),
                        level = catalogState.level(lesson.id, section.id),
                        locked = !isOwnTeacher && !lesson.isSectionOpen(section.id),
                        freeSample = locked && index == 0,
                        isNext = section.id == next?.id,
                        onClick = { onSectionClick(section.id) },
                        onLockedClick = { offerOpen = true }
                    )
                }
                // Someone who has the lesson, or a free one, may still want more time with the teacher.
                if (!locked && !isOwnTeacher && lesson.isOwned == true && lesson.offer?.canUpgrade == true) {
                    item { LessonOfferCard(lesson, isChangingLessons, onOpen = { offerOpen = true }) }
                }
                item { PracticeTipCard() }
                item { Spacer(Modifier.height(12.dp)) }
            }
            if (lesson != null && !isOwnTeacher) {
                LessonOfferSheet(
                    lesson = lesson,
                    loginGate = loginGate,
                    open = offerOpen,
                    onOpenChange = { offerOpen = it },
                    onLogin = onLogin,
                    onBuy = onBuy
                )
            }
        }
    }
}

/** Who teaches the lesson, its level and size on one line, then what it is about. */
@Composable
private fun LessonHero(lesson: Lesson, onTeacherClick: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lesson.teacher.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MelovaPillShape)
                        .clickable(onClick = onTeacherClick)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeacherAvatar(lesson.teacher, size = 28.dp, teacherId = lesson.teacherId, photoVersion = lesson.teacherPhotoVersion)
                    Text(
                        text = lesson.teacher,
                        // Trimmed to the letters, so the name sits level with the avatar and arrow.
                        style = MaterialTheme.typography.labelLarge.copy(lineHeightStyle = CenteredLine),
                        color = MelovaAccentSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    ForwardArrow(tint = MelovaAccentSoft)
                }
            }
            if (lesson.level.isNotBlank()) MelovaChip(text = lesson.level)
            MelovaChip(text = persianDigits("${lesson.sections.size} بخش"), color = MelovaAccentSoft)
        }
        // The singer the lesson is about; the stored field is still called "subtitle".
        if (lesson.subtitle.isNotBlank()) {
            MelovaChip(text = "خواننده: ${lesson.subtitle}", color = MelovaGold)
        }
        if (lesson.description.isNotBlank()) {
            // Two lines until tapped, so the sections stay near the top.
            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

/** The one thing to do on this page: sing the next section. */
@Composable
private fun StartPracticeButton(number: Int, isFirst: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(MelovaDimens.PrimaryButtonHeight),
        shape = MelovaSmallShape,
        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
    ) {
        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(MelovaDimens.Icon))
        Spacer(Modifier.width(8.dp))
        Text(
            // The number only: section titles often start with «بخش» themselves.
            text = (if (isFirst) "شروع تمرین" else "ادامه تمرین") + persianDigits(" • بخش $number"),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The teacher's spoken introduction as one row like the others on the page: a play button, what it
 * is, and once it plays the time and a thin seek line under it.
 */
@Composable
private fun IntroPlayerCard(
    state: LessonIntroUiState,
    teacher: String,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkip: (Long) -> Unit
) {
    val isUnavailable = state.status == IntroStatus.UNAVAILABLE
    val isReady = state.status == IntroStatus.READY
    MelovaSurfaceCard(contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = onToggle,
                    enabled = !isUnavailable && state.status != IntroStatus.LOADING,
                    shape = CircleShape,
                    color = if (isUnavailable) MelovaAccentTint else MelovaAccent,
                    modifier = Modifier.size(MelovaDimens.ListLead)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            state.status == IntroStatus.LOADING -> CircularProgressIndicator(
                                modifier = Modifier.size(MelovaDimens.IconSmall),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            isUnavailable -> Icon(
                                Icons.Outlined.RecordVoiceOver,
                                contentDescription = null,
                                tint = MelovaAccentSoft,
                                modifier = Modifier.size(MelovaDimens.Icon)
                            )
                            else -> Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "توقف آموزش" else "پخش آموزش",
                                tint = Color.White,
                                modifier = Modifier.size(MelovaDimens.IconLarge)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "آموزش کلی درس", style = MaterialTheme.typography.titleSmall, color = MelovaText)
                    Text(
                        text = when (state.status) {
                            IntroStatus.UNAVAILABLE -> "به‌زودی اضافه می‌شود"
                            IntroStatus.LOADING -> "در حال آماده‌سازی صدا…"
                            IntroStatus.ERROR -> state.errorMessage ?: "پخش ممکن نشد؛ دوباره بزنید."
                            else -> if (state.positionMs > 0 || state.isPlaying) {
                                formatClock(state.positionMs) + " از " + formatClock(state.durationMs)
                            } else if (teacher.isNotBlank()) {
                                "توضیح $teacher، پیش از تمرین"
                            } else {
                                "توضیح درس، پیش از تمرین"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isReady) {
                    // Audio time runs left to right, even on a right-to-left page.
                    LtrContent {
                        Row {
                            IconButton(onClick = { onSkip(-10_000L) }, modifier = Modifier.size(MelovaDimens.CompactButtonHeight)) {
                                Icon(
                                    Icons.Default.Replay10,
                                    contentDescription = "۱۰ ثانیه عقب",
                                    tint = MelovaTextMuted,
                                    modifier = Modifier.size(MelovaDimens.Icon)
                                )
                            }
                            IconButton(onClick = { onSkip(10_000L) }, modifier = Modifier.size(MelovaDimens.CompactButtonHeight)) {
                                Icon(
                                    Icons.Default.Forward10,
                                    contentDescription = "۱۰ ثانیه جلو",
                                    tint = MelovaTextMuted,
                                    modifier = Modifier.size(MelovaDimens.Icon)
                                )
                            }
                        }
                    }
                }
            }
            if (isReady) {
                LtrContent { SeekLine(progress = state.progress, onSeek = onSeek) }
            }
        }
    }
}

@Composable
private fun SectionCard(
    number: Int,
    section: LessonSection,
    bestScore: Int?,
    level: SectionLevel,
    locked: Boolean,
    isNext: Boolean,
    onClick: () -> Unit,
    /** The first section of a lesson not bought yet, which anyone can sing. */
    freeSample: Boolean = false,
    /** Tapping a locked section shows what buying the lesson gives. */
    onLockedClick: () -> Unit = {}
) {
    val enabled = section.isAvailable && !locked
    MelovaSurfaceCard(
        onClick = if (locked) onLockedClick else onClick,
        enabled = section.isAvailable,
        highlighted = isNext,
        modifier = Modifier.alpha(if (enabled) 1f else 0.55f),
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bestScore != null) {
                ScoreBadge(score = bestScore, size = 40.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MelovaCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (section.isAvailable && !locked) {
                        Text(
                            text = persianDigits(number.toString()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MelovaAccentSoft
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MelovaTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // Two lines: the badge already says practiced, scored or locked, so no status chip below.
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (section.durationMs > 0L) {
                        Text(
                            text = persianDigits("${(section.durationMs + 999) / 1000} ثانیه"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MelovaTextMuted
                        )
                    }
                }
                val line = when {
                    !section.isAvailable -> "به‌زودی"
                    locked -> "بعد از خرید درس باز می‌شود"
                    section.description.isNotBlank() -> section.description
                    bestScore != null -> "بهترین نمره‌ی شما"
                    else -> "هنوز تمرین نشده"
                }
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (freeSample && section.isAvailable) {
                MelovaChip(text = "رایگان", color = MelovaSuccess)
            } else if (isNext) {
                MelovaChip(text = "بعدی", color = MelovaAccentSoft)
            } else if (section.isAvailable && !locked) {
                if (level != SectionLevel.NOT_SUNG) {
                    SectionLevelChip(level)
                } else {
                    ForwardArrow(tint = MelovaTextMuted)
                }
            }
        }
    }
}

@Composable
private fun PracticeTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MelovaSmallShape)
            .background(MelovaGold.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.Headphones, contentDescription = null, tint = MelovaGold)
        Text(
            text = "برای نتیجه‌ی دقیق‌تر با هدفون تمرین کنید و در جای آرام بخوانید.",
            style = MaterialTheme.typography.bodySmall,
            color = MelovaText.copy(alpha = 0.85f)
        )
    }
}
