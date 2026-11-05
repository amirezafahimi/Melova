package app.melova.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.LineHeightStyle
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaSmallShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.platform.PlatformBackHandler
import app.melova.platform.persianDigits
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaBackgroundEnd
import app.melova.ui.theme.MelovaBackgroundStart
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaCardShape
import app.melova.ui.theme.MelovaPillShape
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
                    // Softer on the light theme, where a strong glow hides the top bar's line.
                    colors = listOf(MelovaAccent.copy(alpha = if (LocalMelovaPalette.current.isDark) 0.22f else 0.12f), Color.Transparent),
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
    /** Marks the card the student should open next with an accent edge. */
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = MelovaCardShape
    val cardModifier = modifier
        .fillMaxWidth()
        .then(
            if (highlighted) Modifier.border(1.5.dp, MelovaAccent, shape)
            else Modifier.border(1.dp, MelovaOutline, shape)
        )
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

/**
 * The top of every inner page: a back button, the title, and a thin line that closes it off, so
 * what follows starts below a clear edge instead of crowding the title.
 */
/**
 * The top of every inner page: a back button, the title, and a thin line that closes it off, so
 * what follows starts below a clear edge instead of crowding the title.
 */
@Composable
fun MelovaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit
) {
    // Back from the system or the browser does exactly what this page's button does.
    PlatformBackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MelovaOutline),
                modifier = Modifier.size(MelovaDimens.CompactButtonHeight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MelovaText,
                        modifier = Modifier.size(MelovaDimens.Icon)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 1.dp, color = MelovaOutline)
    }
}

/**
 * The small action that sits beside something else — «درس جدید» next to a heading, «دنبال کردن»
 * at the end of a teacher's row. A tinted pill, never a solid fill: a page keeps its solid
 * button for its one main action, and these stay quiet next to what they belong to. [done] empties
 * the pill out to its outline, for an action already taken (following, a code already copied).
 */
@Composable
fun MelovaSoftAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    done: Boolean = false
) {
    val content = if (enabled) MelovaAccentSoft else MelovaTextMuted
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(MelovaDimens.CompactButtonHeight),
        shape = MelovaPillShape,
        color = if (done || !enabled) Color.Transparent else MelovaAccentTint,
        border = if (done || !enabled) BorderStroke(1.dp, MelovaOutline) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(MelovaDimens.IconSmall))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MelovaText,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted)
        }
    }
}

/**
 * The Persian font leaves more room above its letters than below, so a single line beside an
 * icon or avatar looks high; this trims the line to its letters and centres them.
 */
val CenteredLine = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)

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
            .clip(MelovaPillShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
            text = persianDigits(score.toString()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun formatClock(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    return persianDigits("${totalSeconds / 60}:${(totalSeconds % 60).pad2()}")
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
            .border(1.dp, MelovaOutline, RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                enabled = enabled && !selected,
                shape = RoundedCornerShape(999.dp),
                // A soft tint marks the choice; a solid fill would compete with the page's main button.
                color = if (selected) MelovaAccentTint else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        selected -> MelovaAccentSoft
                        enabled -> MelovaTextMuted
                        else -> MelovaTextMuted
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * The ‹ after something that opens a page. Its own drawing that never mirrors: Compose's mirrored
 * arrow came out flipped one way in some rows and the other way in others.
 */
@Composable
fun ForwardArrow(tint: Color, modifier: Modifier = Modifier) {
    Icon(ForwardChevron, contentDescription = null, tint = tint, modifier = modifier.size(MelovaDimens.Icon))
}

private val ForwardChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "ForwardChevron",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false
    ).apply {
        addPath(
            pathData = addPathNodes("M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z"),
            fill = SolidColor(Color.Black)
        )
    }.build()
}

/** A row that opens something, alone in its own card. */
@Composable
fun MelovaNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    iconTint: Color = MelovaAccentSoft,
    trailing: @Composable () -> Unit = {}
) {
    MelovaNavGroup({ MelovaGroupRow(icon, title, subtitle, onClick, showArrow, iconTint, trailing) })
}

/**
 * Rows that belong together in one card, split by thin lines, like a phone's settings list; a null
 * row is left out, so rows shown only sometimes can be passed as they are.
 */
@Composable
fun MelovaNavGroup(vararg rows: (@Composable () -> Unit)?) {
    val shown = rows.filterNotNull()
    if (shown.isEmpty()) return
    MelovaSurfaceCard(contentPadding = 0.dp) {
        Column {
            shown.forEachIndexed { index, row ->
                if (index > 0) {
                    // Starts after the icon, so the icons read as one column.
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 1.dp,
                        color = MelovaOutline
                    )
                }
                row()
            }
        }
    }
}

/** One row of a [MelovaNavGroup]: an icon in a tinted square, a title, a line under it. */
@Composable
fun MelovaGroupRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    iconTint: Color = MelovaAccentSoft,
    trailing: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MelovaSmallShape)
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(MelovaDimens.Icon))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MelovaText)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted) }
        }
        trailing()
        if (showArrow) {
            ForwardArrow(tint = MelovaTextMuted)
        }
    }
}
