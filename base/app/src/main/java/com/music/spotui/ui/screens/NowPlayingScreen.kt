package com.music.spotui.ui.screens

import com.music.spotui.ui.navigation.pullDownToClosePlayer
import androidx.compose.ui.graphics.drawscope.scale
import com.music.spotui.ui.components.rememberSqueezeShape
import com.music.spotui.ui.components.pressScale
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.di.SongPlayer
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OndemandVideo
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.components.squeezeButton
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotField
import com.music.spotui.ui.dots.DotFieldBackground
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.dots.HalftoneCover
import com.music.spotui.ui.dots.MorphPlayButton
import com.music.spotui.ui.dots.WavyProgress
import com.music.spotui.ui.navigation.PlayerSheet
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.theme.ExpressiveColor
import com.music.spotui.ui.theme.FlexCondensed
import com.music.spotui.ui.theme.FlexWide
import com.music.spotui.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

/**
 * Now Playing in the design: everything in the album colours (a deep page in the cover's hue),
 * the design's dot field behind a tilted capsule cover printed in halftone, a big morphing play
 * button overlapping it, the title huge and wide, the elapsed time tall and narrow, the wavy
 * seek bar, big previous/next pills, and chips for the sleep timer, lyrics and the queue (plus
 * devices and the music video). The Canvas clips are gone: the dots are the background.
 */
