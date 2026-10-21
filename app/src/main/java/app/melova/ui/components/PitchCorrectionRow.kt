package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.audio.scoring.VocalScorer
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaText

@Composable
fun PitchCorrectionRow(
    correction: VocalScorer.PitchCorrection,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val accent = when (correction.direction) {
        VocalScorer.CorrectionDirection.SHARP -> MelovaWarning
        VocalScorer.CorrectionDirection.FLAT -> MelovaInfo
        VocalScorer.CorrectionDirection.GOOD -> MelovaSuccess
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MelovaText.copy(alpha = 0.12f)
                else MelovaText.copy(alpha = 0.06f)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "زمان \u200E${correction.timeLabel}\u200F • نت هدف: ${correction.expectedNote}",
            style = MaterialTheme.typography.labelMedium,
            color = MelovaText.copy(alpha = 0.7f),
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
