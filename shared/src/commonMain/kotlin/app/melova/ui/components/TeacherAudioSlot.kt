package app.melova.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.melova.audio.capture.TeacherTakeRecorder
import app.melova.audio.io.WavUtils
import app.melova.audio.io.WaveformPeaksExtractor
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.platform.persianDigits
import app.melova.ui.layout.LtrContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.appContext
import app.melova.platform.rememberAudioPicker
import app.melova.platform.rememberMicPermission
import app.melova.platform.ioDispatcher
import app.melova.platform.currentTimeMillis
import app.melova.platform.cacheDir

/** What a take is for; decides the format, the length cap and whether a count-in helps. */
enum class TakeKind(val maxMs: Long, val countInSeconds: Int, val minMs: Long) {
    /** A sung reference the student is scored against: lossless, trimmed, after a 3-2-1. */
    SECTION(maxMs = 3 * 60_000L, countInSeconds = 3, minMs = 1_000L),

    /** The teacher talking about the whole lesson: compressed so long takes stay small. */
    INTRO(maxMs = 20 * 60_000L, countInSeconds = 0, minMs = 2_000L),

    /** A voice message in a chat. */
    VOICE(maxMs = 5 * 60_000L, countInSeconds = 0, minMs = 1_000L)
}

enum class TakePhase { EMPTY, COUNT_IN, RECORDING, PAUSED, PROCESSING, READY }

enum class LevelHint { TOO_LOUD, TOO_QUIET }

private const val TICK_MS = 50L
private const val LIVE_BAR_COUNT = 48
private const val REVIEW_BAR_COUNT = 64
private val INTRO_EXTENSIONS = setOf("mp3", "m4a", "aac", "ogg", "webm", "wav")

/**
 * Holds one teacher take, whether recorded here or picked from the phone, from the first tap
 * to the upload: count-in, live level, pause, review with playback. The file it records is
 * its own and is deleted on [clear] or [release].
 */
