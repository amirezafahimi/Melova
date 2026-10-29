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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.data.model.ScoreMetric
import app.melova.ui.layout.LtrContent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaText

@Composable
fun ScoreMetricsList(
    metrics: List<ScoreMetric>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    // Column (not LazyColumn): this list lives inside VocalTrainingScreen's verticalScroll.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { metric ->
            ScoreMetricRow(metric = metric)
        }
    }
}

@Composable
private fun ScoreMetricRow(metric: ScoreMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
            if (!metric.hint.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                LtrContent {
                    Text(
                        text = metric.hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaText.copy(alpha = 0.45f)
                    )
                }
            }
        }
        LtrContent {
            Text(
                text = "${metric.value}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MelovaAccentSoft
            )
        }
    }
}
