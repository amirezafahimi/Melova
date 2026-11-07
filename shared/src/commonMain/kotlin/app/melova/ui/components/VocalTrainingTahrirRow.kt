package app.melova.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import app.melova.data.model.TahrirComparison
import app.melova.data.model.TahrirKind
import app.melova.platform.pad2
import app.melova.platform.persianDigits
import app.melova.ui.theme.MelovaText

fun tahrirKindLabel(kind: TahrirKind): String = when (kind) {
    TahrirKind.SUSTAINED -> "تحریر کششی"
    TahrirKind.ALTERNATING -> "تحریر چهچهه‌ای"
    TahrirKind.VIBRATO -> "ویبراتو"
    TahrirKind.UNKNOWN -> "تحریر"
}

/** One tahrir or vibrato beside the teacher's: how close it came is its badge. */
@Composable
fun VocalTrainingTahrirRow(
    comparison: TahrirComparison,
    isStudentSnippetPlaying: Boolean,
    isTeacherSnippetPlaying: Boolean,
    onPlayStudentSnippet: () -> Unit,
    onPlayTeacherSnippet: () -> Unit,
    onShowDetails: () -> Unit,
    /** Until the student has opened one, the detail buttons blink to be found. */
    invitesDetails: Boolean
) {
    val color = scoreColor(comparison.score)
    val kind = tahrirKindLabel(comparison.kind)
    val word = quotedWord(comparison.word)

    MistakeCard(
        isLit = isStudentSnippetPlaying || isTeacherSnippetPlaying,
        badge = {
            MistakeBadge(color = color) {
                Text(
                    text = persianDigits("${comparison.score}٪"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        },
        title = word ?: kind,
        facts = listOfNotNull(
            kind.takeIf { word != null },
            "تطابق با استاد",
            "‎${formatTahrirClock(comparison.refStartMs)}‏"
        ).joinToString(" • "),
        message = comparison.message,
        messageColor = MelovaText,
        teacherLabel = "تحریر استاد",
        isStudentPlaying = isStudentSnippetPlaying,
        isTeacherPlaying = isTeacherSnippetPlaying,
        hasStudentPart = comparison.hasStudentRegion,
        onPlayStudent = onPlayStudentSnippet,
        onPlayTeacher = onPlayTeacherSnippet,
        onShowDetails = onShowDetails,
        invitesDetails = invitesDetails
    )
}

private fun formatTahrirClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return persianDigits("$minutes:${seconds.pad2()}")
}
