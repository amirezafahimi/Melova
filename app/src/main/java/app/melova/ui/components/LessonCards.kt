package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** Lesson number in a soft square, or a check once every section is practiced. */
@Composable
private fun LessonBadge(number: Int, isDone: Boolean, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(
                if (isDone) {
                    Brush.linearGradient(listOf(MelovaSuccess.copy(alpha = 0.3f), MelovaSuccess.copy(alpha = 0.18f)))
                } else {
                    Brush.linearGradient(listOf(MelovaAccent.copy(alpha = 0.35f), MelovaSecondary.copy(alpha = 0.25f)))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(Icons.Default.Check, contentDescription = "تکمیل شد", tint = MelovaSuccess)
        } else {
            Text(number.toString(), style = MaterialTheme.typography.titleLarge, color = MelovaAccentSoft)
        }
    }
}

@Composable
private fun LessonProgress(practiced: Int, available: Int, modifier: Modifier = Modifier) {
    if (available <= 0) {
        Text("به‌زودی", style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted, modifier = modifier)
        return
    }
    val isDone = practiced >= available
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MelovaProgressBar(
            progress = practiced.toFloat() / available,
            modifier = Modifier.weight(1f),
            color = if (isDone) MelovaSuccess else MelovaAccent,
            trackColor = MelovaText.copy(alpha = 0.08f),
            followLayoutDirection = true
        )
        Text(
            text = if (isDone) "تکمیل" else "$practiced/$available",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDone) MelovaSuccess else MelovaTextMuted
        )
    }
}

/** Compact lesson card for a teacher's horizontal row on the practice tab. */
@Composable
fun LessonTile(
    number: Int,
    lesson: Lesson,
    practiced: Int,
    onClick: () -> Unit
) {
    val available = lesson.availableSections
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp, modifier = Modifier.width(210.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LessonBadge(number, isDone = available > 0 && practiced >= available, size = 40)
                Spacer(Modifier.weight(1f))
                if (!lesson.published) MelovaChip(text = "پیش‌نویس", color = MelovaGold)
                else if (lesson.hasIntro) MelovaChip(text = "آموزش صوتی", color = MelovaGold)
            }
            Column(modifier = Modifier.height(44.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lesson.subtitle.ifBlank { "${lesson.sections.size} بخش" + if (lesson.level.isNotBlank()) " • ${lesson.level}" else "" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LessonProgress(practiced, available, Modifier.fillMaxWidth())
        }
    }
}

/** Full-width lesson row, for a teacher's page. */
@Composable
fun LessonRowCard(
    number: Int,
    lesson: Lesson,
    practiced: Int,
    onClick: () -> Unit
) {
    val available = lesson.availableSections
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LessonBadge(number, isDone = available > 0 && practiced >= available, size = 46)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("${lesson.sections.size} بخش")
                            if (lesson.level.isNotBlank()) append(" • ${lesson.level}")
                            if (lesson.hasIntro) append(" • آموزش صوتی")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!lesson.published) MelovaChip(text = "پیش‌نویس", color = MelovaGold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MelovaTextMuted)
            }
            LessonProgress(practiced, available, Modifier.fillMaxWidth())
        }
    }
}
