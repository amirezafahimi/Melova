package app.melova.ui

import app.melova.platform.PlatformBackHandler as BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.data.model.Review
import app.melova.data.model.ReviewStatus
import app.melova.data.model.StudentCard
import app.melova.data.model.TeacherReport
import app.melova.presentation.TeacherDeskUiState
import app.melova.presentation.TeacherDeskViewModel
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSegmentedControl
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.SectionHeader
import app.melova.ui.components.TakeKind
import app.melova.ui.components.TeacherAudioSlot
import app.melova.ui.components.rememberTakeController
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.platform.formatFixed

/** Persian digits for whole numbers typed with either digit set. */
private fun parseWhole(text: String): Long? {
    val latin = text.trim().map { ch ->
        when (ch) {
            in '۰'..'۹' -> '0' + (ch - '۰')
            in '٠'..'٩' -> '0' + (ch - '٠')
            else -> ch
        }
    }.filter { it.isDigit() }.joinToString("")
    return latin.takeIf { it.isNotEmpty() }?.toLongOrNull()
}

private fun parseMinutes(text: String): Double? =
    text.trim().replace('٫', '.').replace('/', '.').let { raw ->
        val latin = raw.map { ch ->
            when (ch) {
                in '۰'..'۹' -> '0' + (ch - '۰')
                in '٠'..'٩' -> '0' + (ch - '٠')
                else -> ch
            }
        }.joinToString("")
        latin.toDoubleOrNull()?.takeIf { it > 0 }
    }

// ------------------------------------------------------------------ panel entry

/** On the teacher panel: the way into reviews, students and the report. */
@Composable
internal fun TeacherDeskEntryCard(
    onOpenReviews: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenReport: () -> Unit
) {
    MelovaSurfaceCard(contentPadding = 6.dp) {
        Column {
            DeskEntryRow(Icons.Outlined.Inbox, "درخواست‌های هنرجوها", "اجراهایی که برای شما فرستاده‌اند", onOpenReviews)
            DeskEntryRow(Icons.Outlined.PeopleOutline, "هنرجوها و پرداخت‌ها", "ثبت پرداخت کارت‌به‌کارت و باز کردن درس", onOpenStudents)
            DeskEntryRow(Icons.Outlined.Insights, "گزارش درآمد و زمان", "درآمد هر ساعت وقت شما، در مقایسه با کلاس خصوصی", onOpenReport)
        }
    }
}

@Composable
private fun DeskEntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 10.dp, modifier = Modifier.padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MelovaAccentSoft)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MelovaText)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
        }
    }
}

// ------------------------------------------------------------------ review queue

private val reviewFilters = listOf(ReviewStatus.OPEN, ReviewStatus.ANSWERED, null)

@Composable
fun TeacherReviewsScreen(
    state: TeacherDeskUiState,
    viewModel: TeacherDeskViewModel,
    lessonTitle: (String) -> String?,
    onOpenReview: (String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadReviews() }

    TeacherScaffold(title = "درخواست‌های هنرجوها", subtitle = null, onBack = onBack, busyLabel = state.busyLabel) {
        item {
            MelovaSegmentedControl(
                options = listOf("در انتظار", "پاسخ‌داده", "همه"),
                selectedIndex = reviewFilters.indexOf(state.filter).coerceAtLeast(0),
                onSelect = { viewModel.loadReviews(reviewFilters[it]) }
            )
        }
        if (state.isLoadingReviews && state.reviews.isEmpty()) {
            item { CenteredProgress() }
        } else if (state.reviews.isEmpty()) {
            item {
                EmptyHint(
                    Icons.Outlined.Inbox,
                    if (state.filter == ReviewStatus.OPEN) "درخواستی در انتظار نیست." else "چیزی برای نمایش نیست."
                )
            }
        }
        items(state.reviews, key = { it.id }) { review ->
            ReviewQueueRow(review, lessonTitle(review.lessonId), onClick = { onOpenReview(review.id) })
        }
    }
}

