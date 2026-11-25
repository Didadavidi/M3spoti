package com.music.spotui.ui.dots

import androidx.compose.ui.graphics.asImageBitmap
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * The dot clock: seconds-ish time that only runs while music plays, the screen is visible, and
 * neither battery saver nor Android's "Remove animations" is on. Frozen otherwise (the picture
 * stays where it was, it does not jump). Read its value inside a draw lambda, not in composition,
 * so a tick redraws the dots without recomposing anything. ~14 ticks a second, like the design.
 */
@Composable
fun rememberDotTime(animate: Boolean = true): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    val playing = com.music.spotui.di.Graph.currentSongState.playingState.value
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
    val run = animate && playing && !reduce && !energy.policy.saverEnabled
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(run, lifecycle) {
        if (!run) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = withFrameNanos { it }
            while (isActive) {
                delay(66)
                val now = withFrameNanos { it }
                // 1.1 "design units" a second (0.12 every 110 ms in the mock-ups).
                t.floatValue += ((now - last) / 1e9f).coerceAtMost(0.25f) * 1.1f
                last = now
            }
        }
    }
    return t
}

/** Separations already made, by cover address (process-wide; a few KB each). */
private val artCache = LruCache<String, HalftoneArt>(256)

/** Static renders of covers, by address and pixel size (lists scroll without redrawing dots). */
private val renderCache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

/** The separation of the cover at [url] (null if it cannot load): cached, made off the main thread. */
suspend fun loadHalftone(context: Context, url: String): HalftoneArt? {
    if (url.isBlank()) return null
    artCache.get(url)?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            val bmp: Bitmap = Glide.with(context.applicationContext).asBitmap().load(url)
                .override(96, 96).disallowHardwareConfig().submit().get()
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            Halftone.separate(px, bmp.width, bmp.height)
        }.getOrNull()?.also { artCache.put(url, it) }
    }
}

/** The cached separation for [url], if made already (to draw it in the first frame). */
fun cachedHalftone(url: String): HalftoneArt? = if (url.isBlank()) null else artCache.get(url)

/**
 * A cover printed in the design's halftone: the real picture at [url] separated into paper and
 * two inks, drawn as dots or wavy lines (whichever suits the cover), clipped to [shape].
 * [cells] = dots across (≈16 for a mini player, ≈22 for tiles, ≈30 for the big covers).
 * [animate]: drift and pulse while music plays (big covers only; lists stay still and cached).
 * With no cover, the design's banded art in colours picked from [fallbackSeed].
 */
@Composable
fun HalftoneCover(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = DotShapes.Squircle,
    cells: Float = 22f,
    animate: Boolean = false,
    fallbackSeed: String = url.orEmpty(),
    placeholder: Color = Color(0x22888888),
) {
    val context = LocalContext.current
    val key = url.orEmpty()
    val coverMode = com.music.spotui.data.preferences.CoverStylePref.mode
    // "Covers: Original": the picture itself, no halftone (a cover without a picture keeps the design's art).
    if (coverMode == com.music.spotui.data.preferences.CoverStylePref.ORIGINAL && key.isNotBlank()) {
        OriginalCover(key, modifier.clip(shape).background(placeholder))
        return
    }
    val art by produceState(cachedHalftone(key) ?: if (key.isBlank()) Halftone.synthetic(fallbackSeed) else null, key) {
        // The initial value only counts for the first cover: on a new one (a skipped song in the
        // player) the state still held the old art, so nothing loaded and the old cover stayed.
        value = cachedHalftone(key) ?: if (key.isBlank()) Halftone.synthetic(fallbackSeed) else null
        if (key.isNotBlank() && value == null) value = loadHalftone(context, key) ?: Halftone.synthetic(fallbackSeed.ifBlank { key })
    }
    val box = modifier.clip(shape)
    // "Covers: Dots / Waves": every cover in that style; "Automatic": whichever suits the picture.
    val a = art?.let {
        when (coverMode) {
            com.music.spotui.data.preferences.CoverStylePref.DOTS -> it.withStyle(HalftoneArt.Style.DOTS)
            com.music.spotui.data.preferences.CoverStylePref.WAVES -> it.withStyle(HalftoneArt.Style.LINES)
            else -> it
        }
    }
    if (a == null) {
        Box(box.background(placeholder))
        return
    }
    if (animate) {
        val t = rememberDotTime()
        Canvas(box) { drawHalftone(a, cells, t.value) }
    } else {
        StaticHalftone(a, "$key|$fallbackSeed|$cells|${a.style}", cells, box)
    }
}

