package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.data.model.PracticeTake
import app.melova.platform.PlatformBackHandler
import app.melova.platform.formatMessageDay
import app.melova.platform.formatTimeOfDay
import app.melova.platform.persianDigits
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/**
 * The section's saved takes, opened from the notebook button in the top bar. They come in three
 * groups, each with its own heading: the best score, the first take, and the last three. A take
 * that is in two groups shows in both, and says so under its time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTakesSheet(
    takes: List<PracticeTake>,
    onOpenTake: (PracticeTake) -> Unit,
    onDismiss: () -> Unit
) {
    PlatformBackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MelovaCard
    ) {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "تاریخچهٔ اجراها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                if (takes.isEmpty()) {
                    Text(
                        "بعد از اولین تحلیل، اجرای شما اینجا نگه داشته می‌شود.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaTextMuted,
                        textAlign = TextAlign.Start
                    )
                } else {
                    TakeGroup(
                        title = "بهترین نمره",
                        icon = Icons.Filled.EmojiEvents,
                        color = MelovaGold,
                        takes = takes.filter { it.isBest },
                        onOpenTake = onOpenTake
                    )
                    TakeGroup(
                        title = "اولین اجرا",
                        icon = Icons.Filled.Flag,
                        color = MelovaSecondary,
                        takes = takes.filter { it.isFirst },
                        onOpenTake = onOpenTake
                    )
                    TakeGroup(
                        title = "سه اجرای آخر",
                        icon = Icons.Filled.History,
                        color = MelovaAccentSoft,
                        takes = takes.filter { it.isRecent },
                        onOpenTake = onOpenTake
                    )
                }
            }
        }
    }
}

/** A heading with its takes in one card under it; a group with no takes is left out. */
@Composable
private fun TakeGroup(
    title: String,
    icon: ImageVector,
    color: Color,
    takes: List<PracticeTake>,
    onOpenTake: (PracticeTake) -> Unit
) {
    if (takes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(MelovaSmallShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(MelovaDimens.IconSmall))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MelovaText,
                modifier = Modifier.weight(1f)
            )
        }
        MelovaSurfaceCard(contentPadding = 0.dp) {
            Column {
                takes.forEachIndexed { index, take ->
                    if (index > 0) HorizontalDivider(thickness = 1.dp, color = MelovaOutline)
                    PastTakeRow(take, color, listedAs = title, onClick = { onOpenTake(take) })
                }
            }
        }
    }
}

@Composable
private fun PastTakeRow(take: PracticeTake, color: Color, listedAs: String, onClick: () -> Unit) {
    // What else this take is, when it sits in more than one group.
    val alsoAs = listOfNotNull(
        "بهترین نمره".takeIf { take.isBest },
        "اولین اجرا".takeIf { take.isFirst },
        "سه اجرای آخر".takeIf { take.isRecent }
    ).filter { it != listedAs }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(MelovaDimens.ListLead)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(persianDigits(take.score.toString()), style = MaterialTheme.typography.titleSmall, color = color)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${formatMessageDay(take.createdAtMs)}، ${formatTimeOfDay(take.createdAtMs)}",
                style = MaterialTheme.typography.titleSmall,
                color = MelovaText
            )
            if (alsoAs.isNotEmpty()) {
                Text("همچنین: $alsoAs", style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            }
        }
        ForwardArrow(tint = MelovaTextMuted)
    }
}