@Composable
private fun ReviewQueueRow(review: Review, lessonTitle: String?, onClick: () -> Unit) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = review.studentName.ifBlank { review.studentId },
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ReviewStatusChip(review)
            }
            Text(
                text = lessonTitle ?: review.lessonId,
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (review.note.isNotBlank()) {
                Text(review.note, style = MaterialTheme.typography.bodySmall, color = MelovaText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReviewStatusChip(review: Review) {
    when {
        review.status == ReviewStatus.ANSWERED -> MelovaChip("پاسخ‌داده", color = MelovaSuccess)
        review.status == ReviewStatus.REFUNDED -> MelovaChip("بسته‌شده", color = MelovaTextMuted)
        review.isReferral -> MelovaChip("ارجاع سیستم", color = MelovaGold)
        else -> MelovaChip("درخواست هنرجو", color = MelovaAccent)
    }
}

// ------------------------------------------------------------------ one review

@Composable
fun TeacherReviewScreen(
    reviewId: String,
    state: TeacherDeskUiState,
    viewModel: TeacherDeskViewModel,
    lessonTitle: (String) -> String?,
    sectionTitle: (lessonId: String, sectionId: String) -> String?,
    onBack: () -> Unit
) {
    LaunchedEffect(reviewId) { viewModel.openReview(reviewId) }
    DisposableEffect(Unit) { onDispose { viewModel.stopTake() } }

    val review = state.current?.takeIf { it.id == reviewId }
    var replyText by rememberSaveable { mutableStateOf("") }
    var minutesText by rememberSaveable { mutableStateOf("") }
    var confirmDecline by rememberSaveable { mutableStateOf(false) }
    val voice = rememberTakeController(TakeKind.INTRO)
    var pendingLeave by remember { mutableStateOf(false) }
    val hasDraft = replyText.isNotBlank() || voice.hasUnsavedWork
    BackHandler(enabled = hasDraft && review?.status == ReviewStatus.OPEN) { pendingLeave = true }

    TeacherScaffold(
        title = review?.studentName?.ifBlank { null } ?: "درخواست هنرجو",
        subtitle = review?.let { lessonTitle(it.lessonId) },
        onBack = { if (hasDraft && review?.status == ReviewStatus.OPEN) pendingLeave = true else onBack() },
        busyLabel = state.busyLabel
    ) {
        if (review == null) {
            item { CenteredProgress() }
            return@TeacherScaffold
        }
        item {
            MelovaSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = sectionTitle(review.lessonId, review.sectionId) ?: "بخش",
                            style = MaterialTheme.typography.titleMedium,
                            color = MelovaText,
                            modifier = Modifier.weight(1f)
                        )
                        ReviewStatusChip(review)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::toggleTake, enabled = !state.isLoadingTake) {
                            if (state.isLoadingTake) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            } else {
                                Icon(
                                    imageVector = if (state.isPlayingTake) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = if (state.isPlayingTake) "توقف" else "پخش اجرای هنرجو",
                                    tint = MelovaAccent
                                )
                            }
                        }
                        Text("اجرای هنرجو", style = MaterialTheme.typography.bodyMedium, color = MelovaText)
                    }
                    if (review.note.isNotBlank()) {
                        Text("یادداشت هنرجو: ${review.note}", style = MaterialTheme.typography.bodyMedium, color = MelovaText)
                    }
                    if (review.isReferral && review.referralReasons.isNotEmpty()) {
                        Text("سیستم مطمئن نبود چون:", style = MaterialTheme.typography.bodySmall, color = MelovaGold)
                        review.referralReasons.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                        }
                    }
                }
            }
        }
        review.analysis?.let { analysis ->
            item {
                MelovaSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "آنچه سیستم به هنرجو گفت" + if (analysis.overall > 0) " (نمره ${analysis.overall})" else "",
                            style = MaterialTheme.typography.titleSmall,
                            color = MelovaText
                        )
                        if (analysis.summary.isNotBlank()) {
                            Text(analysis.summary, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                        }
                        analysis.weakPoints.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                        }
                    }
                }
            }
        }

        if (review.status == ReviewStatus.OPEN) {
            item { SectionHeader(title = "پاسخ شما") }
            item {
                MelovaSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MelovaTextField(
                            value = replyText,
                            onValueChange = { replyText = it.take(4000) },
                            label = "پاسخ نوشتاری",
                            singleLine = false,
                            minLines = 3
                        )
                        Text("پاسخ صوتی (اختیاری)", style = MaterialTheme.typography.titleSmall, color = MelovaText)
                        TeacherAudioSlot(controller = voice, enabled = state.busyLabel == null)
                        MelovaTextField(
                            value = minutesText,
                            onValueChange = { minutesText = it.take(6) },
                            label = "چند دقیقه وقت گذاشتید؟ (خالی = خودکار)"
                        )
                        val canSend = (replyText.isNotBlank() || voice.canUpload) && !voice.isRecording
                        Button(
                            onClick = {
                                voice.pauseAll()
                                viewModel.reply(
                                    text = replyText,
                                    voice = voice.audio?.takeIf { voice.canUpload },
                                    minutes = parseMinutes(minutesText),
                                    onDone = {
                                        replyText = ""
                                        minutesText = ""
                                        voice.clear()
                                        onBack()
                                    }
                                )
                            },
                            enabled = canSend && state.busyLabel == null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("ارسال پاسخ") }
                        OutlinedButton(
                            onClick = { confirmDecline = true },
                            enabled = state.busyLabel == null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) { Text("بستن بدون بازبینی (اعتبار برمی‌گردد)", color = MelovaError) }
                    }
                }
            }
        } else {
            item {
                MelovaSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("پاسخ شما", style = MaterialTheme.typography.titleSmall, color = MelovaText)
                        Text(
                            text = review.replyText.ifBlank { if (review.hasReplyAudio) "فقط پاسخ صوتی" else "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MelovaText
                        )
                        review.teacherMinutes?.let {
                            Text("زمان ثبت‌شده: ${formatFixed(it, 1)} دقیقه", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                        }
                    }
                }
            }
        }
    }

    if (confirmDecline) {
        ConfirmDialog(
            title = "بستن بدون بازبینی؟",
            message = "هنرجو اعتبارش را پس می‌گیرد. اگر متنی در کادر پاسخ نوشته‌اید، به‌عنوان توضیح برایش فرستاده می‌شود.",
            confirmLabel = "بستن",
            onConfirm = {
                confirmDecline = false
                viewModel.decline(replyText, onDone = {
                    replyText = ""
                    onBack()
                })
            },
            onDismiss = { confirmDecline = false }
        )
    }
    if (pendingLeave) {
        ConfirmDialog(
            title = "پاسخ فرستاده نشده",
            message = "پاسخی که نوشته یا ضبط کرده‌اید از بین می‌رود.",
            confirmLabel = "خروج",
            onConfirm = {
                pendingLeave = false
                voice.clear()
                onBack()
            },
            onDismiss = { pendingLeave = false }
        )
    }
}

