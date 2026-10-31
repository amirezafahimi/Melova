package app.melova.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.melova.data.model.Chat
import app.melova.data.model.ChatMessage
import app.melova.data.model.ChatMessageKind
import app.melova.data.model.ChatSide
import app.melova.data.model.PracticeResultLink
import app.melova.data.model.ReviewStatus
import app.melova.platform.LocalFile
import app.melova.platform.formatMessageDay
import app.melova.platform.formatMessageTime
import app.melova.platform.formatPersianInteger
import app.melova.platform.formatTimeOfDay
import app.melova.platform.localDay
import app.melova.platform.rememberMicPermission
import app.melova.presentation.ChatUiState
import app.melova.presentation.ChatViewModel
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaNavRow
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.ScoreBadge
import app.melova.ui.components.TakeKind
import app.melova.ui.components.TakePhase
import app.melova.ui.components.formatClock
import app.melova.ui.components.rememberTakeController
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.PageBottomGap
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaBubbleReceived
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaControl
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

private const val MAX_TEXT_LENGTH = 4_000

/** The material tick is drawn on a 24-unit square and its lowest point is at 19. */
private const val ICON_UNITS = 24
private const val TICK_BOTTOM_UNIT = 19
private const val ARROW_LINE_UNIT = 17
private const val VOICE_EMPTY_BARS = 48
private const val VOICE_BAR_FILL = 0.6f

/** The live connection to the chats, open while the app is on screen. */
@Composable
internal fun StayConnected(key: Any?, viewModel: ChatViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, key) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.stayConnected() }
    }
}

@Composable
fun ChatListScreen(
    state: ChatUiState,
    onAppear: () -> Unit,
    onChatClick: (String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onAppear() }

    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { MelovaTopBar(title = "گفتگوها", onBack = onBack) }
                when {
                    state.chats.isNotEmpty() -> items(state.chats, key = { it.id }) { chat ->
                        ChatRow(chat = chat, onClick = { onChatClick(chat.id) })
                    }
                    state.hasLoaded -> item { EmptyChats() }
                    state.listError != null -> item {
                        Text(state.listError, style = MaterialTheme.typography.bodyMedium, color = MelovaError)
                    }
                    else -> item { CenteredSpinner() }
                }
            }
        }
    }
}

@Composable
private fun EmptyChats() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = MelovaTextMuted,
            modifier = Modifier.size(40.dp)
        )
        Text("هنوز گفتگویی ندارید", style = MaterialTheme.typography.titleMedium, color = MelovaText)
        Text(
            "گفتگو با فرستادن نتیجه‌ی تمرین برای استاد باز می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MelovaTextMuted
        )
    }
}

@Composable
private fun CenteredSpinner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, color = MelovaAccent, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ChatRow(chat: Chat, onClick: () -> Unit) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MelovaAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.peerName.take(1), style = MaterialTheme.typography.titleMedium, color = MelovaAccentSoft)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = chat.peerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    chat.lastMessage?.takeIf { it.sender == chat.side }?.let { SeenMark(it.id <= chat.peerReadId) }
                    Text(
                        text = chat.lastMessage?.preview(chat.side).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatMessageTime(chat.lastMessageAtMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaTextMuted
                )
                if (chat.unread > 0) UnreadBadge(chat.unread)
            }
        }
    }
}

private fun ChatMessage.preview(side: ChatSide): String {
    val body = when (kind) {
        ChatMessageKind.TEXT -> text
        ChatMessageKind.VOICE -> "پیام صوتی"
        ChatMessageKind.PRACTICE -> "نتیجه‌ی تمرین: ${practice?.sectionTitle.orEmpty()}"
    }
    return if (sender == side) "شما: $body" else body
}

/**
 * One tick once the server has a message, two once the other side has seen it. Sits on the
 * baseline of the text beside it, which centring can't do: the text's digits ride high in their box.
 */
/** Material icons are drawn on 24 units; this rests the icon's [unit] on the baseline of the text beside it. */
private fun RowScope.iconOnBaseline(unit: Int) = Modifier.alignBy { it.measuredHeight * unit / ICON_UNITS }

@Composable
private fun RowScope.SeenMark(isSeen: Boolean) {
    Icon(
        imageVector = if (isSeen) Icons.Filled.DoneAll else Icons.Filled.Done,
        contentDescription = if (isSeen) "دیده شد" else "ارسال شد",
        tint = if (isSeen) MelovaAccentSoft else MelovaTextMuted,
        modifier = iconOnBaseline(TICK_BOTTOM_UNIT).size(15.dp)
    )
}

