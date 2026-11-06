package com.music.spotui.ui.dots

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** The design's spring: a quick move with a small overshoot (cubic-bezier(.34, 1.56, .64, 1)). */
fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

/**
 * The play/pause button whose shape answers the state: its corners spring between
 * [playingRadius] and [pausedRadius] (a circle while playing and a rounded square when paused
 * on the big ones, the other way round on the mini player, as in the mock-ups).
 */
@Composable
fun MorphPlayButton(
    playing: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = width,
    playingRadius: Dp = width / 2,
    pausedRadius: Dp = width * 0.32f,
    iconSize: Dp = width * 0.4f,
    label: String,
    /** A ring in [ringColor] at the edge, in the button's own (morphing) shape. */
    ring: Dp = 0.dp,
    ringColor: Color = Color.Transparent,
) {
    val radius by animateDpAsState(if (playing) playingRadius else pausedRadius, bouncy(), label = "morph")
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .size(width, height)
            .clip(shape)
            .background(container)
            // Drawn here, not as a circle around the button: paused, the button is a rounded
            // square and a round ring cut into its corners.
            .then(if (ring > 0.dp) Modifier.border(ring, ringColor, shape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = content),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(iconSize)) { if (playing) drawPauseGlyph(content) else drawPlayGlyph(content) }
    }
}

/** The design's pause: two rounded bars (on a 24-unit grid: x 5.5 and 13.5, 5 wide, 15 tall). */
fun DrawScope.drawPauseGlyph(color: Color) {
    val u = size.minDimension / 24f
    val r = CornerRadius(2f * u)
    drawRoundRect(color, Offset(5.5f * u, 4.5f * u), Size(5f * u, 15f * u), r)
    drawRoundRect(color, Offset(13.5f * u, 4.5f * u), Size(5f * u, 15f * u), r)
}

/** The design's play: a soft-cornered triangle. */
fun DrawScope.drawPlayGlyph(color: Color) {
    val u = size.minDimension / 24f
    val p = Path().apply {
        moveTo(8f * u, 5.5f * u)
        lineTo(8f * u, 18.5f * u)
        quadraticTo(8f * u, 20f * u, 9.5f * u, 19.36f * u)
        lineTo(19.5f * u, 12.86f * u)
        quadraticTo(20.4f * u, 12f * u, 19.5f * u, 11.14f * u)
        lineTo(9.5f * u, 4.64f * u)
        quadraticTo(8f * u, 4f * u, 8f * u, 5.5f * u)
        close()
    }
    drawPath(p, color)
}

/**
 * The wavy progress line: the part already played is a sine wave (rolling along while music
 * plays), the rest a plain thin track, with an optional upright thumb at the join.
 * [progress] is read at draw time, so a ticking position redraws only this.
 */
@Composable
fun WavyProgress(
    progress: () -> Float,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 14.dp,
    stroke: Dp = 4.dp,
    thumb: Color? = null,
    animate: Boolean = true,
) {
    val t = rememberDotTime(animate)
    Canvas(modifier) {
        val p = progress().coerceIn(0f, 1f)
        val amp = amplitude.toPx()
        val k = (2f * Math.PI.toFloat()) / wavelength.toPx()
        val sw = stroke.toPx()
        val cy = size.height / 2f
        val end = size.width * p
        val gap = if (thumb != null) 8.dp.toPx() else sw
        // The track after the wave.
        if (end + gap < size.width - sw / 2f) {
            drawLine(track, Offset(end + gap, cy), Offset(size.width - sw / 2f, cy), strokeWidth = sw, cap = StrokeCap.Round)
        }
        // The wave itself.
        if (end > sw) {
            val path = Path()
            val phase = t.value * 3f
            var x = sw / 2f
            path.moveTo(x, cy + amp * sin(x * k - phase))
            while (x < end) {
                x = (x + 2f).coerceAtMost(end)
                path.lineTo(x, cy + amp * sin(x * k - phase))
            }
            drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        thumb?.let {
            val tw = 6.dp.toPx()
            val th = size.height.coerceAtMost(26.dp.toPx())
            drawRoundRect(it, Offset(end - tw / 2f, cy - th / 2f), Size(tw, th), CornerRadius(tw / 2f))
        }
    }
}