// ------------------------------------------------------------------ students

@Composable
fun TeacherStudentsScreen(
    state: TeacherDeskUiState,
    viewModel: TeacherDeskViewModel,
    lessons: List<Lesson>,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadStudents() }
    var paying by remember { mutableStateOf<StudentCard?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = state.students.filter {
        query.isBlank() || it.id.contains(query.trim(), ignoreCase = true) || it.name.contains(query.trim(), ignoreCase = true)
    }

    TeacherScaffold(title = "هنرجوها و پرداخت‌ها", subtitle = null, onBack = onBack, busyLabel = state.busyLabel) {
        item {
            Text(
                text = "هنرجو کد هنرجویی‌اش را از صفحه‌ی «من» به شما می‌دهد. پس از دریافت مبلغ، اینجا پیدایش کنید و پرداخت را ثبت کنید تا درس یا بازبینی‌ها برایش باز شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
        item { MelovaTextField(value = query, onValueChange = { query = it }, label = "جستجو با کد یا نام") }
        if (state.isLoadingStudents && state.students.isEmpty()) {
            item { CenteredProgress() }
        } else if (shown.isEmpty()) {
            item { EmptyHint(Icons.Outlined.PeopleOutline, "هنرجویی پیدا نشد.") }
        }
        items(shown, key = { it.id }) { student ->
            MelovaSurfaceCard(onClick = { paying = student }, contentPadding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name.ifBlank { "بی‌نام" }, style = MaterialTheme.typography.titleMedium, color = MelovaText)
                        Text(student.id, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                        Text(
                            text = "${lessons.count { it.id in student.ownedLessons && !it.isFree }} درس پولی • ${student.reviewCredits} بازبینی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaAccentSoft
                        )
                    }
                    Text("ثبت پرداخت", style = MaterialTheme.typography.labelLarge, color = MelovaAccent)
                }
            }
        }
    }

    paying?.let { student ->
        PaymentDialog(
            student = student,
            paidLessons = lessons.filter { !it.isFree },
            busy = state.busyLabel != null,
            onConfirm = { lessonId, reviews, price ->
                viewModel.recordPayment(student.id, lessonId, reviews, price, onDone = { paying = null })
            },
            onDismiss = { paying = null }
        )
    }
}

