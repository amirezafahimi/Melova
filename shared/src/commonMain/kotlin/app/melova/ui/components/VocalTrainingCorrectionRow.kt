package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.isNotSung
import app.melova.platform.persianDigits
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSecondaryTint
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaWarning
import kotlin.math.abs
import kotlin.math.roundToInt

/** «قربونت» in quotes, or nothing when the mistake has no word. */
fun quotedWord(word: String?): String? = word?.takeIf { it.isNotBlank() }?.let { "«$it»" }

fun correctionHeader(correction: VocalScorer.PitchCorrection): String = listOfNotNull(
    quotedWord(correction.word)?.let { "کلمه‌ی $it" },
    "زمان ‎${correction.timeLabel}‏"
).joinToString(" • ")

/** Which way a note mistake went, as the student reads it at a glance. */
fun correctionKind(correction: VocalScorer.PitchCorrection): String = when {
    correction.isNotSung -> "نت خوانده نشد"
    correction.direction == VocalScorer.CorrectionDirection.SHARP -> "بالاتر از استاد"
    correction.direction == VocalScorer.CorrectionDirection.FLAT -> "پایین‌تر از استاد"
    else -> "خطای نت"
}

@Composable
fun correctionColor(correction: VocalScorer.PitchCorrection): Color = when {
    correction.isNotSung -> MelovaTextMuted
    correction.direction == VocalScorer.CorrectionDirection.SHARP -> MelovaWarning
    correction.direction == VocalScorer.CorrectionDirection.FLAT -> MelovaInfo
    else -> MelovaSuccess
}

/**
 * One note mistake: the word it falls on as its title, what went wrong under it, both voices to
 * hear, and the way into the detail view.
 */
@Composable
fun VocalTrainingCorrectionRow(
    correction: VocalScorer.PitchCorrection,
    isSelected: Boolean,
    isStudentSnippetPlaying: Boolean,
    isTeacherSnippetPlaying: Boolean,
    onPlayStudentSnippet: () -> Unit,
    onPlayTeacherSnippet: () -> Unit,
    onShowDetails: () -> Unit,
    /** Until the student has opened one, the detail buttons blink to be found. */
    invitesDetails: Boolean
) {
    val accent = correctionColor(correction)
    val kind = correctionKind(correction)
    val word = quotedWord(correction.word)
    val cents = abs(correction.centsOff).roundToInt()

    MistakeCard(
        isLit = isSelected || isStudentSnippetPlaying || isTeacherSnippetPlaying,
        badge = {
            MistakeBadge(color = accent) {
                Icon(
                    imageVector = when {
                        correction.isNotSung -> Icons.Rounded.MusicOff
                        correction.direction == VocalScorer.CorrectionDirection.SHARP -> Icons.Rounded.ArrowUpward
                        else -> Icons.Rounded.ArrowDownward
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(MelovaDimens.Icon)
                )
            }
        },
        title = word ?: kind,
        facts = listOfNotNull(
            kind.takeIf { word != null },
            persianDigits("$cents سنت").takeIf { !correction.isNotSung && cents > 0 },
            "‎${correction.timeLabel}‏"
        ).joinToString(" • "),
        message = correction.message,
        messageColor = accent,
        teacherLabel = "نمونه استاد",
        isStudentPlaying = isStudentSnippetPlaying,
        isTeacherPlaying = isTeacherSnippetPlaying,
        hasStudentPart = true,
        onPlayStudent = onPlayStudentSnippet,
        onPlayTeacher = onPlayTeacherSnippet,
        onShowDetails = onShowDetails,
        invitesDetails = invitesDetails
    )
}

/**
 * The shape both kinds of mistake take: a badge and a title with its facts in one line, the
 * advice, the two voices side by side and the detail button under them.
 */
@Composable
internal fun MistakeCard(
    isLit: Boolean,
    badge: @Composable () -> Unit,
    title: String,
    facts: String,
    message: String,
    messageColor: Color,
    teacherLabel: String,
    isStudentPlaying: Boolean,
    isTeacherPlaying: Boolean,
    hasStudentPart: Boolean,
    onPlayStudent: () -> Unit,
    onPlayTeacher: () -> Unit,
    onShowDetails: () -> Unit,
    invitesDetails: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MelovaSmallShape)
            .background(if (isLit) MelovaSecondaryTint else MelovaCardElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badge()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MelovaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = facts,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = messageColor,
                textAlign = TextAlign.Start
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SnippetButton(
                label = "اجرای شما",
                isPlaying = isStudentPlaying,
                enabled = hasStudentPart,
                containerColor = MelovaSecondaryTint,
                contentColor = MelovaSecondary,
                onClick = onPlayStudent,
                modifier = Modifier.weight(1f)
            )
            SnippetButton(
                label = teacherLabel,
                isPlaying = isTeacherPlaying,
                enabled = true,
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft,
                onClick = onPlayTeacher,
                modifier = Modifier.weight(1f)
            )
        }
        ShowDetailsButton(onClick = onShowDetails, isInviting = invitesDetails, modifier = Modifier.fillMaxWidth())
    }
}

/** The square mark at the start of a mistake, in its colour. */
@Composable
internal fun MistakeBadge(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(MelovaDimens.ListLead)
            .clip(MelovaSmallShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SnippetButton(
    label: String,
    isPlaying: Boolean,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(MelovaDimens.CompactButtonHeight),
        shape = MelovaSmallShape,
        color = when {
            !enabled -> MelovaText.copy(alpha = 0.06f)
            isPlaying -> contentColor
            else -> containerColor
        },
        contentColor = when {
            !enabled -> contentColor.copy(alpha = 0.4f)
            isPlaying -> Color.White
            else -> contentColor
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
