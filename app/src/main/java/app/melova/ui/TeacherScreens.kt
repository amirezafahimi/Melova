package app.melova.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Course
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.repository.CourseFields
import app.melova.data.repository.LessonFields
import app.melova.presentation.TeacherUiState
import app.melova.presentation.TeacherViewModel
import app.melova.ui.components.BusyOverlay
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.TakeController
import app.melova.ui.components.TakeKind
import app.melova.ui.components.TakePhase
import app.melova.ui.components.TeacherAudioSlot
import app.melova.ui.components.rememberTakeController
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

// ------------------------------------------------------------------ panel

@Composable
fun TeacherPanelScreen(
    state: TeacherUiState,
    viewModel: TeacherViewModel,
    followedIds: Set<String>,
    onFollowChange: (courseId: String, followed: Boolean) -> Unit,
    onOpenCourse: (String) -> Unit,
    onBack: () -> Unit
) {
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var creatingCourse by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile

    TeacherScaffold(title = "پنل استاد", subtitle = profile?.name, onBack = onBack, busyLabel = state.busyLabel) {
        if (profile == null) {
            item { LoadingOrRetry(state, onRetry = viewModel::refresh) }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.name.take(1), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleLarge, color = MelovaText)
                        Text(
                            text = profile.bio.ifBlank { "معرفی کوتاهی از خودتان بنویسید." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { editingProfile = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "ویرایش پروفایل استاد", tint = MelovaAccentSoft)
                    }
                }
            }
        }
        item {
            HeaderWithAction(
                title = "دوره‌های شما",
                actionLabel = "دوره‌ی جدید",
                onAction = { creatingCourse = true }
            )
        }
        if (profile.courses.isEmpty()) {
            item { EmptyHint(Icons.Outlined.LibraryMusic, "اولین دوره‌تان را بسازید؛ بعد درس‌ها و بخش‌ها را به آن اضافه می‌کنید.") }
        }
        profile.courses.forEach { course ->
            item(key = course.id) {
                TeacherCourseCard(
                    course = course,
                    isFollowed = course.id in followedIds,
                    onFollowChange = { onFollowChange(course.id, it) },
                    onClick = { onOpenCourse(course.id) }
                )
            }
        }
    }

    if (editingProfile && profile != null) {
        var name by rememberSaveable { mutableStateOf(profile.name) }
        var bio by rememberSaveable { mutableStateOf(profile.bio) }
        FormDialog(
            title = "پروفایل استاد",
            confirmLabel = "ذخیره",
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                viewModel.updateProfile(name, bio)
                editingProfile = false
            },
            onDismiss = { editingProfile = false }
        ) {
            MelovaTextField(value = name, onValueChange = { name = it }, label = "نام نمایشی")
            MelovaTextField(value = bio, onValueChange = { bio = it }, label = "معرفی کوتاه", singleLine = false, minLines = 3)
        }
    }
    if (creatingCourse) {
        CourseFormDialog(
            title = "دوره‌ی جدید",
            initial = CourseFields("", "", "مقدماتی", ""),
            confirmLabel = "ساختن",
            onConfirm = {
                viewModel.createCourse(it)
                creatingCourse = false
            },
            onDismiss = { creatingCourse = false }
        )
    }
}

@Composable
private fun TeacherCourseCard(
    course: Course,
    isFollowed: Boolean,
    onFollowChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    MelovaSurfaceCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${course.lessons.size} درس • ${course.sectionCount} بخش",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PublishChip(course.published)
                if (course.level.isNotBlank()) MelovaChip(text = course.level)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Text("در تب تمرین", style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted)
                MelovaSwitch(checked = isFollowed, onCheckedChange = onFollowChange)
            }
        }
    }
}

// ------------------------------------------------------------------ course

