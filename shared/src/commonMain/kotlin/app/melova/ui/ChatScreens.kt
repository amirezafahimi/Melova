package app.melova.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.melova.data.model.Chat
import app.melova.data.model.Lesson
import app.melova.data.model.MessageState
import app.melova.platform.currentTimeMillis
import app.melova.data.model.ChatMessage
import app.melova.data.model.ChatMessageKind
import app.melova.data.model.ChatSide
import app.melova.data.model.PracticeResultLink
import app.melova.data.model.ReviewStatus
import app.melova.platform.LocalFile
import app.melova.platform.PlatformBackHandler
import app.melova.platform.formatMessageDay
import app.melova.platform.formatMessageTime
import app.melova.platform.formatPersianInteger
import app.melova.platform.formatTimeOfDay
import app.melova.platform.localDay
import app.melova.platform.persianDigits
import app.melova.platform.rememberMicPermission
import app.melova.presentation.ChatUiState
import app.melova.presentation.ChatViewModel
import app.melova.presentation.OutgoingMessage
import app.melova.ui.components.MelovaSoftAction
import app.melova.ui.components.AfarinStamp
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.ForwardArrow
import app.melova.ui.components.LiveLevelBars
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaGroupRow
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.MelovaNavRow
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.MelovaWaveSkeleton
import app.melova.ui.components.ScoreBadge
import app.melova.ui.components.TakeController
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
import app.melova.ui.theme.MelovaCardShape
import app.melova.ui.theme.MelovaControl
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaWarning
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MelovaAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.peerName.take(1), style = MaterialTheme.typography.titleSmall, color = MelovaAccentSoft)
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
        modifier = iconOnBaseline(TICK_BOTTOM_UNIT).size(16.dp)
    )
}

@Composable
internal fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(MelovaAccent)
            .padding(horizontal = 4.dp),
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
    MelovaGroupRow(
        icon = Icons.Outlined.ChatBubbleOutline,
        title = "گفتگوها",
        subtitle = "پاسخ استادها به تمرین‌هایتان",
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
    onBack: () -> Unit,
    /** The student's lessons of [teacherId]: each one's messages left show above the thread. */
    lessonsOf: (teacherId: String) -> List<Lesson> = { emptyList() },
    onOpenLesson: (lessonId: String) -> Unit = {}
) {
    DisposableEffect(chatId) {
        viewModel.openThread(chatId)
        onDispose { viewModel.closeThread() }
    }

    val thread = state.thread?.takeIf { it.chat.id == chatId }
    val chat = thread?.chat ?: state.chats.firstOrNull { it.id == chatId }
    var editingId by rememberSaveable(chatId) { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable(chatId) { mutableStateOf<Long?>(null) }
    // A message taken back while it was open leaves no half-written edit behind.
    val editing = editingId?.let { id -> thread?.messages?.firstOrNull { it.id == id } }
    val deleting = deletingId?.let { id -> thread?.messages?.firstOrNull { it.id == id } }
    val replying = state.replyToId?.let { id -> thread?.messages?.firstOrNull { it.id == id } }

    // Taking a message back can't be undone, for either side, so it is asked for first.
    if (deleting != null) {
        ConfirmDialog(
            title = "حذف پیام؟",
            message = "این پیام برای شما و «${chat?.peerName.orEmpty().ifBlank { "طرف گفتگو" }}» حذف می‌شود و برنمی‌گردد.",
            confirmLabel = "حذف",
            onConfirm = {
                viewModel.deleteMessage(deleting.id)
                deletingId = null
            },
            onDismiss = { deletingId = null }
        )
    }

    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
            ) {
                // Nothing between the bar and the thread: the messages start right under it.
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                    MelovaTopBar(
                        title = chat?.peerName?.ifBlank { null } ?: "گفتگو",
                        subtitle = chat?.let { if (it.side == ChatSide.STUDENT) "استاد" else "هنرجو" },
                        onBack = onBack
                    )
                }
                if (chat?.side == ChatSide.STUDENT && chat.teacherId.isNotBlank()) {
                    MessagesLeftBanner(lessons = lessonsOf(chat.teacherId), onOpenLesson = onOpenLesson)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        thread?.messages?.isEmpty() == true && state.outgoing.isEmpty() -> EmptyThread()
                        thread != null -> Messages(
                            chat = thread.chat,
                            messages = thread.messages,
                            state = state,
                            onSeen = viewModel::markSeen,
                            actions = rememberMessageActions(
                                viewModel = viewModel,
                                onOpenPractice = onOpenPractice,
                                onEdit = { editingId = it },
                                onDelete = { deletingId = it }
                            )
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
                    editing = editing,
                    replying = replying,
                    onCancelEdit = { editingId = null },
                    onCancelReply = viewModel::cancelReply,
                    onSaveEdit = { text ->
                        editing?.let { viewModel.editText(it.id, text) }
                        editingId = null
                    },
                    onRecordStart = viewModel::stopVoice,
                    onSendText = viewModel::sendText,
                    onSendVoice = viewModel::sendVoice
                )
            }
        }
    }
}

