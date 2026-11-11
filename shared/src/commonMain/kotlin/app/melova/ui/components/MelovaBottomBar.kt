package app.melova.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import app.melova.ui.layout.PageBottomGap
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaBackgroundEnd
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaTextMuted

enum class MainTab { EXPLORE, PRACTICE, PROFILE }

/** Bottom space tab screens leave so their last item clears the floating bar. */
val BottomBarContentPadding = 120.dp

/**
 * Floating bottom bar. In RTL the first item sits on the right: explore (right),
 * practice as the raised centre button, profile (left).
 */
@Composable
fun MelovaBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    /** The page under the bar, blurred into the strip around it as the page scrolls beneath. */
    backdrop: HazeState,
    modifier: Modifier = Modifier
) {
    val background = MelovaBackgroundEnd
    RtlContent {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .hazeEffect(state = backdrop) {
                    blurRadius = 12.dp
                    backgroundColor = background
                    tints = listOf(HazeTint(background.copy(alpha = 0.25f)))
                    // Fades in from the strip's top, so the page has no hard edge where the blur starts.
                    progressive = HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f)
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = PageBottomGap),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = MelovaAccent, spotColor = MelovaAccent)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MelovaCard)
                    .border(1.dp, MelovaOutline, RoundedCornerShape(20.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SideTab(
                    label = "جستجو",
                    icon = Icons.Filled.Search,
                    selected = selected == MainTab.EXPLORE,
                    onClick = { onSelect(MainTab.EXPLORE) },
                    modifier = Modifier.weight(1f)
                )
                // Room for the raised centre button.
                Box(Modifier.weight(1f))
                SideTab(
                    label = "پروفایل",
                    icon = if (selected == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    selected = selected == MainTab.PROFILE,
                    onClick = { onSelect(MainTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
            PracticeButton(
                selected = selected == MainTab.PRACTICE,
                onClick = { onSelect(MainTab.PRACTICE) },
                // Lines its label up with the side tabs; the circle rises above the bar.
                modifier = Modifier.offset(y = (-8).dp)
            )
        }
    }
}

@Composable
private fun SideTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(if (selected) MelovaAccent else MelovaTextMuted, label = "tabTint")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun PracticeButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (selected) 1f else 0.9f, label = "practiceScale")
    // Off the practice tab the circle goes quiet, like a disabled control: a flat grey-violet fill (the
    // muted text colour laid thinly over the bar, so it shows on both themes), a muted clef and no
    // glow. On it, the accent gradient with a white clef.
    val quiet = MelovaTextMuted.copy(alpha = 0.28f).compositeOver(MelovaCard)
    val fromColor by animateColorAsState(if (selected) MelovaAccent else quiet, label = "practiceFrom")
    val toColor by animateColorAsState(if (selected) MelovaSecondary else quiet, label = "practiceTo")
    val clefTint by animateColorAsState(
        if (selected) Color.White else MelovaTextMuted.copy(alpha = 0.7f),
        label = "practiceClef"
    )
    val glow by animateColorAsState(if (selected) MelovaAccent else Color.Transparent, label = "practiceGlow")
    Column(
        modifier = modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.Tab,
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(58.dp)
                .shadow(if (selected) 10.dp else 0.dp, CircleShape, ambientColor = glow, spotColor = glow)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(fromColor, toColor)))
                .border(4.dp, MelovaCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(TrebleClefIcon, contentDescription = null, tint = clefTint, modifier = Modifier.size(36.dp))
        }
        Text(
            text = "تمرین",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MelovaAccentSoft else MelovaTextMuted
        )
    }
}
