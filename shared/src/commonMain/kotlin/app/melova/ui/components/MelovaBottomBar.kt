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
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.melova.ui.layout.PageBottomGap
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaTextMuted

enum class MainTab { EXPLORE, PRACTICE, PROFILE }

/** Bottom space tab screens leave so their last item clears the floating bar. */
val BottomBarContentPadding = 140.dp

/**
 * Floating bottom bar. In RTL the first item sits on the right: explore (right),
 * practice as the raised centre button, profile (left).
 */
@Composable
fun MelovaBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    RtlContent {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = PageBottomGap),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = MelovaAccent, spotColor = MelovaAccent)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MelovaCard)
                    .border(1.dp, MelovaOutline.copy(alpha = 0.7f), RoundedCornerShape(26.dp)),
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
                modifier = Modifier.offset(y = (-12).dp)
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
            .clip(RoundedCornerShape(18.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 8.dp),
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
                .size(66.dp)
                .shadow(14.dp, CircleShape, ambientColor = MelovaAccent, spotColor = MelovaAccent)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary)))
                .border(4.dp, MelovaCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Text(
            text = "تمرین",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MelovaAccentSoft else MelovaTextMuted
        )
    }
}
