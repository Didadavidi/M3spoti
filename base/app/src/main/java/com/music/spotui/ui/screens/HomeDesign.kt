package com.music.spotui.ui.screens

import androidx.compose.material.icons.outlined.Notifications
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.data.api.Api
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotField
import com.music.spotui.ui.dots.DotFieldBackground
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.dots.HalftoneCover
import com.music.spotui.ui.dots.MorphPlayButton
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.TabReselect
import com.music.spotui.ui.theme.FlexWide
import com.music.spotui.ui.theme.SystemColors
import com.music.spotui.ui.theme.card
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.deep
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.onDeep
import com.music.spotui.ui.theme.onPink
import com.music.spotui.ui.theme.onSoft
import com.music.spotui.ui.theme.pink
import com.music.spotui.ui.theme.soft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The id an item is known by (its shape, its dismissal). */
internal val HomeItem.key: String
    get() = when (this) {
        is HomeItem.Album -> "album:$id"
        is HomeItem.Artist -> "artist:$id"
        is HomeItem.Playlist -> "playlist:$id"
        is HomeItem.Show -> "show:$id"
    }.let { if (it.endsWith(":")) "$it$name" else it }

internal val HomeItem.subtitleText: String
    get() = when (this) {
        is HomeItem.Album -> subtitle
        is HomeItem.Playlist -> subtitle
        is HomeItem.Artist -> ""
        is HomeItem.Show -> subtitle
    }

/** The Spotify uri a home item plays as a context, or null (artists and shows open instead). */
private val HomeItem.contextUri: String?
    get() = when (this) {
        is HomeItem.Album -> id.takeIf { it.isNotBlank() }?.let { "spotify:album:$it" }
        is HomeItem.Playlist -> when {
            id == Api.HomeCache.LIKED_SONGS_ID -> null
            id.isNotBlank() -> "spotify:playlist:$id"
            else -> null
        }
        else -> null
    }

/**
 * Plays an album or playlist from its first song, as its own page's play button does: the page is
 * loaded (or taken from the cache) and Spotify plays it as the context, with the queue set.
 */
private suspend fun playHomeItem(item: HomeItem) {
    val api = Graph.api
    val flow = when (item) {
        is HomeItem.Album -> api.getAlbumPage(item.id)
        is HomeItem.Playlist -> api.getPlaylistPage(item.id)
        else -> return
    }
    val page = (withContext(Dispatchers.IO) { flow.first { it !is Response.Loading } } as? Response.Success)?.data ?: return
    val first = page.tracks.firstOrNull { it.spotifyTrackId.isNotBlank() } ?: return
    val state = Graph.currentSongState
    state.updateQueue(page.tracks)
    SongPlayer.registerContextName(page.uri, page.name)
    SongPlayer.playSong(first.url, contextUri = page.uri, songs = page.tracks)
    state.updateSongState(first.coverUri, first.title, first.singer, true, first.id, 0, first.album)
}

/** Keep-listening cards the user marked finished (by key), kept between launches. */
private object HomeDismissed {
    private const val PREF = "settings_prefs"
    private const val KEY = "home_dismissed"
    var keys by mutableStateOf(prefs(Graph.app).getStringSet(KEY, emptySet())!!.toSet())
        private set