@Composable
fun CourseEditorScreen(
    courseId: String,
    state: TeacherUiState,
    viewModel: TeacherViewModel,
    isFollowed: Boolean,
    onFollowChange: (Boolean) -> Unit,
    onOpenLesson: (String) -> Unit,
    onBack: () -> Unit
) {
    val course = state.course(courseId)
    var editing by rememberSaveable { mutableStateOf(false) }
    var addingLesson by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    TeacherScaffold(
        title = course?.title ?: "دوره",
        subtitle = "ویرایش دوره",
        onBack = onBack,
        busyLabel = state.busyLabel
    ) {
        if (course == null) {
            item { LoadingOrRetry(state, onRetry = viewModel::refresh, missingMessage = "این دوره حذف شده است.") }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(course.title, style = MaterialTheme.typography.titleLarge, color = MelovaText)
                            if (course.subtitle.isNotBlank()) {
                                Text(course.subtitle, style = MaterialTheme.typography.bodyMedium, color = MelovaAccentSoft)
                            }
                        }
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "ویرایش اطلاعات دوره", tint = MelovaAccentSoft)
                        }
                    }
                    if (course.level.isNotBlank()) MelovaChip(text = course.level)
                    Text(
                        text = course.description.ifBlank { "توضیحی برای این دوره ننوشته‌اید." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
        }
        item {
            MelovaSurfaceCard(contentPadding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SwitchRow(
                        title = "انتشار برای هنرجوها",
                        description = if (course.published) {
                            "همه‌ی هنرجوها این دوره را می‌بینند."
                        } else {
                            "پیش‌نویس است؛ فقط شما آن را می‌بینید."
                        },
                        checked = course.published,
                        onCheckedChange = { viewModel.setCoursePublished(course.id, it) }
                    )
                    SwitchRow(
                        title = "نمایش در تب تمرین من",
                        description = "دوره را مثل یک هنرجو دنبال کنید تا خودتان تمرینش کنید.",
                        checked = isFollowed,
                        onCheckedChange = onFollowChange
                    )
                }
            }
        }
        item { HeaderWithAction(title = "درس‌ها", actionLabel = "درس جدید", onAction = { addingLesson = true }) }
        if (course.lessons.isEmpty()) {
            item { EmptyHint(Icons.Outlined.GraphicEq, "درسی ندارید. هر درس یک آموزش کلی و چند بخش برای تمرین دارد.") }
        }
        itemsIndexed(course.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            TeacherLessonRow(number = index + 1, lesson = lesson, onClick = { onOpenLesson(lesson.id) })
        }
        item { DangerButton(label = "حذف دوره", onClick = { confirmDelete = true }) }
    }

    if (editing && course != null) {
        CourseFormDialog(
            title = "ویرایش دوره",
            initial = CourseFields(course.title, course.subtitle, course.level, course.description),
            confirmLabel = "ذخیره",
            onConfirm = {
                viewModel.updateCourse(course.id, it)
                editing = false
            },
            onDismiss = { editing = false }
        )
    }
    if (addingLesson && course != null) {
        LessonFormDialog(
            title = "درس جدید",
            initial = LessonFields("", "", ""),
            confirmLabel = "ساختن",
            onConfirm = {
                viewModel.createLesson(course.id, it)
                addingLesson = false
            },
            onDismiss = { addingLesson = false }
        )
    }
    if (confirmDelete && course != null) {
        ConfirmDialog(
            title = "حذف دوره؟",
            message = "«${course.title}» با همه‌ی درس‌ها و فایل‌های صوتی‌اش حذف می‌شود و برنمی‌گردد.",
            confirmLabel = "حذف",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteCourse(course.id)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun TeacherLessonRow(number: Int, lesson: Lesson, onClick: () -> Unit) {
    val ready = lesson.sections.count { it.isAvailable }
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberBadge(number)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (lesson.hasIntro) "آموزش کلی ✓" else "بدون آموزش کلی")
                        append(" • $ready بخش")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
        }
    }
}

// ------------------------------------------------------------------ lesson