@Composable
fun PlayerScreen(navController: NavController) {
    val vm: PlayerViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val album = ExpressiveColor.scheme
    val title = vm.currentSongTitle.value
    val singer = vm.currentSongSinger.value
    val cover = vm.currentSongCoverUri.value
    val playing = vm.currentSongPlayingState.value
    val buffering = SongPlayer.isBuffering
    val adPlaying = SongPlayer.adPlaying
    val isLiked = vm.likeState
    val shuffle = vm.shuffleState.value
    val repeatMode = SongPlayer.repeatMode.value
    val queueSongs = vm.queue.value
    val np by SongPlayer.nowPlaying
    val mv = com.music.spotui.video.MusicVideoController
    val remote = SongPlayer.activeDevice.value

    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    if (showDevices) com.music.spotui.ui.components.DevicesSheet { showDevices = false }
    if (showSleep) com.music.spotui.ui.components.SleepTimerSheet { showSleep = false }
    if (showMenu) PlayerOptionsSheet(
        navController = navController,
        playerViewModel = vm,
        context = context,
        onDismiss = { showMenu = false },
        onOpenQueue = { com.music.spotui.ui.navigation.PlayerSheet.openQueue(navController) },
    )
    BackHandler(enabled = showLyrics) { showLyrics = false }

    DarkSystemBars()

    // A Live Radio station: the design's radio player (station colours, its mark, Live, stations).
    com.music.spotui.radio.RadioPlayer.current()?.let { station ->
        RadioNowPlaying(station, playing, onSleep = { showSleep = true })
        return
    }

    // The music video: looked up for the song shown, offered as a chip once ready, full screen.
    DisposableEffect(mv) {
        mv.attachPlayerScreen()
        SongPlayer.refreshLikedIfStale()
        onDispose { mv.detachPlayerScreen() }
    }
    val trackId = com.music.spotui.ui.viewmodel.confirmedTrackIdFromUri(np.uri)
    LaunchedEffect(trackId, title, buffering) {
        if (!buffering && com.music.spotui.data.preferences.MusicVideoPref.on) mv.prepare(trackId, title, singer, SongPlayer.getDuration())
    }
    val readyVideo = mv.state.value as? com.music.spotui.video.MusicVideoController.State.Ready
    val videoReady = trackId.isNotBlank() && readyVideo?.trackId == trackId && remote == null && !buffering &&
        !com.music.spotui.data.DataSaver.active && com.music.spotui.data.preferences.MusicVideoPref.on
    // Leaving the full-screen clip ends it: the design's player has no inline video.
    LaunchedEffect(mv.fullscreen.value) { if (!mv.fullscreen.value && mv.on.value) mv.setOn(false) }

    val frame = com.music.spotui.ui.components.rememberFrameClock(playing && !buffering, fps = 8)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val fraction = {
        frame.value
        if (dragging) dragValue else {
            val d = mv.duration()
            if (d > 0) (mv.position().toFloat() / d).coerceIn(0f, 1f) else 0f
        }
    }
    // Pulled down, the player follows the finger as a card and closes past a threshold or on a flick.
    val pullEnabled = !showLyrics && !mv.fullscreen.value
    val reveal = com.music.spotui.ui.navigation.LocalPlayerReveal.current

    MaterialTheme(colorScheme = album, typography = MaterialTheme.typography) {
    Box(
        Modifier
            .fillMaxSize()
            .background(album.surface)
            .pullDownToClosePlayer(pullEnabled)
    ) {
        Box(Modifier.fillMaxWidth().height(520.dp)) {
            DotFieldBackground(DotField.PLAYER, inkA = album.primary, inkB = album.tertiaryContainer, step = 9f)
        }
        PlayerBloom(album.primary, dotsEnd = 440.dp)
        // The white status bar icons over pale dots were hard to read.
        com.music.spotui.ui.theme.StatusBarScrim(album.surface)
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = reveal() }
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top: close, the list it plays from, the menu.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            ) {
                RoundIcon(DesignIcons.Down, stringResource(R.string.player_close), album.surfaceContainerLowest, album.onSurface) { PlayerSheet.close() }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val name = contextDisplayName(np, vm.currentSongAlbum.value)
                    if (name.isNotBlank()) Text(
                        name,
                        color = album.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .widthIn(max = 220.dp)
                            .squeezeButton(album.surfaceContainerLowest, 20.dp, 11.dp) { contextRoute(np)?.let { navController.navigate(it); PlayerSheet.close() } }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                RoundIcon(DesignIcons.More, stringResource(R.string.content_description_context_menu), album.surfaceContainerLowest, album.onSurface) { showMenu = true }
            }

            // The cover: a tilted capsule in halftone, ringed in the page colour; swipe it to skip.
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(370.dp)
                    .pointerInput(queueSongs) {
                        var dx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dx = 0f },
                            onDragEnd = {
                                val t = 80.dp.toPx()
                                if (dx < -t) vm.playNextSongs(queueSongs, context)
                                else if (dx > t) vm.playPreviousSong(queueSongs, context)
                            },
                        ) { change, amount -> change.consume(); dx += amount }
                    },
            ) {
                val w = 236.dp
                val h = 318.dp
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = (-24).dp, y = (-6).dp)
                        .size(w, h)
                        .rotate(-14f)
                        .border(7.dp, album.surface, DotShapes.Pill)
                        .padding(3.dp),
                ) {
                    HalftoneCover(cover, Modifier.fillMaxSize(), DotShapes.Pill, cells = 26f, animate = true, placeholder = album.surfaceContainerHigh)
                }
                MorphPlayButton(
                    playing = playing,
                    onClick = {
                        if (playing) SongPlayer.pause() else SongPlayer.play()
                        vm.updateSongState(cover, title, singer, !playing, vm.currentSongId.value, vm.currentSongIndex.value, vm.currentSongAlbum.value)
                    },
                    container = album.primary,
                    content = album.onPrimary,
                    width = 116.dp,
                    playingRadius = 58.dp,
                    pausedRadius = 36.dp,
                    iconSize = 42.dp,
                    label = stringResource(if (playing) R.string.notif_action_pause else R.string.notif_action_play),
                    // The ring that cuts the button out of the cover, in the button's own shape.
                    ring = 7.dp,
                    ringColor = album.surface,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 34.dp, bottom = 14.dp),
                )
            }

            // Title, artist and the list.
            Column(Modifier.padding(horizontal = 24.dp)) {
                com.music.spotui.ui.components.FitText(
                    if (adPlaying) stringResource(R.string.ad_playing) else title,
                    fontSize = 40.sp, minSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 2,
                    color = album.onSurface, fontFamily = FlexWide,
                )
                if (!adPlaying) Text(
                    listOf(singer, vm.currentSongAlbum.value).filter { it.isNotBlank() }.joinToString(" · "),
                    color = album.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp).clickable {
                        openNowPlayingAlbum(vm, singer, scope) { navController.navigate(it); PlayerSheet.close() }
                    },
                )
            }

            // The elapsed time, tall and narrow, then the like button.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                    Text(
                        clockOf(run { frame.value; if (dragging) (dragValue * mv.duration()).toLong() else mv.position() }),
                        color = album.primary, fontFamily = FlexCondensed, fontWeight = FontWeight.Medium,
                        fontSize = 64.sp, lineHeight = 60.sp,
                    )
                    val total = mv.duration()
                    if (total > 0) Text(
                        stringResource(R.string.player_of_total, clockOf(total)),
                        color = album.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                    )
                }
                if (!adPlaying && SongPlayer.canLike) {
                    val liked = isLiked.value
                    val radius by androidx.compose.animation.core.animateDpAsState(if (liked) 20.dp else 28.dp, com.music.spotui.ui.dots.bouncy(), label = "like")
                    Box(
                        Modifier.size(56.dp).pressable(0.9f) { vm.updateLikeState(!liked) }
                            .clip(RoundedCornerShape(radius))
                            .background(if (liked) album.tertiaryContainer else album.surfaceContainerHigh)
                            .semantics { contentDescription = context.getString(if (liked) R.string.player_unlike else R.string.player_like) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (liked) DesignIcons.Heart else DesignIcons.HeartOutline, null,
                            tint = if (liked) album.onTertiaryContainer else album.onSurface, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // The wavy seek bar: drag or tap to move in the song.
            Box(
                Modifier
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(30.dp)
                    .semantics { contentDescription = context.getString(R.string.playback_seek_description) }
                    .pointerInput(Unit) {
                        detectTapGestures { o -> mv.seekTo((o.x / size.width * mv.duration()).toLong()) }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { o -> dragging = true; dragValue = (o.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = { mv.seekTo((dragValue * mv.duration()).toLong()); dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, _ -> change.consume(); dragValue = (change.position.x / size.width).coerceIn(0f, 1f) }
                    },
            ) {
                WavyProgress(
                    progress = fraction,
                    color = album.primary,
                    track = album.surfaceContainerHigh,
                    modifier = Modifier.fillMaxSize(),
                    amplitude = 6.dp,
                    wavelength = 14.dp,
                    stroke = 6.dp,
                    thumb = album.onSurface,
                )
            }

            // Shuffle, previous, next, repeat: a connected group. The button under the finger grows
            // and pushes its neighbours narrower, then they all spring back.
            val groupSources = List(4) { remember { MutableInteractionSource() } }
            val groupWidths = transportGroupWidths(groupSources, listOf(0.6f, 1f, 1f, 0.6f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                val smartOn = SmartShuffle.on.value != null && SmartShuffle.on.value == np.contextUri
                ToggleIcon(DesignIcons.Shuffle, stringResource(R.string.play_mode_item_shuffle_title), shuffle || smartOn, album.primary, album.onSurface,
                    modifier = Modifier.weight(groupWidths[0]), source = groupSources[0]) {
                    // As in Spotify: off → shuffle → Smart Shuffle (playlists, Liked Songs) → off.
                    val ctx = np.contextUri
                    when {
                        smartOn -> { SmartShuffle.on.value = null; vm.updateShuffleState(false) }
                        shuffle && SmartShuffle.canPlay(ctx) -> {
                            SmartShuffle.on.value = ctx
                            android.widget.Toast.makeText(context, context.getString(R.string.smart_shuffle_on), android.widget.Toast.LENGTH_SHORT).show()
                            scope.launch {
                                if (!SmartShuffle.startFor(ctx, SongPlayer.currentTrackId)) {
                                    SmartShuffle.on.value = null
                                    android.widget.Toast.makeText(context, context.getString(R.string.page_load_failed), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else -> vm.updateShuffleState(!shuffle)
                    }
                }
                BigPill(DesignIcons.Previous, stringResource(R.string.np_content_desc_prev), album.surfaceContainerHigh, album.onSurface,
                    Modifier.weight(groupWidths[1]), source = groupSources[1], pressedScale = 1f) {
                    vm.playPreviousSong(queueSongs, context)
                }
                BigPill(DesignIcons.Next, stringResource(R.string.np_content_desc_next), album.surfaceContainerHigh, album.onSurface,
                    Modifier.weight(groupWidths[2]), source = groupSources[2], pressedScale = 1f) {
                    vm.playNextSongs(queueSongs, context)
                }
                ToggleIcon(DesignIcons.Repeat, stringResource(R.string.np_content_desc_repeat), repeatMode != 0, album.primary, album.onSurface,
                    badge = when (repeatMode) { 2 -> "1"; 3 -> "1×"; else -> null },
                    modifier = Modifier.weight(groupWidths[3]), source = groupSources[3]) { SongPlayer.cycleRepeat() }
            }

            Spacer(Modifier.weight(1f))

            // The chips: sleep, lyrics, queue, then the devices and the clip when there is one.
            // In one centred row: when the labels do not all fit, the devices, the clip and then
            // the sleep timer show as round icons (a remote device keeps its name: it says where music plays).
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                val sleepLabel = stringResource(R.string.player_sleep)
                val lyricsLabel = stringResource(R.string.lyrics_card_title)
                val queueLabel = stringResource(R.string.immersive_queue_title)
                val devicesLabel = remote?.name ?: stringResource(R.string.player_devices)
                val videoLabel = stringResource(R.string.player_video)
                // Indices: 0 sleep, 1 lyrics, 2 queue, 3 devices, 4 clip (absent ones get an empty label and no width).
                val labels = listOf(sleepLabel, if (!adPlaying) lyricsLabel else "", queueLabel, devicesLabel, if (videoReady) videoLabel else "")
                val present = labels.map { it.isNotEmpty() }
                val drop = buildList { if (remote == null) add(3); add(4); add(0); add(1) }
                val shown = chipLabels(maxWidth, labels.filterIndexed { i, _ -> present[i] }, drop.mapNotNull { d ->
                    if (!present[d]) null else (0 until d).count { present[it] }
                })
                // Back from the compact list to the five slots.
                var k = 0
                val showLabel = present.map { if (it) shown[k++] else false }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChipGap, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    PlayerChip(DesignIcons.Sleep, sleepLabel, album, showLabel = showLabel[0]) { showSleep = true }
                    if (!adPlaying) PlayerChip(DesignIcons.Lyrics, lyricsLabel, album, showLabel = showLabel[1]) { showLyrics = true }
                    PlayerChip(DesignIcons.Queue, queueLabel, album, showLabel = showLabel[2]) { com.music.spotui.ui.navigation.PlayerSheet.openQueue(navController) }
                    PlayerChip(
                        null, devicesLabel, album,
                        painter = R.drawable.ic_devices, highlighted = remote != null, showLabel = showLabel[3],
                    ) { showDevices = true }
                    if (videoReady) PlayerChip(Icons.Default.OndemandVideo, videoLabel, album, showLabel = showLabel[4]) {
                        mv.setOn(true); mv.fullscreen.value = true
                    }
                }
            }
        }

        LyricsOverlay(
            visible = showLyrics,
            title = title,
            artist = singer,
            album = vm.currentSongAlbum.value,
            accentColor = album.primary,
            onClose = { showLyrics = false },
        )
        if (mv.on.value && mv.fullscreen.value) VideoFullscreen()
    }
    }
}

/** m:ss (h:mm:ss past an hour). */
private fun clockOf(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    return if (t >= 3600) "%d:%02d:%02d".format(t / 3600, t / 60 % 60, t % 60) else "%d:%02d".format(t / 60, t % 60)
}

/**
 * The design's radio player: the station's colours (from its logo), a dot field, "● LIVE RADIO",
 * the station's mark set huge and narrow on a deep card, its name wide, a Live card with what the
 * directory says of it, previous / stop / next through the favourites (else the popular picks),
 * and Favourite, Sleep, Share. Pulled down, it closes like the song player.
 */
@Composable
private fun RadioNowPlaying(station: com.music.spotui.radio.RadioStation, playing: Boolean, onSleep: () -> Unit) {
    val album = ExpressiveColor.scheme
    val context = LocalContext.current
    val reveal = com.music.spotui.ui.navigation.LocalPlayerReveal.current
    val fav = com.music.spotui.radio.RadioFavoritesStore.favorites.containsKey(station.stationUuid)
    val favorites = com.music.spotui.radio.RadioFavoritesStore.list()
    val ring = if (favorites.any { it.stationUuid == station.stationUuid }) favorites else com.music.spotui.radio.RadioBrowserApi.curatedPopular
    val at = ring.indexOfFirst { it.stationUuid == station.stationUuid }
    fun step(by: Int) {
        if (ring.isEmpty()) return
        val next = if (at < 0) 0 else Math.floorMod(at + by, ring.size)
        com.music.spotui.radio.RadioPlayer.play(ring[next])
    }
    MaterialTheme(colorScheme = album, typography = MaterialTheme.typography) {
        Box(
            Modifier.fillMaxSize().background(album.surface)
                .pullDownToClosePlayer(true),
        ) {
            Box(Modifier.fillMaxWidth().height(460.dp)) {
                DotFieldBackground(DotField.PLAYER, inkA = album.primary, inkB = album.tertiaryContainer, step = 9f)
            }
            PlayerBloom(album.primary, dotsEnd = 400.dp)
            com.music.spotui.ui.theme.StatusBarScrim(album.surface)
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = reveal() }.statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    RoundIcon(DesignIcons.Down, stringResource(R.string.player_close), album.surfaceContainerLowest, album.onSurface) {
                        com.music.spotui.ui.navigation.PlayerSheet.close()
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(album.surfaceContainerLowest).padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(album.error))
                            Text(stringResource(R.string.live_radio_live_radio).uppercase(), color = album.onSurface, fontSize = 13.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    RoundIcon(DesignIcons.Share, stringResource(R.string.action_share_label), album.surfaceContainerLowest, album.onSurface) {
                        com.music.spotui.radio.RadioPlayer.share(context)
                    }
                }
                // The station's mark, huge and narrow, on a deep card.
                Box(
                    Modifier.padding(start = 24.dp, top = 28.dp).clip(RoundedCornerShape(32.dp)).background(album.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    Text(com.music.spotui.radio.ui.stationMark(station.name), color = album.primary, fontFamily = FlexCondensed,
                        fontWeight = FontWeight.Medium, fontSize = 120.sp, lineHeight = 116.sp, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Column(Modifier.padding(horizontal = 24.dp)) {
                    com.music.spotui.ui.components.FitText(
                        station.name, fontSize = 40.sp, minSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 2,
                        color = album.onSurface, fontFamily = FlexWide,
                    )
                    val sub = listOfNotNull(station.tags.firstOrNull()?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() },
                        station.country.takeIf { it.isNotBlank() }).joinToString(" · ")
                    if (sub.isNotBlank()) Text(sub, color = album.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                }
                // Live: what the directory says of the stream (no programme guide exists for stations).
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(album.surfaceContainerHigh).padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(stringResource(R.string.live_radio_on_air).uppercase(), color = album.primary, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    val stream = listOfNotNull(station.codec.takeIf { it.isNotBlank() }, station.bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" },
                        station.language.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }).joinToString(" · ")
                    Text(station.name, color = album.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp))
                    if (stream.isNotBlank()) Text(stream, color = album.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                // Previous / stop / next: through the favourites, else the popular picks.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                ) {
                    BigPill(DesignIcons.Previous, stringResource(R.string.live_radio_previous_station), album.surfaceContainerHigh, album.onSurface, Modifier.width(96.dp)) { step(-1) }
                    MorphPlayButton(
                        playing = playing,
                        onClick = { if (playing) SongPlayer.pause() else SongPlayer.play() },
                        container = album.primary, content = album.onPrimary,
                        width = 104.dp, playingRadius = 52.dp, pausedRadius = 32.dp, iconSize = 38.dp,
                        label = stringResource(if (playing) R.string.notif_action_pause else R.string.notif_action_play),
                    )
                    BigPill(DesignIcons.Next, stringResource(R.string.live_radio_next_station), album.surfaceContainerHigh, album.onSurface, Modifier.width(96.dp)) { step(1) }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChipGap, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    PlayerChip(if (fav) DesignIcons.Heart else DesignIcons.HeartOutline,
                        stringResource(R.string.live_radio_favorite_chip), album, highlighted = fav) {
                        com.music.spotui.radio.RadioFavoritesStore.toggle(station)
                    }
                    PlayerChip(DesignIcons.Sleep, stringResource(R.string.player_sleep), album, onClick = onSleep)
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).pressable(0.9f, onClick = onClick).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(22.dp))
    }
}

/**
 * The widths of a connected button group, as row weights from [base]: the button whose [sources]
 * is pressed grows by a third, its direct neighbours give way, on a bouncy spring. Reduced
 * motion keeps them still.
 */
@Composable
internal fun transportGroupWidths(sources: List<MutableInteractionSource>, base: List<Float>): List<Float> {
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    val held = sources.map { it.collectIsPressedAsState().value }.indexOf(true).takeUnless { reduce } ?: -1
    return base.mapIndexed { i, w ->
        val target = w * when {
            held == i -> 1.32f
            held >= 0 && kotlin.math.abs(held - i) == 1 -> 0.8f
            else -> 1f
        }
        androidx.compose.animation.core.animateFloatAsState(
            target,
            androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            label = "groupWidth$i",
        ).value
    }
}

/**
 * The bloom under the dots: a soft oval glow of the cover's [color], centred on the bottom edge and
 * reaching up to where the dot field ends ([dotsEnd]), so the page does not go flat under the
 * dots. It follows the album colours as they change, so it fades with each new song.
 */
@Composable
private fun PlayerBloom(color: Color, dotsEnd: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val span = size.height - dotsEnd.toPx()
        if (span <= 0f) return@Canvas
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height)
        val rx = size.width * 0.85f
        val ry = span * 1.05f
        val peak = 0.42f
        // A smooth fall-off (smoothstep), strongest at the bottom centre.
        val brush = androidx.compose.ui.graphics.Brush.radialGradient(
            0f to color.copy(alpha = peak),
            0.25f to color.copy(alpha = peak * 0.84f),
            0.5f to color.copy(alpha = peak * 0.5f),
            0.75f to color.copy(alpha = peak * 0.16f),
            1f to Color.Transparent,
            center = center, radius = rx,
        )
        // The circle stretched into an oval as tall as the space under the dots.
        scale(scaleX = 1f, scaleY = ry / rx, pivot = center) {
            drawRect(brush, topLeft = androidx.compose.ui.geometry.Offset(0f, center.y - rx), size = androidx.compose.ui.geometry.Size(size.width, rx))
        }
    }
}

/** Shuffle / repeat: an icon that turns the primary colour (with a dot under it) when on. */
@Composable
private fun ToggleIcon(
    icon: ImageVector, label: String, on: Boolean, onColor: Color, offColor: Color, badge: String? = null,
    modifier: Modifier = Modifier.size(48.dp),
    source: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
) {
    Box(
        modifier.height(48.dp).pressScale(source, 0.9f).clip(CircleShape)
            .clickable(source, indication = null, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (on) onColor else offColor, modifier = Modifier.size(24.dp))
        if (badge != null) Text(badge, color = onColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp))
        if (on) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).clip(CircleShape).background(onColor))
    }
}

