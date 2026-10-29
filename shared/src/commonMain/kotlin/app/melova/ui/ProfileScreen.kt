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
import androidx.compose.runtime.LaunchedEffect
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
import app.melova.platform.appVersionName
import app.melova.data.model.Review
import app.melova.data.model.Teacher
import app.melova.presentation.CatalogUiState
import app.melova.presentation.StudentUiState
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
import app.melova.ui.components.TeacherAvatar
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
    catalogState: CatalogUiState,
    teacherState: TeacherUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFollowChange: (teacherId: String, followed: Boolean) -> Unit,
    onTeacherClick: (teacherId: String) -> Unit,
    onTeacherSignIn: (code: String) -> Unit,
    onTeacherSignOut: () -> Unit,
    onOpenTeacherPanel: () -> Unit,
    studentState: StudentUiState = StudentUiState(),
    onStudentNameChange: (String) -> Unit = {},
    onToggleReply: (Review) -> Unit = {},
    onStudentAppear: () -> Unit = {}
) {
    var showSignIn by rememberSaveable { mutableStateOf(false) }
    var editingName by rememberSaveable { mutableStateOf(false) }

    // The teacher may have answered since the last visit.
    LaunchedEffect(Unit) { onStudentAppear() }

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
                item { StatsRow(catalogState) }
                item { StudentAccountCard(studentState, onEditName = { editingName = true }) }

                if (studentState.reviews.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "پاسخ‌های استاد",
                            trailing = studentState.answeredCount.takeIf { it > 0 }?.let { "$it پاسخ" }
                        )
                    }
                    items(studentState.reviews, key = { "review-" + it.id }) { review ->
                        ReviewReplyCard(
                            review = review,
                            lessonTitle = catalogState.lesson(review.lessonId)?.title,
                            isPlaying = studentState.playingReviewId == review.id,
                            isLoading = studentState.loadingReviewId == review.id,
                            onTogglePlay = { onToggleReply(review) }
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "اساتید من",
                        trailing = catalogState.followedTeachers.size.takeIf { it > 0 }?.let { "$it استاد" }
                    )
                }
                if (catalogState.followedTeachers.isEmpty()) {
                    item {
                        Text(
                            text = "هنوز استادی را دنبال نمی‌کنید. از تب اساتید یکی را دنبال کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                }
                items(catalogState.followedTeachers, key = { it.id }) { teacher ->
                    FollowedTeacherRow(
                        teacher = teacher,
                        practiced = catalogState.practicedSections(teacher.id),
                        total = catalogState.availableSections(teacher.id),
                        onClick = { onTeacherClick(teacher.id) },
                        onUnfollow = { onFollowChange(teacher.id, false) }
                    )
                }

                item { SectionHeader(title = "پنل استاد") }
                item {
                    TeacherModeCard(
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
                        text = "Melova • نسخه‌ی $appVersionName",
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

    if (editingName) {
        var name by rememberSaveable { mutableStateOf(studentState.account?.name.orEmpty()) }
        FormDialog(
            title = "نام شما",
            confirmLabel = "ذخیره",
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                onStudentNameChange(name)
                editingName = false
            },
            onDismiss = { editingName = false }
        ) {
            Text(
                text = "استاد این نام را کنار اجراهایی که می‌فرستید می‌بیند.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            MelovaTextField(value = name, onValueChange = { name = it.take(60) }, label = "نام")
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
private fun StatsRow(state: CatalogUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(value = state.followedTeachers.size.toString(), label = "استاد", modifier = Modifier.weight(1f))
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
private fun FollowedTeacherRow(
    teacher: Teacher,
    practiced: Int,
    total: Int,
    onClick: () -> Unit,
    onUnfollow: () -> Unit
) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TeacherAvatar(teacher.name, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$practiced از $total بخش تمرین شده",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
            }
            FollowButton(isFollowed = true, onFollowChange = { if (!it) onUnfollow() })
        }
    }
}

@Composable
private fun TeacherModeCard(
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
                        Text(text = "مدیریت درس‌ها", style = MaterialTheme.typography.titleMedium, color = MelovaText)
                        Text(
                            text = state.profile?.let { "${it.lessons.size} درس • ${it.lessons.count { l -> l.published }} منتشرشده" }
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
                            text = "درس‌هایتان را بارگذاری یا همین‌جا ضبط کنید؛ هنرجوهایی که شما را دنبال می‌کنند اجرایشان را با صدای شما مقایسه می‌کنند.",
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
