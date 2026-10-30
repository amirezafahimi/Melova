package app.melova.ui

import app.melova.platform.LocalFile
import app.melova.platform.PlatformBackHandler as BackHandler
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
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
import app.melova.ui.components.TeacherAvatar
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
    /** Students waiting for the teacher to confirm a card payment. */
    pendingPurchases: Int,
    onOpenLesson: (String) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenStudents: () -> Unit = {},
    onOpenReport: () -> Unit = {}
) {
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var creatingLesson by rememberSaveable { mutableStateOf(false) }
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile

    TeacherScaffold(title = "پنل استاد", subtitle = profile?.name, onBack = onBack, busyLabel = state.busyLabel) {
        if (profile == null) {
            item { LoadingOrRetry(state, onRetry = viewModel::refresh) }
            // A panel that can't load is still a way out.
            item { ExitTeacherModeButton(onClick = { confirmingSignOut = true }) }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TeacherAvatar(profile.name, size = 56.dp)
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
        item { TeacherDeskEntryCard(pendingPurchases, onOpenReviews, onOpenStudents, onOpenReport) }
        item {
            HeaderWithAction(
                title = "درس‌های شما",
                actionLabel = "درس جدید",
                onAction = { creatingLesson = true }
            )
        }
        if (profile.lessons.isEmpty()) {
            item {
                EmptyHint(
                    Icons.Outlined.LibraryMusic,
                    "اولین درستان را بسازید. هر درس یک آموزش کلی و چند بخش برای تمرین دارد؛ بعد از انتشار، همه آن را در «جستجو» می‌بینند."
                )
            }
        }
        itemsIndexed(profile.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            TeacherLessonRow(number = index + 1, lesson = lesson, onClick = { onOpenLesson(lesson.id) })
        }
        item { ExitTeacherModeButton(onClick = { confirmingSignOut = true }) }
    }

    if (confirmingSignOut) {
        ConfirmDialog(
            title = "خروج از حالت استاد",
            message = "درس‌هایتان سر جایشان می‌مانند. برای برگشتن به پنل، دوباره به کد استاد نیاز دارید.",
            confirmLabel = "خروج",
            onConfirm = {
                confirmingSignOut = false
                onSignOut()
            },
            onDismiss = { confirmingSignOut = false }
        )
    }
    if (editingProfile && profile != null) {
        var name by rememberSaveable { mutableStateOf(profile.name) }
        var bio by rememberSaveable { mutableStateOf(profile.bio) }
        var cardNumber by rememberSaveable { mutableStateOf(profile.cardNumber) }
        var cardHolder by rememberSaveable { mutableStateOf(profile.cardHolder) }
        FormDialog(
            title = "پروفایل استاد",
            confirmLabel = "ذخیره",
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                viewModel.updateProfile(name, bio, cardNumber, cardHolder)
                editingProfile = false
            },
            onDismiss = { editingProfile = false }
        ) {
            MelovaTextField(value = name, onValueChange = { name = it }, label = "نام نمایشی")
            MelovaTextField(value = bio, onValueChange = { bio = it }, label = "معرفی کوتاه", singleLine = false, minLines = 3)
            Text(
                text = "هنرجوها قیمت درس را به این کارت واریز می‌کنند.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            MelovaTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it.take(24) },
                label = "شماره کارت",
                keyboardType = KeyboardType.Number,
                leftToRight = true
            )
            MelovaTextField(value = cardHolder, onValueChange = { cardHolder = it.take(120) }, label = "نام صاحب کارت")
        }
    }
    if (creatingLesson) {
        LessonFormDialog(
            title = "درس جدید",
            initial = LessonFields("", "", profile?.lessons?.lastOrNull()?.level ?: "مقدماتی", ""),
            confirmLabel = "ساختن و افزودن صدا",
            onConfirm = {
                creatingLesson = false
                // Straight into the new lesson, where the intro and sections are recorded.
                viewModel.createLesson(it, onCreated = onOpenLesson)
            },
            onDismiss = { creatingLesson = false }
        )
    }
}

