package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.JournalSection
import app.melova.data.model.JournalTake
import app.melova.data.model.PracticeLevels
import app.melova.platform.persianDigits
import app.melova.presentation.CatalogUiState
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSoftAction
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.ScoreBadge
import app.melova.ui.components.SectionLevelChip
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/**
 * «دفتر تمرین»: every section the student has sung, its first take beside its best, each a tap
 * from being heard again. Hearing week one against now is the plainest proof of progress a
 * singer gets, and it doesn't lean on the score being exact.
 */
@Composable
fun PracticeJournalScreen(
    state: CatalogUiState,
    onLoad: () -> Unit,
    onOpenTake: (lessonId: String, sectionId: String, takeId: String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { MelovaTopBar(title = "دفتر تمرین", onBack = onBack) }
                val journal = state.journal
                when {
                    journal == null && state.journalFailed -> item {
                        MessageCard(
                            icon = Icons.Default.CloudOff,
                            message = "دفتر تمرین باز نشد.",
                            actionLabel = "تلاش دوباره",
                            onAction = onLoad
                        )
                    }
                    journal == null -> item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MelovaAccent)
                        }
                    }
                    journal.isEmpty() -> item {
                        MessageCard(
                            icon = Icons.Outlined.AutoStories,
                            message = "هنوز اجرایی ثبت نشده. هر بخشی که بخوانید، اولین و بهترین اجرایش اینجا می‌ماند."
                        )
                    }
                    else -> {
                        item {
                            Text(
                                text = "اولین اجرا و بهترین اجرای هر بخش را کنار هم بشنوید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MelovaTextMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(journal, key = { "${it.lessonId}/${it.sectionId}" }) { section ->
                            JournalCard(section, state, onOpenTake)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalCard(
    section: JournalSection,
    state: CatalogUiState,
    onOpenTake: (lessonId: String, sectionId: String, takeId: String) -> Unit
) {
    val open: (JournalTake) -> Unit = { take -> take.id?.let { onOpenTake(section.lessonId, section.sectionId, it) } }
    MelovaSurfaceCard(contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScoreBadge(score = section.best.score, size = MelovaDimens.ListLead)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.sectionTitle.ifBlank { "بخش" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = persianDigits("${section.lessonTitle}، ${section.takes} اجرا"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SectionLevelChip(state.level(section.lessonId, section.sectionId).let { level ->
                    // The journal knows of the stamp even before the home page's progress has loaded.
                    if (section.stamped) PracticeLevels.level(section.best.score, stamped = true) else level
                })
            }
            if (section.isSingleStep) {
                TakeButton("تنها اجرا", section.first, open, Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TakeButton("اولین", section.first, open, Modifier.weight(1f))
                    TakeButton("بهترین", section.best, open, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TakeButton(label: String, take: JournalTake, onOpen: (JournalTake) -> Unit, modifier: Modifier) {
    MelovaSoftAction(
        // Half a phone's width holds the label and the score, not a date as well.
        text = persianDigits("$label: ${take.score}"),
        icon = Icons.Default.PlayArrow,
        onClick = { onOpen(take) },
        enabled = take.id != null,
        modifier = modifier
    )
}