@Composable
fun LessonEditorScreen(
    lessonId: String,
    state: TeacherUiState,
    viewModel: TeacherViewModel,
    onPreview: () -> Unit,
    onBack: () -> Unit
) {
    val found = state.lesson(lessonId)
    val lesson = found?.second
    var editing by rememberSaveable { mutableStateOf(false) }
    var addingSection by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<LessonSection?>(null) }
    val introTake = rememberTakeController(TakeKind.INTRO)
    // Leaving the screen drops a take that isn't uploaded yet, so ask first.
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val leave: (() -> Unit) -> Unit = { action ->
        if (introTake.hasUnsavedWork) pendingLeave = action else action()
    }
    BackHandler(enabled = introTake.hasUnsavedWork) { pendingLeave = onBack }

    TeacherScaffold(
        title = lesson?.title ?: "درس",
        subtitle = found?.first?.title,
        onBack = { leave(onBack) },
        busyLabel = state.busyLabel
    ) {
        if (lesson == null) {
            item { LoadingOrRetry(state, onRetry = viewModel::refresh, missingMessage = "این درس حذف شده است.") }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleLarge, color = MelovaText)
                        if (lesson.subtitle.isNotBlank()) {
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium, color = MelovaAccentSoft)
                        }
                        Text(
                            text = lesson.description.ifBlank { "توضیحی برای این درس ننوشته‌اید." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "ویرایش اطلاعات درس", tint = MelovaAccentSoft)
                    }
                }
            }
        }
        item { SectionHeader(title = "آموزش کلی") }
        item {
            IntroCard(
                hasIntro = lesson.hasIntro,
                take = introTake,
                busy = state.busyLabel != null,
                onUpload = { audio -> viewModel.uploadIntro(lesson.id, audio, onSuccess = introTake::clear) }
            )
        }
        item {
            HeaderWithAction(title = "بخش‌های تمرین", actionLabel = "بخش جدید", onAction = { addingSection = true })
        }
        if (lesson.sections.isEmpty()) {
            item {
                EmptyHint(
                    Icons.Outlined.GraphicEq,
                    "هر بخش، اجرای شما از قسمتی از آهنگ است. هنرجو همان را می‌خواند و سیستم با صدای شما مقایسه می‌کند."
                )
            }
        }
        itemsIndexed(lesson.sections, key = { _, section -> section.id }) { index, section ->
            TeacherSectionRow(number = index + 1, section = section, onDelete = { sectionToDelete = section })
        }
        if (lesson.sections.any { it.isAvailable }) {
            item {
                OutlinedButton(
                    onClick = { leave(onPreview) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("دیدن درس از نگاه هنرجو", color = MelovaAccentSoft) }
            }
        }
        item { DangerButton(label = "حذف درس", onClick = { confirmDelete = true }) }
    }

    if (editing && lesson != null) {
        LessonFormDialog(
            title = "ویرایش درس",
            initial = LessonFields(lesson.title, lesson.subtitle, lesson.description),
            confirmLabel = "ذخیره",
            onConfirm = {
                viewModel.updateLesson(lesson.id, it)
                editing = false
            },
            onDismiss = { editing = false }
        )
    }
    if (addingSection && lesson != null) {
        SectionFormDialog(
            suggestedTitle = "بخش ${lesson.sections.size + 1}",
            busyLabel = state.busyLabel,
            onConfirm = { title, description, audio, onFailure ->
                viewModel.addSection(
                    lesson.id,
                    title,
                    description,
                    audio,
                    onSuccess = { addingSection = false },
                    onFailure = onFailure
                )
            },
            onDismiss = { addingSection = false }
        )
    }
    pendingLeave?.let { action ->
        ConfirmDialog(
            title = "ضبط ذخیره نشده",
            message = "آموزشی که ضبط یا انتخاب کرده‌اید هنوز بارگذاری نشده و با خروج از این صفحه از بین می‌رود.",
            confirmLabel = "خروج بدون ذخیره",
            onConfirm = {
                pendingLeave = null
                introTake.clear()
                action()
            },
            onDismiss = { pendingLeave = null }
        )
    }
    sectionToDelete?.let { section ->
        ConfirmDialog(
            title = "حذف بخش؟",
            message = "«${section.title}» و فایل صوتی‌اش حذف می‌شود. نمره‌های هنرجوها برای این بخش دیگر نمایش داده نمی‌شود.",
            confirmLabel = "حذف",
            onConfirm = {
                if (lesson != null) viewModel.deleteSection(lesson.id, section.id)
                sectionToDelete = null
            },
            onDismiss = { sectionToDelete = null }
        )
    }
    if (confirmDelete && lesson != null) {
        ConfirmDialog(
            title = "حذف درس؟",
            message = "«${lesson.title}» با آموزش و همه‌ی بخش‌هایش حذف می‌شود و برنمی‌گردد.",
            confirmLabel = "حذف",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteLesson(lesson.id)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun TeacherSectionRow(number: Int, section: LessonSection, onDelete: () -> Unit) {
    MelovaSurfaceCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberBadge(number)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (section.isAvailable) {
                        "${(section.durationMs + 999) / 1000} ثانیه"
                    } else {
                        "فایل صوتی پیدا نشد"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (section.isAvailable) MelovaTextMuted else MelovaError
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف بخش", tint = MelovaError)
            }
        }
    }
}