/**
 * Above the student's thread: how many takes they can still send this teacher, per lesson,
 * and until when. A lesson whose messages ran out links to «درس با استاد» when it is on offer.
 * Writing in the chat is free, so the banner only speaks of takes.
 */
@Composable
private fun MessagesLeftBanner(lessons: List<Lesson>, onOpenLesson: (String) -> Unit) {
    val nowMs = currentTimeMillis()
    val lines = lessons.mapNotNull { lesson ->
        val offer = lesson.offer ?: return@mapNotNull null
        messagesLine(offer, nowMs)?.let { Triple(lesson, offer, it) }
    }.take(3)
    if (lines.isEmpty()) return
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { (lesson, offer, line) ->
            val runOut = offer.sendState(nowMs) !is MessageState.Left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MelovaSmallShape)
                    .background(MelovaCardElevated)
                    .clickable(onClickLabel = "باز کردن درس") { onOpenLesson(lesson.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "«${lesson.title}»: $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (runOut) MelovaGold else MelovaTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (runOut && offer.canUpgrade) {
                    Text("«درس با استاد»", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
                }
            }
        }
    }
}

/** Everything a message in the thread can have done to it, kept in one place for the rows. */
@Stable
private class MessageActions(
    val toggleVoice: (ChatMessage) -> Unit,
    val scrubVoice: (ChatMessage, Float) -> Unit,
    val seekVoice: (Float) -> Unit,
    val reply: (ChatMessage) -> Unit,
    val edit: (ChatMessage) -> Unit,
    val delete: (ChatMessage) -> Unit,
    val retry: (Long) -> Unit,
    val discard: (Long) -> Unit,
    val openPractice: (PracticeResultLink) -> Unit,
    val stamp: (ChatMessage, Boolean) -> Unit
)