@Stable
class TakeController(
    private val context: PlatformContext,
    private val scope: CoroutineScope,
    val kind: TakeKind
) {
    var phase by mutableStateOf(TakePhase.EMPTY)
        private set
    var countIn by mutableIntStateOf(0)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set
    val liveBars = mutableStateListOf<Float>()
    var hint by mutableStateOf<LevelHint?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** The take to upload: our own recording, or the document the teacher picked. */
    var audio by mutableStateOf<LocalFile?>(null)
        private set
    var pickedName by mutableStateOf<String?>(null)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var reviewBars by mutableStateOf<List<Float>>(emptyList())
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playbackMs by mutableLongStateOf(0L)
        private set

    val isRecording: Boolean
        get() = phase == TakePhase.COUNT_IN || phase == TakePhase.RECORDING || phase == TakePhase.PAUSED

    /** Anything the teacher would lose by leaving now. */
    val hasUnsavedWork: Boolean
        get() = phase != TakePhase.EMPTY

    val isTooShort: Boolean
        get() = phase == TakePhase.READY && durationMs in 1 until kind.minMs

    val canUpload: Boolean
        get() = phase == TakePhase.READY && audio != null && !isTooShort

    private var recorder: TeacherTakeRecorder? = null
    private var takeFile: LocalFile? = null
    private var job: Job? = null
    private var progressJob: Job? = null
    private var player: ReferenceAudioPlayer? = null
    private val allLevels = ArrayList<Float>()

    /** Starts a new take; the caller has already made sure the mic permission is granted. */
    fun record() {
        clear()
        error = null
        job = scope.launch {
            for (second in kind.countInSeconds downTo 1) {
                phase = TakePhase.COUNT_IN
                countIn = second
                delay(1_000L)
            }
            val newRecorder = if (kind == TakeKind.SECTION) {
                TeacherTakeRecorder.lossless(context)
            } else {
                TeacherTakeRecorder.compressed(context)
            }
            val dir = LocalFile(cacheDir(context), "teacher_takes").apply { mkdirs() }
            val file = LocalFile(dir, "take_${currentTimeMillis()}.${newRecorder.extension}")
            try {
                newRecorder.start(file)
            } catch (failure: Exception) {
                fail(failure.message ?: "ضبط شروع نشد")
                return@launch
            }
            recorder = newRecorder
            takeFile = file
            phase = TakePhase.RECORDING
            tick(newRecorder)
        }
    }

    private suspend fun tick(active: TeacherTakeRecorder) {
        var quietTicks = 0
        var loudTicks = 0
        while (scope.isActive && recorder === active) {
            active.error?.let {
                fail(it)
                return
            }
            if (phase == TakePhase.RECORDING) {
                val level = active.level.coerceIn(0f, 1f)
                liveBars.add(level)
                if (liveBars.size > LIVE_BAR_COUNT) liveBars.removeAt(0)
                allLevels.add(level)
                elapsedMs = active.elapsedMs
                loudTicks = if (level > 0.97f) 12 else (loudTicks - 1).coerceAtLeast(0)
                quietTicks = if (level < 0.04f) quietTicks + 1 else 0
                hint = when {
                    loudTicks > 0 -> LevelHint.TOO_LOUD
                    quietTicks > 50 -> LevelHint.TOO_QUIET
                    else -> null
                }
                if (elapsedMs >= kind.maxMs) {
                    finish()
                    return
                }
            }
            delay(TICK_MS)
        }
    }

    fun togglePause() {
        val active = recorder ?: return
        when (phase) {
            TakePhase.RECORDING -> {
                active.pause()
                phase = TakePhase.PAUSED
                hint = null
            }
            TakePhase.PAUSED -> {
                active.resume()
                phase = TakePhase.RECORDING
            }
            else -> Unit
        }
    }

    /** Pauses whatever is running; used when the app goes to the background. */
    fun pauseAll() {
        if (phase == TakePhase.RECORDING) togglePause()
        if (isPlaying) togglePlayback()
    }

    fun finish() {
        val active = recorder ?: return
        val file = takeFile ?: return
        recorder = null
        phase = TakePhase.PROCESSING
        hint = null
        // Judge silence on the raw input: the processed file is normalized, which lifts noise.
        val heardVoice = (allLevels.maxOrNull() ?: 0f) >= 0.005f
        job = scope.launch {
            try {
                val (duration, bars) = withContext(ioDispatcher) {
                    val recorded = active.stop()
                    if (kind == TakeKind.SECTION) {
                        val trimmed = WavUtils.trimSilenceAndNormalize(file)
                        trimmed to WaveformPeaksExtractor.fromWavFile(file, REVIEW_BAR_COUNT)
                    } else {
                        recorded to downsample(allLevels, REVIEW_BAR_COUNT)
                    }
                }
                if (duration <= 0L || !heardVoice) {
                    fail("صدایی ضبط نشد. میکروفون را بررسی کنید و دوباره امتحان کنید.")
                    return@launch
                }
                durationMs = duration
                reviewBars = bars
                setAudio(file, name = null)
            } catch (failure: Exception) {
                fail(failure.message ?: "ضبط ذخیره نشد")
            }
        }
    }

    /** A file the teacher picked, already copied into the cache under its own name. */
    fun pick(file: LocalFile) {
        val name = file.name
        val extension = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (kind == TakeKind.INTRO && extension !in INTRO_EXTENSIONS) {
            clear()
            error = "فرمت فایل آموزش باید mp3، m4a، aac، ogg، webm یا wav باشد."
            return
        }
        clear()
        error = null
        reviewBars = emptyList()
        durationMs = 0L
        setAudio(file, name.ifBlank { "فایل صوتی" })
    }

    private fun setAudio(file: LocalFile, name: String?) {
        audio = file
        pickedName = name
        playbackMs = 0L
        phase = TakePhase.READY
        playerOrCreate().prepare(
            file = file,
            onFinished = {
                isPlaying = false
                playbackMs = 0L
                progressJob?.cancel()
            },
            onReady = {
                player?.durationMs()?.takeIf { it > 0L && durationMs == 0L }?.let { durationMs = it }
            }
        )
    }

    fun togglePlayback() {
        val active = player ?: return
        if (phase != TakePhase.READY) return
        // Track the wanted state ourselves: ExoPlayer only reports playing once it has buffered.
        isPlaying = !isPlaying
        if (isPlaying) active.play() else active.pause()
        progressJob?.cancel()
        if (isPlaying) {
            progressJob = scope.launch {
                while (isActive && isPlaying) {
                    playbackMs = active.currentPositionMs()
                    delay(TICK_MS)
                }
            }
        }
    }

    fun seekTo(fraction: Float) {
        val active = player ?: return
        val total = durationMs.takeIf { it > 0L } ?: return
        playbackMs = (total * fraction.coerceIn(0f, 1f)).toLong()
        active.seekTo(playbackMs)
    }

    /** Throws the take away (cancelling a recording in progress) and goes back to empty. */
    fun clear() {
        job?.cancel()
        progressJob?.cancel()
        recorder?.cancel()
        recorder = null
        player?.stop()
        isPlaying = false
        takeFile?.delete()
        takeFile = null
        audio = null
        pickedName = null
        durationMs = 0L
        elapsedMs = 0L
        playbackMs = 0L
        reviewBars = emptyList()
        liveBars.clear()
        allLevels.clear()
        hint = null
        phase = TakePhase.EMPTY
    }

    fun release() {
        clear()
        player?.release()
        player = null
    }

    fun showError(message: String) {
        error = message
    }

    private fun fail(message: String) {
        clear()
        error = message
    }

    private fun playerOrCreate(): ReferenceAudioPlayer =
        player ?: ReferenceAudioPlayer(context).also { player = it }

    private fun downsample(levels: List<Float>, count: Int): List<Float> {
        if (levels.isEmpty()) return emptyList()
        return List(count) { bar ->
            val start = bar * levels.size / count
            val end = ((bar + 1) * levels.size / count).coerceAtLeast(start + 1).coerceAtMost(levels.size)
            levels.subList(start.coerceAtMost(levels.size - 1), end).maxOrNull() ?: 0f
        }
    }
}

