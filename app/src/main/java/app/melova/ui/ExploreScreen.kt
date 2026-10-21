package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.melova.presentation.CatalogUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.FollowButton
import app.melova.ui.components.LessonRowCard
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.TeacherAvatar
import app.melova.ui.components.TeacherCard
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** The explore tab: every teacher with published lessons, followable in one tap. */
@Composable
fun ExploreScreen(
    state: CatalogUiState,
    onTeacherClick: (String) -> Unit,
    onFollowChange: (teacherId: String, followed: Boolean) -> Unit,
    onRetry: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val teachers = state.teachers.filter { teacher ->
        state.lessonsOf(teacher.id).isNotEmpty() &&
            (query.isBlank() || teacher.name.contains(query.trim(), ignoreCase = true))
    }
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = BottomBarContentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "اساتید", style = MaterialTheme.typography.headlineSmall, color = MelovaText)
                        Text(
                            text = "هر استادی را دنبال کنید، درس‌هایش در تب تمرین می‌آید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MelovaTextMuted
                        )
                    }
                }
                if (state.teachers.size > 4) {
                    item { MelovaTextField(value = query, onValueChange = { query = it }, label = "جست‌وجوی استاد") }
                }
                when {
                    !state.hasContent && state.isLoading -> item { LoadingCard() }
                    !state.hasContent && state.errorMessage != null -> item {
                        MessageCard(Icons.Default.CloudOff, state.errorMessage, actionLabel = "تلاش دوباره", onAction = onRetry)
                    }
                    teachers.isEmpty() -> item {
                        MessageCard(
                            icon = if (query.isBlank()) Icons.Outlined.LibraryMusic else Icons.Outlined.PersonSearch,
                            message = if (query.isBlank()) "هنوز استادی درس منتشر نکرده است." else "استادی با این نام پیدا نشد."
                        )
                    }
                    else -> items(teachers, key = { it.id }) { teacher ->
                        TeacherCard(
                            teacher = teacher,
                            lessonCount = state.lessonsOf(teacher.id).size,
                            isFollowed = state.isFollowed(teacher.id),
                            onFollowChange = { onFollowChange(teacher.id, it) },
                            onClick = { onTeacherClick(teacher.id) }
                        )
                    }
                }
            }
        }
    }
}

/** A teacher's page: who they are, a follow button and all their lessons. */
@Composable
fun TeacherPageScreen(
    teacherId: String,
    state: CatalogUiState,
    onFollowChange: (Boolean) -> Unit,
    onLessonClick: (String) -> Unit,
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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                                        text = "${lessons.size} درس • ${state.availableSections(teacherId)} بخش",
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
                                    text = if (state.isFollowed(teacherId)) "درس‌ها در تب تمرین شماست." else "دنبال کنید تا درس‌ها در تب تمرین بیاید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MelovaTextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                FollowButton(isFollowed = state.isFollowed(teacherId), onFollowChange = onFollowChange)
                            }
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
