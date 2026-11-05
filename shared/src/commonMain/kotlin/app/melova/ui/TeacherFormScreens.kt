package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import app.melova.data.model.TeacherOrnament
import app.melova.data.model.TeacherOrnamentKind
import app.melova.presentation.TakePreviewPhase
import app.melova.presentation.TakePreviewUiState
import app.melova.presentation.TakePreviewViewModel
import app.melova.ui.components.LetterTag
import app.melova.ui.components.ListenButton
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.OrnamentRow
import app.melova.ui.components.TeacherOrnamentWaveform
import app.melova.ui.components.formatPreciseClock
import app.melova.ui.components.ornamentColor
import app.melova.ui.components.ornamentNumbers
import app.melova.ui.layout.LtrContent
import app.melova.ui.theme.LocalMelovaPalette
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.presentation.TeacherListenViewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.melova.data.repository.LessonFields
import app.melova.platform.PlatformBackHandler as BackHandler
import app.melova.platform.persianDigits
import app.melova.platform.rememberImagePicker
import app.melova.presentation.TeacherUiState
import app.melova.presentation.TeacherViewModel
import app.melova.ui.components.BusyOverlay
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.TakeKind
import app.melova.ui.components.TakePhase
import app.melova.ui.components.TeacherAudioSlot
import app.melova.ui.components.TeacherAvatar
import app.melova.ui.components.rememberTakeController
import app.melova.ui.layout.PageBottomGap
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaPillShape
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/**
 * A section on a page of its own, new or to change: the take needs room to record, listen back
 * and write the poem, which a dialog squeezed together. [sectionId] null makes a new section.
 */
