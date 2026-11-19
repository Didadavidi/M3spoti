package com.music.spotui.ui.screens

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.music.spotui.ui.components.animatedRow
import com.music.spotui.ui.theme.AppText

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.components.PressScale
import com.music.spotui.ui.components.squeezeButton
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.dots.HalftoneCover
import com.music.spotui.ui.dots.MorphPlayButton
import com.music.spotui.ui.dots.WavyProgress
import com.music.spotui.ui.theme.ExpressiveColor
import com.music.spotui.ui.theme.FlexDisplay
import com.music.spotui.ui.theme.FlexWide
import com.music.spotui.ui.theme.AppPalette

/**
 * "Up next", in the album colours: a big title with Clear, the song playing as a bright card
 * (its cover in halftone, bars moving while it plays), then the list it plays from under a
 * Shuffle chip, each song numbered in a badge whose shape changes down the list. Swipe a song away
 * to take it out; tap to jump to it. A wavy progress line and the play button sit at the bottom.
 */
@Composable
fun QueueScreen(navController: NavController) {
    // Spotify's real queue, pushed by the engine.
    val q by SongPlayer.engineQueue
    val vm: com.music.spotui.ui.viewmodel.PlayerViewModel = viewModel()
    val s = ExpressiveColor.scheme
    val np by SongPlayer.nowPlaying
    val playing = vm.currentSongPlayingState.value
    val shuffle = vm.shuffleState.value
    val frame = com.music.spotui.ui.components.rememberFrameClock(playing, fps = 4)
    val mv = com.music.spotui.video.MusicVideoController
    DarkSystemBars()

    Column(Modifier.fillMaxSize().background(s.surface).statusBarsPadding().navigationBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Box(
                Modifier.size(48.dp).pressable(0.9f) { navController.navigateUp() }.clip(RoundedCornerShape(16.dp)).background(s.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) { Icon(DesignIcons.Down, stringResource(R.string.bottom_actions_close), tint = s.onSurface, modifier = Modifier.size(22.dp)) }
            Text(
                stringResource(R.string.immersive_player_queue), color = s.onSurface, fontFamily = FlexWide,
                fontWeight = FontWeight.Black, fontSize = 34.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (q.queued.isNotEmpty()) Text(
                stringResource(R.string.queue_clear),
                color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.squeezeButton(s.surfaceContainerHigh, 22.dp, 12.dp) { SongPlayer.clearQueue() }.padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            q.current?.let { song ->
                item(key = "now") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(s.primary).padding(12.dp),
                    ) {
                        Box(Modifier.size(76.dp).border(3.dp, s.onPrimary, CircleShape).padding(3.dp)) {
                            HalftoneCover(song.coverUri, Modifier.fillMaxSize(), CircleShape, cells = 14f, animate = true)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(
                                stringResource(R.string.home_now_playing).uppercase(), color = s.onPrimary, fontSize = 12.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, fontFamily = FlexDisplay,
                            )
                            Text(song.title, color = s.onPrimary, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.singer, color = s.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        PlayingBars(s.onPrimary, playing)
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
            val rows = q.queued + q.nextUp
            if (rows.isNotEmpty()) item(key = "from") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, bottom = 6.dp)) {
                    val from = contextDisplayName(np, vm.currentSongAlbum.value)
                    Text(
                        if (from.isNotBlank()) stringResource(R.string.queue_from, from) else stringResource(R.string.next_up),
                        color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .squeezeButton(if (shuffle) s.tertiaryContainer else s.surfaceContainerHigh, 14.dp, 8.dp) { vm.updateShuffleState(!shuffle) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        val fg = if (shuffle) s.onTertiaryContainer else s.onSurface
                        Icon(DesignIcons.Shuffle, null, tint = fg, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.immersive_queue_shuffle), color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            // Keyed by song and occurrence: a row removed or played lets the ones under it slide up
            // instead of every row after it being drawn anew.
            val keys = queueKeys(rows)
            itemsIndexed(rows, key = { i, _ -> keys[i] }) { i, item ->
                Box(animatedRow()) {
                    SwipeRemove(item) { QueueRow(item.song, i, queued = i < q.queued.size) { SongPlayer.skipTo(item) } }
                }
            }
        }

        // The wave of the song playing and its play button.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)) {
            WavyProgress(
                progress = { frame.value; val d = mv.duration(); if (d > 0) mv.position().toFloat() / d else 0f },
                color = s.primary,
                track = s.surfaceContainerHigh,
                modifier = Modifier.weight(1f).height(20.dp),
                amplitude = 4.dp,
                wavelength = 12.dp,
                stroke = 4.dp,
            )
            Spacer(Modifier.width(18.dp))
            MorphPlayButton(
                playing = playing,
                onClick = { if (playing) SongPlayer.pause() else SongPlayer.play() },
                container = s.primary,
                content = s.onPrimary,
                width = 76.dp,
                height = 64.dp,
                playingRadius = 32.dp,
                pausedRadius = 22.dp,
                iconSize = 28.dp,
                label = stringResource(if (playing) R.string.content_description_pause_button else R.string.content_description_play_button),
            )
        }
    }
}

/** Three bars rising and falling while the song plays (still when paused). */
@Composable
private fun PlayingBars(color: Color, playing: Boolean) {
    val t = rememberInfiniteTransition(label = "bars")
    val phases = listOf(0, 180, 360).map { delay ->
        t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(520, delayMillis = delay), RepeatMode.Reverse), label = "bar$delay")
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(30.dp)) {
        for (p in phases) {
            val h = if (playing) p.value else 0.5f
            Box(Modifier.width(6.dp).height(30.dp * h).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

/** One key per row: the song's uri, with its occurrence after the first ("uri~2"), never twice the same. */
private fun queueKeys(rows: List<SongPlayer.QueueItem>): List<String> {
    val seen = HashMap<String, Int>()
    return rows.map { item ->
        val count = seen.merge(item.uri, 1, Int::plus)!!
        if (count == 1) item.uri else "${item.uri}~$count"
    }
}

@Composable
private fun QueueHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = AppText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(16.dp, 18.dp, 16.dp, 6.dp)
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRemove(item: SongPlayer.QueueItem, content: @Composable () -> Unit) {
    val s = ExpressiveColor.scheme
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                SongPlayer.removeFromQueue(item)
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(s.errorContainer).padding(horizontal = 24.dp),
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.aligned_curation_first_save_default_row_remove_action_description), tint = s.onErrorContainer)
            }
        },
        content = { content() },
    )
}

/** A queued song: its number in a shaped badge, title, artist and length, the drag handle. */
@Composable
private fun QueueRow(song: com.music.spotui.data.entity.SongsModel, index: Int, queued: Boolean, onClick: () -> Unit) {
    val s = ExpressiveColor.scheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(PressScale.CARD) { onClick() }
            .clip(RoundedCornerShape(18.dp))
            .background(s.surfaceContainer)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        val first = index == 0
        Box(
            Modifier.size(44.dp).clip(DotShapes.badges[index % DotShapes.badges.size])
                .background(if (first || queued) s.tertiaryContainer else s.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (index + 1).toString(), color = if (first || queued) s.onTertiaryContainer else s.onSurface,
                fontSize = 17.sp, fontWeight = FontWeight.Black,
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(song.title, color = s.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val len = song.durationMs.toLong().takeIf { it > 0 }?.let { ms -> "%d:%02d".format(ms / 60000, ms / 1000 % 60) }
            Text(
                listOfNotNull(song.singer.takeIf { it.isNotBlank() }, len).joinToString(" · "),
                color = s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(DesignIcons.Reorder, null, tint = s.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}
