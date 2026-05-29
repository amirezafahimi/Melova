package com.example.melova.ui

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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.melova.MelovaUiState
import com.example.melova.MelovaViewModel
import com.example.melova.SessionMode
import com.example.melova.data.ScaleExercise
import com.example.melova.audio.VocalScorer
import com.example.melova.ui.layout.LtrContent
import com.example.melova.ui.layout.RtlContent
import com.example.melova.ui.theme.MelovaAccent
import com.example.melova.ui.theme.MelovaAccentSoft
import com.example.melova.ui.theme.MelovaBackgroundEnd
import com.example.melova.ui.theme.MelovaBackgroundStart
import com.example.melova.ui.theme.MelovaCard
import com.example.melova.ui.theme.MelovaSecondary

@Composable
fun MelovaScreen(
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

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MelovaBackgroundStart, MelovaBackgroundEnd)
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LtrContent {
                    HeaderSection(
                        uiState = uiState,
                        onExerciseSelected = viewModel::selectExercise
                    )
                }

                RtlContent {
                    ExerciseCard(uiState)

                    ProgressSection(uiState)

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
}

@Composable
private fun HeaderSection(
    uiState: MelovaUiState,
    onExerciseSelected: (ScaleExercise) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isBusy = uiState.mode == SessionMode.LISTENING ||
        uiState.mode == SessionMode.MATCHING ||
        uiState.mode == SessionMode.ANALYZING

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MelovaAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MelovaAccent
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                LtrContent {
                    Text(
                        text = "Melova",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                RtlContent {
                    Text(
                        text = "تمرین آواز هوشمند",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                enabled = !isBusy
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "انتخاب تمرین",
                    tint = if (isBusy) Color.White.copy(alpha = 0.4f) else Color.White
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                ScaleExercise.all.forEach { exercise ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = exercise.title,
                                fontWeight = if (exercise == uiState.selectedExercise) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },
                        leadingIcon = {
                            if (exercise == uiState.selectedExercise) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MelovaAccent
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onExerciseSelected(exercise)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(uiState: MelovaUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = uiState.exerciseTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Start
            )
            Text(
                text = uiState.exerciseDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                textAlign = TextAlign.Start
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MelovaAccentSoft,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LtrContent {

                }
            }
        }
    }
}

@Composable
private fun ProgressSection(uiState: MelovaUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusLabel(uiState.mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                LtrContent {
                    Text(
                        text = formatProgressTime(uiState),
                        style = MaterialTheme.typography.labelLarge,
                        color = MelovaAccentSoft
                    )
                }
            }

            LtrContent {
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MelovaAccent,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }

            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Start
            )

            if (uiState.mode == SessionMode.ANALYZING) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MelovaAccent,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "الگوریتم YIN در حال استخراج فاکتورهای آواز...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
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
            .background(Color(0xFF4A3F6B))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Headphones,
            contentDescription = null,
            tint = Color(0xFFFFD180),
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "هندزفری توصیه می‌شود",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Start
            )
            Text(
                text = "هندزفری بزنید تا نمونه در گوش شما پخش شود. اپ تلاش می‌کند از میکروفون خود گوشی ضبط کند، نه میکروفون هندزفری.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onListen,
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
                .height(56.dp),
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
                Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("توقف", color = Color.White)
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
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "نتیجه تحلیل",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MelovaAccent.copy(alpha = 0.15f))
                    .padding(16.dp),
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
                        color = Color.White.copy(alpha = 0.8f),
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
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )

            if (result.corrections.isNotEmpty()) {
                Text(
                    text = "تصحیح‌های پیشنهادی",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
                result.corrections.forEach { correction ->
                    CorrectionItem(correction)
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("تلاش دوباره", color = Color.White)
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
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
            if (ltrSuffix != null) {
                LtrContent {
                    Text(
                        text = ltrSuffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f)
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

@Composable
private fun CorrectionItem(correction: VocalScorer.PitchCorrection) {
    val accent = when (correction.direction) {
        VocalScorer.CorrectionDirection.SHARP -> Color(0xFFFFB74D)
        VocalScorer.CorrectionDirection.FLAT -> Color(0xFF81D4FA)
        VocalScorer.CorrectionDirection.GOOD -> Color(0xFF81C784)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "زمان \u200E${correction.timeLabel}\u200F • نت هدف: ${correction.expectedNote}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Start
        )
        Text(
            text = correction.message,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start
        )
    }
}

private fun formatProgressTime(uiState: MelovaUiState): String {
    val totalSeconds = uiState.durationSeconds.coerceAtLeast(1)
    val elapsedSeconds = (uiState.progress * totalSeconds).toInt().coerceIn(0, totalSeconds)
    return "\u200E$elapsedSeconds / $totalSeconds\u200F sec"
}

private fun statusLabel(mode: SessionMode): String = when (mode) {
    SessionMode.IDLE -> "آماده"
    SessionMode.LISTENING -> "در حال شنیدن"
    SessionMode.MATCHING -> "در حال تطبیق"
    SessionMode.ANALYZING -> "تحلیل AI"
    SessionMode.RESULT -> "نتیجه"
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF81C784)
    score >= 60 -> Color(0xFFFFB74D)
    else -> Color(0xFFE57373)
}