    fun add(c: Context, key: String) {
        keys = (keys + key).let { if (it.size > 60) it.drop(it.size - 60).toSet() else it }
        prefs(c).edit().putStringSet(KEY, keys).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}

/**
 * The top bar: the profile on the left (opens the profile sheet), Search (round) and
 * Settings (rounded square) on the right.
 */
@OptIn(com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
@Composable
internal fun HomeTopBar(navController: NavController, onProfile: () -> Unit) {
    val s = SystemColors.scheme
    val avatarUrl = com.music.spotui.data.api.ProfileCache.imageUrl
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().height(68.dp).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
    ) {
        Box(
            Modifier.size(44.dp).pressable(0.92f, onClick = onProfile).clip(CircleShape).background(s.soft),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                com.bumptech.glide.integration.compose.GlideImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.home_profile),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(DesignIcons.Profile, stringResource(R.string.home_profile), tint = s.onSoft, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        // From Myspoti 2.4.0: What's New (releases of followed artists) and friend activity.
        Box(
            Modifier.size(48.dp).pressable(0.92f) { navController.navigate(Routes.WhatsNew.route) }.clip(CircleShape).background(s.card),
            contentAlignment = Alignment.Center,
        ) { Icon(androidx.compose.material.icons.Icons.Outlined.Notifications, stringResource(R.string.whats_new_title), tint = s.onSurface, modifier = Modifier.size(22.dp)) }
        Box(
            Modifier.size(48.dp).pressable(0.92f) { navController.navigate(Routes.Friends.route) }.clip(CircleShape).background(s.card),
            contentAlignment = Alignment.Center,
        ) { Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_friends_big), stringResource(R.string.friends_title), tint = s.onSurface, modifier = Modifier.size(22.dp)) }
        Box(
            Modifier.size(48.dp).pressable(0.92f) {
                navController.navigate(Routes.Search.route) { launchSingleTop = true }
                TabReselect.emit(Routes.Search.route)
            }.clip(CircleShape).background(s.card),
            contentAlignment = Alignment.Center,
        ) { Icon(DesignIcons.Search, stringResource(R.string.navigationbar_musicappitems_search_title), tint = s.onSurface, modifier = Modifier.size(22.dp)) }
        Box(
            Modifier.size(48.dp).pressable(0.92f) { navController.navigate(Routes.Settings.route) }.clip(RoundedCornerShape(16.dp)).background(s.card),
            contentAlignment = Alignment.Center,
        ) { Icon(DesignIcons.Tune, stringResource(R.string.settings_title), tint = s.onSurface, modifier = Modifier.size(22.dp)) }
    }
}

/** The mix the hero stands for: Spotify's first "Mix" playlist, else the first playlist shown. */
internal fun pickHeroMix(sections: List<HomeSection>): Pair<HomeItem, List<HomeItem>>? {
    for (s in sections) {
        val mix = s.items.firstOrNull { it is HomeItem.Playlist && it.name.contains("mix", ignoreCase = true) }
        if (mix != null) return mix to s.items.filter { it !== mix }
    }
    for (s in sections) {
        val p = s.items.firstOrNull { it is HomeItem.Playlist && it.contextUri != null } ?: continue
        return p to s.items.filter { it !== p }
    }
    return null
}

/**
 * The big hero: a deep card with the design's dot field, the mix's name set huge and wide, its
 * artists under it, a morphing play button, and three covers (the mix's own as a tilted capsule,
 * two others as circles) ringed in the card's colour.
 */