@Composable
internal fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(MelovaAccent)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(formatPersianInteger(count.toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

/** The way into the chats from Home. */
@Composable
internal fun ChatsIconButton(unread: Int, onClick: () -> Unit) {
    Box {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MelovaAccentTint,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "گفتگوها", tint = MelovaAccentSoft)
            }
        }
        if (unread > 0) UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
    }
}

/** The way into the chats from Profile. */
@Composable
internal fun ChatsEntryRow(unread: Int, onClick: () -> Unit) {
    MelovaNavRow(
        icon = Icons.Outlined.ChatBubbleOutline,
        title = "گفتگوها",
        subtitle = "پاسخ استادها و نتیجه‌ی تمرین‌هایی که فرستاده‌اید",
        onClick = onClick,
        trailing = { if (unread > 0) UnreadBadge(unread) }
    )
}

@Composable
fun ChatScreen(
    chatId: String,
    state: ChatUiState,
    viewModel: ChatViewModel,
    onOpenPractice: (PracticeResultLink) -> Unit,
    onBack: () -> Unit
) {
    DisposableEffect(chatId) {
        viewModel.openThread(chatId)
        onDispose { viewModel.closeThread() }
    }

    val thread = state.thread?.takeIf { it.chat.id == chatId }
    val chat = thread?.chat ?: state.chats.firstOrNull { it.id == chatId }

    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    MelovaTopBar(
                        title = chat?.peerName?.ifBlank { null } ?: "گفتگو",
                        subtitle = chat?.let { if (it.side == ChatSide.STUDENT) "استاد" else "هنرجو" },
                        onBack = onBack
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        thread?.messages?.isEmpty() == true -> EmptyThread()
                        thread != null -> Messages(
                            chat = thread.chat,
                            messages = thread.messages,
                            state = state,
                            onSeen = viewModel::markSeen,
                            onToggleVoice = viewModel::toggleVoice,
                            onSeekVoice = viewModel::seekVoice,
                            onOpenPractice = onOpenPractice
                        )
                        state.threadError != null -> Text(
                            text = state.threadError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MelovaError,
                            modifier = Modifier.padding(20.dp)
                        )
                        else -> CenteredSpinner()
                    }
                }
                ChatComposer(
                    isSending = state.isSending,
                    onRecordStart = viewModel::stopVoice,
                    onSendText = viewModel::sendText,
                    onSendVoice = viewModel::sendVoice
                )
            }
        }
    }
}

@Composable
private fun EmptyThread() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("هنوز پیامی نیست. اولین پیام را بفرستید.", style = MaterialTheme.typography.bodyMedium, color = MelovaTextMuted)
    }
}

@Composable
private fun Messages(
    chat: Chat,
    messages: List<ChatMessage>,
    state: ChatUiState,
    onSeen: () -> Unit,
    onToggleVoice: (ChatMessage) -> Unit,
    onSeekVoice: (Float) -> Unit,
    onOpenPractice: (PracticeResultLink) -> Unit
) {
    val side = chat.side
    val listState = rememberLazyListState()
    val newest = messages.lastOrNull()
    // Newest sits at index 0 of the reversed list; someone reading back up stays where they are.
    LaunchedEffect(newest?.id) {
        if (newest != null && (newest.sender == side || listState.firstVisibleItemIndex <= 1)) {
            listState.animateScrollToItem(0)
        }
    }

    // Seen means on screen: not while reading back up, and not with the app in the background.
    val isAtNewest by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, newest?.id, chat.unread, isAtNewest) {
        if (isAtNewest) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { onSeen() }
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
    ) {
        val newestFirst = messages.asReversed()
        itemsIndexed(newestFirst, key = { _, message -> message.id }) { index, message ->
            val isMine = message.sender == side
            // Part of the row, so the newest message stays item 0 for the checks above.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val day = localDay(message.createdAtMs)
                val older = newestFirst.getOrNull(index + 1)
                if (older == null || localDay(older.createdAtMs) != day) DayHeading(message.createdAtMs)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    // The screen is RTL, so the person's own messages sit on the right.
                    horizontalAlignment = if (isMine) Alignment.Start else Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    when (message.kind) {
                        ChatMessageKind.TEXT -> TextBubble(message.text, isMine)
                        ChatMessageKind.VOICE -> VoiceBubble(
                            message = message,
                            isMine = isMine,
                            state = state,
                            onToggle = { onToggleVoice(message) },
                            onSeek = onSeekVoice
                        )
                        ChatMessageKind.PRACTICE -> message.practice?.let { practice ->
                            PracticeBubble(practice, note = message.text, onClick = { onOpenPractice(practice) })
                        }
                    }
                    // Only the day's last message carries the time and the tick.
                    val newer = newestFirst.getOrNull(index - 1)
                    if (newer == null || localDay(newer.createdAtMs) != day) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = formatTimeOfDay(message.createdAtMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MelovaTextMuted,
                                modifier = Modifier.alignByBaseline()
                            )
                            if (isMine) SeenMark(message.id <= chat.peerReadId)
                        }
                    }
                }
            }
        }
    }
}

