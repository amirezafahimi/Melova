package app.melova.ui

import app.melova.ui.components.ForwardArrow
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.presentation.ListenUiState
import app.melova.presentation.TeacherListenViewModel
import app.melova.ui.components.ListenButton
import app.melova.ui.components.MelovaSwitch
import app.melova.ui.components.SeekLine
import app.melova.ui.components.formatClock
import app.melova.ui.layout.LtrContent
import app.melova.ui.theme.MelovaDimens
import app.melova.platform.LocalFile
import app.melova.platform.PlatformBackHandler as BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaCardShape
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaPillShape
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.components.MelovaNavRow
import app.melova.ui.components.MelovaGroupRow
import app.melova.ui.components.MelovaNavGroup
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.repository.LessonFields
import app.melova.platform.digitsOnly
import app.melova.platform.persianDigits
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
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaControl
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
    onNewLesson: () -> Unit,
    onEditProfile: () -> Unit,
    onBack: () -> Unit,
    /** Null when the panel belongs to an account, which can't stop being a teacher. */
    onSignOut: (() -> Unit)? = null,
    unreadChats: Int = 0,
    onOpenChats: () -> Unit = {},
    onOpenStudents: () -> Unit = {},
    onOpenReport: () -> Unit = {}
) {
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile

    TeacherScaffold(title = "پنل استاد", subtitle = profile?.name, onBack = onBack, busyLabel = state.busyLabel) {
        if (profile == null) {
            item { LoadingOrRetry(state, onRetry = viewModel::refresh) }
            // A panel that can't load is still a way out.
            if (onSignOut != null) item { ExitTeacherModeButton(onClick = { confirmingSignOut = true }) }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard(onClick = onEditProfile) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeacherAvatar(profile.name, size = 56.dp, teacherId = profile.id, photoVersion = profile.photoVersion)
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
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Outlined.Edit, contentDescription = "ویرایش پروفایل استاد", tint = MelovaAccentSoft)
                    }
                }
            }
        }
        item {
            TeacherDeskEntryCard(
                pendingPurchases = pendingPurchases,
                unreadChats = unreadChats,
                onOpenChats = onOpenChats,
                onOpenStudents = onOpenStudents,
                onOpenReport = onOpenReport
            )
        }
        item {
            HeaderWithAction(
                title = "درس‌های شما",
                actionLabel = "درس جدید",
                onAction = onNewLesson
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
        if (onSignOut != null) item { ExitTeacherModeButton(onClick = { confirmingSignOut = true }) }
    }

    if (confirmingSignOut) {
        ConfirmDialog(
            title = "خروج از حالت استاد",
            message = "درس‌هایتان سر جایشان می‌مانند. برای برگشتن به پنل، دوباره به کد استاد نیاز دارید.",
            confirmLabel = "خروج",
            onConfirm = {
                confirmingSignOut = false
                onSignOut?.invoke()
            },
            onDismiss = { confirmingSignOut = false }
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
    MelovaSurfaceCard(onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberBadge(number)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PublishChip(lesson.published)
                    Text(
                        text = buildString {
                            append(if (lesson.hasIntro) "آموزش ✓" else "بدون آموزش")
                            append(persianDigits("، $ready بخش"))
                            if (!lesson.isFree) append("، ${formatToman(lesson.priceToman)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
            ForwardArrow(tint = MelovaTextMuted)
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
    onEdit: () -> Unit,
    onAddSection: () -> Unit,
    onEditSection: (String) -> Unit,
    onBack: () -> Unit,
    listenViewModel: TeacherListenViewModel = viewModel { TeacherListenViewModel() }
) {
    val lesson = state.lesson(lessonId)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editingSale by rememberSaveable { mutableStateOf(false) }
    val listen by listenViewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { listenViewModel.pause() } }
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
        busyLabel = state.busyLabel,
        uploadProgress = state.uploadProgress
    ) {
        if (lesson == null) {
            item { LoadingOrRetry(state, onRetry = viewModel::refresh, missingMessage = "این درس حذف شده است.") }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard(onClick = onEdit) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MelovaText)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PublishChip(lesson.published)
                            if (lesson.level.isNotBlank()) MelovaChip(text = lesson.level)
                            if (lesson.subtitle.isNotBlank()) MelovaChip(text = "خواننده: ${lesson.subtitle}", color = MelovaGold)
                        }
                        Text(
                            text = lesson.description.ifBlank { "توضیحی برای این درس ننوشته‌اید." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش اطلاعات درس", tint = MelovaAccentSoft, modifier = Modifier.size(MelovaDimens.Icon))
                }
            }
        }
        item {
            // Who sees the lesson and what it costs, together: both decide who gets it.
            val canPublish = lesson.published || lesson.availableSections > 0
            val hasCard = state.profile?.cardNumber?.isNotBlank() == true
            MelovaNavGroup(
                {
                    MelovaGroupRow(
                        icon = Icons.Outlined.Public,
                        title = "انتشار برای هنرجوها",
                        subtitle = when {
                            lesson.published -> "همه این درس را در «جستجو» می‌بینند."
                            canPublish -> "پیش‌نویس است؛ فقط شما آن را می‌بینید."
                            else -> "برای انتشار، دست‌کم یک بخش اضافه کنید."
                        },
                        onClick = { if (canPublish) viewModel.setLessonPublished(lesson.id, !lesson.published) },
                        showArrow = false,
                        iconTint = if (lesson.published) MelovaSuccess else MelovaAccentSoft,
                        enabled = canPublish && state.busyLabel == null,
                        trailing = {
                            MelovaSwitch(
                                checked = lesson.published,
                                onCheckedChange = { viewModel.setLessonPublished(lesson.id, it) },
                                enabled = canPublish && state.busyLabel == null
                            )
                        }
                    )
                },
                {
                    MelovaGroupRow(
                        icon = Icons.Outlined.Sell,
                        title = if (lesson.priceToman > 0) "قیمت: ${formatToman(lesson.priceToman)}" else "درس رایگان",
                        subtitle = saleNote(lesson, hasCard),
                        onClick = { editingSale = true },
                        iconTint = if (!lesson.isFree && !hasCard) MelovaError else MelovaGold
                    )
                }
            )
        }
        item { SectionHeader(title = "آموزش کلی") }
        item {
            IntroCard(
                lesson = lesson,
                take = introTake,
                listen = listen,
                busy = state.busyLabel != null,
                onListen = {
                    introTake.pauseAll()
                    listenViewModel.toggleIntro(lesson)
                },
                onSeek = listenViewModel::seekTo,
                onSkip = listenViewModel::skipBy,
                onReplace = listenViewModel::pause,
                onUpload = { audio -> viewModel.uploadIntro(lesson.id, audio, onSuccess = introTake::clear) }
            )
        }
        item {
            HeaderWithAction(title = "بخش‌های تمرین", actionLabel = "بخش جدید", onAction = { leave(onAddSection) })
        }
        if (lesson.sections.isEmpty()) {
            item {
                EmptyHint(
                    Icons.Outlined.GraphicEq,
                    "هر بخش، اجرای شما از قسمتی از آهنگ است. هنرجو همان را می‌خواند و سیستم با صدای شما مقایسه می‌کند."
                )
            }
        }
        if (lesson.sections.isNotEmpty()) {
            item {
                MelovaNavGroup(
                    *lesson.sections.map { section ->
                        @Composable {
                            TeacherSectionRow(
                                section = section,
                                listen = listen,
                                onListen = {
                                    introTake.pauseAll()
                                    listenViewModel.toggleSection(lesson, section)
                                },
                                onSeek = listenViewModel::seekTo,
                                onEdit = { leave { onEditSection(section.id) } }
                            )
                        }
                    }.toTypedArray()
                )
            }
        }
        if (lesson.sections.any { it.isAvailable }) {
            item {
                MelovaNavRow(
                    icon = Icons.Outlined.Visibility,
                    title = "دیدن درس از نگاه هنرجو",
                    onClick = { leave(onPreview) }
                )
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

/** A section in the lesson's list: plays where it is, and opens its own page to change it. */
@Composable
private fun TeacherSectionRow(
    section: LessonSection,
    listen: ListenUiState,
    onListen: () -> Unit,
    onSeek: (Float) -> Unit,
    onEdit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(12.dp)) {
        TeacherListenRow(
            title = section.title,
            idleLine = persianDigits("${(section.durationMs + 999) / 1000} ثانیه"),
            active = listen.key == TeacherListenViewModel.sectionKey(section),
            listen = listen,
            onListen = onListen,
            onSeek = onSeek,
            available = section.isAvailable,
            trailing = { ForwardArrow(tint = MelovaTextMuted) }
        )
    }
}

/**
 * One of the lesson's own files as a row: a play button, its name, and its length, or the time
 * played with a line to seek along once it plays.
 */
@Composable
internal fun TeacherListenRow(
    title: String,
    idleLine: String,
    active: Boolean,
    listen: ListenUiState,
    onListen: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkip: ((Long) -> Unit)? = null,
    available: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    val ready = active && listen.isReady
    val failed = !available || (active && listen.errorMessage != null)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (available) {
                ListenButton(
                    isPlaying = active && listen.isPlaying,
                    isLoading = active && listen.isLoading,
                    onClick = onListen,
                    contentDescription = if (active && listen.isPlaying) "توقف" else "پخش"
                )
            } else {
                Box(
                    modifier = Modifier.size(MelovaDimens.ListLead).clip(CircleShape).background(MelovaError.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MelovaError, modifier = Modifier.size(MelovaDimens.Icon))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MelovaText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = when {
                        !available -> "فایل صوتی پیدا نشد"
                        active && listen.errorMessage != null -> listen.errorMessage
                        active && listen.isLoading -> "در حال آماده‌سازی صدا…"
                        ready && (listen.positionMs > 0 || listen.isPlaying) ->
                            formatClock(listen.positionMs) + " از " + formatClock(listen.durationMs)
                        else -> idleLine
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) MelovaError else MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (ready && onSkip != null) {
                // Audio time runs left to right, even on a right-to-left page.
                LtrContent {
                    Row {
                        IconButton(onClick = { onSkip(-10_000L) }, modifier = Modifier.size(MelovaDimens.CompactButtonHeight)) {
                            Icon(Icons.Default.Replay10, contentDescription = "۱۰ ثانیه عقب", tint = MelovaTextMuted, modifier = Modifier.size(MelovaDimens.Icon))
                        }
                        IconButton(onClick = { onSkip(10_000L) }, modifier = Modifier.size(MelovaDimens.CompactButtonHeight)) {
                            Icon(Icons.Default.Forward10, contentDescription = "۱۰ ثانیه جلو", tint = MelovaTextMuted, modifier = Modifier.size(MelovaDimens.Icon))
                        }
                    }
                }
            }
            trailing()
        }
        if (ready) LtrContent { SeekLine(progress = listen.progress, onSeek = onSeek) }
    }
}

// ------------------------------------------------------------------ dialogs

/** What the price row says under the price. */
private fun saleNote(lesson: Lesson, hasCard: Boolean): String = when {
    lesson.isFree && lesson.priceToman > 0 -> "فعلاً همه‌ی درس‌ها رایگان است و این قیمت برای بعد می‌ماند."
    lesson.isFree -> "همه می‌توانند تمرینش کنند. برای فروش، قیمت بگذارید."
    !hasCard -> "برای شروع فروش، شماره کارتتان را در پروفایل استاد بگذارید."
    else -> "هنرجو کارت‌به‌کارت می‌خرد و شما تأیید می‌کنید."
}

/** Price 0 makes the lesson free again. */
@Composable
private fun SaleDialog(lesson: Lesson, onConfirm: (sku: String, priceToman: Long) -> Unit, onDismiss: () -> Unit) {
    var priceText by rememberSaveable { mutableStateOf(lesson.priceToman.takeIf { it > 0 }?.let { persianDigits(it.toString()) }.orEmpty()) }
    var sku by rememberSaveable { mutableStateOf(lesson.bazaarSku) }
    val price = digitsOnly(priceText).toLongOrNull() ?: 0L
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
    uploadProgress: Float? = null,
    content: LazyListScope.() -> Unit
) {
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { MelovaTopBar(title = title, subtitle = subtitle, onBack = onBack) }
                content()
            }
        }
        BusyOverlay(busyLabel, uploadProgress)
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
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MelovaText,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onAction,
            shape = MelovaPillShape,
            contentPadding = PaddingValues(start = 10.dp, end = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MelovaAccentTint, contentColor = MelovaAccentSoft),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(MelovaDimens.IconSmall))
            Text(" $actionLabel", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The lesson's spoken intro: played here once it is uploaded, with recording or picking a new one
 * folded under it until asked for.
 */
@Composable
private fun IntroCard(
    lesson: Lesson,
    take: TakeController,
    listen: ListenUiState,
    busy: Boolean,
    onListen: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkip: (Long) -> Unit,
    onReplace: () -> Unit,
    onUpload: (LocalFile) -> Unit
) {
    // A new upload changes the version, which folds the recorder away again.
    var replacing by rememberSaveable(lesson.introVersion) { mutableStateOf(false) }
    val showSlot = !lesson.hasIntro || replacing || take.phase != TakePhase.EMPTY
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (lesson.hasIntro) {
                TeacherListenRow(
                    title = "آموزش کلی درس",
                    idleLine = "هنرجو پیش از تمرین آن را گوش می‌دهد.",
                    active = listen.key == TeacherListenViewModel.introKey(lesson),
                    listen = listen,
                    onListen = onListen,
                    onSeek = onSeek,
                    onSkip = onSkip
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.AudioFile, contentDescription = null, tint = MelovaGold, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("هنوز آموزشی برای این درس ندارید", style = MaterialTheme.typography.titleMedium, color = MelovaText)
                        Text(
                            "توضیح صوتی شما درباره‌ی کل درس؛ همین‌جا ضبط کنید یا فایلش را انتخاب کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                }
            }
            if (!showSlot) {
                TextButton(
                    onClick = {
                        onReplace()
                        replacing = true
                    },
                    enabled = !busy,
                    shape = MelovaSmallShape,
                    modifier = Modifier.fillMaxWidth().height(MelovaDimens.CompactButtonHeight)
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(MelovaDimens.IconSmall))
                    Text("  ضبط دوباره یا انتخاب فایل دیگر", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
                }
            } else {
                if (lesson.hasIntro) HorizontalDivider(thickness = 1.dp, color = MelovaOutline)
                TeacherAudioSlot(controller = take, enabled = !busy)
                if (take.phase == TakePhase.READY) {
                    Button(
                        onClick = { take.audio?.let(onUpload) },
                        enabled = take.canUpload && !busy,
                        shape = MelovaSmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                        modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(MelovaDimens.Icon))
                        Text(if (lesson.hasIntro) "  جایگزینی آموزش فعلی" else "  بارگذاری آموزش", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (lesson.hasIntro && take.phase == TakePhase.EMPTY) {
                    TextButton(onClick = { replacing = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("انصراف", style = MaterialTheme.typography.labelLarge, color = MelovaTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishChip(published: Boolean) {
    if (published) MelovaChip(text = "منتشرشده", color = MelovaSuccess) else MelovaChip(text = "پیش‌نویس", color = MelovaGold)
}

@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(MelovaSmallShape)
            .background(MelovaAccentTint),
        contentAlignment = Alignment.Center
    ) {
        Text(persianDigits(number.toString()), style = MaterialTheme.typography.titleSmall, color = MelovaAccentSoft)
    }
}

@Composable
internal fun EmptyHint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MelovaCardShape)
            .border(1.dp, MelovaOutline, MelovaCardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MelovaAccentSoft)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
    }
}

@Composable
private fun DangerButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = MelovaSmallShape,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(MelovaDimens.ButtonHeight)
    ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MelovaError, modifier = Modifier.size(MelovaDimens.Icon))
        Text("  $label", color = MelovaError, style = MaterialTheme.typography.labelLarge)
    }
}
