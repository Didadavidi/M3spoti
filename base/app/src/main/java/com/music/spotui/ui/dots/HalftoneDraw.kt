package com.music.spotui.ui.dots

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Screen angles of the two inks (radians), as in the design: ~15° and ~45°. */
private const val ANGLE_A = 0.26f
private const val ANGLE_B = 0.79f

/** The design's pulse: dots swell and ease back while music plays (1 when still). */
fun beatOf(t: Float): Float = 0.86f + 0.14f * sin(t * 2.6f)

/**
 * Draws [art] filling this scope: paper, then ink A, then ink B, as dots or as line ribbons.
 * [cells] is how many dots fit across the shorter side (≈16 small, ≈30 big). [t] is the dot
 * clock (0 = still): it makes the picture breathe and drift slightly while music plays.
 */
fun DrawScope.drawHalftone(art: HalftoneArt, cells: Float, t: Float = 0f) {
    drawRect(Color(art.paper))
    val step = size.minDimension / cells
    val beat = if (t == 0f) 1f else beatOf(t)
    when (art.style) {
        HalftoneArt.Style.DOTS -> {
            dotScreen(Color(art.inkA), step, ANGLE_A, beat, t) { u, v -> art.sampleA(u, v) }
            dotScreen(Color(art.inkB), step, ANGLE_B, 1f, t + 1f) { u, v -> art.sampleB(u, v) }
        }
        HalftoneArt.Style.LINES -> {
            val n = max(6, (size.height / (step * 1.35f)).roundToInt())
            val amp = size.height * 0.05f
            lineScreen(Color(art.inkA), n, amp, 7f, 0.18f, t) { u, v -> art.sampleA(u, v) }
            lineScreen(Color(art.inkB), n, amp * 1.15f, 6.1f, 0.25f, 1f - t * 0.8f) { u, v -> art.sampleB(u, v) }
        }
    }
}

/**
 * One ink as a rotated dot screen: a grid of dots at [angle], each as big as the ink wants at
 * that spot (area ∝ coverage). While animated the grid is sampled through a slow wobble, so the
 * picture drifts like a print under water, and ink A pulses with [beat].
 */
fun DrawScope.dotScreen(
    color: Color,
    step: Float,
    angle: Float,
    beat: Float,
    t: Float,
    tone: (u: Float, v: Float) -> Float,
) {
    val w = size.width
    val h = size.height
    val co = cos(angle)
    val si = sin(angle)
    val cx = w / 2f
    val cy = h / 2f
    val m = ceil(sqrt(w * w + h * h) / 2f / step).toInt() + 1
    val drift = if (t == 0f) 0f else 0.012f
    for (i in -m..m) for (j in -m..m) {
        val x = cx + (i * co - j * si) * step
        val y = cy + (i * si + j * co) * step
        if (x < -step || y < -step || x > w + step || y > h + step) continue
        val u = x / w + drift * sin(6f * y / h + t)
        val v = y / h + drift * sin(6f * x / w + 0.8f * t)
        var cov = tone(u, v) * beat
        if (cov <= 0.02f) continue
        if (cov > 1f) cov = 1f
        drawCircle(color, radius = step * 0.56f * sqrt(cov), center = Offset(x, y))
    }
}

/**
 * One ink as wavy line ribbons: [n] lines down the picture, each a sine wave whose thickness at
 * every point is the ink's coverage there. The second ink's waves run at another frequency,
 * so where both print the lines cross into a moiré.
 */
fun DrawScope.lineScreen(
    color: Color,
    n: Int,
    amp: Float,
    freq: Float,
    shift: Float,
    t: Float,
    tone: (u: Float, v: Float) -> Float,
) {
    val w = size.width
    val h = size.height
    val seg = 36
    val spacing = h / n
    val top = FloatArray((seg + 1) * 2)
    val bottom = FloatArray((seg + 1) * 2)
    val path = Path()
    for (k in 0 until n) {
        val y0 = (k + 0.5f) * spacing
        var any = false
        for (q in 0..seg) {
            val x = q * w / seg
            val u = x / w
            val y = y0 + amp * sin(u * freq + k * shift + t) + amp * 0.5f * sin(u * 2.3f - t * 0.6f + k * 0.07f)
            val cov = tone(u, (y / h).coerceIn(0f, 1f))
            val half = spacing * 0.5f * sqrt(cov.coerceIn(0f, 1f)) * 0.95f
            if (half > 0.2f) any = true
            top[q * 2] = x; top[q * 2 + 1] = y - half
            bottom[q * 2] = x; bottom[q * 2 + 1] = y + half
        }
        if (!any) continue
        path.reset()
        path.moveTo(top[0], top[1])
        for (q in 1..seg) path.lineTo(top[q * 2], top[q * 2 + 1])
        for (q in seg downTo 0) path.lineTo(bottom[q * 2], bottom[q * 2 + 1])
        path.close()
        drawPath(path, color)
    }
}

