/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.tv

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.myspoti.engine.R
import com.music.spotui.di.Graph
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotField
import com.music.spotui.ui.dots.DotFieldBackground
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.dots.HalftoneCover
import com.music.spotui.ui.theme.FlexDisplay
import com.music.spotui.ui.theme.FlexTitle
import com.music.spotui.ui.theme.SystemColors
import com.music.spotui.ui.theme.coral

/*
 * The M3spoti design on a TV (Music Player TV Design): 960 × 540 dp, a dark page in the
 * wallpaper's (or the design's purple) colours, halftone covers, and one focus look for
 * everything the remote can reach: a lift, a slight grow and a coral ring around a thin gap.
 */

/** The TV's colours: the design scheme, always dark (a bright full screen glares on a TV). */
internal val tvScheme: ColorScheme
    @Composable get() {
        val s = SystemColors.scheme
        if (SystemColors.dark) return s
        return remember(s.primary) { SystemColors.build(s.primary.toArgb(), dark = true, oled = false) }
    }

/** A TV short of means (TvCaps): the dot fields and covers stay still there. */
internal object TvLite {
    private var weak: Boolean? = null
    fun on(context: Context): Boolean = weak ?: runCatching { TvCaps.probe(context).weak }.getOrDefault(true).also { weak = it }
}

@Composable
internal fun tvAnimate(): Boolean {
    val context = LocalContext.current
    return remember { !TvLite.on(context) }
}

/**
 * The focus look of the design: lifted 4 dp, grown 6 %, a 4 dp coral ring outside a 3 dp gap in
 * the page's colour, a soft shadow; drawn in the element's own [shape]. [onClick] makes it a
 * button; without it the element only takes the focus.
 */
@Composable
internal fun Modifier.tvFocus(
    shape: Shape,
    gap: Color,
    ring: Color,
    scale: Float = 1.06f,
    onFocus: (Boolean) -> Unit = {},
    /** False: the focus belongs to a child (a text field); this only draws the look around it. */
    own: Boolean = true,
    onClick: (() -> Unit)? = null,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val k by animateFloatAsState(if (focused) 1f else 0f, tween(160), label = "tvFocus")
    val focus = this
        .zIndex(if (focused) 2f else 0f)
        .graphicsLayer {
            val s = 1f + (scale - 1f) * k
            scaleX = s; scaleY = s
            translationY = -4.dp.toPx() * k
        }
        .drawBehind {
            if (k <= 0f) return@drawBehind
            fun ringAt(grow: Float, color: Color) {
                val outline = shape.createOutline(Size(size.width + 2 * grow, size.height + 2 * grow), layoutDirection, this)
                translate(-grow, -grow) { drawOutline(outline, color.copy(alpha = color.alpha * k)) }
            }
            // A shadow under it, then the ring, then the gap the element sits in.
            ringAt(10.dp.toPx(), Color.Black.copy(alpha = 0.25f))
            ringAt(7.dp.toPx(), ring)
            ringAt(3.dp.toPx(), gap)
        }
        .onFocusChanged { val f = if (own) it.isFocused else it.hasFocus; focused = f; onFocus(f) }
    if (!own) return focus
    return if (onClick != null) focus.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    else focus.focusable()
}

/** The design's pill button: an icon and a wide label, filled [container]. */
@Composable
internal fun TvButton(
    label: String,
    icon: ImageVector?,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(28.dp),
    gap: Color = tvScheme.surface,
    /** The icon after the label (an arrow forward) instead of before it. */
    iconAfter: Boolean = false,
    onClick: () -> Unit,
) {
    val ui = tvScheme
    Row(
        modifier.height(height).tvFocus(shape, gap, ui.coral, onClick = onClick).clip(shape).background(container)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null && !iconAfter) Icon(icon, null, tint = content, modifier = Modifier.size(24.dp))
        Text(label, color = content, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FlexTitle, maxLines = 1)
        if (icon != null && iconAfter) Icon(icon, null, tint = content, modifier = Modifier.size(24.dp))
    }
}

/** A round (or [shape]d) icon button. */
@Composable
internal fun TvRoundButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = CircleShape,
    gap: Color = tvScheme.surface,
    iconSize: Dp = size * 0.43f,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(size).tvFocus(shape, gap, tvScheme.coral, onClick = onClick).clip(shape).background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = content, modifier = Modifier.size(iconSize)) }
}