@Composable
fun rememberTakeController(kind: TakeKind): TakeController {
    val context = appContext
    val scope = rememberCoroutineScope()
    val controller = remember(kind) { TakeController(context, scope, kind) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) controller.pauseAll()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }
    return controller
}

/**
 * Where a teacher gives a piece of audio: record it right here or pick a file. The same box
 * then becomes the recorder (count-in, live meter, pause) and finally a player to review the
 * take before it is uploaded.
 */
@Composable
fun TeacherAudioSlot(
    controller: TakeController,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val permission = rememberMicPermission { granted ->
        if (granted) controller.record() else controller.showError("برای ضبط، اجازه‌ی دسترسی به میکروفون لازم است.")
    }
    val pickFile = rememberAudioPicker { file ->
        if (file != null) controller.pick(file)
    }
    val record = {
        if (permission.isGranted()) controller.record() else permission.request()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedContent(
            targetState = controller.phase.let { if (it == TakePhase.PAUSED) TakePhase.RECORDING else it },
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "take-phase"
        ) { phase ->
            when (phase) {
                TakePhase.EMPTY -> SourceChoice(controller.kind, enabled, onRecord = record, onPick = pickFile)
                TakePhase.COUNT_IN -> CountInPanel(controller.countIn, controller.kind.countInSeconds, onCancel = controller::clear)
                TakePhase.RECORDING, TakePhase.PAUSED -> RecordingPanel(controller)
                TakePhase.PROCESSING -> ProcessingPanel(controller.kind)
                TakePhase.READY -> ReviewPanel(controller, enabled, onRecordAgain = record, onPickAnother = pickFile)
            }
        }
        controller.error?.let { message ->
            NoticeRow(icon = Icons.Outlined.Info, text = message, color = MelovaError)
        }
    }
}

@Composable
private fun SourceChoice(kind: TakeKind, enabled: Boolean, onRecord: () -> Unit, onPick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SourceTile(
            icon = Icons.Filled.Mic,
            title = "ضبط همین‌جا",
            subtitle = if (kind == TakeKind.SECTION) "با شمارش ۳، ۲، ۱" else "تا ۲۰ دقیقه، با امکان مکث",
            highlighted = true,
            enabled = enabled,
            onClick = onRecord,
            modifier = Modifier.weight(1f)
        )
        SourceTile(
            icon = Icons.Outlined.FolderOpen,
            title = "انتخاب فایل",
            subtitle = if (kind == TakeKind.SECTION) "هر فرمت صوتی" else "mp3، m4a، wav…",
            highlighted = false,
            enabled = enabled,
            onClick = onPick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SourceTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = MelovaCardElevated,
        modifier = modifier.border(1.dp, if (highlighted) MelovaAccent.copy(alpha = 0.55f) else MelovaOutline, shape)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlighted) {
                            Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))
                        } else {
                            Brush.linearGradient(listOf(MelovaCard, MelovaCard))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (highlighted) Color.White else MelovaAccentSoft)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = MelovaText, textAlign = TextAlign.Center)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MelovaTextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A ring that empties once a second around the beat's number, and a dot per beat. Nothing grows
 * past its own box, so the panel stays the same size from the first beat to the last.
 */
