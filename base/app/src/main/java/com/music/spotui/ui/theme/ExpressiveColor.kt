package com.music.spotui.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Score
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.music.spotui.data.api.SupportReminder
import com.music.spotui.di.COVER_DECODE_PX
import com.music.spotui.di.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The Expressive theme's colour scheme, derived from the playing song's cover (Material You's
 * own algorithm: [QuantizerCelebi] to a palette, [Score] to a seed, built into a tonal
 * [DynamicScheme]) and animated into on every song change. ui/theme/Color.kt's globals and
 * Theme.kt's ColorScheme both read [scheme], so every Classic screen re-skins together.
 */
@SuppressLint("RestrictedApi")
object ExpressiveColor {
    var scheme: ColorScheme by mutableStateOf(fallbackScheme())
        private set

    private val cache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, ColorScheme>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ColorScheme>?) = size > 40
        },
    )

    private var started = false
    private var animation: Job? = null

    /** Starts watching the playing cover, process-wide. Safe to call more than once. */
    fun start(context: Context, scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch {
            snapshotFlow { Graph.currentSongState.coverUri.value }
                .distinctUntilChanged()
                .collect { uri -> onCoverChanged(context.applicationContext, uri, scope) }
        }
    }

    private fun onCoverChanged(context: Context, uri: String, scope: CoroutineScope) {
        if (uri.isBlank()) {
            animateTo(fallbackScheme(), scope)
            return
        }
        cache[uri]?.let { animateTo(it, scope); return }
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .override(COVER_DECODE_PX, COVER_DECODE_PX)
            .disallowHardwareConfig()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val built = schemeFromCover(resource)
                    cache[uri] = built
                    animateTo(built, scope)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                // No cover (failed load): the fallback, never the song before's.
                override fun onLoadFailed(errorDrawable: Drawable?) { animateTo(fallbackScheme(), scope) }
            })
    }

    private fun schemeFromCover(bitmap: Bitmap): ColorScheme {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val ranked = Score.score(QuantizerCelebi.quantize(pixels, 128))
        val seedArgb = ranked.firstOrNull() ?: SupportReminder.accentColor.toArgb()
        return dynamicSchemeFor(seedArgb).toComposeColorScheme()
    }

    private fun dynamicSchemeFor(seedArgb: Int): DynamicScheme {
        val hct = Hct.fromInt(seedArgb)
        val r = (seedArgb shr 16) and 0xFF
        val g = (seedArgb shr 8) and 0xFF
        val b = seedArgb and 0xFF
        val greyscale = maxOf(abs(r - g), abs(g - b), abs(r - b)) <= 10
        return if (greyscale) SchemeNeutral(hct, true, 0.0) else SchemeTonalSpot(hct, true, 0.0)
    }

    private fun fallbackScheme(): ColorScheme =
        dynamicSchemeFor(SupportReminder.accentColor.toArgb()).toComposeColorScheme()

    private fun animateTo(target: ColorScheme, scope: CoroutineScope) {
        animation?.cancel()
        animation = scope.launch {
            val start = scheme
            val steps = 20
            for (i in 1..steps) {
                scheme = lerpColorScheme(start, target, i / steps.toFloat())
                delay(500L / steps)
            }
            scheme = target
        }
    }
}