@Composable
private fun rememberMessageActions(
    viewModel: ChatViewModel,
    onOpenPractice: (PracticeResultLink) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
): MessageActions = remember(viewModel) {
    MessageActions(
        toggleVoice = viewModel::toggleVoice,
        scrubVoice = viewModel::scrubVoice,
        seekVoice = viewModel::seekVoice,
        reply = { message -> viewModel.startReply(message.id) },
        edit = { message -> onEdit(message.id) },
        delete = { message -> onDelete(message.id) },
        retry = viewModel::retrySend,
        discard = viewModel::discardSend,
        openPractice = onOpenPractice,
        stamp = { message, stamped -> viewModel.stampTake(message.id, stamped) }
    )
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
    actions: MessageActions
) {
    val side = chat.side
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Straight to the answered message, a third of the way up from the bottom, without the ride there.
    // The message gone to is washed in the accent colour, which then ebbs away, so the eye finds it.
    var litId by remember { mutableStateOf<Long?>(null) }
    val light = remember { Animatable(0f) }
    var lighting by remember { mutableStateOf<Job?>(null) }
    val showMessage: (Long) -> Unit = { id ->
        val index = messages.size - 1 - messages.indexOfFirst { it.id == id }
        if (index < messages.size) {
            lighting?.cancel()
            lighting = scope.launch {
                listState.scrollToItem(
                    index = state.outgoing.size + index,
                    scrollOffset = -listState.layoutInfo.viewportSize.height / 3
                )
                litId = id
                light.snapTo(0f)
                light.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                delay(650)
                light.animateTo(0f, tween(1300, easing = LinearOutSlowInEasing))
                litId = null
            }
        }
    }
    val newest = messages.lastOrNull()
    // Newest sits at index 0 of the reversed list; someone reading back up stays where they are.
    LaunchedEffect(newest?.id, state.outgoing.size) {
        if (state.outgoing.isNotEmpty() || (newest != null && (newest.sender == side || listState.firstVisibleItemIndex <= 1))) {
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
        // A reversed list draws its first items at the bottom, where what is still going sits.
        val pendingNewestFirst = state.outgoing.asReversed()
        itemsIndexed(pendingNewestFirst, key = { _, pending -> "pending-${pending.localId}" }) { index, pending ->
            val day = localDay(pending.createdAtMs)
            val older = pendingNewestFirst.getOrNull(index + 1)?.createdAtMs ?: messages.lastOrNull()?.createdAtMs
            val newer = pendingNewestFirst.getOrNull(index - 1)
            PendingRow(
                pending = pending,
                state = state,
                actions = actions,
                onShowMessage = showMessage,
                showsDay = older == null || localDay(older) != day,
                showsTime = newer == null || localDay(newer.createdAtMs) != day
            )
        }
        val newestFirst = messages.asReversed()
        itemsIndexed(newestFirst, key = { _, message -> message.id }) { index, message ->
            val isMine = message.sender == side
            // Part of the row, so the newest message stays item 0 for the checks above.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val day = localDay(message.createdAtMs)
                val older = newestFirst.getOrNull(index + 1)
                if (older == null || localDay(older.createdAtMs) != day) DayHeading(message.createdAtMs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (message.id == litId) Modifier.messageLight(MelovaAccent, light.value) else Modifier),
                    // The screen is RTL, so the person's own messages sit on the right.
                    horizontalAlignment = if (isMine) Alignment.Start else Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MessageBubble(message = message, isMine = isMine, state = state, actions = actions, onShowMessage = showMessage)
                    // Only the day's last message carries the time and the tick, counting those still going.
                    val newer = newestFirst.getOrNull(index - 1)?.createdAtMs ?: state.outgoing.firstOrNull()?.createdAtMs
                    if (newer == null || localDay(newer) != day) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formatTimeOfDay(message.createdAtMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MelovaTextMuted,
                                modifier = Modifier.alignByBaseline()
                            )
                            if (message.editedAtMs != null) {
                                Text(
                                    text = "ویرایش شد",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MelovaTextMuted,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }
                            if (isMine) SeenMark(message.id <= chat.peerReadId)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One message, with the small card of things to do with it that holding it opens, where the
 * finger is: anyone can answer a message, and its sender can rewrite or take back their own.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    state: ChatUiState,
    actions: MessageActions,
    onShowMessage: (Long) -> Unit
) {
    var heldAt by remember(message.id) { mutableStateOf<Offset?>(null) }
    var anchor by remember(message.id) { mutableStateOf(Offset.Zero) }
    val isBusy = message.id in state.busyMessageIds
    // A practice result stays: its review is the record, so it can only be answered.
    val canChange = isMine && !isBusy && message.kind != ChatMessageKind.PRACTICE
    val canAnswer = !isBusy
    // Where the finger last came down on the bubble, seen before any part of it takes the press,
    // for the parts that open the menu themselves.
    var lastDown by remember(message.id) { mutableStateOf(Offset.Zero) }
    val openMenu = { if (canAnswer) heldAt = lastDown }
    // The wave has the whole bubble, so it says where the finger was in window terms.
    val holdInWindow: ((Offset) -> Unit)? = if (canAnswer) ({ heldAt = it - anchor }) else null

    // Kept after the menu closes, so it closes where it opened.
    var menuAt by remember(message.id) { mutableStateOf(Offset.Zero) }
    if (heldAt != null) menuAt = heldAt!!

    val quote = quoteOf(message.replyToId, state, onShowMessage, holdInWindow)

    Box(
        modifier = Modifier
            .onGloballyPositioned { anchor = it.positionInWindow() }
            .pointerInput(message.id) {
                awaitEachGesture {
                    lastDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position
                }
            }
    ) {
        Box(
            // Text has nothing else to do with a tap, so a tap opens its menu; the others keep the
            // tap for playing or opening, and open it when held.
            modifier = if (canAnswer) {
                Modifier.pointerInput(message.id, message.kind) {
                    detectTapGestures(
                        onTap = if (message.kind == ChatMessageKind.TEXT) ({ heldAt = it }) else null,
                        onLongPress = { heldAt = it }
                    )
                }
            } else {
                Modifier
            }
        ) {
            when (message.kind) {
                ChatMessageKind.TEXT -> TextBubble(message.text, isMine, isBusy, quote)
                ChatMessageKind.VOICE -> VoiceBubble(
                    message = message,
                    isMine = isMine,
                    state = state,
                    actions = actions,
                    quote = quote,
                    // The wave answers the finger itself, so it opens the menu rather than the Box.
                    onHold = holdInWindow
                )
                ChatMessageKind.PRACTICE -> message.practice?.let { practice ->
                    PracticeBubble(
                        practice = practice,
                        note = message.text,
                        onClick = { actions.openPractice(practice) },
                        onLongClick = openMenu,
                        // A take is always the student's, so the side it isn't "mine" on is the teacher's.
                        onStamp = if (!isMine) ({ stamped -> actions.stamp(message, stamped) }) else null,
                        isBusy = isBusy
                    )
                }
            }
        }
        MessageMenu(at = menuAt, isOpen = heldAt != null, onDismiss = { heldAt = null }) {
            MessageMenuItem("پاسخ", Icons.AutoMirrored.Outlined.Reply, MelovaText, MelovaAccentSoft) {
                heldAt = null
                actions.reply(message)
            }
            if (canChange && message.kind == ChatMessageKind.TEXT) {
                MessageMenuItem("ویرایش", Icons.Outlined.Edit, MelovaText, MelovaAccentSoft) {
                    heldAt = null
                    actions.edit(message)
                }
            }
            if (canChange) {
                MessageMenuItem("حذف", Icons.Outlined.DeleteOutline, MelovaError, MelovaError) {
                    heldAt = null
                    actions.delete(message)
                }
            }
        }
    }
}

/**
 * The card a held message opens, at the finger rather than at the corner of the bubble. [at] is
 * measured from the bubble's left edge, as the finger is, so the card hangs from a point placed
 * there without regard to the page's direction: a menu's own offset counts from the right on a
 * Persian screen, which sent it to the far side of the finger.
 */
@Composable
private fun BoxScope.MessageMenu(
    at: Offset,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    items: @Composable ColumnScope.() -> Unit
) {
    PlatformBackHandler(enabled = isOpen) { onDismiss() }
    Box(modifier = Modifier.matchParentSize(), contentAlignment = AbsoluteAlignment.TopLeft) {
        Box(modifier = Modifier.absoluteOffset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }.size(0.dp)) {
            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = onDismiss,
                shape = MelovaCardShape,
                containerColor = MelovaCardElevated,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MelovaOutline),
                modifier = Modifier.width(IntrinsicSize.Max),
                content = items
            )
        }
    }
}

@Composable
private fun MessageMenuItem(
    label: String,
    icon: ImageVector,
    color: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** The wash over a message gone to, edge to edge across the thread rather than inside its padding. */
private fun Modifier.messageLight(accent: Color, strength: Float): Modifier = drawBehind {
    val side = 16.dp.toPx()
    val gap = 5.dp.toPx()
    drawRect(
        color = accent.copy(alpha = 0.2f * strength),
        topLeft = Offset(-side, -gap),
        size = Size(size.width + side * 2, size.height + gap * 2)
    )
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

/** The message [replyToId] answers, as long as it is still in the thread, to draw inside the bubble. */
private fun quoteOf(
    replyToId: Long?,
    state: ChatUiState,
    onShowMessage: (Long) -> Unit,
    onHold: ((Offset) -> Unit)?
): (@Composable () -> Unit)? {
    val chat = state.thread?.chat
    val answered = replyToId?.let { id -> state.thread?.messages?.firstOrNull { it.id == id } } ?: return null
    return {
        QuotedMessage(
            answered = answered,
            author = if (chat != null && answered.sender == chat.side) "شما" else chat?.peerName.orEmpty().ifBlank { "طرف گفتگو" },
            onClick = { onShowMessage(answered.id) },
            onHold = onHold
        )
    }
}

/** One line standing for a message elsewhere in the thread: what an answer points back at. */
private fun messagePreview(message: ChatMessage): String = when (message.kind) {
    ChatMessageKind.TEXT -> message.text
    ChatMessageKind.VOICE -> "پیام صوتی"
    ChatMessageKind.PRACTICE -> message.practice?.sectionTitle?.ifBlank { null } ?: "تمرین"
}

/**
 * The message an answer is to, at the top inside the answer's own bubble: who wrote it over one
 * line of it, behind a bar in the accent colour.
 */
@Composable
private fun QuotedMessage(
    answered: ChatMessage,
    author: String,
    onClick: () -> Unit,
    onHold: ((Offset) -> Unit)?
) {
    var inWindow by remember { mutableStateOf(Offset.Zero) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { inWindow = it.positionInWindow() }
            // A tap goes to the message; holding it still opens the bubble's menu, as anywhere else on it.
            .pointerInput(answered.id, onHold) {
                detectTapGestures(onTap = { onClick() }, onLongPress = onHold?.let { hold -> { hold(inWindow + it) } })
            }
            .height(IntrinsicSize.Min)
            // The bubble's own corner less the gap around the quote, so the two curves run together.
            .clip(RoundedCornerShape(14.dp))
            .background(MelovaAccentSoft.copy(alpha = 0.12f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(MelovaAccentSoft))
        if (answered.kind == ChatMessageKind.VOICE) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = MelovaAccentSoft,
                modifier = Modifier.padding(start = 8.dp).size(18.dp)
            )
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(
                text = author,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MelovaAccentSoft,
                maxLines = 1
            )
            Text(
                text = messagePreview(answered),
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TextBubble(
    text: String,
    isMine: Boolean,
    isBusy: Boolean = false,
    quote: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bubbleColor(isMine),
        modifier = Modifier.widthIn(max = 300.dp).alpha(if (isBusy) 0.5f else 1f)
    ) {
        // As wide as the wider of the two, so the quote and the text share one edge.
        Column(modifier = Modifier.width(IntrinsicSize.Max).padding(if (quote != null) 6.dp else 0.dp)) {
            quote?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText,
                modifier = Modifier.padding(horizontal = if (quote != null) 6.dp else 12.dp, vertical = if (quote != null) 4.dp else 8.dp)
            )
        }
    }
}

@Composable
private fun VoiceBubble(
    message: ChatMessage,
    isMine: Boolean,
    state: ChatUiState,
    actions: MessageActions,
    quote: (@Composable () -> Unit)?,
    onHold: ((Offset) -> Unit)?
) {
    val isActive = state.activeVoiceId == message.id
    val isLoading = isActive && state.isVoiceLoading
    val isPlaying = isActive && state.isVoicePlaying
    val isHere = message.id in state.readyVoiceIds
    val totalMs = if (isActive && state.voiceDurationMs > 0L) state.voiceDurationMs else message.durationMs ?: 0L

    VoiceRow(
        isMine = isMine,
        peaks = message.peaks,
        progress = if (isActive && totalMs > 0L) state.voicePositionMs.toFloat() / totalMs else 0f,
        clock = formatClock(if (isActive) state.voicePositionMs else totalMs),
        // Until it is here it is drawn faint, with the arrow that brings it in.
        faded = !isHere && !isActive,
        button = {
            when {
                isLoading -> ProgressRing(
                    progress = state.voiceArrived,
                    onClick = { actions.toggleVoice(message) },
                    description = "در حال گرفتن پیام صوتی"
                )
                else -> VoiceButton(
                    icon = when {
                        isPlaying -> Icons.Filled.Pause
                        isHere -> Icons.Filled.PlayArrow
                        else -> Icons.Outlined.FileDownload
                    },
                    description = if (isPlaying) "توقف" else "پخش",
                    onClick = { actions.toggleVoice(message) }
                )
            }
        },
        onScrub = { fraction -> actions.scrubVoice(message, fraction) },
        onScrubEnd = { fraction -> actions.seekVoice(fraction) },
        canScrub = !isLoading,
        quote = quote,
        onHold = onHold
    )
}

/**
 * A message still on its way out, drawn as it will be once it is there; only the clock in place
 * of the tick says it hasn't gone yet, and a voice's button follows it out.
 */
@Composable
private fun PendingRow(
    pending: OutgoingMessage,
    state: ChatUiState,
    actions: MessageActions,
    onShowMessage: (Long) -> Unit,
    showsDay: Boolean,
    showsTime: Boolean
) {
    val quote = quoteOf(pending.replyTo, state, onShowMessage, onHold = null)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showsDay) DayHeading(pending.createdAtMs)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (pending.kind == ChatMessageKind.VOICE) {
                VoiceRow(
                    isMine = true,
                    peaks = pending.peaks,
                    progress = 0f,
                    clock = formatClock(pending.durationMs),
                    faded = false,
                    button = {
                        if (pending.hasFailed) {
                            VoiceButton(Icons.Outlined.Refresh, "فرستادن دوباره") { actions.retry(pending.localId) }
                        } else {
                            ProgressRing(pending.progress, description = "در حال فرستادن") {}
                        }
                    },
                    onScrub = {},
                    onScrubEnd = {},
                    canScrub = false,
                    quote = quote
                )
            } else {
                TextBubble(pending.text, isMine = true, quote = quote)
            }
            if (pending.hasFailed) {
                FailedRow(onRetry = { actions.retry(pending.localId) }, onDiscard = { actions.discard(pending.localId) })
            } else if (showsTime) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = formatTimeOfDay(pending.createdAtMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaTextMuted,
                        modifier = Modifier.alignByBaseline()
                    )
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "در حال فرستادن",
                        tint = MelovaTextMuted,
                        modifier = iconOnBaseline(TICK_BOTTOM_UNIT).size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FailedRow(onRetry: () -> Unit, onDiscard: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MelovaError,
            modifier = Modifier.size(14.dp)
        )
        Text("فرستاده نشد", style = MaterialTheme.typography.labelSmall, color = MelovaError)
        QuietAction("تلاش دوباره", MelovaAccentSoft, onRetry)
        QuietAction("حذف", MelovaTextMuted, onDiscard)
    }
}

@Composable
private fun QuietAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** The shape every voice message takes: a round button, its wave, and its clock. */
@Composable
private fun VoiceRow(
    isMine: Boolean,
    peaks: List<Float>,
    progress: Float,
    clock: String,
    faded: Boolean,
    canScrub: Boolean,
    button: @Composable () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    quote: (@Composable () -> Unit)? = null,
    onHold: ((Offset) -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bubbleColor(isMine),
        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth()
    ) {
        Column {
            quote?.let { Box(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)) { it() } }
            // Sound runs left to right, on a Persian screen too.
            LtrContent {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    button()
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        VoiceWave(
                            peaks = peaks,
                            progress = progress,
                            faded = faded,
                            canScrub = canScrub,
                            onScrub = onScrub,
                            onScrubEnd = onScrubEnd,
                            onHold = onHold,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )
                        Text(clock, style = MaterialTheme.typography.labelSmall, color = MelovaTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    // Playing a voice leaves the keyboard up, as the composer's own buttons do.
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MelovaAccent,
        modifier = Modifier.focusProperties { canFocus = false }.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

/** How much of a voice has gone or arrived, drawn around the place its play button sits. */
@Composable
private fun ProgressRing(progress: Float, description: String, onClick: () -> Unit) {
    val fraction = progress.coerceIn(0f, 1f)
    val sweep by animateFloatAsState(targetValue = fraction, animationSpec = tween(220), label = "voice-progress")
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MelovaAccent,
        modifier = Modifier.focusProperties { canFocus = false }.size(44.dp).semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { sweep },
                strokeWidth = 2.5.dp,
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = persianDigits((sweep * 100).toInt().toString()),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/** The voice's own sound, filling from the left as it plays and following a finger on it. */
@Composable
private fun VoiceWave(
    peaks: List<Float>,
    progress: Float,
    faded: Boolean,
    canScrub: Boolean,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onHold: ((Offset) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val played = MelovaAccentSoft
    val unplayed = if (faded) MelovaControl.copy(alpha = 0.4f) else MelovaControl
    val scrub by rememberUpdatedState(onScrub)
    val scrubEnd by rememberUpdatedState(onScrubEnd)
    // A voice the server couldn't read keeps the same place, as a flat line of bars.
    val bars = peaks.ifEmpty { List(VOICE_EMPTY_BARS) { 0f } }
    val hold by rememberUpdatedState(onHold)
    var held by remember { mutableStateOf(0f) }
    // Where the wave itself sits, so a finger on it can be reported where the bubble can use it.
    var inWindow by remember { mutableStateOf(Offset.Zero) }
    val place = Modifier.onGloballyPositioned { inWindow = it.positionInWindow() }
    val gestures = if (!canScrub) {
        place.pointerInput(onHold == null) {
            // The wave takes the whole bubble, so holding it is how a voice message is got at.
            if (hold != null) detectTapGestures(onLongPress = { hold?.invoke(inWindow + it) })
        }
    } else {
        place
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { hold?.invoke(inWindow + it) },
                    onTap = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        scrub(fraction)
                        scrubEnd(fraction)
                    }
                )
            }
            .pointerInput(Unit) {
                // Holding the playhead moves it with the finger; the sound goes on where it is let go.
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        held = (offset.x / size.width).coerceIn(0f, 1f)
                        scrub(held)
                    },
                    onDragEnd = { scrubEnd(held) },
                    onDragCancel = { scrubEnd(held) }
                ) { change, _ ->
                    held = (change.position.x / size.width).coerceIn(0f, 1f)
                    scrub(held)
                }
            }
    }
    Canvas(modifier = modifier.then(gestures)) {
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
        val fraction = progress.coerceIn(0f, 1f)
        clipRect(right = size.width * fraction) { drawBars(played) }
        if (fraction > 0f) {
            drawCircle(color = played, radius = 4.dp.toPx(), center = Offset(size.width * fraction, size.height / 2f))
        }
    }
}

/** A take the student sent; tapping it opens the page the student saw. */
@Composable
private fun PracticeBubble(
    practice: PracticeResultLink,
    note: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    /** The teacher's way to say «آفرین» to this take, or take it back; null for the student. */
    onStamp: ((Boolean) -> Unit)? = null,
    isBusy: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)
    // Played only when the stamp arrives while the chat is open, not for one already there.
    val wasStamped = remember(practice.reviewId) { practice.stamped }
    Box(modifier = Modifier.widthIn(max = 310.dp)) {
    Surface(
        shape = shape,
        color = MelovaCard,
        modifier = Modifier
            .widthIn(max = 310.dp)
            .fillMaxWidth()
            .border(1.dp, MelovaAccent.copy(alpha = 0.55f), shape)
            .clip(shape)
            // Holding it for its menu leaves the keyboard up.
            .focusProperties { canFocus = false }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val score = practice.score
                if (score != null) {
                    ScoreBadge(score = score)
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MelovaOutline),
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
                    // The stamp is the teacher's answer to the student; it stays open only on the teacher's side.
                    ReviewStatus.OPEN -> if (!practice.stamped || onStamp != null) {
                        MelovaChip("در انتظار استاد", color = MelovaGold)
                    }
                    ReviewStatus.ANSWERED -> MelovaChip("پاسخ داده شد", color = MelovaSuccess)
                    ReviewStatus.REFUNDED -> MelovaChip("بسته شد، پیام برگشت", color = MelovaTextMuted)
                    ReviewStatus.RETURNED -> MelovaChip("بی‌پاسخ ماند، پیام برگشت", color = MelovaTextMuted)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    Text(
                        text = "دیدن نتیجه",
                        style = MaterialTheme.typography.labelLarge,
                        color = MelovaAccentSoft,
                        modifier = Modifier.alignByBaseline()
                    )
                    ForwardArrow(tint = MelovaAccentSoft, modifier = iconOnBaseline(ARROW_LINE_UNIT))
                }
            }
            if (onStamp != null) {
                MelovaSoftAction(
                    text = if (practice.stamped) "برداشتن آفرین" else "آفرین",
                    icon = if (practice.stamped) null else Icons.Outlined.Verified,
                    onClick = { onStamp(!practice.stamped) },
                    enabled = !isBusy,
                    done = practice.stamped
                )
            }
        }
    }
    if (practice.stamped) {
        AfarinStamp(
            pressed = !wasStamped,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = (-10).dp)
        )
    }
    }
}