@Composable
internal fun HomeHero(navController: NavController, mix: HomeItem, others: List<HomeItem>) {
    val s = SystemColors.scheme
    val deep = s.deep()
    val onDeep = s.onDeep()
    val scope = rememberCoroutineScope()
    val np by SongPlayer.nowPlaying
    val playingThis = np.contextUri.isNotBlank() && np.contextUri == mix.contextUri
    val playing = playingThis && Graph.currentSongState.playingState.value
    val name = mix.name
    val titleSize = when {
        name.length <= 10 -> 68.sp
        name.length <= 16 -> 54.sp
        name.length <= 24 -> 44.sp
        else -> 36.sp
    }
    Box(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(420.dp)
            .pressable(0.98f) { onHomeItemClick(navController, mix) }
            .clip(RoundedCornerShape(44.dp))
            .background(deep),
    ) {
        DotFieldBackground(
            field = DotField.HOME_HERO,
            // Light: a mid purple between primary and the soft fill. Dark: deeper, so the
            // title (light on the field) keeps its contrast.
            inkA = if (SystemColors.dark) androidx.compose.ui.graphics.lerp(s.primaryContainer, s.primary, 0.3f)
                else androidx.compose.ui.graphics.lerp(s.primary, s.primaryContainer, 0.45f),
            inkB = s.coral,
            step = 8f,
        )
        // The covers, lower right, ringed in the card's colour so they sit "cut out" of it.
        val ring = Modifier.border(6.dp, deep, CircleShape)
        Box(
            Modifier.offset(x = 140.dp, y = 168.dp).size(150.dp, 230.dp).rotate(28f)
                .border(6.dp, deep, DotShapes.Pill).padding(3.dp)
        ) {
            HalftoneCover(mix.imageUrl, Modifier.fillMaxWidth().fillMaxHeight(), DotShapes.Pill, cells = 26f, animate = true)
        }
        others.getOrNull(0)?.let {
            Box(Modifier.offset(x = 30.dp, y = 214.dp).size(92.dp).then(ring).padding(3.dp)) {
                HalftoneCover(it.imageUrl, Modifier.size(86.dp), CircleShape, cells = 18f, animate = true)
            }
        }
        others.getOrNull(1)?.let {
            Box(Modifier.offset(x = 64.dp, y = 324.dp).size(64.dp).then(ring).padding(3.dp)) {
                HalftoneCover(it.imageUrl, Modifier.size(58.dp), CircleShape, cells = 14f)
            }
        }
        Column(Modifier.padding(start = 22.dp, top = 22.dp, end = 140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Smaller until the longest word fits on a line: a word never breaks in the middle
            // ("Mixta / pe"), it only moves to the next line.
            androidx.compose.foundation.layout.BoxWithConstraints {
                val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                val density = androidx.compose.ui.platform.LocalDensity.current
                val widest = remember(name) { name.split(' ').filter { it.isNotBlank() }.maxByOrNull { it.length }.orEmpty() }
                val maxPx = with(density) { maxWidth.roundToPx() }
                val size = remember(name, maxPx, titleSize) {
                    var s = titleSize.value
                    while (s > 26f && measurer.measure(
                            widest,
                            androidx.compose.ui.text.TextStyle(fontFamily = FlexWide, fontWeight = FontWeight.Black, fontSize = s.sp, letterSpacing = (-1.5).sp),
                            softWrap = false, maxLines = 1,
                        ).size.width > maxPx) s -= 2f
                    s.sp
                }
                Text(
                    text = name,
                    color = onDeep,
                    fontFamily = FlexWide,
                    fontWeight = FontWeight.Black,
                    fontSize = size,
                    lineHeight = size * 0.88f,
                    letterSpacing = (-1.5).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val sub = mix.subtitleText
            if (sub.isNotBlank()) Text(
                text = sub,
                color = onDeep.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(x = (-6).dp).clip(RoundedCornerShape(8.dp)).background(deep).padding(horizontal = 6.dp, vertical = 2.dp).width(190.dp),
            )
        }
        MorphPlayButton(
            playing = playing,
            onClick = {
                when {
                    playing -> SongPlayer.pause()
                    playingThis -> SongPlayer.play()
                    else -> scope.launch { playHomeItem(mix) }
                }
            },
            container = s.soft,
            content = s.onSoft,
            width = 104.dp,
            playingRadius = 52.dp,
            pausedRadius = 34.dp,
            iconSize = 38.dp,
            label = stringResource(if (playing) R.string.home_pause_mix else R.string.home_play_mix, name),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 20.dp),
        )
    }
}

/** A keep-listening card's content: what it is, how far along, what its buttons do. */
private class KeepCard(
    val key: String,
    val label: String,
    val title: String,
    val subtitle: String,
    val cover: String,
    val progress: Float,
    val pillText: String,
    val playing: Boolean,
    val onPill: () -> Unit,
    val coralIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val coralLabel: String,
    val onCoral: () -> Unit,
)

/**
 * "Keep listening": what was playing (with the songs left in it) and the lists Spotify puts first
 * on its home, as cards: a label, the title big, the artist, the cover, then a progress pill that
 * plays, a check that sets the card aside, and a coral button (the queue, or the page).
 */
@Composable
internal fun HomeKeepListening(navController: NavController, shortcuts: List<HomeItem>) {
    val s = SystemColors.scheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = Graph.currentSongState
    val np by SongPlayer.nowPlaying
    val queue = state.queue.value
    val index = state.songIndex.value
    val dismissed = HomeDismissed.keys
    val cards = ArrayList<KeepCard>()

    // What is (or was last) playing, with the songs left in its list.
    val ctxName = np.contextName.ifBlank { state.album.value }
    if (state.title.value.isNotBlank() && ctxName.isNotBlank() && "now:${np.contextUri}" !in dismissed) {
        val left = (queue.size - index - 1).coerceAtLeast(0)
        val playing = state.playingState.value
        cards += KeepCard(
            key = "now:${np.contextUri}",
            label = stringResource(if (playing) R.string.home_now_playing else R.string.home_last_played),
            title = ctxName,
            subtitle = state.singer.value,
            cover = state.coverUri.value,
            progress = if (queue.isNotEmpty()) ((index + 1f) / queue.size).coerceIn(0f, 1f) else 0f,
            pillText = if (queue.size > 1) pluralStringResource(R.plurals.home_tracks_left, left, left) else state.title.value,
            playing = playing,
            onPill = { if (playing) SongPlayer.pause() else SongPlayer.play() },
            coralIcon = DesignIcons.Queue,
            coralLabel = stringResource(R.string.home_show_queue),
            onCoral = { navController.navigate(Routes.Queue.route) },
        )
    }
    // Then the lists Spotify leads its home with (the shortcuts), not set aside, not the one playing.
    for (item in shortcuts) {
        if (cards.size >= 3) break
        if (item.key in dismissed || (item.contextUri != null && item.contextUri == np.contextUri)) continue
        cards += KeepCard(
            key = item.key,
            label = stringResource(R.string.home_jump_back_in),
            title = item.name,
            subtitle = item.subtitleText,
            cover = item.imageUrl,
            progress = 0f,
            pillText = stringResource(R.string.content_description_play_button),
            playing = false,
            onPill = { if (item.contextUri != null) scope.launch { playHomeItem(item) } else onHomeItemClick(navController, item) },
            coralIcon = DesignIcons.Chevrons,
            coralLabel = stringResource(R.string.home_open, item.name),
            onCoral = { onHomeItemClick(navController, item) },
        )
    }
    if (cards.isEmpty()) return

    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 6.dp).height(44.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(s.pink), contentAlignment = Alignment.Center) {
                Text(cards.size.toString(), color = s.onPink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Text(stringResource(R.string.home_keep_listening), style = MaterialTheme.typography.headlineSmall, color = s.primary)
            Icon(DesignIcons.Chevrons, null, tint = s.primary, modifier = Modifier.size(22.dp))
        }
        for (c in cards) KeepListeningCard(c, onDismiss = { HomeDismissed.add(context, c.key) })
    }
}

@Composable
private fun KeepListeningCard(c: KeepCard, onDismiss: () -> Unit) {
    val s = SystemColors.scheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(s.card).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(c.label, color = s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(c.title, style = MaterialTheme.typography.titleLarge, color = s.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (c.subtitle.isNotBlank()) Text(c.subtitle, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HalftoneCover(c.cover, Modifier.size(76.dp), RoundedCornerShape(24.dp), cells = 16f, fallbackSeed = c.title)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
            Box(
                Modifier.weight(1f).height(50.dp).pressable(0.97f, onClick = c.onPill).clip(RoundedCornerShape(25.dp)).background(s.surface),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(c.progress.coerceAtLeast(0.001f)).background(s.soft))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 18.dp)) {
                    Icon(if (c.playing) DesignIcons.Pause else DesignIcons.Play, null, tint = s.onSurface, modifier = Modifier.size(18.dp))
                    Text(c.pillText, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                Modifier.size(50.dp).pressable(0.92f, onClick = onDismiss).clip(CircleShape).background(s.surface),
                contentAlignment = Alignment.Center,
            ) { Icon(DesignIcons.Check, stringResource(R.string.home_mark_finished, c.title), tint = s.onSurface, modifier = Modifier.size(22.dp)) }
            Box(
                Modifier.size(50.dp).pressable(0.92f, onClick = c.onCoral).clip(RoundedCornerShape(16.dp)).background(s.coral),
                contentAlignment = Alignment.Center,
            ) { Icon(c.coralIcon, c.coralLabel, tint = s.onCoral, modifier = Modifier.size(22.dp)) }
        }
    }
}

/** A shelf: a wide black title, then covers printed in halftone, each in its own shape. */
@Composable
internal fun HomeShelf(navController: NavController, title: String, items: List<HomeItem>) {
    val s = SystemColors.scheme
    if (title.isNotBlank()) Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = s.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
    )
    val keys = remember(items) { items.mapIndexed { i, it -> "${it.key}#$i" } }
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items.size, key = { keys[it] }) { i -> HomeShelfCard(items[i]) { onHomeItemClick(navController, items[i]) } }
    }
}

@Composable
private fun HomeShelfCard(item: HomeItem, onClick: () -> Unit) {
    val s = SystemColors.scheme
    val artist = item is HomeItem.Artist
    Column(
        horizontalAlignment = if (artist) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.width(150.dp).pressable(0.96f, onPressHint = { prefetchHomeItem(item) }, onClick = onClick),
    ) {
        Box {
            HalftoneCover(item.imageUrl, Modifier.size(150.dp), DotShapes.coverShapeFor(item.key, artist), cells = 24f, fallbackSeed = item.name)
            if (item is HomeItem.Show && !item.isEpisode) {
                com.music.spotui.ui.components.NewEpisodeDot(item.id, s.primary, Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
        Text(
            item.name, color = s.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (artist) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.padding(top = 8.dp),
        )
        val sub = if (artist) stringResource(R.string.artist_subtitle) else item.subtitleText
        Text(
            sub, color = s.onSurfaceVariant, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = if (artist) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
        )
    }
}
