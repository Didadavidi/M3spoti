package com.music.spotui.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import app.myspoti.engine.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Official-app press feedback: the control shrinks a little under the finger and
 * springs back on release, no ripple. Animates only while pressed.
 */
fun Modifier.pressable(
    pressedScale: Float = 0.9f,
    enabled: Boolean = true,
    onPressHint: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val hinted = if (onPressHint == null) Modifier else Modifier.pressHint(onPressHint)
    hinted.then(pressScale(source, pressedScale)).clickable(source, indication = null, enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
}

/**
 * Calls [onHint] when a finger rests on the element (80 ms without moving past the
 * touch slop) or taps it: the page behind it starts loading ~100 ms before the
 * click. A touch that turns into a scroll asks for nothing.
 */
fun Modifier.pressHint(onHint: () -> Unit): Modifier = composed {
    // The gesture is not restarted when a caller passes a new lambda: a recomposition under a finger keeps the touch.
    val currentOnHint by rememberUpdatedState(onHint)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val slop = viewConfiguration.touchSlop
            var moved = false
            withTimeoutOrNull(80) {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    if ((change.position - down.position).getDistance() > slop) { moved = true; break }
                    if (!change.pressed) break
                }
            }
            if (!moved) currentOnHint()
        }
    }
}

/** The shrink of [pressable], for a control whose clickable sits further down the chain. */
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.9f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    // Reduced motion (the phone's setting or the app's): no bounce, the control stays put.
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    val scale by animateFloatAsState(
        if (pressed && !reduce) pressedScale else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * The press scale for size: a small control (icon, chip) 0.9, a button 0.94, a card or a row
 * as wide as the screen 0.97 (a big thing shrinking far looks like it falls away).
 */
object PressScale {
    const val ICON = 0.9f
    const val BUTTON = 0.94f
    const val CARD = 0.97f
}

/**
 * [pressable] for an element that also has a long press (a song's menu): the same spring under
 * the finger, a tap calls [onClick], holding calls [onLongClick].
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.pressableLong(
    pressedScale: Float = PressScale.CARD,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    pressScale(source, pressedScale).combinedClickable(
        interactionSource = source, indication = null, enabled = enabled,
        role = androidx.compose.ui.semantics.Role.Button, onLongClick = onLongClick, onClick = onClick,
    )
}

/**
 * The design's squeeze for a pill or rounded button: while it is held its corners spring from
 * [rest] to [pressed] (a pill becomes a rounded square), then bounce back. Clip and fill the
 * button with the returned shape and give its clickable the same [source]:
 *
 *     val source = remember { MutableInteractionSource() }
 *     val shape = rememberSqueezeShape(source, 28.dp, 16.dp)
 *     Modifier.pressScale(source, PressScale.BUTTON).clip(shape).background(c)
 *         .clickable(source, indication = null) { … }
 */
@androidx.compose.runtime.Composable
fun rememberSqueezeShape(
    source: MutableInteractionSource,
    rest: androidx.compose.ui.unit.Dp,
    pressed: androidx.compose.ui.unit.Dp,
): androidx.compose.ui.graphics.Shape {
    val down by source.collectIsPressedAsState()
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    val corner by androidx.compose.animation.core.animateDpAsState(
        if (down && !reduce) pressed else rest,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "squeeze",
    )
    return androidx.compose.foundation.shape.RoundedCornerShape(corner)
}

/**
 * A pill or rounded button in one call: springy scale, corners squeezing from [rest] to
 * [pressed] while held, filled with [color]. Put it where the button's clip, background and
 * clickable were; padding and content come after it.
 */
fun Modifier.squeezeButton(
    color: Color,
    rest: androidx.compose.ui.unit.Dp,
    pressed: androidx.compose.ui.unit.Dp,
    pressedScale: Float = PressScale.BUTTON,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val shape = rememberSqueezeShape(source, rest, pressed)
    pressScale(source, pressedScale)
        .then(androidx.compose.ui.Modifier.clip(shape))
        .then(androidx.compose.ui.Modifier.background(color, shape))
        .clickable(source, indication = null, enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
}

/**
 * The few durations the Classic look's small transitions share (the same values as the
 * Immersive look's [com.music.spotui.ui.immersive.motion.ImmersiveMotion], so both feel alike):
 * a colour or glyph changing state, and content arriving in place of its placeholder.
 */
object Motion {
    const val FADE_MS = 150
    const val CONTENT_MS = 220
}

/**
 * One glyph replacing another (play/pause, a heart filling, a follow check), as in the official
 * app: the new one springs in from a smaller size while the old one fades out, instead of
 * swapping in a single frame. Shown at once on first display; a plain swap under "Remove
 * animations". [target] is what the glyph shows (a Boolean, an icon...).
 */
@Composable
fun <T> GlyphSwap(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            if (reduce) EnterTransition.None togetherWith ExitTransition.None
            else (scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) + fadeIn(tween(120))) togetherWith
                (scaleOut(tween(120), targetScale = 0.6f) + fadeOut(tween(90)))
        },
        contentAlignment = Alignment.Center,
        label = "glyphSwap",
    ) { content(it) }
}