@Composable
fun SectionEditorScreen(
    lessonId: String,
    sectionId: String?,
    state: TeacherUiState,
    viewModel: TeacherViewModel,
    onBack: () -> Unit,
    listenViewModel: TeacherListenViewModel = viewModel { TeacherListenViewModel() },
    previewViewModel: TakePreviewViewModel = viewModel { TakePreviewViewModel() }
) {
    val lesson = state.lesson(lessonId)
    val section = sectionId?.let { id -> lesson?.sections?.firstOrNull { it.id == id } }
    val isNew = sectionId == null
    var title by rememberSaveable {
        mutableStateOf(section?.title ?: persianDigits("بخش ${(lesson?.sections?.size ?: 0) + 1}"))
    }
    var poem by rememberSaveable { mutableStateOf(section?.description.orEmpty()) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var confirmingLeave by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    // An existing section shows its take to listen to; the recorder opens only when asked.
    var replacing by rememberSaveable { mutableStateOf(isNew) }
    val take = rememberTakeController(TakeKind.SECTION)
    val listen by listenViewModel.uiState.collectAsStateWithLifecycle()
    val preview by previewViewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        onDispose {
            listenViewModel.pause()
            previewViewModel.stopPlayback()
        }
    }
    // Each finished take goes up straight away to be read; a cleared one is dropped there too.
    val readyTake = take.audio?.takeIf { take.canUpload }
    LaunchedEffect(readyTake) {
        if (readyTake != null) previewViewModel.preview(lessonId, sectionId, readyTake) else previewViewModel.discard()
    }
    val busy = state.busyLabel != null
    val textChanged = section != null && (title.trim() != section.title || poem.trim() != section.description)
    val unsaved = take.hasUnsavedWork || textChanged
    // Leaving drops what isn't saved yet, so ask first.
    val leave = { if (unsaved) confirmingLeave = true else onBack() }
    BackHandler(enabled = unsaved) { confirmingLeave = true }

    TeacherFormPage(
        title = if (isNew) "بخش جدید" else "ویرایش بخش",
        subtitle = lesson?.title,
        onBack = leave,
        busyLabel = state.busyLabel,
        actionLabel = if (isNew) "بارگذاری بخش" else "ذخیره‌ی تغییرات",
        actionIcon = if (isNew || take.canUpload) Icons.Outlined.CloudUpload else null,
        actionEnabled = lesson != null && title.isNotBlank() && !busy &&
            (if (take.canUpload) preview.canConfirm else !isNew && section != null && textChanged),
        actionNote = uploadError,
        onAction = {
            uploadError = null
            take.pauseAll()
            listenViewModel.pause()
            previewViewModel.stopPlayback()
            val done = {
                // The take is the section's now; dropping it here must not delete it.
                previewViewModel.confirmed()
                take.clear()
                onBack()
            }
            val takeId = preview.takeId?.takeIf { take.canUpload }
            if (sectionId == null) {
                takeId?.let {
                    viewModel.addSection(lessonId, title.trim(), poem.trim(), it, onSuccess = done, onFailure = { uploadError = it })
                }
            } else {
                viewModel.updateSection(
                    lessonId,
                    sectionId,
                    title.trim(),
                    poem.trim(),
                    takeId,
                    onSuccess = done,
                    onFailure = { uploadError = it }
                )
            }
        }
    ) {
        if (!isNew && section == null) {
            item { EmptyHint(Icons.Outlined.GraphicEq, "این بخش پیدا نشد؛ شاید حذف شده است.") }
            return@TeacherFormPage
        }
        item {
            FormStep(
                number = 1,
                title = "اجرای شما",
                hint = if (replacing) {
                    "فقط صدای خواندن خودتان، بدون ساز یا با ساز خیلی آرام، در جای ساکت. هنرجو همین را می‌خواند و با صدای شما مقایسه می‌شود."
                } else {
                    null
                }
            ) {
                if (section != null && lesson != null) {
                    TeacherListenRow(
                        title = if (replacing) "اجرای فعلی" else "اجرای این بخش",
                        idleLine = persianDigits("${(section.durationMs + 999) / 1000} ثانیه"),
                        active = listen.key == TeacherListenViewModel.sectionKey(section),
                        listen = listen,
                        onListen = {
                            take.pauseAll()
                            listenViewModel.toggleSection(lesson, section)
                        },
                        onSeek = listenViewModel::seekTo,
                        available = section.isAvailable
                    )
                }
                if (replacing) {
                    if (section != null) HorizontalDivider(thickness = 1.dp, color = MelovaOutline)
                    TeacherAudioSlot(controller = take, enabled = !busy)
                    if (section != null && take.phase == TakePhase.EMPTY) {
                        TextButton(onClick = { replacing = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("انصراف", style = MaterialTheme.typography.labelLarge, color = MelovaTextMuted)
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            listenViewModel.pause()
                            replacing = true
                        },
                        enabled = !busy,
                        shape = MelovaSmallShape,
                        modifier = Modifier.fillMaxWidth().height(MelovaDimens.CompactButtonHeight)
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(MelovaDimens.IconSmall))
                        Text("  ضبط دوباره یا انتخاب فایل دیگر", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
                    }
                }
            }
        }
        if (replacing) {
            item {
                FormStep(number = 2, title = "تحریرها، ویبراتوها و گذرها") {
                    TakePreview(
                        preview = preview,
                        onRetry = previewViewModel::retry,
                        onTogglePlayback = {
                            take.pauseAll()
                            listenViewModel.pause()
                            previewViewModel.togglePlayback()
                        },
                        onSeekToMs = {
                            take.pauseAll()
                            previewViewModel.playAt(it)
                        },
                        onZoomChange = previewViewModel::setZoom,
                        onSegment = {
                            take.pauseAll()
                            listenViewModel.pause()
                            previewViewModel.toggleSegment(it)
                        }
                    )
                }
            }
        }
        item {
            FormStep(
                number = if (replacing) 3 else 2,
                title = "نام و شعر",
                hint = when {
                    !isNew && take.canUpload -> "اگر شعر اجرای تازه فرق دارد، متن را پاک کنید تا برنامه آن را از روی صدای تازه بنویسد."
                    !isNew || take.phase == TakePhase.READY -> "شعر را همان‌طور که می‌خوانید بنویسید؛ هنرجو آن را در صفحه‌ی تمرین می‌بیند."
                    else -> "اگر شعر را ننویسید، برنامه پس از بارگذاری تلاش می‌کند آن را از روی صدای شما بنویسد."
                }
            ) {
                MelovaTextField(value = title, onValueChange = { title = it }, label = "نام بخش")
                MelovaTextField(value = poem, onValueChange = { poem = it }, label = "شعر (اختیاری)", singleLine = false, minLines = 3)
            }
        }
        if (section != null) {
            item {
                TextButton(
                    onClick = { confirmingDelete = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MelovaError, modifier = Modifier.size(MelovaDimens.Icon))
                    Text("  حذف بخش", style = MaterialTheme.typography.labelLarge, color = MelovaError)
                }
            }
        }
    }

    if (confirmingLeave) {
        ConfirmDialog(
            title = if (take.hasUnsavedWork) "ضبط ذخیره نشده" else "تغییرات ذخیره نشده",
            message = if (take.hasUnsavedWork) {
                "اجرایی که ضبط یا انتخاب کرده‌اید هنوز بارگذاری نشده و با خروج از این صفحه از بین می‌رود."
            } else {
                "نام یا شعری که تغییر داده‌اید ذخیره نشده و با خروج از این صفحه از بین می‌رود."
            },
            confirmLabel = "خروج بدون ذخیره",
            onConfirm = {
                confirmingLeave = false
                take.clear()
                previewViewModel.discard()
                onBack()
            },
            onDismiss = { confirmingLeave = false }
        )
    }
    if (confirmingDelete && section != null) {
        ConfirmDialog(
            title = "حذف بخش؟",
            message = "«${section.title}» و فایل صوتی‌اش حذف می‌شود. نمره‌های هنرجوها برای این بخش دیگر نمایش داده نمی‌شود.",
            confirmLabel = "حذف",
            onConfirm = {
                confirmingDelete = false
                take.clear()
                previewViewModel.discard()
                listenViewModel.pause()
                viewModel.deleteSection(lessonId, section.id, onSuccess = onBack)
            },
            onDismiss = { confirmingDelete = false }
        )
    }
}

/**
 * What the server found in a take, before it is confirmed: the wave with each ornament over it,
 * how many of each kind, and every one to hear on its own.
 */
@Composable
private fun TakePreview(
    preview: TakePreviewUiState,
    onRetry: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onSegment: (Int) -> Unit
) {
    when (preview.phase) {
        TakePreviewPhase.IDLE -> Text(
            text = "بعد از ضبط یا انتخاب اجرا، برنامه تحریرها، ویبراتوها و گذرهایش را اینجا نشان می‌دهد تا پیش از بارگذاری ببینید.",
            style = MaterialTheme.typography.bodySmall,
            color = MelovaTextMuted
        )
        TakePreviewPhase.UPLOADING -> PreviewProgress(
            label = "در حال فرستادن اجرا…",
            progress = preview.uploadProgress
        )
        TakePreviewPhase.ANALYZING -> PreviewProgress(
            label = "در حال یافتن تحریرها، ویبراتوها و گذرها…",
            progress = null
        )
        TakePreviewPhase.FAILED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(preview.errorMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MelovaError)
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(MelovaDimens.CompactButtonHeight)) {
                Text("تلاش دوباره", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
            }
        }
        TakePreviewPhase.READY -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (preview.peaks.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ListenButton(
                        isPlaying = preview.isPlaying && preview.playingIndex == null,
                        isLoading = false,
                        onClick = onTogglePlayback,
                        contentDescription = if (preview.isPlaying) "توقف" else "پخش کل اجرا"
                    )
                    LtrContent {
                        Text(
                            text = "${formatPreciseClock(preview.positionMs)} / ${formatPreciseClock(preview.durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MelovaTextMuted
                        )
                    }
                }
                LtrContent {
                    TeacherOrnamentWaveform(
                        peaks = preview.peaks,
                        durationMs = preview.durationMs,
                        playheadMs = preview.positionMs,
                        ornaments = preview.ornaments,
                        selectedIndex = preview.playingIndex ?: preview.selectedIndex,
                        zoom = preview.zoom,
                        maxZoom = TakePreviewViewModel.MAX_ZOOM,
                        onZoomChange = onZoomChange,
                        onSeekToMs = onSeekToMs,
                        onOrnamentClick = onSegment
                    )
                }
            }
            OrnamentCounts(preview.ornaments)
            when {
                preview.analysisNote != null -> Text(preview.analysisNote, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                preview.ornaments.isEmpty() -> Text(
                    text = "در این اجرا تحریر، ویبراتو یا گذری پیدا نشد. اگر خوانده‌اید، در جای ساکت‌تر و نزدیک‌تر به میکروفون دوباره ضبط کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
                else -> {
                    val numbers = ornamentNumbers(preview.ornaments)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.ornaments.forEachIndexed { index, ornament ->
                            OrnamentRow(
                                ornament = ornament,
                                number = numbers[index],
                                isSelected = index == (preview.playingIndex ?: preview.selectedIndex),
                                isPlaying = index == preview.playingIndex && preview.isPlaying,
                                onClick = { onSegment(index) }
                            )
                        }
                    }
                    Text(
                        text = "هنرجو همین‌ها را تمرین می‌کند. اگر جایی درست پیدا نشده، پیش از بارگذاری دوباره ضبط کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
        }
    }
}

/** How many of each kind, as the letters on the wave. */
@Composable
private fun OrnamentCounts(ornaments: List<TeacherOrnament>) {
    val palette = LocalMelovaPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        TeacherOrnamentKind.entries.forEach { kind ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                LetterTag(kind.letter, ornamentColor(kind, palette))
                Text(
                    text = persianDigits("${kind.title} ${ornaments.count { it.kind == kind }}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MelovaText
                )
            }
        }
    }
}

@Composable
private fun PreviewProgress(label: String, progress: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MelovaText, modifier = Modifier.weight(1f))
            if (progress != null) {
                Text(persianDigits("${(progress * 100).toInt()}٪"), style = MaterialTheme.typography.labelMedium, color = MelovaAccentSoft)
            }
        }
        if (progress != null) {
            MelovaProgressBar(
                progress = progress,
                color = MelovaAccent,
                trackColor = MelovaAccentTint,
                followLayoutDirection = true,
                thickness = 6.dp
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MelovaPillShape),
                color = MelovaAccent,
                trackColor = MelovaAccentTint
            )
        }
    }
}

