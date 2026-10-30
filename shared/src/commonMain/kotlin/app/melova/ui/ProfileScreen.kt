package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.platform.appVersionName
import app.melova.data.model.Review
import app.melova.data.model.Teacher
import app.melova.presentation.CatalogUiState
import app.melova.presentation.StudentUiState
import app.melova.presentation.TeacherUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.StudentAvatar
import app.melova.ui.components.TeacherAvatar
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.ThemeMode

@Composable
fun ProfileScreen(
    catalogState: CatalogUiState,
    teacherState: TeacherUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTeacherSignIn: (code: String) -> Unit,
    onOpenTeacherPanel: () -> Unit,
    studentState: StudentUiState,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit,
    onTeacherClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    onToggleReply: (Review) -> Unit,
    onStudentAppear: () -> Unit
) {
    var showTeacherSignIn by rememberSaveable { mutableStateOf(false) }
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

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
                item {
                    if (studentState.needsLogin) {
                        LoginCard(onLogin = onLogin)
                    } else {
                        IdentityCard(studentState, onEdit = onEditProfile)
                    }
                }
                // A teacher signs in like anyone else; only then can they say they teach.
                if (teacherState.isSignedIn || !studentState.needsLogin) {
                    item {
                        TeacherPanelCard(
                            state = teacherState,
                            onSignIn = { showTeacherSignIn = true },
                            onOpenPanel = onOpenTeacherPanel
                        )
                    }
                }

                if (!studentState.needsLogin) {
                    item { SectionHeader(title = "استادهایی که دنبال می‌کنید") }
                    item {
                        FollowedTeachers(
                            teachers = catalogState.followedTeachers,
                            onTeacherClick = onTeacherClick,
                            onExploreClick = onExploreClick
                        )
                    }
                }

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

                item { SectionHeader(title = "تنظیمات") }
                item { ThemePicker(themeMode = themeMode, onThemeModeChange = onThemeModeChange) }
                if (studentState.account?.signedIn == true) {
                    item { SignOutRow(onClick = { confirmingSignOut = true }) }
                }
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

    if (showTeacherSignIn) {
        var code by rememberSaveable { mutableStateOf("") }
        FormDialog(
            title = "ورود به پنل استاد",
            confirmLabel = "ورود",
            confirmEnabled = code.isNotBlank(),
            onConfirm = {
                onTeacherSignIn(code)
                showTeacherSignIn = false
            },
            onDismiss = { showTeacherSignIn = false }
        ) {
            Text(
                text = if (studentState.account?.signedIn == true) {
                    "کد استاد فقط همین یک بار لازم است. بعد از این با ورود به حسابتان، پنل را روی هر گوشی‌ای می‌بینید."
                } else {
                    "کد استادی که روی سرور تعریف شده را وارد کنید."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            MelovaTextField(value = code, onValueChange = { code = it }, label = "کد استاد", isSecret = true)
        }
    }

    if (confirmingSignOut) {
        ConfirmDialog(
            title = "خروج از حساب",
            message = "درس‌ها و پاسخ‌های استاد روی حسابتان می‌مانند و با ورود دوباره برمی‌گردند.",
            confirmLabel = "خروج",
            onConfirm = {
                confirmingSignOut = false
                onSignOut()
            },
            onDismiss = { confirmingSignOut = false }
        )
    }
}

@Composable
private fun IdentityCard(state: StudentUiState, onEdit: () -> Unit) {
    val account = state.account
    val name = account?.name.orEmpty()
    MelovaSurfaceCard(onClick = onEdit, enabled = account != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudentAvatar(name, state.photo, size = 72.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name.ifBlank { "هنرجوی ملووا" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = account?.signedInAs ?: "نام و عکس خود را بگذارید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Outlined.Edit, contentDescription = "ویرایش پروفایل", tint = MelovaTextMuted)
        }
    }
}

/** Stands in for the name and photo: there are only two states, signed in or not. */
@Composable
private fun LoginCard(onLogin: () -> Unit) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("هنوز وارد نشده‌اید", style = MaterialTheme.typography.titleLarge, color = MelovaText)
            Text(
                text = "بدون ورود هم می‌توانید تمرین کنید. برای فرستادن تمرین برای استاد باید وارد شوید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted
            )
            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) { Text("ورود یا ثبت‌نام") }
        }
    }
}

@Composable
private fun TeacherPanelCard(state: TeacherUiState, onSignIn: () -> Unit, onOpenPanel: () -> Unit) {
    MelovaSurfaceCard(
        onClick = if (state.isSignedIn) onOpenPanel else onSignIn,
        enabled = state.isSignedIn || state.busyLabel == null,
        contentPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MelovaGold.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CoPresent, contentDescription = null, tint = MelovaGold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.isSignedIn) "پنل استاد" else "استاد هستید؟",
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText
                )
                Text(
                    text = when {
                        !state.isSignedIn -> "ورود با کد استاد"
                        else -> state.profile?.let { "${it.lessons.size} درس، ${it.lessons.count { l -> l.published }} منتشرشده" }
                            ?: "در حال بارگذاری…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
        }
    }
}

@Composable
private fun FollowedTeachers(
    teachers: List<Teacher>,
    onTeacherClick: (String) -> Unit,
    onExploreClick: () -> Unit
) {
    if (teachers.isEmpty()) {
        MelovaSurfaceCard(onClick = onExploreClick, contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "هنوز استادی را دنبال نکرده‌اید. در «جستجو» پیدایشان کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelovaTextMuted,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
            }
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(teachers, key = { it.id }) { teacher ->
            Column(
                modifier = Modifier
                    .width(78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTeacherClick(teacher.id) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TeacherAvatar(teacher.name, size = 60.dp)
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SignOutRow(onClick: () -> Unit) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MelovaTextMuted)
            Text("خروج از حساب", style = MaterialTheme.typography.titleSmall, color = MelovaText)
        }
    }
}