@Composable
private fun ExitTeacherModeButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("خروج از حالت استاد", color = MelovaTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TeacherLessonRow(number: Int, lesson: Lesson, onClick: () -> Unit) {
    val ready = lesson.availableSections
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberBadge(number)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PublishChip(lesson.published)
                    Text(
                        text = buildString {
                            append(if (lesson.hasIntro) "آموزش ✓" else "بدون آموزش")
                            append("، $ready بخش")
                            if (!lesson.isFree) append("، ${formatToman(lesson.priceToman)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
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
    val lesson = state.lesson(lessonId)
    var editing by rememberSaveable { mutableStateOf(false) }
    var addingSection by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editingSale by rememberSaveable { mutableStateOf(false) }
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
        subtitle = lesson?.let { if (it.published) "منتشرشده" else "پیش‌نویس" },
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
                        if (lesson.level.isNotBlank()) MelovaChip(text = lesson.level)
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
        item {
            val canPublish = lesson.published || lesson.availableSections > 0
            MelovaSurfaceCard(contentPadding = 14.dp) {
                SwitchRow(
                    title = "انتشار برای هنرجوها",
                    description = when {
                        lesson.published -> "همه این درس را در «جستجو» می‌بینند."
                        canPublish -> "پیش‌نویس است؛ فقط شما آن را می‌بینید."
                        else -> "برای انتشار، دست‌کم یک بخش اضافه کنید."
                    },
                    checked = lesson.published,
                    enabled = canPublish && state.busyLabel == null,
                    onCheckedChange = { viewModel.setLessonPublished(lesson.id, it) }
                )
            }
        }
        item {
            SaleCard(
                lesson = lesson,
                hasCard = state.profile?.cardNumber?.isNotBlank() == true,
                onEdit = { editingSale = true }
            )
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

    if (editingSale && lesson != null) {
        SaleDialog(
            lesson = lesson,
            onConfirm = { sku, price ->
                editingSale = false
                viewModel.setLessonSale(lesson.id, sku, price)
            },
            onDismiss = { editingSale = false }
        )
    }
    if (editing && lesson != null) {
        LessonFormDialog(
            title = "ویرایش درس",
            initial = LessonFields(lesson.title, lesson.subtitle, lesson.level, lesson.description),
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
private fun LessonFormDialog(
    title: String,
    initial: LessonFields,
    confirmLabel: String,
    onConfirm: (LessonFields) -> Unit,
    onDismiss: () -> Unit
) {
    var lessonTitle by rememberSaveable { mutableStateOf(initial.title) }
    var subtitle by rememberSaveable { mutableStateOf(initial.subtitle) }
    var level by rememberSaveable { mutableStateOf(initial.level) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    FormDialog(
        title = title,
        confirmLabel = confirmLabel,
        confirmEnabled = lessonTitle.isNotBlank(),
        onConfirm = { onConfirm(LessonFields(lessonTitle.trim(), subtitle.trim(), level.trim(), description.trim())) },
        onDismiss = onDismiss
    ) {
        MelovaTextField(value = lessonTitle, onValueChange = { lessonTitle = it }, label = "عنوان درس *")
        MelovaTextField(value = subtitle, onValueChange = { subtitle = it }, label = "زیرعنوان")
        MelovaTextField(value = level, onValueChange = { level = it }, label = "سطح (مثلاً مقدماتی)")
        MelovaTextField(value = description, onValueChange = { description = it }, label = "توضیحات", singleLine = false, minLines = 3)
    }
}

@Composable
private fun SectionFormDialog(
    suggestedTitle: String,
    busyLabel: String?,
    onConfirm: (title: String, description: String, audio: LocalFile, onFailure: (String) -> Unit) -> Unit,
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

@Composable
private fun SaleCard(lesson: Lesson, hasCard: Boolean, onEdit: () -> Unit) {
    MelovaSurfaceCard(onClick = onEdit, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lesson.priceToman > 0) "قیمت: ${formatToman(lesson.priceToman)}" else "درس رایگان",
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText
                )
                Text(
                    text = when {
                        lesson.isFree && lesson.priceToman > 0 -> "فعلاً همه‌ی درس‌ها رایگان است و این قیمت برای بعد می‌ماند."
                        lesson.isFree -> "همه می‌توانند تمرینش کنند. برای فروش، قیمت بگذارید."
                        !hasCard -> "برای شروع فروش، شماره کارتتان را در پروفایل استاد بگذارید."
                        else -> "هنرجو کارت‌به‌کارت می‌خرد و شما تأیید می‌کنید."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!lesson.isFree && !hasCard) MelovaError else MelovaTextMuted
                )
            }
            Icon(Icons.Outlined.Edit, contentDescription = "قیمت درس", tint = MelovaAccentSoft)
        }
    }
}

/** Price 0 makes the lesson free again. */
@Composable
private fun SaleDialog(lesson: Lesson, onConfirm: (sku: String, priceToman: Long) -> Unit, onDismiss: () -> Unit) {
    var priceText by rememberSaveable { mutableStateOf(lesson.priceToman.takeIf { it > 0 }?.toString().orEmpty()) }
    var sku by rememberSaveable { mutableStateOf(lesson.bazaarSku) }
    val price = priceText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    FormDialog(
        title = "قیمت درس",
        confirmLabel = "ذخیره",
        onConfirm = { onConfirm(if (price > 0) sku else "", price) },
        onDismiss = onDismiss
    ) {
        Text(
            text = "قیمت را خالی بگذارید تا درس رایگان بماند. هنرجو مبلغ را به کارت شما واریز می‌کند و شما در «هنرجوها و پرداخت‌ها» تأیید می‌کنید تا درس برایش باز شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MelovaTextMuted
        )
        MelovaTextField(value = priceText, onValueChange = { priceText = it.take(12) }, label = "قیمت (تومان)")
        if (price > 0) Text(formatToman(price), style = MaterialTheme.typography.bodySmall, color = MelovaAccentSoft)
        MelovaTextField(value = sku, onValueChange = { sku = it.trim().take(80) }, label = "شناسه محصول کافه‌بازار (بعداً)")
    }
}

// ------------------------------------------------------------------ shared pieces

@Composable
internal fun TeacherScaffold(
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
private fun IntroCard(hasIntro: Boolean, take: TakeController, busy: Boolean, onUpload: (LocalFile) -> Unit) {
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
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MelovaText)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
        MelovaSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun MelovaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
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
internal fun EmptyHint(icon: ImageVector, text: String) {
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
