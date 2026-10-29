package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.presentation.CatalogUiState
import app.melova.presentation.IntroStatus
import app.melova.presentation.LessonIntroUiState
import app.melova.presentation.LessonIntroViewModel
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.ScoreBadge
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.formatClock
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
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
    studentId: String? = null,
    introViewModel: LessonIntroViewModel = viewModel()
) {
    val introState by introViewModel.uiState.collectAsStateWithLifecycle()
    val lesson = catalogState.lesson(lessonId)

    // Refresh best scores when the student comes back from a practice section.
    LaunchedEffect(Unit) { onResume() }
    DisposableEffect(Unit) { onDispose { introViewModel.pause() } }

    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                item { LessonHero(lesson) }
                val locked = !lesson.isOpen && !isOwnTeacher
                if (locked) item { LockedLessonCard(lesson, studentId) }
                item {
                    IntroPlayerCard(
                        state = introState,
                        teacher = lesson.teacher,
                        onToggle = introViewModel::togglePlayback,
                        onSeek = introViewModel::seekTo,
                        onSkip = introViewModel::skipBy
                    )
                }
                item {
                    SectionHeader(
                        title = "بخش‌های تمرین",
                        trailing = "${catalogState.practicedSections(lesson)} از ${lesson.sections.size}"
                    )
                }
                itemsIndexed(lesson.sections, key = { _, section -> section.id }) { index, section ->
                    SectionCard(
                        number = index + 1,
                        section = section,
                        bestScore = catalogState.bestScore(lesson.id, section.id),
                        locked = locked,
                        onClick = { onSectionClick(section.id) }
                    )
                }
                item { PracticeTipCard() }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun LessonHero(lesson: Lesson) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lesson.level.isNotBlank()) MelovaChip(text = lesson.level)
            MelovaChip(text = "${lesson.sections.size} بخش", color = MelovaAccentSoft)
        }
        if (lesson.subtitle.isNotBlank()) {
            Text(
                text = lesson.subtitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MelovaText
            )
        }
        if (lesson.teacher.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MelovaAccentSoft,
                    modifier = Modifier.size(18.dp)
                )
                Text(text = lesson.teacher, style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
            }
        }
        if (lesson.description.isNotBlank()) {
            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted
            )
        }
    }
}

@Composable
private fun IntroPlayerCard(
    state: LessonIntroUiState,
    teacher: String,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkip: (Long) -> Unit
) {
    val isUnavailable = state.status == IntroStatus.UNAVAILABLE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(MelovaAccent, MelovaSecondary)
                )
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                onClick = onToggle,
                enabled = !isUnavailable && state.status != IntroStatus.LOADING,
                shape = CircleShape,
                color = if (isUnavailable) Color.White.copy(alpha = 0.12f) else Color.White,
                modifier = Modifier.size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        state.status == IntroStatus.LOADING -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MelovaAccent,
                            strokeWidth = 2.5.dp
                        )
                        isUnavailable -> Icon(
                            Icons.Outlined.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        else -> Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "توقف آموزش" else "پخش آموزش",
                            tint = LocalMelovaPalette.current.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "آموزش کلی درس",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = when (state.status) {
                        IntroStatus.UNAVAILABLE -> "آموزش صوتی این درس به‌زودی اضافه می‌شود."
                        IntroStatus.LOADING -> "در حال آماده‌سازی صدا…"
                        IntroStatus.ERROR -> state.errorMessage ?: "پخش ممکن نشد؛ دوباره بزنید."
                        else -> if (teacher.isNotBlank()) {
                            "$teacher کل درس را توضیح می‌دهد؛ قبل از تمرین گوش دهید."
                        } else {
                            "قبل از تمرین بخش‌ها، توضیح کامل درس را گوش دهید."
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }

        if (state.status == IntroStatus.READY) {
            var dragFraction by remember { mutableStateOf<Float?>(null) }
            LtrContent {
                Slider(
                    value = dragFraction ?: state.progress,
                    onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let(onSeek)
                        dragFraction = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )
            }
            LtrContent {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatClock(state.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onSkip(-10_000L) }) {
                        Icon(Icons.Default.Replay10, contentDescription = "۱۰ ثانیه عقب", tint = Color.White)
                    }
                    IconButton(onClick = { onSkip(10_000L) }) {
                        Icon(Icons.Default.Forward10, contentDescription = "۱۰ ثانیه جلو", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatClock(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    number: Int,
    section: LessonSection,
    bestScore: Int?,
    locked: Boolean,
    onClick: () -> Unit
) {
    val enabled = section.isAvailable && !locked
    MelovaSurfaceCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.alpha(if (enabled) 1f else 0.55f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (bestScore != null) {
                ScoreBadge(score = bestScore)
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MelovaCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (section.isAvailable) {
                        Text(
                            text = number.toString(),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (section.description.isNotBlank()) {
                    Text(
                        text = section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    when {
                        !section.isAvailable -> MelovaChip(text = "به‌زودی", color = MelovaTextMuted)
                        bestScore != null -> MelovaChip(text = "بهترین نمره", color = MelovaGold)
                        else -> MelovaChip(text = "تمرین نشده", color = MelovaAccentSoft)
                    }
                    if (section.durationMs > 0L) {
                        Text(
                            text = "${(section.durationMs + 999) / 1000} ثانیه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MelovaTextMuted
                        )
                    }
                }
            }
            if (section.isAvailable) {
                Icon(
                    imageVector = if (bestScore != null) Icons.Default.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MelovaTextMuted
                )
            }
        }
    }
}

@Composable
private fun PracticeTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MelovaGold.copy(alpha = 0.10f))
            .padding(14.dp),
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