@Composable
private fun ChatComposer(
    editing: ChatMessage?,
    replying: ChatMessage?,
    onCancelEdit: () -> Unit,
    onCancelReply: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onRecordStart: () -> Unit,
    onSendText: (String) -> Unit,
    onSendVoice: (voice: LocalFile, durationMs: Long, peaks: List<Float>) -> Unit
) {
    val take = rememberTakeController(TakeKind.VOICE)
    var text by rememberSaveable(editing?.id) { mutableStateOf(editing?.text.orEmpty()) }

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
            onSendVoice(voice, take.durationMs, take.reviewBars)
            take.clear()
        }
    }

    // Stopping a recording takes a moment; the voice goes out as soon as the file is ready. The
    // only ways here are sending it and running out of room, so both send what was recorded.
    LaunchedEffect(take.phase) {
        if (take.phase == TakePhase.READY) sendTake()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = PageBottomGap),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        take.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MelovaError)
        }
        if (editing != null) {
            PlatformBackHandler(onBack = onCancelEdit)
            EditingBanner("ویرایش پیام", editing.text, Icons.Outlined.Edit, "بی‌خیال ویرایش", onCancelEdit)
        } else if (replying != null) {
            PlatformBackHandler(onBack = onCancelReply)
            EditingBanner(
                title = "پاسخ به پیام",
                text = messagePreview(replying),
                icon = Icons.AutoMirrored.Outlined.Reply,
                cancelDescription = "بی‌خیال پاسخ",
                onCancel = onCancelReply
            )
        }
        // The field stays under the recorder rather than leaving: a field that leaves takes the
        // keyboard with it, and the person is often only a word away from typing again.
        val isRecording = take.phase != TakePhase.EMPTY
        Box(contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier.alpha(if (isRecording) 0f else 1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessageField(
                    value = text,
                    onValueChange = { text = it.take(MAX_TEXT_LENGTH) },
                    isEditing = editing != null,
                    modifier = Modifier.weight(1f)
                )
                if (text.isBlank() && editing == null) {
                    RoundAction(Icons.Filled.Mic, "ضبط پیام صوتی") {
                        if (permission.isGranted()) startRecording() else permission.request()
                    }
                } else {
                    RoundAction(
                        icon = if (editing != null) Icons.Filled.Check else Icons.AutoMirrored.Filled.Send,
                        description = if (editing != null) "ذخیره‌ی ویرایش" else "ارسال",
                        enabled = text.isNotBlank()
                    ) {
                        if (editing != null) onSaveEdit(text) else onSendText(text)
                        text = ""
                    }
                }
            }
            if (isRecording) RecordingBar(take = take, onSend = take::finish)
        }
    }
}

