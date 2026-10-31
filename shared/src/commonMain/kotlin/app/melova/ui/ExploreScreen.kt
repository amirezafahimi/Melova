package app.melova.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.melova.platform.persianDigits
import app.melova.presentation.CatalogUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.FollowButton
import app.melova.ui.components.LessonGridTile
import app.melova.ui.components.LessonRowCard
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.TeacherAvatar
import app.melova.ui.components.TeacherCard
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/**
 * The explore tab, the way to every lesson: a grid of lessons, and one search over lessons and
 * teachers, since ten teachers may offer a lesson of the same name.
 */
@Composable
fun ExploreScreen(
    state: CatalogUiState,
    onLessonClick: (String) -> Unit,
    onTeacherClick: (String) -> Unit,
    onFollowChange: (teacherId: String, followed: Boolean) -> Unit,
    onRetry: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searching = query.isNotBlank()
    val lessons = if (searching) state.searchLessons(query) else state.exploreLessons
    val teachers = if (searching) state.searchTeachers(query) else emptyList()
    MelovaBackground {
        RtlContent {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = BottomBarContentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fullWidth("title") {
                    Text(text = "جستجو", style = MaterialTheme.typography.headlineSmall, color = MelovaText)
                }
                fullWidth("search") {
                    MelovaTextField(value = query, onValueChange = { query = it }, label = "جستجوی درس یا استاد")
                }
                when {
                    !state.hasContent && state.isLoading -> fullWidth("loading") { LoadingCard() }
                    !state.hasContent && state.errorMessage != null -> fullWidth("error") {
                        MessageCard(Icons.Default.CloudOff, state.errorMessage, actionLabel = "تلاش دوباره", onAction = onRetry)
                    }
                    lessons.isEmpty() && teachers.isEmpty() -> fullWidth("nothing") {
                        MessageCard(
                            icon = if (searching) Icons.Outlined.PersonSearch else Icons.Outlined.LibraryMusic,
                            message = if (searching) "درس یا استادی با این نام پیدا نشد." else "هنوز درسی منتشر نشده است."
                        )
                    }
                    else -> {
                        if (teachers.isNotEmpty()) {
                            fullWidth("teachers") { SectionHeader(title = "استادها") }
                            items(teachers, key = { "teacher-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { teacher ->
                                TeacherCard(
                                    teacher = teacher,
                                    lessonCount = state.lessonsOf(teacher.id).size,
                                    isFollowed = state.isFollowed(teacher.id),
                                    onFollowChange = { onFollowChange(teacher.id, it) },
                                    onClick = { onTeacherClick(teacher.id) }
                                )
                            }
                        }
                        if (searching && lessons.isNotEmpty()) {
                            fullWidth("lessons") { SectionHeader(title = "درس‌ها", trailing = persianDigits("${lessons.size} درس")) }
                        }
                        items(lessons, key = { "lesson-" + it.id }) { lesson ->
                            LessonGridTile(lesson = lesson, onClick = { onLessonClick(lesson.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageTeacherButton(isBusy: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isBusy,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MelovaOutline),
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = MelovaAccentSoft, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MelovaAccentSoft,
                modifier = Modifier.size(18.dp)
            )
            Text(" پیام به استاد", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** A teacher's page: who they are, ways to follow and write to them, and all their lessons. */
@Composable
fun TeacherPageScreen(
    teacherId: String,
    state: CatalogUiState,
    onFollowChange: (Boolean) -> Unit,
    onLessonClick: (String) -> Unit,
    isOpeningChat: Boolean,
    onMessage: (() -> Unit)?,
    onBack: () -> Unit
) {
    val teacher = state.teacher(teacherId)
    val lessons = state.lessonsOf(teacherId)
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { MelovaTopBar(title = teacher?.name ?: "استاد", subtitle = "صفحه‌ی استاد", onBack = onBack) }
                if (teacher == null) {
                    item {
                        if (state.isLoading) LoadingCard()
                        else MessageCard(Icons.Outlined.PersonSearch, "این استاد پیدا نشد.")
                    }
                    return@LazyColumn
                }
                item {
                    MelovaSurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                TeacherAvatar(teacher.name, size = 60.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(teacher.name, style = MaterialTheme.typography.titleLarge, color = MelovaText)
                                    Text(
                                        text = persianDigits("${lessons.size} درس، ${state.availableSections(teacherId)} بخش"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MelovaAccentSoft
                                    )
                                }
                            }
                            if (teacher.bio.isNotBlank()) {
                                Text(teacher.bio, style = MaterialTheme.typography.bodyMedium, color = MelovaTextMuted)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (state.isFollowed(teacherId)) {
                                        "این استاد در پروفایل شماست و درس‌هایش اول «جستجو» می‌آید."
                                    } else {
                                        "دنبال کنید تا این استاد در پروفایل شما بماند."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MelovaTextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                FollowButton(isFollowed = state.isFollowed(teacherId), onFollowChange = onFollowChange)
                            }
                            onMessage?.let { MessageTeacherButton(isBusy = isOpeningChat, onClick = it) }
                        }
                    }
                }
                item { SectionHeader(title = "درس‌ها") }
                if (lessons.isEmpty()) {
                    item { Text("هنوز درسی منتشر نکرده است.", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted) }
                }
                itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                    LessonRowCard(
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
