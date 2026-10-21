package app.melova.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.presentation.MelovaUiState
import app.melova.presentation.MelovaViewModel
import app.melova.presentation.SessionMode
import app.melova.data.ScaleExercise
import app.melova.data.ScalePattern
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSegmentedControl
import app.melova.ui.components.MelovaTopBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import app.melova.audio.scoring.VocalScorer
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.PitchCorrectionRow
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaBackgroundEnd
import app.melova.ui.theme.MelovaBackgroundStart
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaText

@Composable
fun MelovaScreen(
    onBack: (() -> Unit)? = null,
    viewModel: MelovaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onMatchClicked()
        }
    }

    fun requestMatch() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.onMatchClicked()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isBusy = uiState.mode == SessionMode.LISTENING ||
        uiState.mode == SessionMode.MATCHING ||
        uiState.mode == SessionMode.ANALYZING

    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MelovaTopBar(
                    title = "گام و سلفژ",
                    subtitle = uiState.exerciseTitle,
                    onBack = { onBack?.invoke() }
                )

                ExercisePicker(
                    selected = uiState.selectedExercise,
                    enabled = !isBusy,
                    onExerciseSelected = viewModel::selectExercise
                )

                ExerciseCard(uiState)

                ProgressSection(
                    uiState = uiState,
                    onTogglePlayback = viewModel::togglePlayback
                )

                HeadphoneHintBanner()

                ActionButtons(
                    uiState = uiState,
                    onListen = viewModel::onListenClicked,
                    onMatch = ::requestMatch,
                    onStop = viewModel::onStopClicked
                )

                AnimatedVisibility(
                    visible = uiState.scoreResult != null,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    uiState.scoreResult?.let { result ->
                        ScoreResultCard(
                            result = result,
                            onDismiss = viewModel::dismissResults
                        )
                    }
                }
            }
        }
    }
}

/** Scale (major/minor) and direction as two visible selectors instead of a hidden menu. */
@Composable
private fun ExercisePicker(
    selected: ScaleExercise,
    enabled: Boolean,
    onExerciseSelected: (ScaleExercise) -> Unit
) {
    val patterns = listOf(ScalePattern.ASCENDING, ScalePattern.DESCENDING, ScalePattern.ASCENDING_DESCENDING)
    fun select(isMinor: Boolean, pattern: ScalePattern) {
        ScaleExercise.all
            .firstOrNull { it.isMinor == isMinor && it.pattern == pattern }
            ?.let(onExerciseSelected)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MelovaSegmentedControl(
            options = listOf("ماژور", "مینور"),
            selectedIndex = if (selected.isMinor) 1 else 0,
            enabled = enabled,
            onSelect = { index -> select(isMinor = index == 1, pattern = selected.pattern) }
        )
        MelovaSegmentedControl(
            options = listOf("بالارونده", "پایین‌رونده", "بالا و پایین"),
            selectedIndex = patterns.indexOf(selected.pattern),
            enabled = enabled,
            onSelect = { index -> select(isMinor = selected.isMinor, pattern = patterns[index]) }
        )
    }
}

@Composable
private fun ExerciseCard(uiState: MelovaUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = uiState.exerciseTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
            Text(
                text = uiState.exerciseDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ProgressSection(
    uiState: MelovaUiState,
    onTogglePlayback: () -> Unit
) {
    val playbackLocked = uiState.mode == SessionMode.MATCHING ||
            uiState.mode == SessionMode.ANALYZING
    val showPause = uiState.mode == SessionMode.LISTENING && uiState.isPlaying

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusLabel(uiState.mode, uiState.isPlaying),
                style = MaterialTheme.typography.titleMedium,
                color = MelovaText,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MelovaProgressBar(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MelovaAccent,
                        trackColor = MelovaText.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatProgressTime(uiState),
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaAccentSoft
                    )
                }

                MelovaMediaIconButton(
                    onClick = onTogglePlayback,
                    enabled = !playbackLocked,
                    icon = if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (showPause) "توقف موقت" else "پخش",
                    containerColor = MelovaAccent
                )
            }

            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.75f),
                textAlign = TextAlign.Start
            )

            if (uiState.mode == SessionMode.ANALYZING) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MelovaAccent,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "در حال بررسی نت‌های اجرای شما…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaText.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadphoneHintBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MelovaCardElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Headphones,
            contentDescription = null,
            tint = MelovaGold,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "هندزفری توصیه می‌شود",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
            Text(
                text = "هندزفری بزنید تا نمونه در گوش شما پخش شود. اپ تلاش می‌کند از میکروفون خود گوشی ضبط کند، نه میکروفون هندزفری.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaText.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ActionButtons(
    uiState: MelovaUiState,
    onListen: () -> Unit,
    onMatch: () -> Unit,
    onStop: () -> Unit
) {
    val isBusy = uiState.mode == SessionMode.LISTENING ||
            uiState.mode == SessionMode.MATCHING ||
            uiState.mode == SessionMode.ANALYZING

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onListen,
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MelovaSecondary,
                disabledContainerColor = MelovaSecondary.copy(alpha = 0.4f)
            )
        ) {
            Icon(Icons.Outlined.Hearing, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("فقط شنیدن")
        }

        Button(
            onClick = onMatch,
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MelovaAccent,
                disabledContainerColor = MelovaAccent.copy(alpha = 0.4f)
            )
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تطبیق — همزمان بخوانید")
        }

        if (isBusy && uiState.mode != SessionMode.ANALYZING) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = MelovaText)
                Spacer(modifier = Modifier.width(8.dp))
                Text("توقف", color = MelovaText)
            }
        }
    }
}

@Composable
private fun ScoreResultCard(
    result: VocalScorer.VocalScoreResult,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "نتیجه تحلیل",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MelovaAccent.copy(alpha = 0.15f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LtrContent {
                        Text(
                            text = "${result.overall}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MelovaAccentSoft
                        )
                    }
                    Text(
                        text = "امتیاز کلی",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            ScoreRow("دقت نت", result.pitchAccuracy, ltrSuffix = "(Pitch)")
            ScoreRow("زمان‌بندی", result.timing, ltrSuffix = "(Timing)")
            ScoreRow("پایداری صدا", result.stability)
            ScoreRow("بیان و پوشش", result.expression)

            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )

            if (result.corrections.isNotEmpty()) {
                Text(
                    text = "تصحیح‌های پیشنهادی",
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                result.corrections.forEach { correction ->
                    PitchCorrectionRow(correction = correction)
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("تلاش دوباره", color = MelovaText)
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int, ltrSuffix: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
            if (ltrSuffix != null) {
                LtrContent {
                    Text(
                        text = ltrSuffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaText.copy(alpha = 0.55f)
                    )
                }
            }
        }
        LtrContent {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor(score)
            )
        }
    }
}

private fun formatProgressTime(uiState: MelovaUiState): String {
    val totalSeconds = uiState.durationSeconds.coerceAtLeast(1)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun statusLabel(mode: SessionMode, isPlaying: Boolean): String = when (mode) {
    SessionMode.IDLE -> "آماده"
    SessionMode.LISTENING -> if (isPlaying) "در حال شنیدن" else "متوقف موقت"
    SessionMode.MATCHING -> "در حال تطبیق"
    SessionMode.ANALYZING -> "تحلیل AI"
    SessionMode.RESULT -> "نتیجه"
}

@Composable
@ReadOnlyComposable
private fun scoreColor(score: Int): Color = when {
    score >= 80 -> MelovaSuccess
    score >= 60 -> MelovaWarning
    else -> MelovaError
}
