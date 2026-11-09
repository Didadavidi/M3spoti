/*
 * The player began as Apple Music TV's (https://github.com/JObersi10/apple-music-tv), GPL-3.0,
 * android/app/src/main/java/com/applemusicktv/ui/screens/NowPlayingScreen.kt at 7e5c594
 * (NowPlayingScreen, PlayerProgressBar). Modified for Myspoti, 2026-11; redrawn in the M3spoti
 * TV design, 2026-11.
 *
 * Changes: Myspoti's player state and actions (rememberImmersivePlayer); the album colours and a
 * dot field instead of the beat-driven orbs; the cover in halftone; the wavy seek bar; the queue,
 * lyrics, shuffle, repeat and Connect devices in a side panel; the artist opens its page.
 */
package com.music.spotui.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import app.myspoti.engine.R
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.ArtistChooser
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotField
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.dots.MorphPlayButton
import com.music.spotui.ui.dots.WavyProgress
import com.music.spotui.ui.immersive.lyrics.ImmersiveLyricsPanel
import com.music.spotui.ui.immersive.lyrics.ImmersiveQueuePanel
import com.music.spotui.ui.immersive.player.ImmersivePlayerActions
import com.music.spotui.ui.immersive.player.ImmersivePlayerState
import com.music.spotui.ui.immersive.player.ImmersiveRepeat
import com.music.spotui.ui.immersive.player.rememberImmersivePlayer
import com.music.spotui.ui.theme.ExpressiveColor
import com.music.spotui.ui.theme.FlexTitle
import com.music.spotui.ui.theme.coral
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The full-screen player in the playing cover's colours: the cover in halftone on the left; on
 * the right where it plays from, the title, the artist (it opens its page), the wavy seek bar the
 * remote scrubs by 10 s, then Like, previous, play/pause, next and the panel button, and what
 * comes next.
 */
@Composable
internal fun TvNowPlaying(onArtist: (id: String, name: String) -> Unit) {
    // The TV opens its own artist page: the controller only satisfies the bridge.
    val bridge = rememberImmersivePlayer(rememberNavController())
    val state = bridge.state
    var devices by remember { mutableStateOf(false) }
    if (devices) TvDevicesDialog { devices = false }
    val scope = rememberCoroutineScope()
    TvPlayerScreen(
        state, bridge.actions, ExpressiveColor.scheme,
        onArtist = {
            scope.launch {
                val a = ArtistChooser.artistsOf(state.trackId, state.artist).firstOrNull { it.uri.isNotBlank() } ?: return@launch
                onArtist(a.uri.substringAfterLast(':'), a.name)
            }
        },
        onDevices = { devices = true },
    )
}

private enum class TvPanel { Queue, Lyrics }

