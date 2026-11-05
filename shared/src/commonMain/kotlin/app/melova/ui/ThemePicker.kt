package app.melova.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.DarkPalette
import app.melova.ui.theme.LightPalette
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaPalette
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.ThemeMode

/** Each theme as a tiny app screen in its own colors, so the choice shows what it does. */
@Composable
internal fun ThemePicker(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeTile(
            label = "خودکار",
            selected = themeMode == ThemeMode.SYSTEM,
            onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
            modifier = Modifier.weight(1f)
        ) {
            drawTinyScreen(LightPalette)
            clipPath(lowerRightTriangle()) { drawTinyScreen(DarkPalette) }
        }
        ThemeTile(
            label = "روشن",
            selected = themeMode == ThemeMode.LIGHT,
            onClick = { onThemeModeChange(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f)
        ) { drawTinyScreen(LightPalette) }
        ThemeTile(
            label = "تیره",
            selected = themeMode == ThemeMode.DARK,
            onClick = { onThemeModeChange(ThemeMode.DARK) },
            modifier = Modifier.weight(1f)
        ) { drawTinyScreen(DarkPalette) }
    }
}

@Composable
private fun ThemeTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: DrawScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor by animateColorAsState(if (selected) MelovaAccent else MelovaOutline)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MelovaCard)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp)),
            onDraw = preview
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MelovaAccent, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MelovaText else MelovaTextMuted
            )
        }
    }
}

private fun DrawScope.lowerRightTriangle() = Path().apply {
    moveTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** A card with an avatar and two lines of text, and a button below it. */
private fun DrawScope.drawTinyScreen(palette: MelovaPalette) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(palette.backgroundStart, palette.backgroundEnd)))
    drawRoundRect(
        color = palette.card,
        topLeft = Offset(w * 0.1f, h * 0.12f),
        size = Size(w * 0.8f, h * 0.46f),
        cornerRadius = CornerRadius(w * 0.06f)
    )
    drawCircle(palette.accent, radius = h * 0.09f, center = Offset(w * 0.73f, h * 0.35f))
    val line = CornerRadius(h * 0.02f)
    drawRoundRect(palette.text, Offset(w * 0.2f, h * 0.27f), Size(w * 0.36f, h * 0.05f), line)
    drawRoundRect(palette.textMuted, Offset(w * 0.2f, h * 0.38f), Size(w * 0.26f, h * 0.04f), line)
    drawRoundRect(
        color = palette.accent,
        topLeft = Offset(w * 0.1f, h * 0.68f),
        size = Size(w * 0.8f, h * 0.16f),
        cornerRadius = CornerRadius(h * 0.08f)
    )
}
