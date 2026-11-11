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
import androidx.compose.material.icons.outlined.Payments
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
import app.melova.data.model.TeacherEarnings
import app.melova.data.model.TeacherReport
import app.melova.data.model.TeacherSale
import app.melova.data.model.euroInput
import app.melova.data.model.formatEuro
import app.melova.data.model.parseEuroCents
import app.melova.platform.formatMessageDay
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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

/** On the teacher panel: the way into chats, sales and the report. */
@Composable
internal fun TeacherDeskEntryCard(
    /** This month's share, once loaded. */
    earnings: TeacherEarnings?,
    unreadChats: Int,
    onOpenChats: () -> Unit,
    onOpenEarnings: () -> Unit,
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
                icon = Icons.Outlined.Payments,
                title = "فروش و درآمد",
                subtitle = earnings?.takeIf { it.thisMonth.count > 0 }
                    ?.let { "سهم شما در این ماه: ${formatEuro(it.thisMonth.teacherCents)}" }
                    ?: "فروش درس‌ها و سهم شما از هر خرید",
                onClick = onOpenEarnings
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

// ------------------------------------------------------------------ earnings

/**
 * What the teacher's lessons sold and their share of each sale. Nothing is paid out yet,
 * and the page says so, so no one waits for money that isn't on its way.
 */
@Composable
fun TeacherEarningsScreen(
    state: TeacherDeskUiState,
    viewModel: TeacherDeskViewModel,
    lessons: List<Lesson>,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadEarnings() }
    TeacherScaffold(title = "فروش و درآمد", subtitle = null, onBack = onBack, busyLabel = state.busyLabel) {
        val earnings = state.earnings
        if (earnings == null) {
            item {
                if (state.isLoadingEarnings) CenteredProgress() else EmptyHint(Icons.Outlined.Payments, "فروش‌ها باز نشد. دوباره امتحان کنید.")
            }
            return@TeacherScaffold
        }
        item { EarningsSummary(earnings) }
        item {
            Text(
                text = "از هر خرید ${persianDigits("${100 - earnings.cutPercent}")}٪ سهم شما و ${persianDigits("${earnings.cutPercent}")}٪ سهم ملووا است. " +
                    "سهم شما فعلاً فقط ثبت می‌شود؛ پرداخت به استادها هنوز شروع نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
        item { SectionHeader(title = "خریدها") }
        if (earnings.sales.isEmpty()) {
            item { EmptyHint(Icons.Outlined.Payments, "هنوز خریدی نیست. وقتی هنرجویی درسی از شما بخرد، اینجا می‌آید.") }
        } else {
            items(earnings.sales, key = { it.id }) { sale ->
                SaleRow(sale, lessonTitle = lessons.firstOrNull { it.id == sale.lessonId }?.title)
            }
        }
    }
}

@Composable
private fun EarningsSummary(earnings: TeacherEarnings) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("سهم شما در این ماه", style = MaterialTheme.typography.titleSmall, color = MelovaTextMuted)
            Text(
                text = formatEuro(earnings.thisMonth.teacherCents),
                style = MaterialTheme.typography.headlineSmall,
                color = MelovaText
            )
            ReportLine("خرید در این ماه", persianDigits(earnings.thisMonth.count.toString()))
            ReportLine("فروش این ماه", formatEuro(earnings.thisMonth.grossCents))
            ReportLine("سهم شما از همه‌ی فروش‌ها", formatEuro(earnings.total.teacherCents))
            ReportLine("همه‌ی فروش‌ها", formatEuro(earnings.total.grossCents))
        }
    }
}

/** One purchase: who bought which lesson and version, and the teacher's share of it. */
@Composable
private fun SaleRow(sale: TeacherSale, lessonTitle: String?) {
    MelovaSurfaceCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(MelovaDimens.ListLead).clip(CircleShape).background(MelovaCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Payments, contentDescription = null, tint = MelovaSuccess, modifier = Modifier.size(MelovaDimens.Icon))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = lessonTitle ?: "درس",
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (sale.upgrade) "ارتقا به ${tierName(sale.tier)}" else tierName(sale.tier))
                        append(" • ")
                        append(sale.studentName.ifBlank { "هنرجو" })
                        append(" • ")
                        append(formatMessageDay(sale.createdAtMs))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(formatEuro(sale.teacherCents), style = MaterialTheme.typography.titleSmall, color = MelovaSuccess)
        }
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
                            text = state.privateHourlyRateCents?.let(::formatEuro) ?: "تعیین نشده؛ برای مقایسه وارد کنید",
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
        var rateText by rememberSaveable { mutableStateOf(state.privateHourlyRateCents?.let(::euroInput).orEmpty()) }
        FormDialog(
            title = "درآمد هر ساعت کلاس خصوصی",
            confirmLabel = "ذخیره",
            onConfirm = {
                viewModel.setPrivateHourlyRate(parseEuroCents(rateText)?.takeIf { it > 0 })
                editingRate = false
            },
            onDismiss = { editingRate = false }
        ) {
            MelovaTextField(value = rateText, onValueChange = { rateText = it.take(9) }, label = "یورو در ساعت")
            parseEuroCents(rateText)?.takeIf { it > 0 }?.let {
                Text(formatEuro(it), style = MaterialTheme.typography.bodySmall, color = MelovaAccentSoft)
            }
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
                text = report.netPerHourCents?.let(::formatEuro) ?: "هنوز زمانی ثبت نشده",
                style = MaterialTheme.typography.headlineSmall,
                color = when (report.worthIt) {
                    true -> MelovaSuccess
                    false -> MelovaError
                    null -> MelovaText
                }
            )
            Text(
                text = when (report.worthIt) {
                    true -> "بیشتر از کلاس خصوصی (${report.privateHourlyRateCents?.let(::formatEuro)}). ارزشش را دارد."
                    false -> "کمتر از کلاس خصوصی (${report.privateHourlyRateCents?.let(::formatEuro)}). یا قیمت را بالا ببرید یا زمان بازبینی را کم کنید."
                    null -> if (report.privateHourlyRateCents == null) "درآمد ساعتی کلاس خصوصی را وارد کنید تا مقایسه شود." else "با ثبت زمان، مقایسه انجام می‌شود."
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
            ReportLine("فروش", formatEuro(report.grossCents))
            ReportLine("سهم شما", formatEuro(report.netCents))
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
