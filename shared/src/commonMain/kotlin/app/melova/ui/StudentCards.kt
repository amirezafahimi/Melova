package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.data.model.Review
import app.melova.data.model.ReviewStatus
import app.melova.presentation.StudentUiState
import app.melova.presentation.TeacherAskState
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.platform.formatPersianInteger

/** Toman with Persian digits and separators, e.g. «۱۵۰٬۰۰۰ تومان». */
internal fun formatToman(amount: Long): String =
    formatPersianInteger(amount) + " تومان"

/**
 * Under the score: send this take to the teacher. When the scorer itself wasn't sure
 * ([referralReasons] not empty) the card leads with that, and the review is free.
 */
@Composable
internal fun AskTeacherCard(
    state: TeacherAskState,
    referralReasons: List<String>,
    onSend: (note: String) -> Unit
) {
    var composing by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    val isReferral = referralReasons.isNotEmpty()

    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (state.isSent) Icons.Outlined.CheckCircle else Icons.Outlined.SupportAgent,
                    contentDescription = null,
                    tint = when {
                        state.isSent -> MelovaSuccess
                        isReferral -> MelovaGold
                        else -> MelovaSecondary
                    },
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.isSent -> "برای استاد فرستاده شد"
                            isReferral -> "بهتر است استاد این اجرا را بشنود"
                            else -> "بپرس از استاد"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText
                    )
                    Text(
                        text = when {
                            state.isSent -> "پاسخ استاد در صفحه‌ی «من» می‌آید." +
                                (state.creditsLeft?.let { " بازبینی باقی‌مانده: $it" } ?: "")
                            isReferral -> "تحلیل خودکار درباره‌ی این اجرا مطمئن نیست؛ ارسالش برای استاد رایگان است."
                            else -> "همین اجرا را برای استاد بفرستید تا خودش گوش دهد و پاسخ بدهد. یک بازبینی از اعتبار شما کم می‌شود."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
            if (isReferral && !state.isSent) {
                referralReasons.forEach { reason ->
                    Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                }
            }
            state.error?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall, color = MelovaError)
                if (state.needsCredit) {
                    Text(
                        text = "برای خرید بازبینی، کد هنرجویی خود را به استاد بدهید تا پس از پرداخت برایتان ثبت کند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                    state.studentId?.let { StudentCodeRow(it) }
                }
            }
            if (!state.isSent) {
                Button(
                    onClick = { composing = true },
                    enabled = !state.isSending,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isReferral) MelovaGold else MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text("  در حال ارسال…")
                    } else {
                        Text(if (isReferral) "ارسال رایگان برای استاد" else "ارسال برای استاد")
                    }
                }
            }
        }
    }

    if (composing) {
        FormDialog(
            title = "ارسال برای استاد",
            confirmLabel = "ارسال",
            onConfirm = {
                composing = false
                onSend(note)
                note = ""
            },
            onDismiss = { composing = false }
        ) {
            Text(
                text = "اگر سؤال خاصی دارید بنویسید؛ مثلاً «تحریر بخش دوم را درست زدم؟»",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            MelovaTextField(
                value = note,
                onValueChange = { note = it.take(1000) },
                label = "یادداشت برای استاد (اختیاری)",
                singleLine = false,
                minLines = 3
            )
        }
    }
}

/** The student's code, to give the teacher when paying by card; tap to copy. */
@Composable
internal fun StudentCodeRow(studentId: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("کد هنرجویی:", style = MaterialTheme.typography.bodyMedium, color = MelovaTextMuted)
        Text(studentId, style = MaterialTheme.typography.titleSmall, color = MelovaText, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                clipboard.setText(AnnotatedString(studentId))
                copied = true
            },
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(if (copied) " کپی شد" else " کپی", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A paid lesson this student hasn't bought yet. */
@Composable
internal fun LockedLessonCard(lesson: Lesson, studentId: String?) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MelovaGold, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("این درس پولی است", style = MaterialTheme.typography.titleMedium, color = MelovaText)
                    Text(
                        text = buildString {
                            if (lesson.priceToman > 0) append("قیمت: ${formatToman(lesson.priceToman)}")
                            if (lesson.includedReviews > 0) {
                                if (isNotEmpty()) append(" • ")
                                append("همراه با ${lesson.includedReviews} بازبینی استاد")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaSecondary
                    )
                }
            }
            Text(
                text = "فعلاً خرید از طریق خود استاد انجام می‌شود: مبلغ را به استاد پرداخت کنید و کد هنرجویی‌تان را بدهید تا درس برایتان باز شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            if (studentId != null) {
                StudentCodeRow(studentId)
            } else {
                Text("در حال ساخت کد هنرجویی… اتصال اینترنت را بررسی کنید.", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            }
        }
    }
}

/** One take the student sent, with the teacher's reply once it came. */
@Composable
internal fun ReviewReplyCard(
    review: Review,
    lessonTitle: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onTogglePlay: () -> Unit
) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = lessonTitle ?: "درس",
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    modifier = Modifier.weight(1f)
                )
                when (review.status) {
                    ReviewStatus.OPEN -> MelovaChip("در انتظار استاد", color = MelovaGold)
                    ReviewStatus.ANSWERED -> MelovaChip("پاسخ داده شد", color = MelovaSuccess)
                    ReviewStatus.REFUNDED -> MelovaChip("بسته شد", color = MelovaTextMuted)
                }
            }
            if (review.note.isNotBlank()) {
                Text("سؤال شما: ${review.note}", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            }
            if (review.replyText.isNotBlank()) {
                Text(review.replyText, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
            }
            if (review.status == ReviewStatus.REFUNDED) {
                Text("اعتبار این بازبینی به شما برگشت.", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            }
            if (review.hasReplyAudio) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePlay, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (isPlaying) "توقف" else "پخش",
                                tint = MelovaAccent
                            )
                        }
                    }
                    Text("پاسخ صوتی استاد", style = MaterialTheme.typography.bodyMedium, color = MelovaText)
                }
            }
        }
    }
}

/** The student's code, name and review credits, in the profile tab. */
@Composable
internal fun StudentAccountCard(state: StudentUiState, onEditName: () -> Unit) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("حساب هنرجو", style = MaterialTheme.typography.titleMedium, color = MelovaText)
                    Text(
                        text = state.account?.name?.takeIf { it.isNotBlank() } ?: "نامی ثبت نشده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
                IconButton(onClick = onEditName, enabled = state.account != null) {
                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش نام", tint = MelovaTextMuted)
                }
            }
            val credits = state.account?.reviewCredits?.values?.sum() ?: 0
            Text(
                text = if (credits > 0) "بازبینی‌های باقی‌مانده: $credits" else "اعتبار بازبینی ندارید.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (credits > 0) MelovaSuccess else MelovaTextMuted
            )
            when {
                state.studentId != null -> StudentCodeRow(state.studentId)
                state.isLoading -> Text("در حال ساخت حساب…", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                else -> Text(
                    text = state.loadError ?: "حساب هنوز ساخته نشده؛ اتصال اینترنت را بررسی کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
            }
            Text(
                text = "این کد را هنگام پرداخت به استاد بدهید تا درس یا بازبینی برایتان ثبت شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
        }
    }
}
