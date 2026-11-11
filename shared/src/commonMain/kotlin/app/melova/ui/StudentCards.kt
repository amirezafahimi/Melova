package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import app.melova.platform.PlatformBackHandler
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaCard
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.data.model.LessonOffer
import app.melova.data.model.LessonTier
import app.melova.data.model.MessageState
import app.melova.data.model.formatEuro
import app.melova.platform.currentTimeMillis
import app.melova.presentation.LoginGate
import app.melova.presentation.TeacherAskState
import app.melova.ui.components.FormDialog
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/**
 * Under the score: send this take to the teacher. When the scorer itself wasn't sure
 * ([referralReasons] not empty) the card leads with that. A take sent is one of the lesson's
 * messages to the teacher, so the card says how many are left, and offers «درس با استاد»
 * ([onSeeVersions]) once they run out.
 */
@Composable
internal fun AskTeacherCard(
    state: TeacherAskState,
    referralReasons: List<String>,
    onSend: (note: String) -> Unit,
    /** A guest signs in first; the take waits here and they carry on when they come back. */
    loginGate: LoginGate = LoginGate.OPEN,
    onLogin: () -> Unit = {},
    onOpenChat: (chatId: String) -> Unit = {},
    /** Null without an account, or offline: the server decides when the take is sent. */
    offer: LessonOffer? = null,
    /** A free lesson gives its messages on the first take sent, so having it isn't needed first. */
    isFreeLesson: Boolean = true,
    onSeeVersions: () -> Unit = {}
) {
    var composing by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    val isReferral = referralReasons.isNotEmpty()
    val startComposing = rememberLoginGated(loginGate, onLogin) { composing = true }
    val nowMs = currentTimeMillis()
    val sendState = offer?.sendState(nowMs)?.let { found ->
        if (found == MessageState.NotOwned && isFreeLesson) MessageState.Left(offer.lessonMessages, Long.MAX_VALUE) else found
    }
    val blocked = !state.isSent && (sendState is MessageState.UsedUp || sendState is MessageState.Expired || sendState == MessageState.NotOwned)
    val canUpgrade = when (sendState) {
        is MessageState.UsedUp -> sendState.canUpgrade
        is MessageState.Expired -> sendState.canUpgrade
        MessageState.NotOwned -> true
        else -> false
    }

    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (state.isSent) Icons.Outlined.CheckCircle else Icons.Outlined.SupportAgent,
                    contentDescription = null,
                    tint = when {
                        state.isSent -> MelovaSuccess
                        blocked -> MelovaTextMuted
                        isReferral -> MelovaGold
                        else -> MelovaSecondary
                    },
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.isSent -> "برای استاد فرستاده شد"
                            sendState == MessageState.NotOwned -> "فرستادن برای استاد با خرید درس"
                            sendState is MessageState.Expired -> "مهلت پیام‌ها به استاد تمام شده"
                            sendState is MessageState.UsedUp -> "پیام‌ها به استاد تمام شده"
                            isReferral -> "بهتر است استاد این اجرا را بشنود"
                            else -> "بپرس از استاد"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText
                    )
                    Text(
                        text = when {
                            state.isSent -> "این اجرا و پاسخ استاد در گفتگوی شما با استاد است."
                            sendState == MessageState.NotOwned ->
                                "با خرید درس، اجراهایتان را برای استاد می‌فرستید تا خودش گوش دهد."
                            blocked && canUpgrade ->
                                "با «درس با استاد» پیام‌هایتان به استاد روی هم به ${persianCount(offer.teacherMessages)} می‌رسد، تا ${persianCount(offer.messageMonths)} ماه. تمرین همچنان بی‌حد است."
                            blocked -> "تمرین همچنان بی‌حد است و در گفتگو هم می‌توانید برای استاد بنویسید."
                            isReferral -> "تحلیل خودکار درباره‌ی این اجرا مطمئن نیست."
                            else -> "همین اجرا را برای استاد بفرستید تا خودش گوش دهد و پاسخ بدهد."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
            if (isReferral && !state.isSent && !blocked) {
                referralReasons.forEach { reason ->
                    Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
                }
            }
            state.error?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall, color = MelovaError)
            }
            if (state.isSent && state.chatId != null) {
                Button(
                    onClick = { onOpenChat(state.chatId) },
                    shape = MelovaSmallShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
                ) {
                    Text("باز کردن گفتگو با استاد")
                }
            }
            when {
                state.isSent -> Unit
                blocked && canUpgrade -> Button(
                    onClick = onSeeVersions,
                    shape = MelovaSmallShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
                ) { Text(if (sendState == MessageState.NotOwned) "دیدن نسخه‌های درس" else "دیدن «درس با استاد»") }
                blocked -> Unit
                else -> Button(
                    onClick = startComposing,
                    enabled = !state.isSending,
                    shape = MelovaSmallShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isReferral) MelovaGold else MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text("  در حال ارسال…")
                    } else {
                        Text("ارسال برای استاد")
                    }
                }
            }
            // How many are left, after sending too: the next take counts against the same few.
            val left = offer?.takeIf { sendState is MessageState.Left && sendState.untilMs != Long.MAX_VALUE }
                ?.let { messagesLine(it, nowMs) }
            if (left != null) {
                Text(left, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            } else if (sendState is MessageState.Left && !state.isSent) {
                Text(
                    "این درس رایگان ${persianCount(offer.lessonMessages)} پیام به استاد دارد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
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

private fun persianCount(count: Int): String = app.melova.platform.formatPersianInteger(count.toLong())

/** What this student can buy of [this]; with no account yet, the lesson's own prices. */
internal fun Lesson.saleOffer(): LessonOffer = offer ?: LessonOffer(
    lessonPriceNowCents = priceCents.takeIf { !isFree },
    teacherPriceNowCents = teacherPriceCents.takeIf { sellsWithTeacher }
)

private fun LessonOffer.choices(): List<Pair<LessonTier, Long>> = buildList {
    lessonPriceNowCents?.let { add(LessonTier.LESSON to it) }
    teacherPriceNowCents?.let { add(LessonTier.WITH_TEACHER to it) }
}

/**
 * The short card on the lesson page: what buying gives in one line and a button that opens
 * [LessonOfferSheet]. Short on purpose, so a long list of sections never pushes it off screen.
 */
@Composable
internal fun LessonOfferCard(lesson: Lesson, isBusy: Boolean, onOpen: () -> Unit) {
    val offer = lesson.saleOffer()
    val choices = offer.choices()
    if (choices.isEmpty()) return
    val lockedSections = (lesson.sections.size - 1).coerceAtLeast(0)

    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = MelovaGold,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(offerTitle(lesson, offer), style = MaterialTheme.typography.titleMedium, color = MelovaText)
                    Text(
                        text = when {
                            offer.owned -> messagesLine(offer, currentTimeMillis()) ?: ownershipRule(offer)
                            lesson.isFree -> "این درس رایگان است. با «درس با استاد» اجراهایتان را برای استاد می‌فرستید."
                            lockedSections > 0 ->
                                "بخش اول رایگان است. ${persianCount(lockedSections)} بخش دیگر با خرید باز می‌شود."
                            else -> "با خرید، اجراهایتان را برای استاد هم می‌فرستید."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
            Button(
                onClick = onOpen,
                enabled = !isBusy,
                shape = MelovaSmallShape,
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text("  در حال پرداخت…")
                } else {
                    val from = choices.minOf { it.second }
                    Text(
                        when {
                            choices.size > 1 -> "دیدن نسخه‌ها، از ${formatEuro(from)}"
                            offer.ownedTier == LessonTier.LESSON -> "ارتقا، ${formatEuro(from)}"
                            else -> "دیدن «${tierName(choices.first().first)}»، ${formatEuro(from)}"
                        }
                    )
                }
            }
        }
    }
}

private fun offerTitle(lesson: Lesson, offer: LessonOffer): String = when {
    offer.ownedTier == LessonTier.LESSON -> "ارتقا به «درس با استاد»"
    offer.owned || lesson.isFree -> "«درس با استاد»"
    else -> "خرید این درس"
}

/**
 * The versions to pick from, in a bottom sheet: each with its price and what it gives, the rule
 * (the lesson is theirs for good, messages for three months) and the pay button. Without an
 * account the button signs in first, and the sheet opens again once they are back.
 * Kept outside the page's list so it survives scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonOfferSheet(
    lesson: Lesson,
    loginGate: LoginGate,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onBuy: (LessonTier) -> Unit
) {
    val offer = lesson.saleOffer()
    val choices = offer.choices()
    var picked by rememberSaveable(lesson.id) { mutableStateOf(choices.firstOrNull()?.first ?: LessonTier.LESSON) }
    if (choices.isNotEmpty() && choices.none { it.first == picked }) picked = choices.first().first
    var reopenAfterLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loginGate) {
        when (loginGate) {
            LoginGate.SIGN_IN -> reopenAfterLogin = false
            LoginGate.WAIT -> Unit
            LoginGate.OPEN -> if (reopenAfterLogin) {
                reopenAfterLogin = false
                onOpenChange(true)
            }
        }
    }
    if (!open || choices.isEmpty()) return
    val price = choices.first { it.first == picked }.second

    PlatformBackHandler(onBack = { onOpenChange(false) })
    ModalBottomSheet(
        onDismissRequest = { onOpenChange(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MelovaCard
    ) {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    offerTitle(lesson, offer),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MelovaText
                )
                choices.forEach { (tier, cents) ->
                    TierOption(
                        title = tierName(tier),
                        detail = tierPromise(if (tier == LessonTier.WITH_TEACHER) offer.teacherMessages else offer.lessonMessages),
                        price = formatEuro(cents),
                        selected = tier == picked,
                        single = choices.size == 1,
                        onClick = { picked = tier }
                    )
                }
                Text(
                    if (offer.owned) upgradeRule(offer) else ownershipRule(offer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
                Button(
                    onClick = {
                        onOpenChange(false)
                        if (loginGate == LoginGate.SIGN_IN) {
                            reopenAfterLogin = true
                            onLogin()
                        } else {
                            onBuy(picked)
                        }
                    },
                    enabled = loginGate != LoginGate.WAIT,
                    shape = MelovaSmallShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(MelovaDimens.PrimaryButtonHeight)
                ) {
                    Text(
                        when {
                            loginGate == LoginGate.SIGN_IN -> "ورود برای خرید"
                            offer.ownedTier == LessonTier.LESSON -> "پرداخت ${formatEuro(price)} و ارتقا"
                            else -> "پرداخت ${formatEuro(price)}"
                        }
                    )
                }
            }
        }
    }
}

/** One version to pick: its name and price, and what it gives in one line. */
@Composable
private fun TierOption(
    title: String,
    detail: String,
    price: String,
    selected: Boolean,
    single: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MelovaSmallShape)
            .border(1.dp, if (selected && !single) MelovaAccent else MelovaOutline, MelovaSmallShape)
            .selectable(selected = selected, enabled = !single, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!single) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = MelovaAccent, unselectedColor = MelovaTextMuted)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MelovaText)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
        Text(price, style = MaterialTheme.typography.titleSmall, color = MelovaSecondary)
    }
}

/** A free lesson not yet in the student's list. Singing in it adds it as well. */
@Composable
internal fun FreeLessonCard(isBusy: Boolean, onAdd: () -> Unit) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("این درس رایگان است", style = MaterialTheme.typography.titleMedium, color = MelovaText)
            Text(
                text = "اضافه کنید تا در صفحه‌ی تمرین بماند.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted
            )
            Button(
                onClick = onAdd,
                enabled = !isBusy,
                shape = MelovaSmallShape,
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight)
            ) { Text("افزودن به درس‌های من") }
        }
    }
}