@Composable
internal fun TvText(
    text: String,
    color: Color,
    size: TextUnit,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    family: FontFamily? = null,
    lines: Int = 1,
    spacing: TextUnit = TextUnit.Unspecified,
) = Text(text, modifier, color = color, fontSize = size, fontWeight = weight, fontFamily = family, maxLines = lines,
    overflow = TextOverflow.Ellipsis, letterSpacing = spacing, lineHeight = size * 1.15f)

/** A page title: 42 sp, black, wide. */
@Composable
internal fun TvTitle(text: String, modifier: Modifier = Modifier, size: TextUnit = 42.sp, color: Color = tvScheme.onSurface, lines: Int = 1) =
    TvText(text, color, size, FontWeight.Black, modifier, FlexDisplay, lines, spacing = (-1).sp)

/** A shelf's title: 24 sp, black. */
@Composable
internal fun TvShelfTitle(text: String, modifier: Modifier = Modifier) =
    TvText(text, tvScheme.onSurface, 24.sp, FontWeight.Black, modifier, FlexTitle)

/** The page's dot field: the two inks in soft bands, top right, still on a weak TV. */
@Composable
internal fun TvDots(inkA: Color, inkB: Color, modifier: Modifier = Modifier, field: DotField = DotField.PAGE_HEADER) {
    DotFieldBackground(field, inkA, inkB, modifier.fillMaxSize().graphicsLayer { alpha = 0.3f }, step = 9f, animate = tvAnimate())
}

/** A cover in the design's halftone, in [shape]. */
@Composable
internal fun TvArt(url: String, seed: String, modifier: Modifier, shape: Shape = DotShapes.Squircle, cells: Float = 22f, animate: Boolean = false) {
    HalftoneCover(url.ifBlank { null }, modifier, shape, cells = cells, animate = animate && tvAnimate(), fallbackSeed = seed)
}

/**
 * A cover card for shelves and grids: the halftone cover (focusable, in its own shape) with the
 * title and subtitle under it.
 */
@Composable
internal fun TvCoverCard(
    title: String,
    subtitle: String,
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = DotShapes.Squircle,
    onClick: () -> Unit,
) {
    val ui = tvScheme
    Column(modifier.width(size)) {
        TvArt(url, title, Modifier.size(size).tvFocus(shape, ui.surface, ui.coral, onClick = onClick), shape)
        Spacer(Modifier.height(10.dp))
        TvText(title, ui.onSurface, 18.sp, FontWeight.ExtraBold, Modifier.padding(horizontal = 4.dp))
        if (subtitle.isNotBlank()) TvText(subtitle, ui.onSurfaceVariant, 16.sp, FontWeight.Medium, Modifier.padding(horizontal = 4.dp))
    }
}

/** Home's lead card: landscape, the cover in halftone, its name on a dark label at the foot. */
@Composable
internal fun TvWideCard(title: String, subtitle: String, url: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ui = tvScheme
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.width(280.dp).height(170.dp).tvFocus(shape, ui.surface, ui.coral, onClick = onClick).clip(shape)) {
        TvArt(url, title, Modifier.fillMaxSize(), RoundedCornerShape(24.dp), cells = 30f)
        Column(
            Modifier.align(Alignment.BottomStart).padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(ui.surface.copy(alpha = 0.86f)).padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            TvText(title, ui.onSurface, 18.sp, FontWeight.Black, family = FlexTitle)
            if (subtitle.isNotBlank()) TvText(subtitle, ui.onSurfaceVariant, 16.sp, FontWeight.Medium)
        }
    }
}

/** A filter chip: filled when selected, outlined otherwise. */
@Composable
internal fun TvChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ui = tvScheme
    val shape = RoundedCornerShape(if (selected) 16.dp else 24.dp)
    Box(
        modifier.height(48.dp).tvFocus(shape, ui.surface, ui.coral, onClick = onClick).clip(shape)
            .background(if (selected) ui.surfaceContainerHigh else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(2.dp, ui.outlineVariant, shape))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { TvText(label, ui.onSurface, 18.sp, FontWeight.ExtraBold) }
}

/** The tabs of the floating bar, in their order; Now Playing shows only while something plays. */
internal enum class TvTab { Search, Home, Library, NowPlaying }

/**
 * The floating bar at the top: Search (an icon), Home, Library, and while a song is loaded a
 * coral wave that opens the player. The open tab sits on a deeper fill.
 */
