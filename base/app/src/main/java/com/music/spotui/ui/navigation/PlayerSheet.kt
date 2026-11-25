package com.music.spotui.ui.navigation

import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.composed
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Spring
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigation.NavController
import com.music.spotui.ui.screens.PlayerScreen
import kotlinx.coroutines.flow.drop

/**
 * The full player, shown over the pages instead of being one of them, like the official
 * app: pulled down, it uncovers the page it was opened from, with the mini player and the
 * tabs already in place, and closing it leaves them there.
 */
object PlayerSheet {
    val open = mutableStateOf(false)

    fun show() { open.value = true }
    fun close() { open.value = false }

    /**
     * How far a finger has pulled the open player down (px). The whole sheet follows it as a card
     * (a little smaller, rounded corners); let go, it springs back or glides on into the mini
     * player from right there.
     */
    var drag by mutableFloatStateOf(0f)
    internal var dragBack: kotlinx.coroutines.Job? = null

    /** The mini player's top edge in the window (px), where the player rises from and goes back to. */
    var miniTop by mutableFloatStateOf(0f)

    /**
     * The queue page was opened from the player: leaving it (its close arrow, back) brings the
     * player back instead of the page under it. Opening the queue is a navigation, which closes
     * the player like any other.
     */
    var reopenAfterQueue = false

    /** Opens the queue page from the player. */
    fun openQueue(navController: NavController) {
        reopenAfterQueue = true
        navController.navigate(Routes.Queue.route)
    }
}

/**
 * How far the player's content has come in while it opens (0 .. 1), read in draw lambdas: the
 * cover, the titles and the controls fade in once the sheet is under way, after its background.
 */
val LocalPlayerReveal = androidx.compose.runtime.staticCompositionLocalOf<() -> Float> { { 1f } }

/*
 * Opening, as the official app: the player grows out of the mini player. Its background rises
 * from the mini player's top edge and fades in over the first part of the way, then the cover,
 * titles and controls fade in and settle a few dp up. Closing plays it backwards: the content
 * fades first, then the sheet sinks into the mini player and fades into it. No dimming of the
 * page behind: darker than the player's own background, it made the screen blink dark then light
 * on the first and last frames (measured on a recording, reported).
 *
 * It replaces a cover flown between the two players (a copy that ran out of step with the real
 * one: a ghost square during the slide, a lone cover crossing the page on the way down) and a
 * slide whose clock started on the frame that built the whole player (its first frames lost
 * to that work: the "jump" at the start).
 */
private const val OPEN_MS = 380
private const val CLOSE_MS = 300
/** The sheet's background is fully there by this share of the way. */
private const val SHEET_FADE = 0.3f
private val CloseEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
/** Let go past this, or flicked down faster than [FLING_DP_S], the pulled player closes. */
private const val CLOSE_PULL_DP = 120
private const val FLING_DP_S = 1100
/** Pulled all the way, the card is this much smaller. */
private const val PULL_SHRINK = 0.12f

/**
 * Pulling the player down to close it: the sheet follows the finger ([PlayerSheet.drag]) and,
 * let go past the threshold or flicked, closes from where it is; otherwise it springs back.
 */
fun Modifier.pullDownToClosePlayer(enabled: Boolean): Modifier = composed {
    val scope = rememberCoroutineScope()
    if (!enabled) return@composed Modifier
    Modifier.pointerInput(Unit) {
        val tracker = VelocityTracker()
        val threshold = CLOSE_PULL_DP.dp.toPx()
        val fling = FLING_DP_S.dp.toPx()
        fun springBack(velocity: Float) {
            PlayerSheet.dragBack = scope.launch {
                animate(PlayerSheet.drag, 0f, velocity, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)) { v, _ ->
                    PlayerSheet.drag = v.coerceAtLeast(0f)
                }
            }
        }
        detectVerticalDragGestures(
            onDragStart = { PlayerSheet.dragBack?.cancel(); tracker.resetTracking() },
            onDragEnd = {
                val v = tracker.calculateVelocity().y
                val d = PlayerSheet.drag
                if (d > 0f && (d > threshold || v > fling)) PlayerSheet.close() else if (d > 0f) springBack(v)
            },
            onDragCancel = { if (PlayerSheet.drag > 0f) springBack(0f) },
        ) { change, dy ->
            tracker.addPointerInputChange(change)
            val next = PlayerSheet.drag + dy
            if (next >= 0f) { change.consume(); PlayerSheet.drag = next }
            else if (PlayerSheet.drag > 0f) { change.consume(); PlayerSheet.drag = 0f }
        }
    }
}