private fun lerpColorScheme(a: ColorScheme, b: ColorScheme, t: Float): ColorScheme {
    fun c(x: Color, y: Color) = lerp(x, y, t)
    return ColorScheme(
        primary = c(a.primary, b.primary),
        onPrimary = c(a.onPrimary, b.onPrimary),
        primaryContainer = c(a.primaryContainer, b.primaryContainer),
        onPrimaryContainer = c(a.onPrimaryContainer, b.onPrimaryContainer),
        inversePrimary = c(a.inversePrimary, b.inversePrimary),
        secondary = c(a.secondary, b.secondary),
        onSecondary = c(a.onSecondary, b.onSecondary),
        secondaryContainer = c(a.secondaryContainer, b.secondaryContainer),
        onSecondaryContainer = c(a.onSecondaryContainer, b.onSecondaryContainer),
        tertiary = c(a.tertiary, b.tertiary),
        onTertiary = c(a.onTertiary, b.onTertiary),
        tertiaryContainer = c(a.tertiaryContainer, b.tertiaryContainer),
        onTertiaryContainer = c(a.onTertiaryContainer, b.onTertiaryContainer),
        background = c(a.background, b.background),
        onBackground = c(a.onBackground, b.onBackground),
        surface = c(a.surface, b.surface),
        onSurface = c(a.onSurface, b.onSurface),
        surfaceVariant = c(a.surfaceVariant, b.surfaceVariant),
        onSurfaceVariant = c(a.onSurfaceVariant, b.onSurfaceVariant),
        surfaceTint = c(a.surfaceTint, b.surfaceTint),
        inverseSurface = c(a.inverseSurface, b.inverseSurface),
        inverseOnSurface = c(a.inverseOnSurface, b.inverseOnSurface),
        error = c(a.error, b.error),
        onError = c(a.onError, b.onError),
        errorContainer = c(a.errorContainer, b.errorContainer),
        onErrorContainer = c(a.onErrorContainer, b.onErrorContainer),
        outline = c(a.outline, b.outline),
        outlineVariant = c(a.outlineVariant, b.outlineVariant),
        scrim = c(a.scrim, b.scrim),
        surfaceBright = c(a.surfaceBright, b.surfaceBright),
        surfaceDim = c(a.surfaceDim, b.surfaceDim),
        surfaceContainer = c(a.surfaceContainer, b.surfaceContainer),
        surfaceContainerHigh = c(a.surfaceContainerHigh, b.surfaceContainerHigh),
        surfaceContainerHighest = c(a.surfaceContainerHighest, b.surfaceContainerHighest),
        surfaceContainerLow = c(a.surfaceContainerLow, b.surfaceContainerLow),
        surfaceContainerLowest = c(a.surfaceContainerLowest, b.surfaceContainerLowest),
        primaryFixed = c(a.primaryFixed, b.primaryFixed),
        primaryFixedDim = c(a.primaryFixedDim, b.primaryFixedDim),
        onPrimaryFixed = c(a.onPrimaryFixed, b.onPrimaryFixed),
        onPrimaryFixedVariant = c(a.onPrimaryFixedVariant, b.onPrimaryFixedVariant),
        secondaryFixed = c(a.secondaryFixed, b.secondaryFixed),
        secondaryFixedDim = c(a.secondaryFixedDim, b.secondaryFixedDim),
        onSecondaryFixed = c(a.onSecondaryFixed, b.onSecondaryFixed),
        onSecondaryFixedVariant = c(a.onSecondaryFixedVariant, b.onSecondaryFixedVariant),
        tertiaryFixed = c(a.tertiaryFixed, b.tertiaryFixed),
        tertiaryFixedDim = c(a.tertiaryFixedDim, b.tertiaryFixedDim),
        onTertiaryFixed = c(a.onTertiaryFixed, b.onTertiaryFixed),
        onTertiaryFixedVariant = c(a.onTertiaryFixedVariant, b.onTertiaryFixedVariant),
    )
}

private fun DynamicScheme.toComposeColorScheme(): ColorScheme = ColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    inversePrimary = Color(inversePrimary),
    secondary = Color(secondary),
    onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    onTertiary = Color(onTertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer),
    background = Color(background),
    onBackground = Color(onBackground),
    surface = Color(surface),
    onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant),
    onSurfaceVariant = Color(onSurfaceVariant),
    surfaceTint = Color(surfaceTint),
    inverseSurface = Color(inverseSurface),
    inverseOnSurface = Color(inverseOnSurface),
    error = Color(error),
    onError = Color(onError),
    errorContainer = Color(errorContainer),
    onErrorContainer = Color(onErrorContainer),
    outline = Color(outline),
    outlineVariant = Color(outlineVariant),
    scrim = Color(scrim),
    surfaceBright = Color(surfaceBright),
    surfaceDim = Color(surfaceDim),
    surfaceContainer = Color(surfaceContainer),
    surfaceContainerHigh = Color(surfaceContainerHigh),
    surfaceContainerHighest = Color(surfaceContainerHighest),
    surfaceContainerLow = Color(surfaceContainerLow),
    surfaceContainerLowest = Color(surfaceContainerLowest),
    primaryFixed = Color(primaryFixed),
    primaryFixedDim = Color(primaryFixedDim),
    onPrimaryFixed = Color(onPrimaryFixed),
    onPrimaryFixedVariant = Color(onPrimaryFixedVariant),
    secondaryFixed = Color(secondaryFixed),
    secondaryFixedDim = Color(secondaryFixedDim),
    onSecondaryFixed = Color(onSecondaryFixed),
    onSecondaryFixedVariant = Color(onSecondaryFixedVariant),
    tertiaryFixed = Color(tertiaryFixed),
    tertiaryFixedDim = Color(tertiaryFixedDim),
    onTertiaryFixed = Color(onTertiaryFixed),
    onTertiaryFixedVariant = Color(onTertiaryFixedVariant),
)