@Composable
private fun CountInPanel(countIn: Int, total: Int, onCancel: () -> Unit) {
    val ring = remember { Animatable(1f) }
    LaunchedEffect(countIn) {
        ring.snapTo(1f)
        ring.animateTo(0f, tween(durationMillis = 1_000, easing = LinearEasing))
    }
    val track = MelovaOutline
    val accent = MelovaAccent
    PanelSurface {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("آماده شوید…", style = MaterialTheme.typography.bodyMedium, color = MelovaTextMuted)
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawCircle(color = track, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * ring.value,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Crossfade(targetState = countIn, animationSpec = tween(180), label = "count-in") { value ->
                    Text(
                        text = persianDigits(value.toString()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MelovaText
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Beats already counted fill in, from the first.
                for (beat in total downTo 1) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (beat >= countIn) MelovaAccent else track)
                    )
                }
            }
            Text(
                text = "بعد از شمارش، از اول همان قسمت بخوانید.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onCancel) { Text("لغو", color = MelovaTextMuted) }
        }
    }
}

@Composable
private fun RecordingPanel(controller: TakeController) {
    val paused = controller.phase == TakePhase.PAUSED
    val pulse by rememberInfiniteTransition(label = "rec-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "rec-dot"
    )
    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(if (paused) 1f else pulse)
                        .clip(CircleShape)
                        .background(if (paused) MelovaWarning else MelovaError)
                )
                Text(
                    text = if (paused) "مکث" else "در حال ضبط",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (paused) MelovaWarning else MelovaError
                )
                Spacer(Modifier.weight(1f))
                LtrContent {
                    Text(
                        text = "${formatClock(controller.elapsedMs)} / ${formatClock(controller.kind.maxMs)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText
                    )
                }
            }
            LiveLevelBars(levels = controller.liveBars, dimmed = paused)
            when (controller.hint) {
                LevelHint.TOO_LOUD -> NoticeRow(Icons.Outlined.Info, "صدا خیلی بلند است و می‌شکند؛ کمی از گوشی فاصله بگیرید.", MelovaWarning)
                LevelHint.TOO_QUIET -> NoticeRow(Icons.Outlined.Info, "صدایی شنیده نمی‌شود؛ نزدیک‌تر به گوشی بخوانید.", MelovaWarning)
                null -> Unit
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MelovaMediaIconButton(
                    onClick = controller::clear,
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = "دور انداختن این ضبط",
                    containerColor = MelovaCard,
                    contentColor = MelovaError,
                    buttonSize = 48.dp,
                    cornerRadius = 24.dp
                )
                MelovaMediaIconButton(
                    onClick = controller::finish,
                    icon = Icons.Filled.Check,
                    contentDescription = "پایان ضبط",
                    containerColor = MelovaAccent,
                    buttonSize = 64.dp,
                    iconSize = 30.dp,
                    cornerRadius = 32.dp
                )
                MelovaMediaIconButton(
                    onClick = controller::togglePause,
                    icon = if (paused) Icons.Filled.Mic else Icons.Filled.Pause,
                    contentDescription = if (paused) "ادامه‌ی ضبط" else "مکث",
                    containerColor = MelovaCard,
                    contentColor = MelovaAccentSoft,
                    buttonSize = 48.dp,
                    cornerRadius = 24.dp
                )
            }
            Text(
                text = if (paused) "برای ادامه، دکمه‌ی میکروفون را بزنید." else "هر وقت تمام شد، ✓ را بزنید.",
                style = MaterialTheme.typography.labelSmall,
                color = MelovaTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProcessingPanel(kind: TakeKind) {
    PanelSurface {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MelovaAccent, strokeWidth = 2.5.dp)
            Text(
                text = if (kind == TakeKind.SECTION) "در حال حذف سکوت ابتدا و انتها…" else "در حال ذخیره‌ی ضبط…",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText
            )
        }
    }
}

