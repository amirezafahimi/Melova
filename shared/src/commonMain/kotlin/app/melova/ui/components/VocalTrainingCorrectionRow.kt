package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.audio.scoring.VocalScorer
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaText

/** «کلمه‌ی «قربونت»», or nothing when the mistake has no word. */
fun wordLabel(word: String?): String? = word?.takeIf { it.isNotBlank() }?.let { "کلمه‌ی «$it»" }

fun correctionHeader(correction: VocalScorer.PitchCorrection): String = listOfNotNull(
    wordLabel(correction.word),
    "زمان \u200E${correction.timeLabel}\u200F"
).joinToString(" • ")

@Composable
fun VocalTrainingCorrectionRow(
    correction: VocalScorer.PitchCorrection,
    isSelected: Boolean,
    isStudentSnippetPlaying: Boolean,
    isTeacherSnippetPlaying: Boolean,
    onPlayStudentSnippet: () -> Unit,
    onPlayTeacherSnippet: () -> Unit,
    onShowDetails: (() -> Unit)? = null,
    header: String = correctionHeader(correction),
    accentColor: Color? = null
) {
    val accent = accentColor ?: when (correction.direction) {
        VocalScorer.CorrectionDirection.SHARP -> MelovaWarning
        VocalScorer.CorrectionDirection.FLAT -> MelovaInfo
        VocalScorer.CorrectionDirection.GOOD -> MelovaSuccess
    }

    val backgroundColor = when {
        isStudentSnippetPlaying || isTeacherSnippetPlaying -> MelovaSecondary.copy(alpha = 0.22f)
        isSelected -> MelovaText.copy(alpha = 0.12f)
        else -> MelovaText.copy(alpha = 0.06f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelMedium,
                color = MelovaText.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            onShowDetails?.let { ShowDetailsButton(onClick = it) }
        }
        Text(
            text = correction.message,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CorrectionSnippetButton(
                label = "اجرای شما",
                icon = Icons.Outlined.Mic,
                isPlaying = isStudentSnippetPlaying,
                containerColor = MelovaSecondary.copy(alpha = 0.22f),
                contentColor = MelovaAccentSoft,
                onClick = onPlayStudentSnippet,
                modifier = Modifier.weight(1f)
            )
            CorrectionSnippetButton(
                label = "نمونه استاد",
                icon = Icons.Outlined.Hearing,
                isPlaying = isTeacherSnippetPlaying,
                containerColor = MelovaAccent.copy(alpha = 0.28f),
                contentColor = MelovaAccentSoft,
                onClick = onPlayTeacherSnippet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CorrectionSnippetButton(
    label: String,
    icon: ImageVector,
    isPlaying: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) containerColor.copy(alpha = 0.95f) else containerColor,
        contentColor = if (isPlaying) Color.White else contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
            if (!isPlaying) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                    tint = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}
