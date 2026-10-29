package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.data.model.Teacher
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** Round avatar with the teacher's initial. */
@Composable
fun TeacherAvatar(name: String, size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).ifBlank { "؟" },
            style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

/** A teacher to discover: avatar, name, lesson count and bio, with a follow button. */
@Composable
fun TeacherCard(
    teacher: Teacher,
    lessonCount: Int,
    isFollowed: Boolean,
    onFollowChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    MelovaSurfaceCard(onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeacherAvatar(teacher.name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (teacher.bio.isBlank()) "$lessonCount درس" else "$lessonCount درس • ${teacher.bio}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FollowButton(isFollowed = isFollowed, onFollowChange = onFollowChange)
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
