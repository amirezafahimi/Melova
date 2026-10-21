package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Course
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** "Teacher name • N lessons" under a course title. */
fun courseMeta(course: Course): String = buildString {
    if (course.teacherName.isNotBlank()) append(course.teacherName).append(" • ")
    append("${course.lessons.size} درس")
}

/** Course title row shown above its lessons in the practice tab. */
@Composable
fun CourseHeading(course: Course) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                color = MelovaText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = courseMeta(course), style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
        if (!course.published) MelovaChip(text = "پیش‌نویس", color = MelovaGold)
    }
}

/** A course to discover, with a follow button. */
@Composable
fun CourseCard(
    course: Course,
    isFollowed: Boolean,
    onFollowChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    MelovaSurfaceCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LibraryMusic,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = courseMeta(course),
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted
                    )
                }
            }
            val summary = course.description.ifBlank { course.subtitle }
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (course.level.isNotBlank()) MelovaChip(text = course.level)
                if (!course.published) MelovaChip(text = "پیش‌نویس", color = MelovaGold)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                FollowButton(isFollowed = isFollowed, onFollowChange = onFollowChange)
            }
        }
    }
}

@Composable
fun FollowButton(isFollowed: Boolean, onFollowChange: (Boolean) -> Unit) {
    val padding = PaddingValues(horizontal = 14.dp)
    if (isFollowed) {
        OutlinedButton(
            onClick = { onFollowChange(false) },
            shape = RoundedCornerShape(999.dp),
            contentPadding = padding,
            border = androidx.compose.foundation.BorderStroke(1.dp, MelovaOutline),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MelovaAccentSoft, modifier = Modifier.size(18.dp))
            Text(" دنبال می‌کنید", style = MaterialTheme.typography.labelLarge, color = MelovaAccentSoft)
        }
    } else {
        Button(
            onClick = { onFollowChange(true) },
            shape = RoundedCornerShape(999.dp),
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" دنبال کردن", style = MaterialTheme.typography.labelLarge)
        }
    }
}
