package app.melova.ui

import app.melova.ui.components.ForwardArrow
import app.melova.ui.theme.MelovaCardShape
import app.melova.data.model.LessonSection
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import app.melova.ui.theme.MelovaDimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.melova.ui.components.MelovaNavRow
import app.melova.ui.components.PracticeWeekCard
import app.melova.ui.components.WeekGoalDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.melova.data.model.Lesson
import app.melova.platform.formatPersianInteger
import app.melova.platform.persianDigits
import app.melova.presentation.CatalogUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaMark
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MyLessonRow
import app.melova.ui.components.SectionHeader
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** A practice tool that needs no teacher; shown in the strip near the top of the practice tab. */
private data class FreePractice(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

/**
 * The practice tab: the student's own lessons. Free practice is a compact strip near the top,
 * and the card on top opens the lesson to continue in a single tap. Someone with no lessons
 * yet is sent to find one, as nothing arrives here on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: CatalogUiState,
    onRefresh: () -> Unit,
    onAppear: () -> Unit,
    onLessonClick: (String) -> Unit,
    /** Straight into a section's practice, for the card that says what to sing next. */
    onSectionClick: (lessonId: String, sectionId: String) -> Unit,
    onSolfegeClick: () -> Unit,
    onExploreClick: () -> Unit,
    onJournalClick: () -> Unit = {},
    onSaveWeekGoal: (goal: Int, reminders: Boolean) -> Unit = { _, _ -> },
    /** Whether this device can get the weekly reminder at all. */
    canRemind: Boolean = false,
    reminderNote: String? = null,
    unreadChats: Int = 0,
    /** Null until the person has someone to talk to, that is before they sign in. */
    onChatsClick: (() -> Unit)? = null
) {
    // Catch up with changes made in the teacher panel or by another device.
    LaunchedEffect(Unit) { onAppear() }
    var editingGoal by remember { mutableStateOf(false) }

    val freePractice = listOf(
        FreePractice(Icons.Default.MusicNote, "گام و سلفژ", "ماژور و مینور، با دقت هر نت", onSolfegeClick)
    )

    MelovaBackground {
        RtlContent {
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.hasContent,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = BottomBarContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { HomeHeader(unreadChats, onChatsClick) }

                    val myLessons = state.myLessons
                    if (state.totalSections > 0) {
                        val board = state.lastPracticedLesson
                            ?: state.nextLesson
                            ?: myLessons.firstOrNull { it.isOpen && it.availableSections > 0 }
                        if (board != null) item(key = "continue") {
                            val last = state.lastSectionId?.takeIf { state.lastLessonId == board.id }
                                ?.let { board.section(it) }
                            val next = state.nextSection(board)
                            LastPracticeCard(
                                lesson = board,
                                lastSection = last,
                                lastScore = state.lastScore.takeIf { last != null },
                                nextSection = next,
                                onClick = {
                                    val target = next ?: last
                                    if (target != null) onSectionClick(board.id, target.id) else onLessonClick(board.id)
                                }
                            )
                        }
                    }

                    // Only once there is something to sing: an empty week is no use to a newcomer.
                    val week = state.week
                    if (week != null && state.totalSections > 0) {
                        item(key = "week") { PracticeWeekCard(week = week, onEditGoal = { editingGoal = true }) }
                        if (state.progress.bestScores.isNotEmpty()) item(key = "journal") {
                            MelovaNavRow(
                                icon = Icons.Outlined.AutoStories,
                                title = "دفتر تمرین",
                                subtitle = "اولین و بهترین اجرای هر بخش",
                                onClick = onJournalClick
                            )
                        }
                    }

                    // A newcomer's first step is finding a lesson, so that card comes before free practice.
                    val noLessons = state.hasContent && myLessons.isEmpty()
                    if (noLessons) item(key = "no-lessons") { NoLessonsCard(onExploreClick) }

                    item(key = "free") { FreePracticeStrip(freePractice) }

                    when {
                        !state.hasContent && state.isLoading -> item { LoadingCard() }
                        !state.hasContent && state.errorMessage != null -> item {
                            MessageCard(
                                icon = Icons.Default.CloudOff,
                                message = state.errorMessage,
                                actionLabel = "تلاش دوباره",
                                onAction = onRefresh
                            )
                        }
                        !state.hasContent -> item {
                            MessageCard(
                                icon = Icons.Outlined.LibraryMusic,
                                message = "هنوز استادی درس منتشر نکرده است. به‌زودی برمی‌گردیم!"
                            )
                        }
                        noLessons -> Unit
                        else -> {
                            item(key = "lessons-header") {
                                SectionHeader(title = "درس‌های من", trailing = persianDigits("${myLessons.size} درس"))
                            }
                            items(myLessons, key = { it.id }) { lesson ->
                                MyLessonRow(
                                    lesson = lesson,
                                    practiced = state.practicedSections(lesson),
                                    onClick = { onLessonClick(lesson.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val week = state.week
    if (editingGoal && week != null) {
        WeekGoalDialog(
            week = week,
            canRemind = canRemind,
            reminderNote = reminderNote,
            onSave = { goal, reminders ->
                editingGoal = false
                onSaveWeekGoal(goal, reminders)
            },
            onDismiss = { editingGoal = false }
        )
    }
}

@Composable
private fun HomeHeader(unreadChats: Int, onChatsClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MelovaMark(size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Melova",
                style = MaterialTheme.typography.headlineSmall,
                color = MelovaText
            )
            Text(
                text = "امروز چه چیزی تمرین کنیم؟",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted
            )
        }
        onChatsClick?.let { ChatsIconButton(unread = unreadChats, onClick = it) }
    }
}

/**
 * The board on top: the lesson the student last sang in, the section of that take and its score,
 * and one row under a line that says what a tap sings next. Someone who has not sung yet is
 * offered the lesson's first section instead. A faint tahrir curve behind is the only ornament.
 */
@Composable
private fun LastPracticeCard(
    lesson: Lesson,
    /** Sung most recently in [lesson]; null for someone who has not sung in it yet. */
    lastSection: LessonSection?,
    lastScore: Int?,
    /** What a tap sings; null when every section has been sung, and the last one is sung again. */
    nextSection: LessonSection?,
    onClick: () -> Unit
) {
    val sections = lesson.sections.filter { it.isAvailable }
    // «بخش ۳: عنوان», but a section titled «بخش ۳» already says its number.
    fun label(section: LessonSection): String {
        val number = sections.indexOfFirst { it.id == section.id } + 1
        return if (section.title.trim().startsWith("بخش") || number == 0) section.title
        else persianDigits("بخش $number") + ": " + section.title
    }
    // On the dark theme the raised card and an outline-coloured curve are enough. On the light
    // theme they fade into the lavender page, so the card takes a very light violet wash, a little
    // deeper at the curve's corner, and the curve takes the accent.
    val dark = LocalMelovaPalette.current.isDark
    val accent = MelovaAccent
    val curve = if (dark) MelovaOutline else accent.copy(alpha = 0.45f)
    Surface(
        onClick = onClick,
        shape = MelovaCardShape,
        color = if (dark) MelovaCardElevated else MelovaCard,
        border = BorderStroke(1.dp, MelovaOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    if (!dark) {
                        val rtl = layoutDirection == LayoutDirection.Rtl
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(accent.copy(alpha = 0.10f), accent.copy(alpha = 0.04f)),
                                start = Offset(if (rtl) 0f else size.width, 0f),
                                end = Offset(if (rtl) size.width else 0f, size.height)
                            )
                        )
                    }
                    drawTahrirCurve(curve)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (lastSection != null) "آخرین تمرین شما" else "اولین درس شما",
                        style = MaterialTheme.typography.labelMedium,
                        color = MelovaTextMuted
                    )
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = lastSection?.let(::label)
                            ?: "با ${lesson.teacher}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (lastSection != null && lastScore != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = persianDigits("$lastScore"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MelovaGold
                        )
                        Text(text = "نمره", style = MaterialTheme.typography.labelSmall, color = MelovaTextMuted)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MelovaOutline)
            )
            // The row says what a tap does; the whole card does the same.
            val target = nextSection ?: lastSection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        target == null -> "دیدن درس"
                        lastSection == null -> "شروع: " + label(target)
                        nextSection == null -> "دوباره: " + label(target)
                        else -> "بعدی: " + label(target)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MelovaAccentSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ForwardArrow(tint = MelovaAccentSoft)
            }
        }
    }
}