@Composable
internal fun TvTopBar(selected: TvTab, onSelect: (TvTab) -> Unit, modifier: Modifier = Modifier) {
    val ui = tvScheme
    val playing = Graph.currentSongState.title.value.isNotBlank()
    Box(modifier.fillMaxWidth().padding(top = 27.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.shadow(24.dp, RoundedCornerShape(32.dp), clip = false).clip(RoundedCornerShape(32.dp))
                .background(ui.surfaceContainerLow.copy(alpha = 0.95f)).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvTab.entries.forEach { tab ->
                if (tab == TvTab.NowPlaying && !playing) return@forEach
                val on = tab == selected
                val shape = RoundedCornerShape(24.dp)
                val fill by animateColorAsState(if (on) ui.surfaceContainerHigh else Color.Transparent, tween(160), label = "tab")
                val base = Modifier.height(48.dp).tvFocus(shape, ui.surfaceContainerLow, ui.coral, onClick = { onSelect(tab) }).clip(shape).background(fill)
                when (tab) {
                    TvTab.Search -> Box(base.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(DesignIcons.Search, stringResource(R.string.navigationbar_musicappitems_search_title),
                            tint = if (on) ui.onSurface else ui.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
                    TvTab.NowPlaying -> Box(base.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(DesignIcons.Wave, stringResource(R.string.immersive_shell_now_playing), tint = ui.coral, modifier = Modifier.size(24.dp))
                    }
                    else -> Box(base.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                        TvText(
                            stringResource(if (tab == TvTab.Home) R.string.navigationbar_musicappitems_home_title else R.string.aa_library),
                            if (on) ui.onSurface else ui.onSurfaceVariant, 18.sp, FontWeight.ExtraBold, family = FlexTitle,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A full-page message (offline, an empty library, a failed page): [art] on the left, a big title,
 * a line under it and up to two buttons, the first one focused.
 */
@Composable
internal fun TvMessage(
    title: String,
    body: String,
    art: @Composable () -> Unit,
    primary: Pair<String, ImageVector?>,
    onPrimary: () -> Unit,
    secondary: Pair<String, ImageVector?>? = null,
    onSecondary: () -> Unit = {},
    focus: androidx.compose.ui.focus.FocusRequester,
) {
    val ui = tvScheme
    Row(Modifier.fillMaxSize().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(56.dp)) {
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) { art() }
        Column(Modifier.weight(1f)) {
            TvTitle(title, size = 40.sp, lines = 2)
            Spacer(Modifier.height(12.dp))
            TvText(body, ui.onSurfaceVariant, 20.sp, FontWeight.Medium, lines = 3)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvButton(primary.first, primary.second, ui.primary, ui.onPrimary, Modifier.focusRequester(focus), onClick = onPrimary)
                if (secondary != null) TvButton(secondary.first, secondary.second, ui.surfaceContainerHigh, ui.onSurface, onClick = onSecondary)
            }
        }
    }
}

/** The art of the empty library: an outlined arch, a halftone disc and a coral tile with a note. */
@Composable
internal fun TvEmptyArt() {
    val ui = tvScheme
    Box(Modifier.size(300.dp)) {
        Box(Modifier.offset(0.dp, 50.dp).size(206.dp, 246.dp).clip(RoundedCornerShape(topStart = 103.dp, topEnd = 103.dp, bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(ui.surfaceContainerLow).border(3.dp, ui.outlineVariant,
                RoundedCornerShape(topStart = 103.dp, topEnd = 103.dp, bottomStart = 26.dp, bottomEnd = 26.dp)))
        TvArt("", "library", Modifier.offset(128.dp, 34.dp).size(170.dp), CircleShape)
        Box(Modifier.offset(162.dp, 187.dp).size(113.dp).clip(RoundedCornerShape(30.dp)).background(ui.coral), contentAlignment = Alignment.Center) {
            Icon(DesignIcons.Library, null, tint = Color(0xFF45070A), modifier = Modifier.size(52.dp))
        }
    }
}

/** The art of a failed page: a halftone disc with a crossed-out signal. */
@Composable
internal fun TvOfflineArt() {
    val ui = tvScheme
    Box(Modifier.size(260.dp).clip(CircleShape).background(ui.surfaceContainerLow), contentAlignment = Alignment.Center) {
        TvArt("", "offline", Modifier.fillMaxSize(), CircleShape)
        Box(Modifier.size(150.dp).clip(CircleShape).background(ui.surfaceContainerLow.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Icon(DesignIcons.Wave, null, tint = ui.onSurface, modifier = Modifier.size(84.dp))
        }
        Box(Modifier.size(220.dp, 8.dp).graphicsLayer { rotationZ = 40f }.clip(RoundedCornerShape(4.dp)).background(ui.coral))
    }
}

/** True when the TV has a network that reaches the internet. */
internal fun tvOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
