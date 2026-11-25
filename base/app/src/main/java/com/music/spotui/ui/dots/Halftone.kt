package com.music.spotui.ui.dots

import android.annotation.SuppressLint
import com.google.android.material.color.utilities.ColorUtils
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A cover reduced to a two-ink print, the look of the whole design: a paper colour, two inks,
 * and how much of each ink every point of the picture needs ([a], [b], 0..1 on a [grid]×[grid]
 * raster). Drawn as two dot screens at different angles ([Style.DOTS]) or as wavy line ribbons
 * whose width follows the ink ([Style.LINES]); see HalftoneDraw.kt.
 */
class HalftoneArt(
    val paper: Int,
    val inkA: Int,
    val inkB: Int,
    val style: Style,
    val grid: Int,
    val a: FloatArray,
    val b: FloatArray,
) {
    enum class Style { DOTS, LINES }

    /** The same picture drawn in [s] (the "Covers" setting overrides the automatic choice). */
    fun withStyle(s: Style): HalftoneArt = if (s == style) this else HalftoneArt(paper, inkA, inkB, s, grid, a, b)

    /** Ink A at (u, v) in 0..1, bilinear. */
    fun sampleA(u: Float, v: Float): Float = sample(a, u, v)

    /** Ink B at (u, v) in 0..1, bilinear. */
    fun sampleB(u: Float, v: Float): Float = sample(b, u, v)

    private fun sample(f: FloatArray, u: Float, v: Float): Float {
        val x = (u.coerceIn(0f, 1f) * (grid - 1))
        val y = (v.coerceIn(0f, 1f) * (grid - 1))
        val x0 = x.toInt().coerceAtMost(grid - 2)
        val y0 = y.toInt().coerceAtMost(grid - 2)
        val fx = x - x0
        val fy = y - y0
        val i = y0 * grid + x0
        val top = f[i] + (f[i + 1] - f[i]) * fx
        val bottom = f[i + grid] + (f[i + grid + 1] - f[i + grid]) * fx
        return top + (bottom - top) * fy
    }
}

/**
 * Colour separation: from a cover's pixels to a [HalftoneArt]. Pure (no Android graphics), so it
 * runs off the main thread and in unit tests.
 *
 * 1. The cover is quantized to a dozen colours (Material's own Celebi quantizer).
 * 2. Paper = the colour covering the most of the cover (its background, light or dark).
 * 3. Ink A = the colour that is both common and most unlike the paper; ink B = the next one,
 *    unlike both. A cover with one colour only gets a second ink made from the paper's hue.
 * 4. Each pixel is unmixed into paper + a·(A − paper) + b·(B − paper) by least squares in linear
 *    light, a and b clamped to 0..1: how much of each ink that point is printed with.
 * 5. Busy covers are printed as dots, smooth ones as lines ([detail] against [LINES_BELOW]).
 */
@SuppressLint("RestrictedApi")
object Halftone {
    /** Side of the coverage raster: enough for ~30 dots across the biggest cover. */
    const val GRID = 48

    /** Mean luminance step between neighbours under which a cover counts as smooth (lines). */
    const val LINES_BELOW = 0.055f

