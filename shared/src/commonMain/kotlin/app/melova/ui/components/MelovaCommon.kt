package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaBackgroundEnd
import app.melova.ui.theme.MelovaBackgroundStart
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaText
import app.melova.platform.pad2

/** Full-screen dark gradient with a soft violet glow behind the top of the page. */
@Composable
fun MelovaBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MelovaBackgroundStart, MelovaBackgroundEnd)))
            .background(
                Brush.radialGradient(
                    colors = listOf(MelovaAccent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(900f, -120f),
                    radius = 1100f
                )
            ),
        content = content
    )
}

@Composable
fun MelovaSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val cardModifier = modifier
        .fillMaxWidth()
        .border(1.dp, MelovaOutline.copy(alpha = 0.6f), shape)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = cardModifier,
            shape = shape,
            color = MelovaCard
        ) { Box(Modifier.padding(contentPadding)) { content() } }
    } else {
        Surface(modifier = cardModifier, shape = shape, color = MelovaCard) {
            Box(Modifier.padding(contentPadding)) { content() }
        }
    }
}

@Composable
fun MelovaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MelovaText.copy(alpha = 0.08f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = MelovaText
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MelovaText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MelovaText,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted)
        }
    }
}

@Composable
fun MelovaChip(
    text: String,
    color: Color = MelovaSecondary,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
@ReadOnlyComposable
fun scoreColor(score: Int): Color = when {
    score >= 85 -> MelovaSuccess
    score >= 60 -> MelovaGold
    else -> MelovaWarning
}

@Composable
fun ScoreBadge(score: Int, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val color = scoreColor(score)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun formatClock(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    return "${totalSeconds / 60}:${(totalSeconds % 60).pad2()}"
}

/** Pill-shaped single-choice selector, e.g. major/minor. */
@Composable
fun MelovaSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MelovaCard)
            .border(1.dp, MelovaOutline.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                enabled = enabled && !selected,
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MelovaAccent else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        selected -> Color.White
                        enabled -> MelovaText
                        else -> MelovaTextMuted
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}