/** A lesson's name, singer, level and description; [lessonId] null makes a new lesson. */
@Composable
fun LessonFormScreen(
    lessonId: String?,
    state: TeacherUiState,
    viewModel: TeacherViewModel,
    onCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val lesson = lessonId?.let(state::lesson)
    val isNew = lessonId == null
    var lessonTitle by rememberSaveable { mutableStateOf(lesson?.title.orEmpty()) }
    var singer by rememberSaveable { mutableStateOf(lesson?.subtitle.orEmpty()) }
    var level by rememberSaveable {
        mutableStateOf(lesson?.level ?: state.profile?.lessons?.lastOrNull()?.level ?: LEVELS.first())
    }
    var description by rememberSaveable { mutableStateOf(lesson?.description.orEmpty()) }
    val fields = LessonFields(lessonTitle.trim(), singer.trim(), level.trim(), description.trim())

    TeacherFormPage(
        title = if (isNew) "درس جدید" else "ویرایش درس",
        subtitle = lesson?.title,
        onBack = onBack,
        busyLabel = state.busyLabel,
        actionLabel = if (isNew) "ساختن درس" else "ذخیره",
        actionEnabled = lessonTitle.isNotBlank() && state.busyLabel == null,
        onAction = {
            if (lessonId == null) {
                // Straight into the new lesson, where the intro and sections are recorded.
                viewModel.createLesson(fields, onCreated = onCreated)
            } else {
                viewModel.updateLesson(lessonId, fields)
                onBack()
            }
        }
    ) {
        item {
            FormStep(title = "درباره‌ی درس", hint = "هنرجو درس را با همین نام در «جستجو» پیدا می‌کند.") {
                MelovaTextField(value = lessonTitle, onValueChange = { lessonTitle = it }, label = "نام درس")
                MelovaTextField(value = singer, onValueChange = { singer = it }, label = "نام خواننده (اختیاری)")
                MelovaTextField(value = description, onValueChange = { description = it }, label = "توضیحات", singleLine = false, minLines = 3)
            }
        }
        item {
            FormStep(title = "سطح") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LEVELS.forEach { option ->
                        ChoicePill(text = option, selected = level == option, onClick = { level = option })
                    }
                }
                // Any other level is typed; picking a pill fills the same field.
                MelovaTextField(value = level, onValueChange = { level = it }, label = "یا سطح دیگری بنویسید")
            }
        }
    }
}

