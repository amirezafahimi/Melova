package app.melova.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaSmallShape
import kotlin.math.abs
import kotlin.math.sin

/**
 * Placeholders for what the page is still fetching. A page keeps its own shape while it loads —
 * the wave where the wave will be, a row where each row will be — with a light passing over them,
 * instead of an empty box or a spinner that says nothing about what is coming.
 */
@Composable
private fun shimmer(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600, easing = LinearEasing)),
        label = "sweep"
    )
    return sweep
}

/** A placeholder's own colour, and the brighter crest the light carries across it. */
private val skeletonBase: Color
    @Composable @ReadOnlyComposable get() = MelovaCardElevated
private val skeletonCrest: Color
    @Composable @ReadOnlyComposable get() = lerp(MelovaCardElevated, MelovaAccent, 0.22f)

/** The light that crosses a placeholder, from its left edge to its right. */
private fun shimmerBrush(sweep: Float, width: Float, base: Color, crest: Color): Brush {
    val band = width * 0.7f
    val start = -band + (width + band) * sweep
    return Brush.linearGradient(
        colors = listOf(base, crest, base),
        start = Offset(start, 0f),
        end = Offset(start + band, 0f)
    )
}

/** One grey block standing in for text or a control that hasn't arrived. */
@Composable
fun MelovaSkeleton(modifier: Modifier = Modifier, shape: Shape = MelovaSmallShape) {
    val sweep = shimmer()
    val base = skeletonBase
    val crest = skeletonCrest
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind { drawRect(shimmerBrush(sweep, size.width, base, crest)) }
    )
}

/**
 * A wave's place while its audio is still being fetched: bars of a settled, uneven height, so it
 * reads as a performance about to appear rather than as a wave with nothing in it.
 */
@Composable
fun MelovaWaveSkeleton(modifier: Modifier = Modifier, height: Dp = 170.dp) {
    val sweep = shimmer()
    val base = skeletonBase
    val crest = skeletonCrest
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MelovaSmallShape)
            .drawBehind {
                val brush = shimmerBrush(sweep, size.width, base, crest)
                val barWidth = 3.dp.toPx()
                val gap = 3.dp.toPx()
                val count = (size.width / (barWidth + gap)).toInt().coerceAtLeast(1)
                val middle = size.height / 2f
                for (index in 0 until count) {
                    // A settled shape rather than random noise, so it doesn't flicker on redraw.
                    val wave = abs(sin(index * 0.37f) * 0.6f + sin(index * 0.11f) * 0.4f)
                    val barHeight = (size.height * 0.12f + size.height * 0.62f * wave)
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(index * (barWidth + gap), middle - barHeight / 2f),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }
            }
    )
}

/** A list row's place: the badge at its start, two lines of text, a control at its end. */
@Composable
fun MelovaRowSkeleton(modifier: Modifier = Modifier, showLead: Boolean = true) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showLead) MelovaSkeleton(Modifier.size(MelovaDimens.ListLead), CircleShape)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MelovaSkeleton(Modifier.fillMaxWidth(0.45f).height(12.dp))
            MelovaSkeleton(Modifier.fillMaxWidth(0.7f).height(10.dp))
        }
        MelovaSkeleton(Modifier.width(44.dp).height(MelovaDimens.IconLarge))
    }
}

/**
 * A light passing over something already drawn that is not finished yet — the wave while its
 * ornaments are still being read. What is under it stays visible, and stays playable: the veil
 * takes no touches of its own.
 */
@Composable
fun MelovaShimmerVeil(modifier: Modifier = Modifier, shape: Shape = MelovaSmallShape) {
    val sweep = shimmer()
    val crest = MelovaAccent.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind { drawRect(shimmerBrush(sweep, size.width, Color.Transparent, crest)) }
    )
}
