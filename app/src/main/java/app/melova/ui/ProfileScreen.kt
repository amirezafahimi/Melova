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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.BuildConfig
import app.melova.data.model.Course
import app.melova.presentation.CourseUiState
import app.melova.presentation.TeacherUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.FollowButton
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSegmentedControl
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.courseMeta
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.ThemeMode

@Composable
fun ProfileScreen(
    courseState: CourseUiState,
    teacherState: TeacherUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFollowChange: (courseId: String, followed: Boolean) -> Unit,
    onTeacherSignIn: (code: String) -> Unit,
    onTeacherSignOut: () -> Unit,
    onOpenTeacherPanel: () -> Unit
) {
    var showSignIn by rememberSaveable { mutableStateOf(false) }

    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = BottomBarContentPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Text(text = "پروفایل", style = MaterialTheme.typography.headlineSmall, color = MelovaText) }
                item { IdentityCard(teacherState) }
                item { StatsRow(courseState) }

                item {
                    SectionHeader(
                        title = "دوره‌های من",
                        trailing = courseState.followedCourses.size.takeIf { it > 0 }?.let { "$it دوره" }
                    )
                }
                if (courseState.followedCourses.isEmpty()) {
                    item {
                        Text(
                            text = "هنوز دوره‌ای دنبال نمی‌کنید. از تب تمرین یک دوره را دنبال کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                }
                items(courseState.followedCourses, key = { it.id }) { course ->
                    FollowedCourseRow(
                        course = course,
                        practiced = courseState.practicedSections(course),
                        total = courseState.availableSections(course),
                        onUnfollow = { onFollowChange(course.id, false) }
                    )
                }

                item { SectionHeader(title = "پنل استاد") }
                item {
                    TeacherCard(
                        state = teacherState,
                        onSignIn = { showSignIn = true },
                        onSignOut = onTeacherSignOut,
                        onOpenPanel = onOpenTeacherPanel
                    )
                }

                item { SectionHeader(title = "تنظیمات") }
                item { ThemeSetting(themeMode = themeMode, onThemeModeChange = onThemeModeChange) }
                item {
                    Text(
                        text = "Melova • نسخه‌ی ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaTextMuted,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showSignIn) {
        var code by rememberSaveable { mutableStateOf("") }
        FormDialog(
            title = "ورود به پنل استاد",
            confirmLabel = "ورود",
            confirmEnabled = code.isNotBlank(),
            onConfirm = {
                onTeacherSignIn(code)
                showSignIn = false
            },
            onDismiss = { showSignIn = false }
        ) {
            Text(
                text = "کد استادی که روی سرور تعریف شده را وارد کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            MelovaTextField(value = code, onValueChange = { code = it }, label = "کد استاد", isSecret = true)
        }
    }
}

@Composable
private fun IdentityCard(teacherState: TeacherUiState) {
    val teacherName = teacherState.profile?.name?.takeIf { teacherState.isSignedIn && it.isNotBlank() }
    MelovaSurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))),
                contentAlignment = Alignment.Center
            ) {
                if (teacherName != null) {
                    Text(
                        text = teacherName.take(1),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = teacherName ?: "هنرجوی ملووا",
                    style = MaterialTheme.typography.titleLarge,
                    color = MelovaText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MelovaChip(text = "هنرجو", color = MelovaAccentSoft)
                    if (teacherState.isSignedIn) MelovaChip(text = "استاد", color = MelovaGold)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(state: CourseUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(value = state.followedCourses.size.toString(), label = "دوره", modifier = Modifier.weight(1f))
        StatTile(value = state.bestScores.size.toString(), label = "بخش تمرین‌شده", modifier = Modifier.weight(1f))
        StatTile(value = state.bestScoreOverall?.toString() ?: "—", label = "بهترین نمره", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    MelovaSurfaceCard(modifier = modifier, contentPadding = 14.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = MelovaAccentSoft)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted, maxLines = 1)
        }
    }
}

@Composable
private fun FollowedCourseRow(course: Course, practiced: Int, total: Int, onUnfollow: () -> Unit) {
    MelovaSurfaceCard(contentPadding = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${courseMeta(course)} • $practiced/$total بخش",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
            }
            FollowButton(isFollowed = true, onFollowChange = { if (!it) onUnfollow() })
        }
    }
}

@Composable
private fun TeacherCard(
    state: TeacherUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPanel: () -> Unit
) {
    if (state.isSignedIn) {
        MelovaSurfaceCard(onClick = onOpenPanel) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TeacherIcon()
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "مدیریت دوره‌ها", style = MaterialTheme.typography.titleMedium, color = MelovaText)
                        Text(
                            text = state.profile?.let { "${it.courses.size} دوره • ${it.courses.count { c -> c.published }} منتشرشده" }
                                ?: "در حال بارگذاری…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
                }
                TextButton(onClick = onSignOut, contentPadding = PaddingValues(0.dp)) {
                    Text("خروج از حالت استاد", color = MelovaTextMuted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    } else {
        MelovaSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TeacherIcon()
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "استاد هستید؟", style = MaterialTheme.typography.titleMedium, color = MelovaText)
                        Text(
                            text = "دوره‌ی خودتان را بسازید، درس و بخش اضافه کنید و هنرجوها اجرایشان را با صدای شما مقایسه کنند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSignIn,
                        enabled = state.busyLabel == null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) { Text("ورود با کد استاد") }
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) { Text("درخواست استادی (به‌زودی)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@Composable
private fun TeacherIcon() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MelovaGold.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.CoPresent, contentDescription = null, tint = MelovaGold)
    }
}

@Composable
private fun ThemeSetting(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    MelovaSurfaceCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MelovaCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(20.dp))
                }
                Text(text = "پوسته", style = MaterialTheme.typography.titleMedium, color = MelovaText)
            }
            MelovaSegmentedControl(
                options = listOf("خودکار", "روشن", "تیره"),
                selectedIndex = modes.indexOf(themeMode),
                onSelect = { onThemeModeChange(modes[it]) }
            )
        }
    }
}
