package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import app.melova.data.model.TahrirComparison
import app.melova.data.model.TahrirKind
import app.melova.platform.persianDigits
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaText
import app.melova.platform.pad2

@Composable
fun VocalTrainingTahrirRow(
    comparison: TahrirComparison,
    isStudentSnippetPlaying: Boolean,
    isTeacherSnippetPlaying: Boolean,
    onPlayStudentSnippet: () -> Unit,
    onPlayTeacherSnippet: () -> Unit,
    onShowDetails: (() -> Unit)? = null
) {
    val scoreColor = when {
        comparison.score >= 80 -> MelovaSuccess
        comparison.score >= 55 -> MelovaWarning
        else -> MelovaError
    }

    val backgroundColor = when {
        isStudentSnippetPlaying || isTeacherSnippetPlaying -> MelovaSecondary.copy(alpha = 0.22f)
        else -> MelovaText.copy(alpha = 0.06f)
    }

    val kindLabel = when (comparison.kind) {
        TahrirKind.SUSTAINED -> "تحریر کششی"
        TahrirKind.ALTERNATING -> "تحریر چهچهه‌ای"
        TahrirKind.VIBRATO -> "ویبراتو"
        TahrirKind.UNKNOWN -> "تحریر"
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = listOfNotNull(kindLabel, wordLabel(comparison.word), "زمان ‎${formatTahrirClock(comparison.refStartMs)}‏").joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MelovaText.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = persianDigits("تطابق ${comparison.score}٪"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor,
                textAlign = TextAlign.End
            )
            onShowDetails?.let {
                Spacer(Modifier.width(8.dp))
                ShowDetailsButton(onClick = it)
            }
        }
        Text(
            text = comparison.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MelovaText.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TahrirSnippetButton(
                label = "اجرای شما",
                icon = Icons.Outlined.Mic,
                isPlaying = isStudentSnippetPlaying,
                enabled = comparison.hasStudentRegion,
                containerColor = MelovaSecondary.copy(alpha = 0.22f),
                contentColor = MelovaAccentSoft,
                onClick = onPlayStudentSnippet,
                modifier = Modifier.weight(1f)
            )
            TahrirSnippetButton(
                label = "تحریر استاد",
                icon = Icons.Outlined.Hearing,
                isPlaying = isTeacherSnippetPlaying,
                enabled = true,
                containerColor = MelovaAccent.copy(alpha = 0.28f),
                contentColor = MelovaAccentSoft,
                onClick = onPlayTeacherSnippet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TahrirSnippetButton(
    label: String,
    icon: ImageVector,
    isPlaying: Boolean,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveContainer = if (enabled) {
        if (isPlaying) containerColor.copy(alpha = 0.95f) else containerColor
    } else {
        MelovaText.copy(alpha = 0.06f)
    }
    val effectiveContent = when {
        !enabled -> contentColor.copy(alpha = 0.4f)
        isPlaying -> Color.White
        else -> contentColor
    }

    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = effectiveContainer,
        contentColor = effectiveContent
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
            if (!isPlaying && enabled) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                    tint = effectiveContent.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private fun formatTahrirClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return persianDigits("$minutes:${seconds.pad2()}")
}
