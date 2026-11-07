package app.melova.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.data.model.ScoreMetric
import app.melova.platform.persianDigits
import app.melova.ui.layout.LtrContent
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

@Composable
fun ScoreMetricsList(
    metrics: List<ScoreMetric>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    // Column (not LazyColumn): this list lives inside VocalTrainingScreen's verticalScroll.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metrics.forEach { metric ->
            ScoreMetricRow(metric = metric)
        }
    }
}

/** One part of the score: its name and value over a bar filled to it in the score's colour. */
@Composable
private fun ScoreMetricRow(metric: ScoreMetric) {
    val color = scoreColor(metric.value)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
            if (!metric.hint.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                LtrContent {
                    Text(
                        text = metric.hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = persianDigits("${metric.value}٪"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        MelovaProgressBar(
            progress = metric.value / 100f,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MelovaText.copy(alpha = 0.08f),
            followLayoutDirection = true,
            thickness = 4.dp
        )
    }
}
