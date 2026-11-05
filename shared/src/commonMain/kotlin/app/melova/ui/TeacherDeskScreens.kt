package app.melova.ui

import app.melova.ui.components.ForwardArrow
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.MelovaGold
import app.melova.ui.components.MelovaGroupRow
import app.melova.ui.components.MelovaNavGroup
import app.melova.data.model.Lesson
import app.melova.data.model.LessonPurchaseRequest
import app.melova.data.model.StudentCard
import app.melova.data.model.TeacherReport
import app.melova.platform.persianDigits
import app.melova.presentation.TeacherDeskUiState
import app.melova.presentation.TeacherDeskViewModel
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSegmentedControl
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.SectionHeader
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaError
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
    pendingPurchases: Int,
    unreadChats: Int,
    onOpenChats: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenReport: () -> Unit
) {
    MelovaNavGroup(
        {
            MelovaGroupRow(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "گفتگو با هنرجوها",
                subtitle = if (unreadChats > 0) persianDigits("$unreadChats پیام تازه") else "پیام‌ها و نتیجه‌ی تمرین هنرجوها",
                onClick = onOpenChats
            )
        },
        {
            MelovaGroupRow(
                icon = Icons.Outlined.PeopleOutline,
                title = "هنرجوها و پرداخت‌ها",
                subtitle = if (pendingPurchases > 0) persianDigits("$pendingPurchases خرید منتظر تأیید شماست") else "تأیید خریدها و باز کردن درس برای هنرجو",
                onClick = onOpenStudents,
                iconTint = if (pendingPurchases > 0) MelovaGold else MelovaAccentSoft
            )
        },
        {
            MelovaGroupRow(
                icon = Icons.Outlined.Insights,
                title = "گزارش درآمد و زمان",
                subtitle = "درآمد هر ساعت وقت شما، در مقایسه با کلاس خصوصی",
                onClick = onOpenReport
            )
        }
    )
}

// ------------------------------------------------------------------ students

@Composable
fun TeacherStudentsScreen(
    state: TeacherDeskUiState,
    viewModel: TeacherDeskViewModel,
    lessons: List<Lesson>,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadStudents()
        viewModel.loadPurchaseRequests()
    }
    var paying by remember { mutableStateOf<StudentCard?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = state.students.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }

    TeacherScaffold(title = "هنرجوها و پرداخت‌ها", subtitle = null, onBack = onBack, busyLabel = state.busyLabel) {
        if (state.purchaseRequests.isNotEmpty()) {
            item { SectionHeader(title = "خریدهای منتظر تأیید", trailing = persianDigits("${state.purchaseRequests.size} خرید")) }
            items(state.purchaseRequests, key = { "request-" + it.id }) { request ->
                PurchaseRequestCard(
                    request = request,
                    lessonTitle = lessons.firstOrNull { it.id == request.lessonId }?.title,
                    busy = state.busyLabel != null,
                    onDecide = { approve -> viewModel.decidePurchase(request.id, approve) }
                )
            }
            item { SectionHeader(title = "هنرجوها") }
        }
        item {
            Text(
                text = "اگر هنرجویی بیرون از اپ پرداخت کرده، اینجا پیدایش کنید و درس را برایش باز کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
        item { MelovaTextField(value = query, onValueChange = { query = it }, label = "جستجو با نام") }
        if (state.isLoadingStudents && state.students.isEmpty()) {
            item { CenteredProgress() }
        } else if (shown.isEmpty()) {
            item { EmptyHint(Icons.Outlined.PeopleOutline, "هنرجویی پیدا نشد.") }
        }
        items(shown, key = { it.id }) { student ->
            MelovaSurfaceCard(onClick = { paying = student }, contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name.ifBlank { "بی‌نام" }, style = MaterialTheme.typography.titleMedium, color = MelovaText)
                        Text(
                            text = persianDigits("${lessons.count { it.id in student.ownedLessons && !it.isFree }} درس پولی"),
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
            onConfirm = { lessonId, price ->
                viewModel.recordPayment(student.id, lessonId, price, onDone = { paying = null })
            },
            onDismiss = { paying = null }
        )
    }
}

/** A student says they paid by card: the teacher checks their account and answers here. */
@Composable
private fun PurchaseRequestCard(
    request: LessonPurchaseRequest,
    lessonTitle: String?,
    busy: Boolean,
    onDecide: (approve: Boolean) -> Unit
) {
    MelovaSurfaceCard(contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = request.studentName.ifBlank { "هنرجو" },
                style = MaterialTheme.typography.titleMedium,
                color = MelovaText
            )
            Text(
                text = "${lessonTitle ?: "درس"}، ${formatToman(request.priceToman)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaAccentSoft
            )
            Text("نام واریزکننده: ${request.payer}", style = MaterialTheme.typography.bodySmall, color = MelovaText)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDecide(true) },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("تأیید و باز کردن درس") }
                OutlinedButton(
                    onClick = { onDecide(false) },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) { Text("پیدا نشد", color = MelovaError) }
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    student: StudentCard,
    paidLessons: List<Lesson>,
    busy: Boolean,
    onConfirm: (lessonId: String, priceToman: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var lessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var priceText by rememberSaveable { mutableStateOf("") }
    val lesson = paidLessons.firstOrNull { it.id == lessonId }
    val price = parseWhole(priceText)

    FormDialog(
        title = "ثبت پرداخت برای ${student.name.ifBlank { "هنرجو" }}",
        confirmLabel = "ثبت",
        confirmEnabled = lesson != null && price != null && !busy,
        busy = busy,
        onConfirm = { if (lesson != null && price != null) onConfirm(lesson.id, price) },
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
                    if (!selected && priceText.isBlank() && option.priceToman > 0) priceText = persianDigits(option.priceToman.toString())
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
            MelovaSurfaceCard(onClick = { editingRate = true }, contentPadding = 12.dp) {
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
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
        var rateText by rememberSaveable { mutableStateOf(state.privateHourlyRateToman?.let { persianDigits(it.toString()) }.orEmpty()) }
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportLine("درآمد ناخالص", formatToman(report.grossToman))
            ReportLine("درآمد خالص", formatToman(report.netToman))
            ReportLine("هنرجوی پرداخت‌کننده", persianDigits(report.payingStudents.toString()))
            ReportLine("کل وقت شما", persianDigits("${formatFixed(report.teacherMinutes, 0)} دقیقه"))
            report.minutesPerPayingStudent?.let { ReportLine("وقت برای هر هنرجو", persianDigits("${formatFixed(it, 1)} دقیقه")) }
            ReportLine("بازبینی پاسخ‌داده", persianDigits(report.reviewsAnswered.toString()))
            ReportLine("میانگین هر بازبینی", persianDigits("${formatFixed(report.averageReviewMinutes, 1)} دقیقه"))
            ReportLine("در انتظار پاسخ", persianDigits(report.reviewsWaiting.toString()))
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