/**
 * A melisma drawn as one thin line across the top of the card's far end: a few quick turns, then
 * a long rise, the shape a tahrir takes on the teacher's pitch line. It sits away from where the
 * text starts so the words stay clear.
 */
private fun DrawScope.drawTahrirCurve(color: Color) {
    val w = minOf(size.width * 0.55f, 200.dp.toPx())
    val h = size.height * 0.6f
    val rtl = layoutDirection == LayoutDirection.Rtl
    // Drawn for right-to-left (curve on the left); mirrored when the far end is on the right.
    fun x(f: Float) = if (rtl) f * w else size.width - f * w
    fun y(f: Float) = f * h
    // Points of the curve in a 160 by 90 box.
    val segments = listOf(
        floatArrayOf(20f, 70f, 24f, 30f, 40f, 40f),
        floatArrayOf(56f, 50f, 56f, 70f, 68f, 50f),
        floatArrayOf(80f, 30f, 80f, 20f, 92f, 34f),
        floatArrayOf(104f, 48f, 104f, 56f, 116f, 40f),
        floatArrayOf(128f, 24f, 132f, 14f, 160f, 18f)
    )
    val path = Path().apply {
        moveTo(x(0f), y(70f / 90f))
        segments.forEach { s ->
            cubicTo(
                x(s[0] / 160f), y(s[1] / 90f),
                x(s[2] / 160f), y(s[3] / 90f),
                x(s[4] / 160f), y(s[5] / 90f)
            )
        }
    }
    drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

@Composable
private fun FreePracticeStrip(tools: List<FreePractice>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "تمرین آزاد")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            tools.forEach { tool ->
                MelovaSurfaceCard(onClick = tool.onClick, contentPadding = 12.dp, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MelovaGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, contentDescription = null, tint = MelovaGold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tool.title, style = MaterialTheme.typography.titleSmall, color = MelovaText, maxLines = 1)
                            Text(
                                text = tool.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MelovaTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        ForwardArrow(tint = MelovaTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoLessonsCard(onExploreClick: () -> Unit) {
    MelovaSurfaceCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.LibraryMusic, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(36.dp))
            Text("هنوز درسی ندارید", style = MaterialTheme.typography.titleMedium, color = MelovaText)
            Text(
                text = "از هر استادی می‌توانید درس بگیرید. درس‌هایی که بگیرید اینجا می‌آیند.",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onExploreClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(MelovaDimens.Icon))
                Spacer(modifier = Modifier.width(8.dp))
                Text("جستجوی درس‌ها و استادها")
            }
        }
    }
}

@Composable
internal fun LoadingCard() {
    MelovaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MelovaAccent,
                strokeWidth = 2.dp
            )
            Text(
                text = "در حال دریافت درس‌ها…",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted
            )
        }
    }
}

@Composable
internal fun MessageCard(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    MelovaSurfaceCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MelovaTextMuted, modifier = Modifier.size(36.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                ) { Text(actionLabel) }
            }
        }
    }
}