// ------------------------------------------------------------------ dialogs

@Composable
private fun CourseFormDialog(
    title: String,
    initial: CourseFields,
    confirmLabel: String,
    onConfirm: (CourseFields) -> Unit,
    onDismiss: () -> Unit
) {
    var courseTitle by rememberSaveable { mutableStateOf(initial.title) }
    var subtitle by rememberSaveable { mutableStateOf(initial.subtitle) }
    var level by rememberSaveable { mutableStateOf(initial.level) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    FormDialog(
        title = title,
        confirmLabel = confirmLabel,
        confirmEnabled = courseTitle.isNotBlank(),
        onConfirm = { onConfirm(CourseFields(courseTitle.trim(), subtitle.trim(), level.trim(), description.trim())) },
        onDismiss = onDismiss
    ) {
        MelovaTextField(value = courseTitle, onValueChange = { courseTitle = it }, label = "عنوان دوره *")
        MelovaTextField(value = subtitle, onValueChange = { subtitle = it }, label = "زیرعنوان")
        MelovaTextField(value = level, onValueChange = { level = it }, label = "سطح (مثلاً مقدماتی)")
        MelovaTextField(value = description, onValueChange = { description = it }, label = "توضیحات", singleLine = false, minLines = 3)
    }
}

@Composable
private fun LessonFormDialog(
    title: String,
    initial: LessonFields,
    confirmLabel: String,
    onConfirm: (LessonFields) -> Unit,
    onDismiss: () -> Unit
) {
    var lessonTitle by rememberSaveable { mutableStateOf(initial.title) }
    var subtitle by rememberSaveable { mutableStateOf(initial.subtitle) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    FormDialog(
        title = title,
        confirmLabel = confirmLabel,
        confirmEnabled = lessonTitle.isNotBlank(),
        onConfirm = { onConfirm(LessonFields(lessonTitle.trim(), subtitle.trim(), description.trim())) },
        onDismiss = onDismiss
    ) {
        MelovaTextField(value = lessonTitle, onValueChange = { lessonTitle = it }, label = "عنوان درس *")
        MelovaTextField(value = subtitle, onValueChange = { subtitle = it }, label = "زیرعنوان")
        MelovaTextField(value = description, onValueChange = { description = it }, label = "توضیحات", singleLine = false, minLines = 3)
    }
}

@Composable
private fun SectionFormDialog(
    suggestedTitle: String,
    busyLabel: String?,
    onConfirm: (title: String, description: String, audio: Uri, onFailure: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(suggestedTitle) }
    var description by rememberSaveable { mutableStateOf("") }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val take = rememberTakeController(TakeKind.SECTION)
    val busy = busyLabel != null
    FormDialog(
        title = "بخش جدید",
        confirmLabel = "بارگذاری",
        confirmEnabled = title.isNotBlank() && take.canUpload,
        busy = busy,
        // A stray tap outside must not throw away a take.
        dismissOnOutsideTap = !take.hasUnsavedWork,
        onConfirm = {
            take.audio?.let { audio ->
                uploadError = null
                take.pauseAll()
                onConfirm(title.trim(), description.trim(), audio) { uploadError = it }
            }
        },
        onDismiss = onDismiss
    ) {
        MelovaTextField(value = title, onValueChange = { title = it }, label = "عنوان بخش *")
        MelovaTextField(value = description, onValueChange = { description = it }, label = "راهنمای هنرجو", singleLine = false, minLines = 2)
        Text("اجرای شما *", style = MaterialTheme.typography.titleSmall, color = MelovaText)
        TeacherAudioSlot(controller = take, enabled = !busy)
        when {
            busyLabel != null -> Text(busyLabel, style = MaterialTheme.typography.bodySmall, color = MelovaAccentSoft)
            uploadError != null -> Text(uploadError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MelovaError)
            take.phase == TakePhase.EMPTY -> Text(
                text = "فقط صدای خواندن خودتان، بدون ساز یا با ساز خیلی آرام، در جای ساکت. هنرجو همین را می‌خواند و با صدای شما مقایسه می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
    }
}

// ------------------------------------------------------------------ shared pieces

@Composable
private fun TeacherScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    busyLabel: String?,
    content: LazyListScope.() -> Unit
) {
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { MelovaTopBar(title = title, subtitle = subtitle, onBack = onBack) }
                content()
            }
        }
        BusyOverlay(busyLabel)
    }
}