    fun separate(argb: IntArray, width: Int, height: Int): HalftoneArt {
        val px = resample(argb, width, height, GRID)
        val counts = QuantizerCelebi.quantize(px, 12)
            .filterKeys { (it ushr 24) > 0x80 }
            .ifEmpty { mapOf(0xFF808080.toInt() to 1) }
        val total = counts.values.sum().toDouble()
        val labs = counts.keys.associateWith { ColorUtils.labFromArgb(it) }
        fun dist(x: Int, y: Int): Double {
            val p = labs[x] ?: ColorUtils.labFromArgb(x)
            val q = labs[y] ?: ColorUtils.labFromArgb(y)
            return sqrt((p[0] - q[0]).sq() + (p[1] - q[1]).sq() + (p[2] - q[2]).sq())
        }

        val paper = counts.maxBy { it.value }.key
        val others = counts.keys - paper
        var inkA = others.maxByOrNull { sqrt(counts.getValue(it) / total) * dist(paper, it) }
        if (inkA == null || dist(paper, inkA) < 22) inkA = contrasting(paper, 40.0)
        inkA = punchy(inkA)
        val forB = others - inkA
        var inkB = forB.maxByOrNull { sqrt(counts.getValue(it) / total) * min(dist(paper, it), dist(inkA, it)) }
        if (inkB == null || min(dist(paper, inkB), dist(inkA, inkB)) < 18) inkB = contrasting(paper, 70.0)
        inkB = punchy(inkB)

        val a = FloatArray(GRID * GRID)
        val b = FloatArray(GRID * GRID)
        val pl = linear(paper)
        val e1 = sub(linear(inkA), pl)
        val e2 = sub(linear(inkB), pl)
        val e11 = dot(e1, e1)
        val e22 = dot(e2, e2)
        val e12 = dot(e1, e2)
        val det = e11 * e22 - e12 * e12
        for (i in px.indices) {
            val d = sub(linear(px[i]), pl)
            val d1 = dot(e1, d)
            val d2 = dot(e2, d)
            var ca: Float
            var cb: Float
            if (abs(det) > 1e-6f) {
                ca = (d1 * e22 - d2 * e12) / det
                cb = (d2 * e11 - d1 * e12) / det
            } else {
                ca = if (e11 > 0f) d1 / e11 else 0f
                cb = 0f
            }
            // Re-solve the free one when the other hit a bound: still the closest printable mix.
            if (ca !in 0f..1f) {
                ca = ca.coerceIn(0f, 1f)
                cb = if (e22 > 0f) ((d2 - ca * e12) / e22) else 0f
            }
            if (cb !in 0f..1f) {
                cb = cb.coerceIn(0f, 1f)
                ca = if (e11 > 0f) ((d1 - cb * e12) / e11) else 0f
            }
            a[i] = ca.coerceIn(0f, 1f)
            b[i] = cb.coerceIn(0f, 1f)
        }
        val style = if (detail(px, GRID) < LINES_BELOW) HalftoneArt.Style.LINES else HalftoneArt.Style.DOTS
        return HalftoneArt(paper, inkA, inkB, style, GRID, a, b)
    }

    /** How busy a picture is: the mean luminance step between neighbouring raster points, 0..1. */
    fun detail(px: IntArray, n: Int): Float {
        var sum = 0f
        var count = 0
        for (y in 0 until n) for (x in 0 until n) {
            val l = luma(px[y * n + x])
            if (x + 1 < n) { sum += abs(l - luma(px[y * n + x + 1])); count++ }
            if (y + 1 < n) { sum += abs(l - luma(px[(y + 1) * n + x])); count++ }
        }
        return if (count == 0) 0f else sum / count
    }

    /**
     * Art for something with no cover (a blank playlist, a failed load): the design's own banded
     * dots, in a palette picked from [seed] so the same item always looks the same.
     */
    fun synthetic(seed: String): HalftoneArt {
        val palettes = intArrayOf(
            0xFFFFE9D6.toInt(), 0xFFFF4A1C.toInt(), 0xFF1B1B3A.toInt(),
            0xFF050B2E.toInt(), 0xFF22E4FF.toInt(), 0xFFFF2238.toInt(),
            0xFFFFC83D.toInt(), 0xFF1A1200.toInt(), 0xFFFF6F91.toInt(),
            0xFF1A0B33.toInt(), 0xFFB79CF5.toInt(), 0xFFC6FF5A.toInt(),
            0xFF062A12.toInt(), 0xFF3BD16F.toInt(), 0xFFFF5FD2.toInt(),
            0xFFFF6F91.toInt(), 0xFFFFF0F3.toInt(), 0xFF4A0526.toInt(),
        )
        val h = seed.hashCode()
        val p = (h ushr 1) % (palettes.size / 3) * 3
        val a = FloatArray(GRID * GRID)
        val b = FloatArray(GRID * GRID)
        val ph = (h and 0xFF) / 40f
        for (y in 0 until GRID) for (x in 0 until GRID) {
            val u = x / (GRID - 1f)
            val v = y / (GRID - 1f)
            a[y * GRID + x] = (band(u, v, 0.7f, 0.7f, 0.8f, 0.06f, 0.08f, ph) + 0.08f).coerceAtMost(1f)
            b[y * GRID + x] = band(u, v, 0.7f, 0.7f, 0.2f, 0.04f, 0.06f, ph + 1f)
        }
        val style = if ((h ushr 9) % 3 == 0) HalftoneArt.Style.LINES else HalftoneArt.Style.DOTS
        return HalftoneArt(palettes[p], palettes[p + 1], palettes[p + 2], style, GRID, a, b)
    }

