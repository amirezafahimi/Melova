package app.melova.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.data.model.PracticeWeek
import app.melova.data.model.SectionLevel
import app.melova.platform.persianDigits
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaSecondaryTint
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** Saturday first, as the week is counted. */
private val WEEK_DAYS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

/**
 * This week at a glance: a mark for each day, the days sung against the goal, and one line on
 * what is left. A missed day is only an empty mark; nothing here counts down or breaks.
 */
@Composable
fun PracticeWeekCard(week: PracticeWeek, onEditGoal: () -> Unit) {
    MelovaSurfaceCard(onClick = onEditGoal) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "تمرین این هفته",
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = persianDigits("${week.practised} از ${week.goal} روز"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (week.isGoalReached) MelovaSuccess else MelovaAccentSoft
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WEEK_DAYS.forEachIndexed { index, name ->
                    DayMark(name = name, sung = week.days[index], isToday = index == week.today)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = weekLine(week),
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    modifier = Modifier.weight(1f)
                )
                Text("تغییر هدف", style = MaterialTheme.typography.labelMedium, color = MelovaAccentSoft)
            }
        }
    }
}

private fun weekLine(week: PracticeWeek): String {
    val left = week.goal - week.practised
    // Days still open this week: the ones after today, and today itself if not sung yet.
    val open = week.days.size - 1 - week.today + if (week.days[week.today]) 0 else 1
    val line = when {
        week.isGoalReached && week.fullWeeks > 1 -> "هدف این هفته پر شد. هفته‌های پر تا امروز: ${week.fullWeeks}"
        week.isGoalReached -> "هدف این هفته پر شد."
        // A goal out of reach isn't counted down; what is left of the week still counts.
        open == 0 -> "هفته‌ی تازه از شنبه شروع می‌شود."
        left > open -> "تا آخر هفته $open روز مانده؛ هر روزش حساب است."
        week.days[week.today] -> "امروز خواندید. $left روز دیگر تا هدف."
        else -> "$left روز دیگر تا هدف این هفته."
    }
    return persianDigits(line)
}

@Composable
private fun DayMark(name: String, sung: Boolean, isToday: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val ring = when {
            sung -> null
            isToday -> BorderStroke(1.5.dp, MelovaAccentSoft)
            else -> BorderStroke(1.dp, MelovaOutline)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                // Solid, so the days sung read at a glance against the empty rings.
                .background(if (sung) MelovaAccent else Color.Transparent, CircleShape)
                .then(if (ring != null) Modifier.border(ring, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (sung) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(MelovaDimens.IconSmall)
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MelovaText else MelovaTextMuted,
            fontWeight = if (isToday) FontWeight.Bold else null
        )
    }
}

/** The weekly goal and the one reminder, both the student's to choose. */
@Composable
fun WeekGoalDialog(
    week: PracticeWeek,
    /** False where reminders can't reach this device, so the switch isn't offered. */
    canRemind: Boolean,
    /** Said instead of the switch when the student could fix what keeps reminders away. */
    reminderNote: String? = null,
    onSave: (goal: Int, reminders: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var goal by remember { mutableIntStateOf(week.goal) }
    var reminders by remember { mutableStateOf(week.reminders && canRemind) }
    MelovaDialog(
        title = "هدف هفته",
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = MelovaTextMuted) }
            TextButton(onClick = { onSave(goal, reminders) }) { Text("ذخیره", color = MelovaAccent) }
        }
    ) {
        Text(
            text = "چند روز در هفته تمرین کنید؟ هر روزی که دست‌کم یک اجرای نمره‌دار داشته باشد حساب است.",
            style = MaterialTheme.typography.bodyMedium,
            color = MelovaTextMuted,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            MelovaMediaIconButton(
                onClick = { goal = (goal - 1).coerceAtLeast(PracticeWeek.MIN_GOAL) },
                icon = Icons.Default.Remove,
                contentDescription = "یک روز کمتر",
                enabled = goal > PracticeWeek.MIN_GOAL,
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft
            )
            Text(
                text = persianDigits("$goal روز"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MelovaText,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 96.dp)
            )
            MelovaMediaIconButton(
                onClick = { goal = (goal + 1).coerceAtMost(PracticeWeek.MAX_GOAL) },
                icon = Icons.Default.Add,
                contentDescription = "یک روز بیشتر",
                enabled = goal < PracticeWeek.MAX_GOAL,
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft
            )
        }
        if (canRemind) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MelovaSecondaryTint, MelovaSmallShape)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("یادآوری", style = MaterialTheme.typography.titleSmall, color = MelovaText)
                    Text(
                        text = "هفته‌ای یک بار، عصرِ روزی که برای رسیدن به هدف لازم است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
                Spacer(Modifier.size(8.dp))
                MelovaSwitch(checked = reminders, onCheckedChange = { reminders = it })
            }
        } else if (reminderNote != null) {
            Text(
                text = reminderNote,
                style = MaterialTheme.typography.bodySmall,
                color = MelovaTextMuted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A section's level, at the end of its row; nothing for one not sung yet. */
@Composable
fun SectionLevelChip(level: SectionLevel) {
    val label = level.label ?: return
    MelovaChip(
        text = label,
        // Only mastery, which the teacher's «آفرین» gives, takes the stamp's own ink colour.
        color = when (level) {
            SectionLevel.MASTERED -> MelovaAccentSoft
            SectionLevel.FAMILIAR -> MelovaText
            else -> MelovaTextMuted
        }
    )
}