@Composable
private fun LoadingOrRetry(state: TeacherUiState, onRetry: () -> Unit, missingMessage: String? = null) {
    if (state.isLoading || (state.profile == null && state.message == null)) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MelovaAccent)
        }
    } else {
        MessageCard(
            icon = Icons.Outlined.LibraryMusic,
            message = if (state.profile != null && missingMessage != null) missingMessage else "اطلاعات پنل دریافت نشد.",
            actionLabel = "تلاش دوباره",
            onAction = onRetry
        )
    }
}

@Composable
private fun HeaderWithAction(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MelovaText, modifier = Modifier.weight(1f))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" $actionLabel", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun IntroCard(hasIntro: Boolean, take: TakeController, busy: Boolean, onUpload: (Uri) -> Unit) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (hasIntro) Icons.Outlined.CheckCircle else Icons.Outlined.AudioFile,
                    contentDescription = null,
                    tint = if (hasIntro) MelovaSuccess else MelovaGold,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasIntro) "آموزش این درس آماده است" else "هنوز آموزشی برای این درس ندارید",
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText
                    )
                    Text(
                        text = if (hasIntro) {
                            "هنرجو پیش از تمرین آن را گوش می‌دهد. برای جایگزینی، دوباره ضبط کنید یا فایل دیگری بدهید."
                        } else {
                            "توضیح صوتی شما درباره‌ی کل درس؛ همین‌جا ضبط کنید یا فایلش را انتخاب کنید."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
            TeacherAudioSlot(controller = take, enabled = !busy)
            if (take.phase == TakePhase.READY) {
                Button(
                    onClick = { take.audio?.let(onUpload) },
                    enabled = take.canUpload && !busy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(if (hasIntro) "  جایگزینی آموزش فعلی" else "  بارگذاری آموزش", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MelovaText)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
        MelovaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MelovaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(checkedTrackColor = MelovaAccent, checkedThumbColor = Color.White)
    )
}

@Composable
private fun PublishChip(published: Boolean) {
    if (published) MelovaChip(text = "منتشرشده", color = MelovaSuccess) else MelovaChip(text = "پیش‌نویس", color = MelovaGold)
}

@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MelovaCardElevated),
        contentAlignment = Alignment.Center
    ) {
        Text(number.toString(), style = MaterialTheme.typography.titleMedium, color = MelovaAccentSoft)
    }
}

@Composable
private fun EmptyHint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MelovaCardElevated.copy(alpha = 0.6f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MelovaAccentSoft)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
    }
}

@Composable
private fun DangerButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MelovaError.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(46.dp)
    ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MelovaError)
        Text("  $label", color = MelovaError)
    }
}
