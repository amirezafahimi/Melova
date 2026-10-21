package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.data.model.Teacher
import app.melova.presentation.CatalogUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.LessonTile
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaMark
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.TeacherAvatar
import app.melova.ui.components.TeacherCard
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
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
 * The practice tab. Its two parts are free practice and the lessons of followed teachers.
 * Free practice is a compact strip near the top, so it is on screen however many lessons
 * there are; the lessons grow sideways (one row per teacher) rather than pushing the page
 * down, and the card on top opens the lesson to continue in a single tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: CatalogUiState,
    onRefresh: () -> Unit,
    onAppear: () -> Unit,
    onLessonClick: (String) -> Unit,
    onTeacherClick: (String) -> Unit,
    onSolfegeClick: () -> Unit,
    onFollowChange: (teacherId: String, followed: Boolean) -> Unit,
    onExploreClick: () -> Unit
) {
    // Catch up with changes made in the teacher panel or by another device.
    LaunchedEffect(Unit) { onAppear() }

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
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = BottomBarContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { HomeHeader() }

                    val following = state.followedTeachers
                    if (following.isNotEmpty()) {
                        item(key = "continue") {
                            ContinueCard(
                                lesson = state.nextLesson,
                                practicedInLesson = state.nextLesson?.let { state.practicedSections(it) } ?: 0,
                                practiced = state.totalPracticed,
                                total = state.totalSections,
                                onClick = onLessonClick
                            )
                        }
                    }

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
                        else -> {
                            if (following.isNotEmpty()) {
                                item(key = "lessons-header") {
                                    SectionHeader(title = "درس‌های اساتید من", trailing = "${following.size} استاد")
                                }
                                items(following, key = { "teacher-${it.id}" }) { teacher ->
                                    TeacherLessonsRow(
                                        teacher = teacher,
                                        lessons = state.lessonsOf(teacher.id),
                                        state = state,
                                        onTeacherClick = { onTeacherClick(teacher.id) },
                                        onLessonClick = onLessonClick
                                    )
                                }
                            }
                            val suggested = state.suggestedTeachers
                            if (suggested.isNotEmpty()) {
                                item(key = "suggested-header") {
                                    if (following.isEmpty()) {
                                        FollowPrompt()
                                    } else {
                                        SectionHeader(title = "اساتید دیگر")
                                    }
                                }
                                items(suggested.take(3), key = { "suggested-${it.id}" }) { teacher ->
                                    TeacherCard(
                                        teacher = teacher,
                                        lessonCount = state.lessonsOf(teacher.id).size,
                                        isFollowed = false,
                                        onFollowChange = { onFollowChange(teacher.id, it) },
                                        onClick = { onTeacherClick(teacher.id) }
                                    )
                                }
                                if (suggested.size > 3) {
                                    item(key = "explore") {
                                        TextButton(onClick = onExploreClick, modifier = Modifier.fillMaxWidth()) {
                                            Text("دیدن همه‌ی اساتید (${suggested.size})", color = MelovaAccentSoft)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MelovaMark(size = 44.dp)
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
    }
}

/** One tap into the lesson to work on next, with overall progress across followed teachers. */
@Composable
private fun ContinueCard(
    lesson: Lesson?,
    practicedInLesson: Int,
    practiced: Int,
    total: Int,
    onClick: (String) -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Surface(
        onClick = { lesson?.let { onClick(it.id) } },
        enabled = lesson != null,
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(
                    Brush.linearGradient(listOf(MelovaAccent.copy(alpha = 0.92f), MelovaSecondary.copy(alpha = 0.88f)))
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when {
                            lesson == null && total > 0 -> "همه را تمرین کرده‌اید"
                            lesson == null -> "درس‌ها به‌زودی می‌رسند"
                            practicedInLesson > 0 -> "ادامه‌ی تمرین"
                            else -> "درس بعدی"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = lesson?.title ?: if (total > 0) "برای نمره‌ی بهتر دوباره بخوانید!" else "اساتید شما هنوز بخشی آماده نکرده‌اند.",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lesson != null) {
                        val left = lesson.availableSections - practicedInLesson
                        Text(
                            text = "${lesson.teacher} • $left بخش مانده",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lesson != null) Icons.Default.PlayArrow else Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            if (total > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MelovaProgressBar(
                        progress = practiced.toFloat() / total,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.28f),
                        followLayoutDirection = true
                    )
                    Text(
                        text = "$practiced از $total بخش",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FreePracticeStrip(tools: List<FreePractice>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تمرین آزاد", style = MaterialTheme.typography.titleMedium, color = MelovaText)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
                    }
                }
            }
        }
    }
}

/** A followed teacher's lessons as one sideways row, opened at the first unfinished lesson. */
@Composable
private fun TeacherLessonsRow(
    teacher: Teacher,
    lessons: List<Lesson>,
    state: CatalogUiState,
    onTeacherClick: () -> Unit,
    onLessonClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = onTeacherClick, color = Color.Transparent, shape = RoundedCornerShape(14.dp)) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TeacherAvatar(teacher.name, size = 34.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.name, style = MaterialTheme.typography.titleSmall, color = MelovaText, maxLines = 1)
                    Text(
                        text = "${lessons.size} درس • ${state.practicedSections(teacher.id)} از ${state.availableSections(teacher.id)} بخش",
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaTextMuted
                    )
                }
                Text("همه", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MelovaAccentSoft,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (lessons.isEmpty()) {
            Text("هنوز درسی منتشر نکرده است.", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        } else {
            val firstUnfinished = lessons.indexOfFirst { !state.isDone(it) }.coerceAtLeast(0)
            val rowState = rememberLazyListState(initialFirstVisibleItemIndex = firstUnfinished)
            LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                    LessonTile(
                        number = index + 1,
                        lesson = lesson,
                        practiced = state.practicedSections(lesson),
                        onClick = { onLessonClick(lesson.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowPrompt() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.PersonSearch, contentDescription = null, tint = MelovaAccentSoft)
        Column(modifier = Modifier.weight(1f)) {
            Text("یک استاد را دنبال کنید", style = MaterialTheme.typography.titleMedium, color = MelovaText)
            Text(
                text = "درس‌های اساتیدی که دنبال می‌کنید همین‌جا می‌آید.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
    }
}

@Composable
internal fun LoadingCard() {
    MelovaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                ) { Text(actionLabel) }
            }
        }
    }
}