/** Previous / next: the design's big rounded pills; held, their corners squeeze. */
@Composable
private fun BigPill(
    icon: ImageVector, label: String, bg: Color, fg: Color, modifier: Modifier,
    source: MutableInteractionSource = remember { MutableInteractionSource() },
    /** In a connected group the pill widens instead (1 = no shrink). */
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
) {
    val shape = rememberSqueezeShape(source, 24.dp, 14.dp)
    Box(
        modifier.height(72.dp).pressScale(source, pressedScale).clip(shape).background(bg)
            .clickable(source, indication = null, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun PlayerChip(
    icon: ImageVector?,
    label: String,
    scheme: androidx.compose.material3.ColorScheme,
    painter: Int = 0,
    highlighted: Boolean = false,
    /** Off: a round button with the icon alone (the label is still read out). */
    showLabel: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (highlighted) scheme.primary else scheme.surfaceContainerHigh
    val fg = if (highlighted) scheme.onPrimary else scheme.onSurface
    val iconLabel = if (showLabel) null else label
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.height(46.dp).then(if (showLabel) Modifier else Modifier.width(46.dp))
            .squeezeButton(bg, 23.dp, 13.dp, onClick = onClick)
            .padding(horizontal = if (showLabel) ChipPadding else 0.dp),
    ) {
        when {
            icon != null -> Icon(icon, iconLabel, tint = fg, modifier = Modifier.size(20.dp))
            painter != 0 -> Icon(androidx.compose.ui.res.painterResource(painter), iconLabel, tint = fg, modifier = Modifier.size(20.dp))
        }
        if (showLabel) Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = ChipLabelMax))
    }
}

