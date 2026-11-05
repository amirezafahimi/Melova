package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import app.melova.ui.theme.MelovaDimens
import app.melova.data.model.Lesson
import app.melova.data.model.LessonPurchaseStatus
import app.melova.platform.persianDigits
import app.melova.ui.formatToman
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaError
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
            Text(persianDigits(number.toString()), style = MaterialTheme.typography.titleMedium, color = MelovaAccentSoft)
        }
    }
}

/**
 * How far the student is in a lesson, as a small ring with the count inside, or a check once every
 * section is practiced; it sits at the end of a lesson row instead of taking a line of its own.
 */
@Composable
private fun LessonProgressRing(practiced: Int, available: Int) {
    if (available <= 0) {
        MelovaChip(text = "به‌زودی", color = MelovaTextMuted)
        return
    }
    val isDone = practiced >= available
    val color = if (isDone) MelovaSuccess else MelovaAccent
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { (practiced.toFloat() / available).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = MelovaText.copy(alpha = 0.08f),
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )
        if (isDone) {
            Icon(Icons.Default.Check, contentDescription = "تکمیل شد", tint = color, modifier = Modifier.size(MelovaDimens.IconSmall))
        } else {
            Text(
                text = persianDigits("$practiced/$available"),
                style = MaterialTheme.typography.labelSmall,
                color = MelovaTextMuted
            )
        }
    }
}

/** A lesson in the student's list on the home tab: who teaches it and how far they are, in one compact row. */
@Composable
fun MyLessonRow(lesson: Lesson, practiced: Int, onClick: () -> Unit) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LessonCover(lesson, Modifier.size(MelovaDimens.ListLead).clip(MelovaSmallShape), iconSize = MelovaDimens.Icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lesson.teacher,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            when {
                lesson.isOpen -> LessonProgressRing(practiced, lesson.availableSections)
                lesson.purchaseStatus == LessonPurchaseStatus.REJECTED ->
                    MelovaChip(text = "پرداخت پیدا نشد", color = MelovaError)
                else -> MelovaChip(text = "در انتظار تأیید", color = MelovaGold)
            }
        }
    }
}

/**
 * A lesson in the explore list, in one compact row: cover, title, who teaches it and how big it
 * is, and what it takes to have it.
 */
@Composable
fun LessonSearchRow(lesson: Lesson, onClick: () -> Unit) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LessonCover(lesson, Modifier.size(MelovaDimens.ListLead).clip(MelovaSmallShape), iconSize = MelovaDimens.Icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(lesson.teacher)
                        append(" • ")
                        append(persianDigits("${lesson.sections.size} بخش"))
                        if (lesson.level.isNotBlank()) append(" • ${lesson.level}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MelovaChip(
                text = lesson.saleLabel(),
                color = if (lesson.isOwned == true || lesson.isFree) MelovaSuccess else MelovaGold
            )
        }
    }
}

/** Where a lesson stands for this student: theirs, on its way, free, or its price. */
private fun Lesson.saleLabel(): String = when {
    isOwned == true -> "درس شما"
    !isOpen && purchaseStatus == LessonPurchaseStatus.PENDING -> "در انتظار تأیید"
    isFree -> "رایگان"
    priceToman > 0 -> formatToman(priceToman)
    else -> "پولی"
}

private val CoverColors = listOf(
    Color(0xFF6D4AFF) to Color(0xFFC04CFD),
    Color(0xFFE0527D) to Color(0xFFF79D5C),
    Color(0xFF1F8A8A) to Color(0xFF5BD6A8),
    Color(0xFF3B6FE0) to Color(0xFF58C4F6),
    Color(0xFFB8782E) to Color(0xFFE9C46A),
    Color(0xFF8E3B8A) to Color(0xFFE56AA8)
)

/** Lessons have no pictures, so each gets its own colours; the same lesson always the same. */
@Composable
private fun LessonCover(lesson: Lesson, modifier: Modifier, iconSize: Dp) {
    val (from, to) = CoverColors[lesson.id.hashCode().mod(CoverColors.size)]
    Box(modifier = modifier.background(Brush.linearGradient(listOf(from, to))), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )
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
    MelovaSurfaceCard(onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LessonBadge(number, isDone = available > 0 && practiced >= available, size = 40)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(persianDigits("${lesson.sections.size} بخش"))
                        if (lesson.level.isNotBlank()) append(" • ${lesson.level}")
                        if (lesson.hasIntro) append(" • آموزش صوتی")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            when {
                !lesson.published -> MelovaChip(text = "پیش‌نویس", color = MelovaGold)
                !lesson.isOpen -> MelovaChip(text = lesson.saleLabel(), color = MelovaGold)
                else -> LessonProgressRing(practiced, available)
            }
        }
    }
}