@Composable
private fun ReviewPanel(
    controller: TakeController,
    enabled: Boolean,
    onRecordAgain: () -> Unit,
    onPickAnother: () -> Unit
) {
    val recordedHere = controller.pickedName == null
    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MelovaMediaIconButton(
                    onClick = controller::togglePlayback,
                    icon = if (controller.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (controller.isPlaying) "توقف پخش" else "گوش دادن",
                    containerColor = MelovaAccent,
                    buttonSize = 46.dp,
                    cornerRadius = 23.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (recordedHere) "ضبط شما" else controller.pickedName.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (controller.durationMs > 0L) append(formatClock(controller.durationMs)).append(" • ")
                            append(if (controller.isTooShort) "خیلی کوتاه است" else "پیش از بارگذاری گوش کنید")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (controller.isTooShort) MelovaError else MelovaTextMuted
                    )
                }
                Icon(
                    imageVector = if (recordedHere) Icons.Outlined.Mic else Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MelovaSuccess
                )
            }
            ReviewWaveform(
                bars = controller.reviewBars,
                progress = if (controller.durationMs > 0L) {
                    (controller.playbackMs.toFloat() / controller.durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                },
                onSeek = controller::seekTo
            )
            if (recordedHere && controller.kind == TakeKind.SECTION) {
                Text(
                    text = "سکوت ابتدا و انتها خودکار حذف شد تا هنرجو از همان نت اول شروع کند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaTextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRecordAgain, enabled = enabled) {
                    Icon(Icons.Outlined.Mic, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(18.dp))
                    Text(" ضبط دوباره", color = MelovaAccentSoft)
                }
                TextButton(onClick = onPickAnother, enabled = enabled) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(18.dp))
                    Text(" فایل دیگر", color = MelovaAccentSoft)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = controller::clear, enabled = enabled) {
                    Text("حذف", color = MelovaError)
                }
            }
        }
    }
}

@Composable
private fun PanelSurface(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MelovaCardElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun NoticeRow(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Live input meter: the newest level enters on the right, like a scrolling waveform. */
@Composable
private fun LiveLevelBars(levels: List<Float>, dimmed: Boolean) {
    val active = if (dimmed) MelovaTextMuted else MelovaAccent
    val idle = MelovaOutline
    Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        val slot = size.width / LIVE_BAR_COUNT
        val barWidth = slot * 0.6f
        val centerY = size.height / 2f
        val offset = LIVE_BAR_COUNT - levels.size
        for (index in 0 until LIVE_BAR_COUNT) {
            val level = levels.getOrNull(index - offset)
            // A square-root curve makes quiet singing still visibly move the meter.
            val height = if (level == null) 3f else (kotlin.math.sqrt(level) * size.height).coerceAtLeast(3f)
            drawRoundRect(
                color = if (level == null) idle else active,
                topLeft = Offset(index * slot + (slot - barWidth) / 2f, centerY - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** The finished take, played part in accent; tap anywhere to jump there. */
@Composable
private fun ReviewWaveform(bars: List<Float>, progress: Float, onSeek: (Float) -> Unit) {
    val played = MelovaAccent
    val rest = MelovaOutline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (bars.isEmpty()) 18.dp else 48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek(offset.x / size.width.coerceAtLeast(1)) }
            }
    ) {
        if (bars.isEmpty()) {
            val y = size.height / 2f
            drawRoundRect(rest, Offset(0f, y - 3f), Size(size.width, 6f), CornerRadius(3f))
            drawRoundRect(played, Offset(0f, y - 3f), Size(size.width * progress, 6f), CornerRadius(3f))
            return@Canvas
        }
        val slot = size.width / bars.size
        val barWidth = slot * 0.6f
        val centerY = size.height / 2f
        bars.forEachIndexed { index, level ->
            val height = (kotlin.math.sqrt(level) * size.height).coerceAtLeast(3f)
            drawRoundRect(
                color = if ((index + 0.5f) / bars.size <= progress) played else rest,
                topLeft = Offset(index * slot + (slot - barWidth) / 2f, centerY - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
