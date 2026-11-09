/*
 * Adapted from Apple Music TV (https://github.com/JObersi10/apple-music-tv), GPL-3.0,
 * android/app/src/main/java/com/applemusicktv/ui/components/TopNavBar.kt, AlbumCard.kt,
 * SpotlightCards.kt (GradientCard) and Skeleton.kt at 7e5c594.
 * Modified for Myspoti, 2026-11.
 *
 * Changes: built on Compose foundation instead of androidx.tv (no new library on weak TVs): the
 * focus scale, white halo and hairline border of tv-material's Card are drawn by hand. Myspoti's
 * accent, Material icons (no SF Symbols), Glide for the covers, no motion artwork, no beat-reactive
 * bars, no settings tab.
 */
package com.music.spotui.ui.tv

import com.music.spotui.ui.theme.coral

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

// The design's roles, for the pages that name them directly (artist, podcast, devices).
internal val TvBg: Color @Composable get() = tvScheme.surface
internal val TvAccent: Color @Composable get() = tvScheme.coral
internal val TvTitle: Color @Composable get() = tvScheme.onSurface
internal val TvSubtle: Color @Composable get() = tvScheme.onSurfaceVariant

/**
 * The focus look of a card: a soft scale, a white halo around it and a hairline white border,
 * as tv-material's Card draws them. [onFocus] hears the focus too.
 */
@Composable
internal fun Modifier.tvCard(
    shape: RoundedCornerShape,
    corner: Dp,
    focusedScale: Float = 1.08f,
    borderAlpha: Float = 0.55f,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, tween(150), label = "cardScale")
    val halo by animateFloatAsState(if (focused) 1f else 0f, tween(150), label = "cardHalo")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .drawBehind {
            if (halo <= 0f) return@drawBehind
            // A glow of 18 dp: three widening rounded rects, fading out.
            val glow = 18.dp.toPx()
            for (i in 1..3) {
                val g = glow * i / 3f
                drawRoundRect(
                    Color.White.copy(alpha = 0.07f * halo * (4 - i) / 3f),
                    topLeft = Offset(-g, -g), size = Size(size.width + 2 * g, size.height + 2 * g),
                    cornerRadius = CornerRadius(corner.toPx() + g),
                )
            }
        }
        .border(1.5.dp, Color.White.copy(alpha = borderAlpha * halo), shape)
        .clip(shape)
        .onFocusChanged { focused = it.isFocused || it.hasFocus; onFocus(focused) }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

/** A focusable round or pill control with a light fill when focused. */
@Composable
internal fun Modifier.tvPill(
    shape: RoundedCornerShape = RoundedCornerShape(50),
    base: Color = Color.Transparent,
    focusedFill: Color = Color(0x33FFFFFF),
    focusedScale: Float = 1.1f,
    onClick: () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, tween(150), label = "pillScale")
    val fill by animateColorAsState(if (focused) focusedFill else base, tween(150), label = "pillFill")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .background(fill)
        .onFocusChanged { focused = it.isFocused }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

/** A cover with its title and subtitle under it, for shelves and grids: the design's halftone card. */
@Composable
internal fun TvAlbumCard(title: String, subtitle: String, coverUrl: String, size: Dp, round: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) =
    TvCoverCard(title, subtitle, coverUrl, size, modifier, if (round) androidx.compose.foundation.shape.CircleShape else com.music.spotui.ui.dots.DotShapes.Squircle, onClick)

/** A song of an artist or a podcast: its number (or halftone cover), title and artist, and its length, on a soft row. */
@Composable
internal fun TvSongRow(s: com.music.spotui.data.entity.SongsModel, number: Int?, onClick: () -> Unit) {
    val ui = tvScheme
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().height(64.dp).tvFocus(shape, ui.surface, ui.coral, scale = 1.03f, onClick = onClick).clip(shape)
            .background(ui.surfaceContainerLow).padding(horizontal = if (number != null) 20.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) TvText("$number", ui.onSurfaceVariant, 18.sp, FontWeight.Bold, Modifier.width(32.dp))
        else TvArt(s.coverUri, s.title, Modifier.size(48.dp), RoundedCornerShape(12.dp), cells = 14f)
        Column(Modifier.weight(1f).padding(start = if (number == null) 14.dp else 0.dp)) {
            TvText(s.title, ui.onSurface, 18.sp, FontWeight.ExtraBold)
            TvText(s.singer, ui.onSurfaceVariant, 16.sp, FontWeight.Medium)
        }
        if (s.durationMs > 0) TvText(formatMs(s.durationMs.toLong()), ui.onSurfaceVariant, 16.sp, FontWeight.SemiBold, Modifier.padding(start = 12.dp, end = 8.dp))
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun TvCover(url: String, modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(8.dp)) {
    Box(modifier.clip(shape).background(tvScheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) GlideImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
    }
}

/** A Material icon in a focusable round button. */
@Composable
internal fun TvIconButton(icon: ImageVector, label: String, size: Dp = 48.dp, tint: Color = Color.White, base: Color = Color.Transparent,
                          modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).tvPill(base = base, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** A shimmering placeholder while a page loads: a big shelf first, then the standard ones. */
@Composable
internal fun TvShelfSkeleton(rows: Int = 3) {
    val t = rememberInfiniteTransition(label = "sk")
    val a by t.animateFloat(0.05f, 0.16f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val bar = tvScheme.onSurface.copy(alpha = a)
    Column(Modifier.fillMaxSize().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        SkeletonRow(bar, 170.dp, 5)
        repeat(rows) { SkeletonRow(bar, 150.dp, 6) }
    }
}

@Composable
private fun SkeletonRow(bar: Color, cardSize: Dp, cards: Int) {
    Column {
        Box(Modifier.padding(start = 48.dp, bottom = 14.dp).width(180.dp).height(18.dp).clip(RoundedCornerShape(5.dp)).background(bar))
        Row(Modifier.padding(start = 48.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(cards) {
                Column {
                    Box(Modifier.size(cardSize).clip(RoundedCornerShape(32)).background(bar))
                    Box(Modifier.padding(top = 8.dp).width(cardSize * 0.7f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(bar))
                }
            }
        }
    }
}
