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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Settings
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
import app.melova.platform.formatPersianInteger
import app.melova.platform.persianDigits
import app.melova.platform.rememberInstallOffer
import app.melova.presentation.CatalogUiState
import app.melova.presentation.StudentUiState
import app.melova.presentation.TeacherUiState
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaNavRow
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.StudentAvatar
import app.melova.ui.components.TeacherCard
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

@Composable
fun ProfileScreen(
    catalogState: CatalogUiState,
    teacherState: TeacherUiState,
    onTeacherSignIn: (code: String) -> Unit,
    onOpenTeacherPanel: () -> Unit,
    studentState: StudentUiState,
    onLogin: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenFollowedTeachers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFaq: () -> Unit,
    canChat: Boolean,
    unreadChats: Int,
    onOpenChats: () -> Unit,
    onStudentAppear: () -> Unit
) {
    var showTeacherSignIn by rememberSaveable { mutableStateOf(false) }
    val install = rememberInstallOffer()

    // The account may have changed on another device since the last visit.
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

                if (canChat) {
                    item { ChatsEntryRow(unread = unreadChats, onClick = onOpenChats) }
                }
                if (!studentState.needsLogin) {
                    item { FollowedTeachersRow(count = catalogState.followedTeachers.size, onClick = onOpenFollowedTeachers) }
                }

                item { MelovaNavRow(icon = Icons.Outlined.Settings, title = "تنظیمات", onClick = onOpenSettings) }
                item {
                    MelovaNavRow(icon = Icons.AutoMirrored.Outlined.HelpOutline, title = "پرسش و پاسخ", onClick = onOpenFaq)
                }
                if (install != null) {
                    item {
                        MelovaNavRow(
                            icon = Icons.Outlined.InstallMobile,
                            title = "نصب ملووا",
                            subtitle = "مثل یک اپ، با لوگوی خودش باز می‌شود",
                            onClick = install
                        )
                    }
                }
                item {
                    Text(
                        text = persianDigits("Melova • نسخه‌ی $appVersionName"),
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
                    "کدی را که برای استادی به شما داده شده وارد کنید."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            MelovaTextField(value = code, onValueChange = { code = it }, label = "کد استاد", isSecret = true)
        }
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
                        else -> state.profile?.let { persianDigits("${it.lessons.size} درس، ${it.lessons.count { l -> l.published }} منتشرشده") }
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
private fun FollowedTeachersRow(count: Int, onClick: () -> Unit) {
    MelovaNavRow(
        icon = Icons.Outlined.Groups,
        title = "استادهایی که دنبال می‌کنید",
        subtitle = if (count == 0) "هنوز استادی را دنبال نکرده‌اید" else "${formatPersianInteger(count.toLong())} استاد",
        onClick = onClick
    )
}

/** The teachers the student follows, opened from Profile. */
@Composable
fun FollowedTeachersScreen(
    state: CatalogUiState,
    onTeacherClick: (String) -> Unit,
    onFollowChange: (teacherId: String, followed: Boolean) -> Unit,
    onExploreClick: () -> Unit,
    onBack: () -> Unit
) {
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { MelovaTopBar(title = "استادهایی که دنبال می‌کنید", onBack = onBack) }
                if (state.followedTeachers.isEmpty()) {
                    item {
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
                    }
                }
                items(state.followedTeachers, key = { it.id }) { teacher ->
                    TeacherCard(
                        teacher = teacher,
                        lessonCount = state.lessonsOf(teacher.id).size,
                        isFollowed = true,
                        onFollowChange = { onFollowChange(teacher.id, it) },
                        onClick = { onTeacherClick(teacher.id) }
                    )
                }
            }
        }
    }
}