/** Which of the design's decorative dot fields to draw (each mock-up has its own band layout). */
enum class DotField { HOME_HERO, PAGE_HEADER, PLAYER }

/**
 * A decorative two-ink dot field over the whole scope (no picture under it, only the design's
 * soft bands): behind the Home hero, an album page's header and the player. [stepPx] is the dot
 * pitch; the headers fade out toward their bottom so the content below reads on the page.
 */
fun DrawScope.drawDotField(field: DotField, inkA: Color, inkB: Color, stepPx: Float, t: Float) {
    val beat = if (t == 0f) 1f else beatOf(t)
    val (a, b) = fieldCoverage(field, t)
    dotScreen(inkA, stepPx, 0.52f, beat, 0f, a)
    dotScreen(inkB, stepPx, 1.31f, 1f, 0f, b)
}

/**
 * How much of each ink a dot field has at (u, v) (0..1 across and down) at the dot clock [t]:
 * ink A, then ink B. The dots and the gradient (the "Backgrounds" setting) draw the same pattern.
 */
fun fieldCoverage(field: DotField, t: Float): Pair<(Float, Float) -> Float, (Float, Float) -> Float> {
    val beat = if (t == 0f) 1f else beatOf(t)
    val b = Halftone::band
    return when (field) {
        DotField.HOME_HERO -> Pair(
            { u, v -> b(u, v, 0.6f, 0.8f, 0.78f + 0.06f * sin(t * 0.4f), 0.06f, 0.12f, t) + 0.05f / beat },
            { u, v -> b(u, v, -0.5f, 1f, 0.35f, 0.025f, 0.08f, t * 1.2f + 2f) * 0.95f },
        )
        DotField.PAGE_HEADER -> {
            val fade = { v: Float -> ((0.92f - v) * 1.8f).coerceIn(0f, 1f) }
            Pair(
                { u, v -> (b(u, v, 0.85f, 0.6f, 0.5f + 0.05f * sin(t * 0.4f), 0.06f, 0.1f, t) + 0.06f / beat) * fade(v) },
                { u, v -> b(u, v, 1f, -0.45f, 0.1f, 0.025f, 0.08f, t * 1.2f + 2f) * fade(v) * 0.9f },
            )
        }
        DotField.PLAYER -> {
            val fade = { v: Float -> ((0.86f - v) * 1.7f).coerceIn(0f, 1f) }
            Pair(
                { u, v -> (b(u, v, 0.85f, 0.55f, 0.62f + 0.06f * sin(t * 0.4f), 0.07f, 0.1f, t) + 0.07f / beat) * fade(v) },
                { u, v -> b(u, v, 1f, -0.45f, 0.12f, 0.03f, 0.08f, t * 1.2f + 2f) * fade(v) * 0.9f },
            )
        }
    }
}

/**
 * The Lyrics background: two sets of thin wavy lines at different frequencies crossing into a
 * moiré, [color] fading from top to bottom ([alpha] overall, 0.32 in the design).
 */
fun DrawScope.drawMoire(color: Color, t: Float, lines: Int = 64, alpha: Float = 0.32f) {
    val w = size.width
    val h = size.height
    val scale = h / 844f
    fun set(amp: Float, freq: Float, shift: Float, ph: Float) {
        val path = Path()
        val seg = 30
        for (k in 0 until lines) {
            val y0 = (k + 0.5f) * h / lines
            for (q in 0..seg) {
                val x = q * w / seg
                val u = x / w
                val y = y0 + amp * sin(u * freq + k * shift + ph) + amp * 0.5f * sin(u * 2.3f - ph * 0.6f + k * 0.07f)
                if (q == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        drawPath(
            path,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color, color.copy(alpha = color.alpha * 0.35f))),
            alpha = alpha,
            style = Stroke(width = 1.6f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    set(16f * scale, 5.5f, 0.16f, t)
    set(19f * scale, 4.6f, 0.22f, 2f - t * 0.7f)
}