    /** The design's tone band: a soft diagonal stripe that wobbles, as in every mock-up. */
    fun band(u: Float, v: Float, ax: Float, ay: Float, c: Float, wd: Float, wob: Float, ph: Float): Float {
        val d = u * ax + v * ay - c - wob * kotlin.math.sin(5f * (v - u) + ph)
        return kotlin.math.exp(-d * d / wd)
    }

    // --- helpers ---

    /** Box-resamples the cover to n×n (Glide hands over ~64-128 px; the raster wants [GRID]). */
    private fun resample(src: IntArray, w: Int, h: Int, n: Int): IntArray {
        val out = IntArray(n * n)
        for (y in 0 until n) for (x in 0 until n) {
            val x0 = x * w / n
            val x1 = max(x0 + 1, (x + 1) * w / n)
            val y0 = y * h / n
            val y1 = max(y0 + 1, (y + 1) * h / n)
            var r = 0; var g = 0; var bl = 0; var c = 0
            for (yy in y0 until min(y1, h)) for (xx in x0 until min(x1, w)) {
                val p = src[yy * w + xx]
                r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; bl += p and 0xFF; c++
            }
            if (c == 0) c = 1
            out[y * n + x] = (0xFF shl 24) or ((r / c) shl 16) or ((g / c) shl 8) or (bl / c)
        }
        return out
    }

    /** An ink of the paper's hue, [toneGap] tones away from it (darker on light paper). */
    private fun contrasting(paper: Int, toneGap: Double): Int {
        val p = Hct.fromInt(paper)
        val tone = if (p.tone > 50) (p.tone - toneGap).coerceAtLeast(8.0) else (p.tone + toneGap).coerceAtMost(94.0)
        return Hct.from(p.hue, max(p.chroma, 24.0), tone).toInt()
    }

    /** Printing inks are a touch more saturated than the photo they reproduce. */
    private fun punchy(argb: Int): Int {
        val h = Hct.fromInt(argb)
        return if (h.chroma < 8) argb else Hct.from(h.hue, h.chroma * 1.18, h.tone).toInt()
    }

    private fun luma(p: Int): Float =
        (0.2126f * ((p shr 16) and 0xFF) + 0.7152f * ((p shr 8) and 0xFF) + 0.0722f * (p and 0xFF)) / 255f

    private fun lin(c: Int): Float {
        val s = c / 255f
        return if (s <= 0.04045f) s / 12.92f else Math.pow(((s + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }

    private fun linear(p: Int) = floatArrayOf(lin((p shr 16) and 0xFF), lin((p shr 8) and 0xFF), lin(p and 0xFF))
    private fun sub(x: FloatArray, y: FloatArray) = floatArrayOf(x[0] - y[0], x[1] - y[1], x[2] - y[2])
    private fun dot(x: FloatArray, y: FloatArray) = x[0] * y[0] + x[1] * y[1] + x[2] * y[2]
    private fun Double.sq() = this * this
}