/**
 * The teacher's own card as students see it: photo, name and a short bio, and the bank card their
 * lessons are paid to. The photo is sent as soon as it is picked; the rest with «ذخیره».
 */
@Composable
fun TeacherProfileScreen(state: TeacherUiState, viewModel: TeacherViewModel, onBack: () -> Unit) {
    val profile = state.profile
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var bio by rememberSaveable(profile?.id) { mutableStateOf(profile?.bio.orEmpty()) }
    var cardNumber by rememberSaveable(profile?.id) { mutableStateOf(profile?.cardNumber.orEmpty()) }
    var cardHolder by rememberSaveable(profile?.id) { mutableStateOf(profile?.cardHolder.orEmpty()) }
    var confirmingRemove by rememberSaveable { mutableStateOf(false) }
    val pickPhoto = rememberImagePicker { picked -> picked?.let(viewModel::setPhoto) }
    val changed = profile != null && (
        name.trim() != profile.name || bio.trim() != profile.bio ||
            cardNumber.trim() != profile.cardNumber || cardHolder.trim() != profile.cardHolder
        )

    TeacherFormPage(
        title = "پروفایل استاد",
        subtitle = null,
        onBack = onBack,
        busyLabel = state.busyLabel,
        actionLabel = "ذخیره",
        actionEnabled = changed && name.isNotBlank() && state.busyLabel == null,
        onAction = {
            viewModel.updateProfile(name.trim(), bio.trim(), cardNumber.trim(), cardHolder.trim())
            onBack()
        }
    ) {
        item {
            FormStep(title = "عکس و معرفی", number = 1, hint = "هنرجوها این عکس و نام را روی درس‌ها و صفحه‌ی شما می‌بینند.") {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .clickable(enabled = profile != null && state.busyLabel == null, onClickLabel = "انتخاب عکس", onClick = pickPhoto)
                    ) {
                        TeacherAvatar(
                            name = name,
                            size = 104.dp,
                            teacherId = profile?.id,
                            photoVersion = profile?.photoVersion
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(MelovaDimens.IconSmall))
                        }
                    }
                    if (profile?.photoVersion != null) {
                        TextButton(onClick = { confirmingRemove = true }, enabled = state.busyLabel == null) {
                            Text("حذف عکس", color = MelovaTextMuted)
                        }
                    } else {
                        TextButton(onClick = pickPhoto, enabled = profile != null && state.busyLabel == null) {
                            Text("افزودن عکس", color = MelovaAccentSoft)
                        }
                    }
                }
                MelovaTextField(value = name, onValueChange = { name = it.take(MAX_NAME_LENGTH) }, label = "نام نمایشی")
                MelovaTextField(value = bio, onValueChange = { bio = it }, label = "معرفی کوتاه", singleLine = false, minLines = 3)
            }
        }
        item {
            FormStep(title = "کارت بانکی", number = 2, hint = "هنرجوها قیمت درس را به این کارت واریز می‌کنند.") {
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
    }

    if (confirmingRemove) {
        ConfirmDialog(
            title = "حذف عکس",
            message = "عکس شما از روی درس‌ها و صفحه‌تان برداشته شود؟",
            confirmLabel = "حذف",
            onConfirm = {
                confirmingRemove = false
                viewModel.removePhoto()
            },
            onDismiss = { confirmingRemove = false }
        )
    }
}