/**
 * The play/pause glyph of the official app, on [GlyphSwap]. [playing] picks the pause glyph.
 */
@Composable
fun PlayPauseIcon(
    playing: Boolean,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    GlyphSwap(playing) { isPlaying ->
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_playing else R.drawable.play_svgrepo_com),
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    }
}

/**
 * A page that loads: its content fades in over its placeholder ([Loader]) instead of replacing
 * it in one frame. A page shown from the cache (not loading at first) appears at once, and an
 * update of a page already shown keeps its composition and its state (same [loading], no fade).
 * A plain cut under "Remove animations". [content] gets whether it draws the loading state.
 */
@Composable
fun LoadedFade(loading: Boolean, modifier: Modifier = Modifier, content: @Composable (loading: Boolean) -> Unit) {
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    androidx.compose.animation.Crossfade(
        targetState = loading,
        modifier = modifier,
        animationSpec = if (reduce) androidx.compose.animation.core.snap() else tween(Motion.CONTENT_MS),
        label = "loadedFade",
    ) { content(it) }
}

/**
 * A row of a lazy list arriving in place of the list's placeholder rows: it fades in once instead
 * of popping. A list laid out with its rows at once (from the cache) shows them still, and rows
 * scrolled into view never fade (Compose animates only items new to the list). Nothing moves:
 * no placement animation. Nothing under "Remove animations". Only for items with stable keys.
 */
@Composable
fun LazyItemScope.arrivingRow(): Modifier =
    if (com.music.spotui.ui.look.rememberReduceMotion()) Modifier
    else Modifier.animateItem(fadeInSpec = tween(Motion.CONTENT_MS), placementSpec = null, fadeOutSpec = null)

/**
 * A placeholder row of a lazy list: it fades out as the real rows ([arrivingRow]) fade in over
 * it, instead of vanishing in one frame. Nothing under "Remove animations".
 */
@Composable
fun LazyItemScope.leavingPlaceholder(): Modifier =
    if (com.music.spotui.ui.look.rememberReduceMotion()) Modifier
    else Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = tween(Motion.CONTENT_MS))

/**
 * A row of a lazy list that reorders or loses items: it slides to its new place, on the spring
 * of the library's rows. No fades (a row that comes or goes simply is, or is not, there), and
 * no slide under "Remove animations". Only for items with stable keys.
 */
@Composable
fun LazyItemScope.animatedRow(): Modifier = Modifier.animateItem(
    fadeInSpec = null, fadeOutSpec = null,
    placementSpec = if (com.music.spotui.ui.look.rememberReduceMotion()) null else spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = androidx.compose.ui.unit.IntOffset.VisibilityThreshold,
    ),
)