/** Sits over the first message of each day, so the rows themselves only need the hour. */
@Composable
private fun DayHeading(timeMs: Long) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            text = formatMessageDay(timeMs),
            style = MaterialTheme.typography.labelMedium,
            color = MelovaTextMuted,
            modifier = Modifier
                .clip(CircleShape)
                .background(MelovaText.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun bubbleColor(isMine: Boolean) = if (isMine) MelovaAccent.copy(alpha = 0.22f) else MelovaBubbleReceived

@Composable
private fun TextBubble(text: String, isMine: Boolean) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bubbleColor(isMine),
        modifier = Modifier.widthIn(max = 300.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MelovaText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun VoiceBubble(
    message: ChatMessage,
    isMine: Boolean,
    state: ChatUiState,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit
) {
    val isActive = state.activeVoiceId == message.id
    val isLoading = isActive && state.isVoiceLoading
    val isPlaying = isActive && state.isVoicePlaying
    val totalMs = if (isActive && state.voiceDurationMs > 0L) state.voiceDurationMs else message.durationMs ?: 0L

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bubbleColor(isMine),
        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth()
    ) {
        // Sound runs left to right, on a Persian screen too.
        LtrContent {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onToggle,
                    enabled = !isLoading,
                    shape = CircleShape,
                    color = MelovaAccent,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "توقف" else "پخش",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    VoiceWave(
                        peaks = message.peaks,
                        progress = if (isActive && totalMs > 0L) state.voicePositionMs.toFloat() / totalMs else 0f,
                        onTap = { fraction ->
                            if (!isActive) onToggle() else if (!isLoading) onSeek(fraction)
                        },
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    )
                    Text(
                        text = formatClock(if (isActive) state.voicePositionMs else totalMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaTextMuted
                    )
                }
            }
        }
    }
}

/** The voice's own sound, filling from the left as it plays. */
@Composable
private fun VoiceWave(peaks: List<Float>, progress: Float, onTap: (Float) -> Unit, modifier: Modifier = Modifier) {
    val played = MelovaAccentSoft
    val unplayed = MelovaControl
    val tap by rememberUpdatedState(onTap)
    // A voice the server couldn't read keeps the same place, as a flat line of bars.
    val bars = peaks.ifEmpty { List(VOICE_EMPTY_BARS) { 0f } }
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset -> tap((offset.x / size.width).coerceIn(0f, 1f)) }
        }
    ) {
        val slot = size.width / bars.size
        val barWidth = slot * VOICE_BAR_FILL
        val drawBars = { color: Color ->
            bars.forEachIndexed { index, bar ->
                val barHeight = (bar.coerceIn(0f, 1f) * size.height).coerceAtLeast(barWidth)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, (size.height - barHeight) / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
        }
        drawBars(unplayed)
        clipRect(right = size.width * progress.coerceIn(0f, 1f)) { drawBars(played) }
    }
}