@Composable
internal fun TvPlayerScreen(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    al: ColorScheme,
    onArtist: () -> Unit,
    onDevices: () -> Unit,
) {
    var panel by remember { mutableStateOf<TvPanel?>(null) }
    BackHandler(enabled = panel != null) { panel = null }
    val playFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(al.surface)) {
        TvDots(al.primary, al.tertiaryContainer, field = DotField.PLAYER)
        if (state.title.isBlank()) {
            TvTitle(stringResource(R.string.immersive_shell_now_playing), Modifier.align(Alignment.Center), color = al.onSurfaceVariant)
            return@Box
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(52.dp)) {
            TvArt(state.coverUrl, state.title, Modifier.size(330.dp), DotShapes.Squircle, cells = 30f, animate = state.playing)
            Column(Modifier.weight(1f)) {
                if (state.contextName.isNotBlank()) TvText(stringResource(R.string.tv_playing_from, state.contextName).uppercase(),
                    al.onSurfaceVariant, 16.sp, FontWeight.ExtraBold, spacing = 1.2.sp)
                TvTitle(state.title, Modifier.padding(top = 6.dp), size = 48.sp, color = al.onSurface)
                // The artist opens its page (the first of the credit on a TV, no chooser).
                Box(Modifier.padding(top = 4.dp).tvFocus(RoundedCornerShape(10.dp), al.surface, al.coral, scale = 1.03f, onClick = onArtist)) {
                    TvText(state.artist, al.onSurfaceVariant, 20.sp, FontWeight.SemiBold, Modifier.padding(horizontal = 2.dp, vertical = 2.dp))
                }
                Spacer(Modifier.height(22.dp))
                TvSeek(state, actions, al)
                Spacer(Modifier.height(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (!state.isEpisode) TvRoundButton(
                        if (state.liked) DesignIcons.Heart else DesignIcons.HeartOutline,
                        stringResource(if (state.liked) R.string.immersive_player_unlike else R.string.immersive_player_like),
                        al.tertiaryContainer, al.onTertiaryContainer, shape = RoundedCornerShape(18.dp), gap = al.surface, onClick = actions.toggleLike,
                    )
                    TvRoundButton(DesignIcons.Previous, stringResource(R.string.immersive_player_previous), al.surfaceContainerHigh, al.onSurface,
                        size = 72.dp, shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                        gap = al.surface, iconSize = 30.dp, modifier = Modifier.width(92.dp), onClick = actions.previous)
                    var playFocused by remember { mutableStateOf(false) }
                    val lift by animateFloatAsState(if (playFocused) 1f else 0f, tween(160), label = "playLift")
                    Box(Modifier.focusRequester(playFocus).onFocusChanged { playFocused = it.hasFocus }
                        .graphicsLayer { val s = 1f + 0.06f * lift; scaleX = s; scaleY = s; translationY = -4.dp.toPx() * lift }) {
                        MorphPlayButton(
                            state.playing, actions.playPause, al.primary, al.onPrimary, width = 112.dp,
                            label = stringResource(if (state.playing) R.string.immersive_player_pause else R.string.immersive_player_play),
                            ring = if (playFocused) 4.dp else 0.dp, ringColor = al.coral,
                        )
                    }
                    TvRoundButton(DesignIcons.Next, stringResource(R.string.immersive_player_next), al.surfaceContainerHigh, al.onSurface,
                        size = 72.dp, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                        gap = al.surface, iconSize = 30.dp, modifier = Modifier.width(92.dp), onClick = actions.next)
                    TvRoundButton(DesignIcons.Queue, stringResource(R.string.immersive_player_queue), al.surfaceContainer, al.onSurface,
                        gap = al.surface) { panel = TvPanel.Queue }
                }
                val next = SongPlayer.engineQueue.value.let { (it.queued + it.nextUp).firstOrNull()?.song?.title }
                if (!next.isNullOrBlank()) Row(Modifier.padding(top = 26.dp)) {
                    TvText("${stringResource(R.string.tv_up_next)} · ", al.onSurfaceVariant, 16.sp, FontWeight.SemiBold)
                    TvText(next, al.onSurface, 16.sp, FontWeight.ExtraBold)
                }
            }
        }
        AnimatedVisibility(panel != null, Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
            TvSidePanel(panel ?: TvPanel.Queue, state, actions, al, onPanel = { panel = it }, onDevices = onDevices)
        }
    }
    LaunchedEffect(state.title.isNotBlank()) { if (state.title.isNotBlank()) runCatching { delay(120); playFocus.requestFocus() } }
}

/**
 * The wavy seek bar and its times. Focused, ← and → move a cursor by 10 s and OK seeks there; the
 * playhead is read at draw time, so only the bar redraws.
 */
@Composable
private fun TvSeek(state: ImmersivePlayerState, actions: ImmersivePlayerActions, al: ColorScheme) {
    var positionMs by remember { mutableLongStateOf(state.positionMs()) }
    LaunchedEffect(state.positionMs) { while (isActive) { positionMs = state.positionMs(); delay(250) } }
    val duration = state.durationMs.coerceAtLeast(1L)
    var focused by remember { mutableStateOf(false) }
    var scrubMs by remember(state.title) { mutableLongStateOf(positionMs) }
    val shown = if (focused) scrubMs else positionMs
    Column {
        Box(
            Modifier.fillMaxWidth().height(30.dp)
                .tvFocus(RoundedCornerShape(15.dp), al.surface, al.coral, scale = 1.02f, own = false)
                .onFocusChanged { if (it.isFocused && !focused) scrubMs = positionMs; focused = it.isFocused }
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (e.key) {
                        Key.DirectionLeft -> { scrubMs = (scrubMs - 10_000).coerceAtLeast(0); true }
                        Key.DirectionRight -> { scrubMs = (scrubMs + 10_000).coerceAtMost(duration); true }
                        else -> false
                    }
                }
                .focusable()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.seekTo(scrubMs); positionMs = scrubMs },
        ) {
            WavyProgress(
                progress = { (shown.toFloat() / duration).coerceIn(0f, 1f) },
                color = if (focused) al.coral else al.primary, track = al.surfaceContainerHigh,
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                amplitude = 4.dp, wavelength = 18.dp, stroke = 5.dp, thumb = al.onSurface, animate = state.playing && tvAnimate(),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.SpaceBetween) {
            TvText(formatMs(shown), if (focused) al.onSurface else al.onSurfaceVariant, 18.sp, FontWeight.Bold)
            TvText(formatMs(state.durationMs), al.onSurfaceVariant, 18.sp, FontWeight.Bold)
        }
    }
}