@Composable
fun PlayerOverlay(navController: NavController) {
    // Any navigation from the player (album, artist, playlist…) lands on that page.
    // Back from the queue opened in the player: the player again.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.drop(1).collect { entry ->
            val route = entry.destination.route
            if (route == Routes.Queue.route) PlayerSheet.close()
            else if (PlayerSheet.reopenAfterQueue) { PlayerSheet.reopenAfterQueue = false; PlayerSheet.show() }
            else PlayerSheet.close()
        }
    }
    val open = PlayerSheet.open.value
    val progress = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    val settled = remember { mutableStateOf(false) }
    // "Remove animations": the player is there, or gone, at once.
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    LaunchedEffect(open) {
        if (open) {
            PlayerSheet.dragBack?.cancel()
            PlayerSheet.drag = 0f
            shown = true
            settled.value = false
            // Two frames for the player's first composition and layout before the clock runs.
            if (progress.value == 0f) { withFrameNanos { }; withFrameNanos { } }
            if (reduce) progress.snapTo(1f) else progress.animateTo(1f, tween(OPEN_MS, easing = Decelerate))
            settled.value = true
        } else if (shown) {
            settled.value = false
            PlayerSheet.dragBack?.cancel()
            // Pulled down, it is already moving: it carries on fast and lands softly in the mini player.
            val spec = if (PlayerSheet.drag > 0f) tween<Float>(CLOSE_MS - 40, easing = Decelerate) else tween(CLOSE_MS, easing = CloseEasing)
            if (reduce) progress.snapTo(0f) else progress.animateTo(0f, spec)
            shown = false
            PlayerSheet.drag = 0f
        }
    }
    if (!shown) return
    BackHandler(enabled = open) { PlayerSheet.close() }
    var heightPx by remember { mutableIntStateOf(0) }
    val reveal = remember { { ((progress.value - 0.25f) / 0.6f).coerceIn(0f, 1f) } }
    Box(
        Modifier.fillMaxSize().onSizeChanged { heightPx = it.height }.graphicsLayer {
            val p = progress.value
            val from = PlayerSheet.miniTop.takeIf { it > 0f } ?: heightPx.toFloat()
            // Pulled: the sheet sits where the finger has it; closing, it goes on from there.
            val d = PlayerSheet.drag
            translationY = d + (1f - p) * (from - d)
            // As a card: a little smaller and rounded while pulled, and on its way in or out.
            val pulled = if (size.height > 0f) (d / size.height).coerceIn(0f, 1f) else 0f
            val s = 1f - PULL_SHRINK * pulled
            scaleX = s; scaleY = s
            transformOrigin = TransformOrigin(0.5f, 0f)
            val corner = maxOf(d / 3f, (1f - p) * 32.dp.toPx()).coerceAtMost(32.dp.toPx())
            if (corner > 0.5f) { shape = RoundedCornerShape(corner); clip = true } else clip = false
            // A layer alpha only over the first part of the way: the open player draws plain.
            alpha = (p / SHEET_FADE).coerceIn(0f, 1f)
        },
    ) {
        CompositionLocalProvider(LocalPageSettled provides settled, LocalPlayerReveal provides reveal) {
            // A podcast episode gets the podcast player (±15 s, speed, sleep timer, no shuffle);
            // everything else keeps the music player.
            // The player's bare texts use the body style as it is (no app-wide letter spacing).
            androidx.compose.material3.ProvideTextStyle(com.music.spotui.ui.theme.Typography.bodyLarge) {
                if (com.music.spotui.di.SongPlayer.isEpisode) {
                    com.music.spotui.ui.screens.PodcastPlayerScreen(navController)
                } else {
                    PlayerScreen(navController)
                }
            }
        }
    }
}