private val LEVELS = listOf("مقدماتی", "متوسط", "پیشرفته")

/**
 * A form as a page: the steps scroll, and the one action stays pinned under them, so it is always
 * in reach however long the poem gets.
 */
@Composable
private fun TeacherFormPage(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    busyLabel: String?,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    actionIcon: ImageVector? = null,
    actionNote: String? = null,
    content: LazyListScope.() -> Unit
) {
    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { MelovaTopBar(title = title, subtitle = subtitle, onBack = onBack) }
                    content()
                }
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = PageBottomGap),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actionNote?.let { note ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MelovaError, modifier = Modifier.size(MelovaDimens.IconSmall))
                            Text(note, style = MaterialTheme.typography.bodySmall, color = MelovaError)
                        }
                    }
                    Button(
                        onClick = onAction,
                        enabled = actionEnabled,
                        shape = MelovaSmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(MelovaDimens.PrimaryButtonHeight)
                    ) {
                        if (busyLabel != null) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(MelovaDimens.Icon))
                        } else {
                            actionIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(MelovaDimens.Icon)) }
                            Text(
                                text = if (actionIcon != null) "  $actionLabel" else actionLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        BusyOverlay(busyLabel)
    }
}

/** One step of a form in its own card, numbered when the order matters. */
@Composable
private fun FormStep(
    title: String,
    number: Int? = null,
    hint: String? = null,
    content: @Composable () -> Unit
) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (number != null) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(MelovaAccentTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(persianDigits(number.toString()), style = MaterialTheme.typography.labelMedium, color = MelovaAccentSoft)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MelovaText)
            }
            content()
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted) }
        }
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MelovaAccentSoft else MelovaTextMuted,
        modifier = Modifier
            .clip(MelovaPillShape)
            .background(if (selected) MelovaAccentTint else Color.Transparent)
            .border(1.dp, if (selected) MelovaAccentSoft else MelovaOutline, MelovaPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