/** What is being rewritten or answered, above the field, with the way out of it. */
@Composable
private fun EditingBanner(
    title: String,
    text: String,
    icon: ImageVector,
    cancelDescription: String,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MelovaSmallShape)
            .background(MelovaAccentTint)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MelovaAccentSoft)
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = cancelDescription, tint = MelovaTextMuted)
        }
    }
}

/**
 * The recorder, in the composer's own place: the sound as it comes in, the clock, and the three
 * things to do with it — throw it away, hold it, send it.
 */
@Composable
private fun RecordingBar(take: TakeController, onSend: () -> Unit) {
    val isPaused = take.phase == TakePhase.PAUSED
    val isWorking = take.phase == TakePhase.PROCESSING || take.phase == TakePhase.READY
    val pulse by rememberInfiniteTransition(label = "voice-rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "voice-dot"
    )
    Surface(shape = MelovaCardShape, color = MelovaCardElevated, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .alpha(if (isPaused || isWorking) 1f else pulse)
                        .clip(CircleShape)
                        .background(if (isPaused) MelovaWarning else MelovaError)
                )
                Text(
                    text = when {
                        isWorking -> "آماده‌سازی…"
                        isPaused -> "مکث"
                        else -> "در حال ضبط"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPaused) MelovaWarning else MelovaText
                )
                Spacer(modifier = Modifier.weight(1f))
                LtrContent {
                    Text(
                        text = formatClock(take.elapsedMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = MelovaText
                    )
                }
            }
            if (isWorking) {
                MelovaWaveSkeleton(height = 28.dp)
            } else {
                LiveLevelBars(levels = take.liveBars, dimmed = isPaused, height = 28.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundAction(
                    icon = Icons.Outlined.DeleteOutline,
                    description = "دور انداختن",
                    isQuiet = true,
                    size = MelovaDimens.ButtonHeight,
                    onClick = take::clear
                )
                MelovaMediaIconButton(
                    onClick = take::togglePause,
                    modifier = Modifier.focusProperties { canFocus = false },
                    enabled = !isWorking,
                    icon = if (isPaused) Icons.Filled.Mic else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "ادامه‌ی ضبط" else "مکث",
                    containerColor = MelovaAccentTint,
                    contentColor = MelovaAccentSoft,
                    buttonSize = MelovaDimens.ButtonHeight,
                    cornerRadius = MelovaDimens.ButtonHeight / 2
                )
                Spacer(modifier = Modifier.weight(1f))
                RoundAction(Icons.AutoMirrored.Filled.Send, "ارسال", isBusy = isWorking, onClick = onSend)
            }
        }
    }
}

@Composable
private fun MessageField(
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    MelovaTextField(
        value = value,
        onValueChange = onValueChange,
        label = null,
        placeholder = if (isEditing) "متن تازه" else "پیام",
        singleLine = false,
        maxLines = 4,
        shape = MelovaCardShape,
        modifier = modifier
    )
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    isBusy: Boolean = false,
    isQuiet: Boolean = false,
    enabled: Boolean = true,
    size: Dp = MelovaDimens.PrimaryButtonHeight,
    onClick: () -> Unit
) {
    val iconColor = if (isQuiet) MelovaAccentSoft else Color.White
    Surface(
        onClick = onClick,
        enabled = enabled && !isBusy,
        shape = CircleShape,
        color = if (isQuiet) MelovaAccentTint else MelovaAccent,
        // Never takes focus from the field beside it, so the keyboard stays up while it is pressed.
        modifier = Modifier.focusProperties { canFocus = false }.size(size)
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