private val ChipPadding = 14.dp
private val ChipLabelMax = 160.dp
private val ChipGap = 6.dp

/**
 * Which of the player's chips keep their label in [width]: all when they fit, else the ones in
 * [dropOrder] become round icon buttons one by one until the row fits (the German labels were
 * too long for one row: "Warteschlange" ran off the screen).
 */
@Composable
private fun chipLabels(width: androidx.compose.ui.unit.Dp, labels: List<String>, dropOrder: List<Int>): List<Boolean> {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val style = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    return remember(width, labels, style) {
        val full = labels.map { l ->
            val text = with(density) { measurer.measure(l, style, maxLines = 1).size.width.toDp() }
            ChipPadding * 2 + 20.dp + 8.dp + minOf(text, ChipLabelMax)
        }
        val shown = MutableList(labels.size) { true }
        fun total() = labels.indices.fold(0.dp) { sum, i -> sum + if (shown[i]) full[i] else 46.dp } + ChipGap * (labels.size - 1)
        for (i in dropOrder) { if (total() <= width) break; if (i in labels.indices) shown[i] = false }
        shown
    }
}

/** The album-colour pages are dark whatever the theme: light status and navigation bar icons while one shows. */
@Composable
internal fun DarkSystemBars() = com.music.spotui.ui.theme.DarkSystemBars()