/** A take the student sent; tapping it opens the page the student saw. */
@Composable
private fun PracticeBubble(practice: PracticeResultLink, note: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = MelovaCard,
        modifier = Modifier
            .widthIn(max = 310.dp)
            .fillMaxWidth()
            .border(1.dp, MelovaAccent.copy(alpha = 0.55f), shape)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val score = practice.score
                if (score != null) {
                    ScoreBadge(score = score)
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MelovaOutline.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = MelovaTextMuted)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("نتیجه‌ی تمرین", style = MaterialTheme.typography.labelMedium, color = MelovaAccentSoft)
                    Text(
                        text = practice.sectionTitle.ifBlank { "اجرا" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = practice.lessonTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (note.isNotBlank()) {
                Text(note, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (practice.status) {
                    ReviewStatus.OPEN -> MelovaChip("در انتظار استاد", color = MelovaGold)
                    ReviewStatus.ANSWERED -> MelovaChip("پاسخ داده شد", color = MelovaSuccess)
                    ReviewStatus.REFUNDED -> MelovaChip("بسته شد", color = MelovaTextMuted)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    Text(
                        text = "دیدن نتیجه",
                        style = MaterialTheme.typography.labelLarge,
                        color = MelovaAccentSoft,
                        modifier = Modifier.alignByBaseline()
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MelovaAccentSoft,
                        modifier = iconOnBaseline(ARROW_LINE_UNIT).size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    isSending: Boolean,
    onRecordStart: () -> Unit,
    onSendText: (text: String, onSent: () -> Unit) -> Unit,
    onSendVoice: (voice: LocalFile, durationMs: Long, onSent: () -> Unit) -> Unit
) {
    val take = rememberTakeController(TakeKind.VOICE)
    var text by rememberSaveable { mutableStateOf("") }
    var sendWhenReady by remember { mutableStateOf(false) }

    val startRecording = {
        onRecordStart()
        take.record()
    }
    val permission = rememberMicPermission { granted ->
        if (granted) startRecording() else take.showError("برای ضبط، اجازه‌ی دسترسی به میکروفون لازم است.")
    }
    val sendTake = {
        val voice = take.audio
        if (take.isTooShort) {
            take.clear()
            take.showError("پیام صوتی خیلی کوتاه است.")
        } else if (voice != null) {
            onSendVoice(voice, take.durationMs) { take.clear() }
        }
    }

    // Stopping a recording takes a moment; the upload goes out as soon as the file is ready.
    LaunchedEffect(take.phase) {
        when (take.phase) {
            TakePhase.READY -> if (sendWhenReady) {
                sendWhenReady = false
                sendTake()
            }
            TakePhase.EMPTY -> sendWhenReady = false
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = PageBottomGap),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        take.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MelovaError)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (take.phase) {
                TakePhase.EMPTY -> {
                    MessageField(
                        value = text,
                        onValueChange = { text = it.take(MAX_TEXT_LENGTH) },
                        modifier = Modifier.weight(1f)
                    )
                    if (text.isBlank()) {
                        RoundAction(Icons.Filled.Mic, "ضبط پیام صوتی") {
                            if (permission.isGranted()) startRecording() else permission.request()
                        }
                    } else {
                        RoundAction(Icons.AutoMirrored.Filled.Send, "ارسال", isBusy = isSending) {
                            onSendText(text) { text = "" }
                        }
                    }
                }
                TakePhase.COUNT_IN, TakePhase.RECORDING, TakePhase.PAUSED, TakePhase.PROCESSING -> {
                    RoundAction(Icons.Outlined.DeleteOutline, "حذف", isQuiet = true, onClick = take::clear)
                    TakeStatus(
                        text = if (take.phase == TakePhase.PROCESSING) "آماده‌سازی…" else formatClock(take.elapsedMs),
                        modifier = Modifier.weight(1f),
                        isRecording = take.phase != TakePhase.PROCESSING
                    )
                    RoundAction(
                        Icons.AutoMirrored.Filled.Send,
                        "ارسال",
                        isBusy = take.phase == TakePhase.PROCESSING
                    ) {
                        sendWhenReady = true
                        take.finish()
                    }
                }
                // Only an upload that failed stops here; the take waits for another try.
                TakePhase.READY -> {
                    RoundAction(Icons.Outlined.DeleteOutline, "حذف", isQuiet = true, onClick = take::clear)
                    TakeStatus(
                        text = "پیام صوتی ${formatClock(take.durationMs)}",
                        modifier = Modifier.weight(1f),
                        isRecording = false
                    )
                    RoundAction(Icons.AutoMirrored.Filled.Send, "ارسال", isBusy = isSending, onClick = sendTake)
                }
            }
        }
    }
}

@Composable
private fun MessageField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("پیام", color = MelovaTextMuted) },
        maxLines = 4,
        shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MelovaAccent,
            unfocusedBorderColor = MelovaControl,
            cursorColor = MelovaAccent,
            focusedTextColor = MelovaText,
            unfocusedTextColor = MelovaText
        ),
        modifier = modifier
    )
}

@Composable
private fun TakeStatus(text: String, isRecording: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MelovaCardElevated)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MelovaError)
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    isBusy: Boolean = false,
    isQuiet: Boolean = false,
    onClick: () -> Unit
) {
    val iconColor = if (isQuiet) MelovaAccentSoft else Color.White
    Surface(
        onClick = onClick,
        enabled = !isBusy,
        shape = CircleShape,
        color = if (isQuiet) MelovaAccentTint else MelovaAccent,
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isBusy) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = iconColor, modifier = Modifier.size(20.dp))
            } else {
                Icon(icon, contentDescription = description, tint = iconColor)
            }
        }
    }
}
