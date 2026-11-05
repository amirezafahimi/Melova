package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.melova.data.model.TeacherOrnament
import app.melova.data.model.TeacherOrnamentKind
import app.melova.platform.formatFixed
import app.melova.platform.pad2
import app.melova.platform.persianDigits
import app.melova.ui.layout.LtrContent
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaText

// The teacher's ornaments as rows and marks, on the performance page and in a take's preview.

@Composable
internal fun OrnamentRow(
    ornament: TeacherOrnament,
    number: Int,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val color = ornamentColor(ornament.kind, LocalMelovaPalette.current)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.16f) else MelovaText.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = persianDigits("${ornament.kind.title} $number"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText
            )
            LtrContent {
                Text(
                    text = "${formatPreciseClock(ornament.startMs)} → ${formatPreciseClock(ornament.endMs)}" +
                        "  (${persianDigits(formatFixed(ornament.durationMs / 1000f, 2))}s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.6f)
                )
            }
        }
        MelovaMediaIconButton(
            onClick = onClick,
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "توقف" else "پخش این بازه",
            containerColor = color,
            buttonSize = 40.dp,
            iconSize = 18.dp
        )
    }
}

/** The T/V mark: just the letter, bold, in the ornament's colour. */
@Composable
internal fun LetterTag(letter: String, color: Color) {
    Text(
        text = letter,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        color = color
    )
}

/** Numbers each kind on its own: تحریر ۱، تحریر ۲، ویبراتو ۱، گذر ۱… */
internal fun ornamentNumbers(ornaments: List<TeacherOrnament>): List<Int> {
    val counts = mutableMapOf<TeacherOrnamentKind, Int>()
    return ornaments.map { ornament ->
        val next = (counts[ornament.kind] ?: 0) + 1
        counts[ornament.kind] = next
        next
    }
}

/** m:ss.cc — hundredths, so a vowel's edges can be read exactly. */
internal fun formatPreciseClock(timeMs: Long): String {
    val clamped = timeMs.coerceAtLeast(0L)
    val minutes = clamped / 60_000
    val seconds = (clamped / 1000) % 60
    val hundredths = (clamped % 1000) / 10
    return persianDigits("$minutes:${seconds.pad2()}.${hundredths.pad2()}")
}