/** The panel at the right: what plays next or the lyrics, with shuffle, repeat and where it plays. */
@Composable
private fun TvSidePanel(
    panel: TvPanel,
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    al: ColorScheme,
    onPanel: (TvPanel) -> Unit,
    onDevices: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
    Column(Modifier.width(440.dp).fillMaxHeight().clip(shape).background(al.surfaceContainer).padding(start = 24.dp, end = 24.dp, top = 27.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvPanelChip(stringResource(R.string.immersive_player_queue), panel == TvPanel.Queue, al, Modifier.focusRequester(first)) { onPanel(TvPanel.Queue) }
            TvPanelChip(stringResource(R.string.immersive_player_lyrics), panel == TvPanel.Lyrics, al) { onPanel(TvPanel.Lyrics) }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvRoundButton(DesignIcons.Shuffle,
                stringResource(if (state.shuffle) R.string.immersive_player_shuffle_on else R.string.immersive_player_shuffle_off),
                if (state.shuffle) al.primary else al.surfaceContainerHigh, if (state.shuffle) al.onPrimary else al.onSurface,
                size = 48.dp, gap = al.surfaceContainer, onClick = actions.toggleShuffle)
            val repeatOn = state.repeatMode != ImmersiveRepeat.OFF
            TvRoundButton(
                if (state.repeatMode == ImmersiveRepeat.ONE || state.repeatMode == ImmersiveRepeat.ONCE) Icons.Default.RepeatOne else DesignIcons.Repeat,
                stringResource(when (state.repeatMode) {
                    ImmersiveRepeat.ALL -> R.string.immersive_player_repeat_all
                    ImmersiveRepeat.ONE -> R.string.immersive_player_repeat_one
                    ImmersiveRepeat.ONCE -> R.string.immersive_player_repeat_once
                    else -> R.string.immersive_player_repeat_off
                }),
                if (repeatOn) al.primary else al.surfaceContainerHigh, if (repeatOn) al.onPrimary else al.onSurface,
                size = 48.dp, gap = al.surfaceContainer, onClick = actions.cycleRepeat)
            TvRoundButton(Icons.Default.Devices, stringResource(R.string.listen_on),
                if (state.deviceName != null) al.primary else al.surfaceContainerHigh, if (state.deviceName != null) al.onPrimary else al.onSurface,
                size = 48.dp, gap = al.surfaceContainer, onClick = onDevices)
        }
        state.deviceName?.let { TvText(it, al.onSurfaceVariant, 16.sp, FontWeight.SemiBold, Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (panel == TvPanel.Queue) ImmersiveQueuePanel(Modifier.fillMaxSize(), PaddingValues(vertical = 8.dp))
            else ImmersiveLyricsPanel(Modifier.fillMaxSize(), PaddingValues(vertical = 8.dp))
        }
    }
    LaunchedEffect(Unit) { runCatching { delay(120); first.requestFocus() } }
}

@Composable
private fun TvPanelChip(label: String, selected: Boolean, al: ColorScheme, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape: Shape = RoundedCornerShape(if (selected) 16.dp else 24.dp)
    Box(
        modifier.height(48.dp).tvFocus(shape, al.surfaceContainer, al.coral, onClick = onClick).clip(shape)
            .background(if (selected) al.primary else al.surfaceContainerHigh).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { TvText(label, if (selected) al.onPrimary else al.onSurface, 18.sp, FontWeight.ExtraBold, family = FlexTitle) }
}