@Composable
private fun PaymentDialog(
    student: StudentCard,
    paidLessons: List<Lesson>,
    busy: Boolean,
    onConfirm: (lessonId: String?, reviews: Int?, priceToman: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var lessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewsText by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    val lesson = paidLessons.firstOrNull { it.id == lessonId }
    val reviews = parseWhole(reviewsText)?.toInt()
    val price = parseWhole(priceText)
    val valid = (lesson != null || (reviews ?: 0) > 0) && price != null

    FormDialog(
        title = "ثبت پرداخت برای ${student.name.ifBlank { student.id }}",
        confirmLabel = "ثبت",
        confirmEnabled = valid && !busy,
        busy = busy,
        onConfirm = { if (price != null) onConfirm(lesson?.id, reviews?.takeIf { it > 0 }, price) },
        onDismiss = onDismiss
    ) {
        Text("کدام درس را خرید؟", style = MaterialTheme.typography.titleSmall, color = MelovaText)
        if (paidLessons.isEmpty()) {
            Text("درس پولی ندارید؛ قیمت را در صفحه‌ی هر درس تعیین کنید.", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
        paidLessons.forEach { option ->
            val owned = option.id in student.ownedLessons
            val selected = option.id == lessonId
            MelovaSurfaceCard(
                onClick = {
                    lessonId = if (selected) null else option.id
                    if (!selected && priceText.isBlank() && option.priceToman > 0) priceText = option.priceToman.toString()
                },
                enabled = !owned,
                contentPadding = 10.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(option.title, style = MaterialTheme.typography.bodyMedium, color = MelovaText, modifier = Modifier.weight(1f))
                    when {
                        owned -> MelovaChip("دارد", color = MelovaTextMuted)
                        selected -> MelovaChip("انتخاب شد", color = MelovaSuccess)
                        option.priceToman > 0 -> Text(formatToman(option.priceToman), style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                    }
                }
            }
        }
        MelovaTextField(
            value = reviewsText,
            onValueChange = { reviewsText = it.take(3) },
            label = if (lesson != null) "بازبینی اضافه (خالی = ${lesson.includedReviews} بازبینی درس)" else "تعداد بازبینی"
        )
        MelovaTextField(value = priceText, onValueChange = { priceText = it.take(12) }, label = "مبلغ دریافتی (تومان) *")
        price?.let { Text(formatToman(it), style = MaterialTheme.typography.bodySmall, color = MelovaAccentSoft) }
    }
}

// ------------------------------------------------------------------ report

/** Reviews time themselves; these are the hours around them. */
private val timeKinds = listOf("prep" to "آماده‌سازی درس", "support" to "پیام‌ها", "admin" to "کارهای اداری")

@Composable
fun TeacherReportScreen(
    state: TeacherDeskUiState,
    viewModel: TeacherDeskViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadReport() }
    var editingRate by rememberSaveable { mutableStateOf(false) }
    var loggingTime by rememberSaveable { mutableStateOf(false) }

    TeacherScaffold(title = "گزارش ۳۰ روز اخیر", subtitle = null, onBack = onBack, busyLabel = state.busyLabel) {
        val report = state.report
        if (report == null) {
            item { if (state.isLoadingReport) CenteredProgress() else EmptyHint(Icons.Outlined.Insights, "گزارش دریافت نشد.") }
        } else {
            item { WorthItCard(report) }
            item { ReportNumbers(report) }
        }
        item {
            MelovaSurfaceCard(onClick = { editingRate = true }, contentPadding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("درآمد هر ساعت کلاس خصوصی شما", style = MaterialTheme.typography.titleSmall, color = MelovaText)
                        Text(
                            text = state.privateHourlyRateToman?.let(::formatToman) ?: "تعیین نشده؛ برای مقایسه وارد کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted
                        )
                    }
                    Text("تغییر", style = MaterialTheme.typography.labelLarge, color = MelovaAccent)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { loggingTime = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) { Text("ثبت زمان دیگر (آماده‌سازی، پیام‌ها، …)", color = MelovaAccentSoft) }
        }
        item {
            Text(
                text = "زمان بازبینی‌ها خودکار ثبت می‌شود. وقتی برای ضبط درس یا جواب پیام‌ها می‌گذارید را هم ثبت کنید تا حساب واقعی باشد.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
    }

    if (editingRate) {
        var rateText by rememberSaveable { mutableStateOf(state.privateHourlyRateToman?.toString().orEmpty()) }
        FormDialog(
            title = "درآمد هر ساعت کلاس خصوصی",
            confirmLabel = "ذخیره",
            onConfirm = {
                viewModel.setPrivateHourlyRate(parseWhole(rateText))
                editingRate = false
            },
            onDismiss = { editingRate = false }
        ) {
            MelovaTextField(value = rateText, onValueChange = { rateText = it.take(12) }, label = "تومان در ساعت")
            parseWhole(rateText)?.let { Text(formatToman(it), style = MaterialTheme.typography.bodySmall, color = MelovaAccentSoft) }
        }
    }
    if (loggingTime) {
        var minutesText by rememberSaveable { mutableStateOf("") }
        var kind by rememberSaveable { mutableStateOf(timeKinds.first().first) }
        var note by rememberSaveable { mutableStateOf("") }
        val minutes = parseMinutes(minutesText)
        FormDialog(
            title = "ثبت زمان",
            confirmLabel = "ثبت",
            confirmEnabled = minutes != null && state.busyLabel == null,
            busy = state.busyLabel != null,
            onConfirm = { minutes?.let { viewModel.logTime(it, kind, note, onDone = { loggingTime = false }) } },
            onDismiss = { loggingTime = false }
        ) {
            MelovaSegmentedControl(
                options = timeKinds.map { it.second },
                selectedIndex = timeKinds.indexOfFirst { it.first == kind }.coerceAtLeast(0),
                onSelect = { kind = timeKinds[it].first }
            )
            MelovaTextField(value = minutesText, onValueChange = { minutesText = it.take(6) }, label = "چند دقیقه؟ *")
            MelovaTextField(value = note, onValueChange = { note = it.take(200) }, label = "توضیح (اختیاری)")
        }
    }
}

@Composable
private fun WorthItCard(report: TeacherReport) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("درآمد خالص هر ساعت وقت شما", style = MaterialTheme.typography.titleSmall, color = MelovaTextMuted)
            Text(
                text = report.netPerHourToman?.let(::formatToman) ?: "هنوز زمانی ثبت نشده",
                style = MaterialTheme.typography.headlineSmall,
                color = when (report.worthIt) {
                    true -> MelovaSuccess
                    false -> MelovaError
                    null -> MelovaText
                }
            )
            Text(
                text = when (report.worthIt) {
                    true -> "بیشتر از کلاس خصوصی (${report.privateHourlyRateToman?.let(::formatToman)}). ارزشش را دارد."
                    false -> "کمتر از کلاس خصوصی (${report.privateHourlyRateToman?.let(::formatToman)}). یا قیمت را بالا ببرید یا زمان بازبینی را کم کنید."
                    null -> if (report.privateHourlyRateToman == null) "درآمد ساعتی کلاس خصوصی را وارد کنید تا مقایسه شود." else "با ثبت زمان، مقایسه انجام می‌شود."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
    }
}

@Composable
private fun ReportNumbers(report: TeacherReport) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ReportLine("درآمد ناخالص", formatToman(report.grossToman))
            ReportLine("درآمد خالص", formatToman(report.netToman))
            ReportLine("هنرجوی پرداخت‌کننده", report.payingStudents.toString())
            ReportLine("کل وقت شما", "${formatFixed(report.teacherMinutes, 0)} دقیقه")
            report.minutesPerPayingStudent?.let { ReportLine("وقت برای هر هنرجو", "${formatFixed(it, 1)} دقیقه") }
            ReportLine("بازبینی پاسخ‌داده", report.reviewsAnswered.toString())
            ReportLine("میانگین هر بازبینی", "${formatFixed(report.averageReviewMinutes, 1)} دقیقه")
            ReportLine("در انتظار پاسخ", report.reviewsWaiting.toString())
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MelovaTextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
    }
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MelovaAccent)
    }
}
