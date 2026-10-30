package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.data.model.LessonPayment
import app.melova.data.model.LessonPurchaseStatus
import app.melova.presentation.TeacherAskState
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.layout.LtrContent
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
 * ([referralReasons] not empty) the card leads with that.
 */
@Composable
internal fun AskTeacherCard(
    state: TeacherAskState,
    referralReasons: List<String>,
    onSend: (note: String) -> Unit,
    /** A guest signs in first; the take waits here and they carry on when they come back. */
    loginRequired: Boolean = false,
    onLogin: () -> Unit = {},
    onOpenChat: (chatId: String) -> Unit = {}
) {
    var composing by rememberSaveable { mutableStateOf(false) }
    var composeAfterLogin by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    val isReferral = referralReasons.isNotEmpty()

    LaunchedEffect(loginRequired) {
        if (!loginRequired && composeAfterLogin) {
            composeAfterLogin = false
            composing = true
        }
    }

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
                            state.isSent -> "این اجرا و پاسخ استاد در گفتگوی شما با استاد است."
                            isReferral -> "تحلیل خودکار درباره‌ی این اجرا مطمئن نیست."
                            else -> "همین اجرا را برای استاد بفرستید تا خودش گوش دهد و پاسخ بدهد."
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
            }
            if (state.isSent && state.chatId != null) {
                OutlinedButton(
                    onClick = { onOpenChat(state.chatId) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("رفتن به گفتگو", color = MelovaText)
                }
            }
            if (!state.isSent) {
                Button(
                    onClick = {
                        if (loginRequired) {
                            composeAfterLogin = true
                            onLogin()
                        } else {
                            composing = true
                        }
                    },
                    enabled = !state.isSending,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isReferral) MelovaGold else MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text("  در حال ارسال…")
                    } else {
                        Text("ارسال برای استاد")
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

/** A paid lesson this student doesn't have yet: they pay by card and the teacher opens it. */
@Composable
internal fun LessonPurchaseCard(
    lesson: Lesson,
    loginRequired: Boolean,
    isBusy: Boolean,
    onLogin: () -> Unit,
    onBuy: (payer: String) -> Unit
) {
    var paying by rememberSaveable { mutableStateOf(false) }
    val status = lesson.purchaseStatus
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (status == LessonPurchaseStatus.PENDING) Icons.Outlined.HourglassTop else Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = MelovaGold,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (status) {
                            LessonPurchaseStatus.PENDING -> "پرداخت شما ثبت شد"
                            LessonPurchaseStatus.REJECTED -> "استاد پرداخت شما را پیدا نکرد"
                            null -> "خرید این درس"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText
                    )
                    if (lesson.priceToman > 0) {
                        Text(formatToman(lesson.priceToman), style = MaterialTheme.typography.bodyMedium, color = MelovaSecondary)
                    }
                }
            }
            Text(
                text = when {
                    status == LessonPurchaseStatus.PENDING -> "استاد پرداخت را بررسی می‌کند و بعد از تأیید، درس باز می‌شود."
                    status == LessonPurchaseStatus.REJECTED -> "اگر مبلغ را واریز کرده‌اید، نام واریزکننده را بررسی کنید و دوباره ثبت کنید."
                    lesson.payment == null && !loginRequired -> "فروش این درس هنوز شروع نشده است."
                    else -> "با خرید، همه‌ی بخش‌ها برای همیشه باز می‌شود و می‌توانید تمرینتان را برای استاد بفرستید."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            when {
                status == LessonPurchaseStatus.PENDING -> Unit
                loginRequired -> Button(
                    onClick = onLogin,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text("ورود برای خرید درس") }
                lesson.payment != null -> Button(
                    onClick = { paying = true },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text(if (status == LessonPurchaseStatus.REJECTED) "ثبت دوباره‌ی پرداخت" else "خرید درس") }
            }
        }
    }

    if (paying && lesson.payment != null) {
        PayByCardDialog(
            lesson = lesson,
            payment = lesson.payment,
            onConfirm = { payer ->
                paying = false
                onBuy(payer)
            },
            onDismiss = { paying = false }
        )
    }
}

@Composable
private fun PayByCardDialog(
    lesson: Lesson,
    payment: LessonPayment,
    onConfirm: (payer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var payer by rememberSaveable { mutableStateOf("") }
    FormDialog(
        title = "خرید «${lesson.title}»",
        confirmLabel = "پرداخت کردم",
        confirmEnabled = payer.isNotBlank(),
        onConfirm = { onConfirm(payer) },
        onDismiss = onDismiss
    ) {
        Text(
            text = "مبلغ را به کارت استاد واریز کنید و نام واریزکننده را بنویسید تا استاد پرداخت را پیدا کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MelovaTextMuted
        )
        if (lesson.priceToman > 0) {
            Text(formatToman(lesson.priceToman), style = MaterialTheme.typography.titleMedium, color = MelovaSecondary)
        }
        CardNumberRow(payment.cardNumber)
        if (payment.cardHolder.isNotBlank()) {
            Text("به نام ${payment.cardHolder}", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
        MelovaTextField(value = payer, onValueChange = { payer = it.take(120) }, label = "نام واریزکننده")
    }
}

/** Tap to copy; the digits read left to right whatever the screen's direction. */
@Composable
private fun CardNumberRow(cardNumber: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            LtrContent {
                Text(
                    text = cardNumber.chunked(4).joinToString(" "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1
                )
            }
        }
        OutlinedButton(
            onClick = {
                clipboard.setText(AnnotatedString(cardNumber))
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

/** A free lesson not yet in the student's list. Singing in it adds it as well. */
@Composable
internal fun FreeLessonCard(isBusy: Boolean, onAdd: () -> Unit) {
    MelovaSurfaceCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("این درس رایگان است", style = MaterialTheme.typography.titleSmall, color = MelovaText)
                Text(
                    text = "اضافه کنید تا در صفحه‌ی تمرین بماند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
            }
            Button(
                onClick = onAdd,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
            ) { Text("افزودن به درس‌های من") }
        }
    }
}