/** A still halftone, rendered once per size into a bitmap and reused (scrolling lists). */
@Composable
private fun StaticHalftone(art: HalftoneArt, key: String, cells: Float, modifier: Modifier) {
    val density = LocalDensity.current
    val dir = LocalLayoutDirection.current
    Box(modifier.drawWithCache {
        val w = size.width.toInt().coerceAtLeast(1)
        val h = size.height.toInt().coerceAtLeast(1)
        val k = "$key|${w}x$h"
        val img = renderCache.get(k) ?: run {
            val bmp = ImageBitmap(w, h)
            CanvasDrawScope().draw(density, dir, androidx.compose.ui.graphics.Canvas(bmp), IntSize(w, h).toSize()) {
                drawHalftone(art, cells, 0f)
            }
            renderCache.put(k, bmp)
            bmp
        }
        onDrawBehind { drawImage(img) }
    })
}

/**
 * A decorative dot field filling [modifier]: the design's soft bands of two inks, behind the
 * Home hero, an album header or the player. [step] is the dot pitch in dp (8 to 9 in the design).
 */
@Composable
fun DotFieldBackground(
    field: DotField,
    inkA: Color,
    inkB: Color,
    modifier: Modifier = Modifier,
    step: Float = 8f,
    animate: Boolean = true,
) {
    if (com.music.spotui.data.preferences.FieldStylePref.gradient) {
        GradientField(field, inkA, inkB, modifier)
        return
    }
    val t = rememberDotTime(animate)
    val density = LocalDensity.current.density
    Canvas(modifier.fillMaxSize()) { drawDotField(field, inkA, inkB, step * density, t.value) }
}

/** A cover as the picture itself (the "Covers: Original" setting), cropped to fill. */
@OptIn(com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
@Composable
private fun OriginalCover(url: String, modifier: Modifier) {
    com.bumptech.glide.integration.compose.GlideImage(
        model = url, contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier,
    )
}

/**
 * A dot field as a soft, still gradient: the same pattern as the dots ([fieldCoverage]), computed
 * on a small grid once per size and colours and drawn stretched with smooth filtering.
 */
@Composable
private fun GradientField(field: DotField, inkA: Color, inkB: Color, modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier.fillMaxSize().drawWithCache {
        val gw = 40
        val gh = (gw * size.height / size.width.coerceAtLeast(1f)).toInt().coerceIn(8, 120)
        val (covA, covB) = fieldCoverage(field, 0f)
        val px = IntArray(gw * gh)
        for (y in 0 until gh) for (x in 0 until gw) {
            val u = (x + 0.5f) / gw
            val v = (y + 0.5f) / gh
            // A dot covers about 80 % of its cell at full ink: the gradient matches that density.
            val ab = (covB(u, v) * 0.8f).coerceIn(0f, 1f)
            val aa = (covA(u, v) * 0.8f).coerceIn(0f, 1f)
            // Ink A over ink B over nothing.
            val outA = aa + ab * (1 - aa)
            fun ch(ca: Float, cb: Float) = if (outA <= 0f) 0f else (ca * aa + cb * ab * (1 - aa)) / outA
            val r = ch(inkA.red, inkB.red)
            val g = ch(inkA.green, inkB.green)
            val b = ch(inkA.blue, inkB.blue)
            px[y * gw + x] = (((outA * 255).toInt() shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt())
        }
        val img = android.graphics.Bitmap.createBitmap(px, gw, gh, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
        onDrawBehind {
            drawImage(
                img, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
            )
        }
    })
}

/** The Lyrics screen's moiré of thin wavy lines, in [color]. */
@Composable
fun MoireBackground(color: Color, modifier: Modifier = Modifier, animate: Boolean = true) {
    val t = rememberDotTime(animate)
    Canvas(modifier.fillMaxSize()) { drawMoire(color, t.value) }
}
