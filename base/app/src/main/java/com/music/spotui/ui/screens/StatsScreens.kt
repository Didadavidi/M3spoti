package com.music.spotui.ui.screens

import com.music.spotui.ui.theme.coral
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.myspoti.api.FriendActivity
import app.myspoti.api.TimeRange
import app.myspoti.api.TopContent
import app.myspoti.api.best
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.api.TopStats
import com.music.spotui.data.local.ListenHistory
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.spotifyImageUrl
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.profileRoute
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val StatsSubtle: Color get() = AppMuted

internal val rangeLabels = listOf(
    TimeRange.SHORT_TERM to R.string.stats_range_short,
    TimeRange.MEDIUM_TERM to R.string.stats_range_medium,
    TimeRange.LONG_TERM to R.string.stats_range_long,
)

@Composable
private fun StatsChip(label: String, selected: Boolean, onClick: () -> Unit) =
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppAccent, selectedLabelColor = com.music.spotui.ui.theme.AppOnAccent,
            labelColor = AppText, containerColor = tone(0xFF2A2A2A)))

/** The tab switch at the top of the recap: Spotify's ranking of the account, or this phone's own counter. */
@Composable
internal fun StatsTabs(tab: Int, onTab: (Int) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.stats_tab_spotify, R.string.stats_tab_phone).forEachIndexed { i, label ->
            StatsChip(stringResource(label), i == tab) { onTab(i) }
        }
    }
}

/**
 * The account's listening stats as Spotify ranks them (the official app's "Listening stats" and
 * the profile's top artists and tracks): top artists and tracks over the past 4 weeks, 6 months
 * or year, from `userTopContent`. Spotify sends ranks only, no play counts or minutes.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun SpotifyStatsTab(navController: NavController) {
    var range by rememberSaveable { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    val timeRange = rangeLabels[range].first
    val state by produceState<Result<TopContent>?>(TopStats.cached(timeRange)?.let { Result.success(it) }, timeRange, attempt) {
        value = runCatching { TopStats.get(timeRange, force = attempt > 0) }
    }
    val top = state?.getOrNull()
    LazyColumn(contentPadding = PaddingValues(bottom = com.music.spotui.ui.components.bottomBarsSpace())) {
        item {
            Row(Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rangeLabels.forEachIndexed { i, (_, label) -> StatsChip(stringResource(label), i == range) { range = i } }
            }
        }
        when {
            state == null -> item { com.music.spotui.ui.components.SkeletonList(count = 6) }
            top == null -> item {
                com.music.spotui.ui.components.ErrorRetry(stringResource(R.string.stats_failed)) { attempt++ }
            }
            top.artists.isEmpty() && top.tracks.isEmpty() -> item {
                Text(stringResource(R.string.stats_empty), color = StatsSubtle, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
            }
            else -> {
                top.artistTotal?.takeIf { it > 0 }?.let { total ->
                    item {
                        Text(stringResource(R.string.stats_artists_ranked, total), color = StatsSubtle, fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 0.dp))
                    }
                }
                if (top.artists.isNotEmpty()) {
                    item { StatsSectionTitle(stringResource(R.string.recap_top_artists)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            itemsIndexed(top.artists, key = { _, a -> a.uri }) { i, a ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(120.dp).clip(RoundedCornerShape(8.dp))
                                        .clickable { navController.navigate(artistRoute(a.name, a.uri.substringAfterLast(':'))) },
                                ) {
                                    com.music.spotui.ui.components.ProfileAvatar(
                                        url = spotifyImageUrl(a.images.best(300)?.url), name = a.name, size = 120.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("${i + 1}. ${a.name}", color = AppText, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                if (top.tracks.isNotEmpty()) {
                    item { StatsSectionTitle(stringResource(R.string.recap_top_tracks)) }
                    itemsIndexed(top.tracks, key = { _, t -> t.uri }) { i, t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { SongPlayer.playSong(t.uri) }.padding(16.dp, 6.dp),
                        ) {
                            Text("${i + 1}", color = StatsSubtle, fontSize = 16.sp, modifier = Modifier.width(32.dp))
                            GlideImage(transition = CrossFade, model = spotifyImageUrl(t.album?.images?.best(160)?.url), contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)))
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(t.name, color = AppText, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artists.joinToString(", ") { it.name }, color = StatsSubtle, fontSize = 12.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.stats_spotify_note), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(16.dp, 24.dp))
        }
    }
}

@Composable
private fun StatsSectionTitle(text: String) =
    Text(text, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 18.dp, 16.dp, 10.dp))

/** Minutes played per calendar week on this phone (the recap's own counter), the current week last and highlighted. */
@Composable
internal fun WeeklyMinutesChart(modifier: Modifier = Modifier) {
    val weeks by produceState<List<ListenHistory.Week>?>(null) {
        value = withContext(Dispatchers.IO) { ListenHistory.weekly(8) }
    }
    val w = weeks ?: return
    if (w.all { it.minutes == 0L }) return
    val max = w.maxOf { it.minutes }.coerceAtLeast(1)
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "dMMM")
    val format = remember(pattern) { java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()) }
    Column(modifier.padding(16.dp, 8.dp)) {
        Text(stringResource(R.string.stats_weekly_minutes), color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.stats_this_week, w.last().minutes), color = StatsSubtle, fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            w.forEachIndexed { i, week ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (week.minutes > 0) Text("${week.minutes}", color = StatsSubtle, fontSize = 10.sp, maxLines = 1)
                    Box(
                        Modifier.fillMaxWidth()
                            .height((96f * week.minutes / max).coerceAtLeast(2f).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (i == w.lastIndex) AppAccent else tone(0xFF535353)),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            w.forEach { week ->
                Text(format.format(java.util.Date(week.startMs)), color = StatsSubtle, fontSize = 10.sp, maxLines = 1,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * "Your #3 artist of the past 4 weeks" under an artist's name, when the account's Spotify top
 * artists hold them (the closest the Web Player's data comes to the official app's fan badges:
 * Spotify sends no top-listener rank or "early listener" date for an artist).
 */
@Composable
fun ArtistTopBadge(artistId: String, modifier: Modifier = Modifier) {
    if (artistId.isBlank()) return
    val uri = if (artistId.startsWith("spotify:")) artistId else "spotify:artist:$artistId"
    val rank by produceState<Pair<TimeRange, Int>?>(null, uri) {
        value = runCatching {
            TopStats.rankOf(uri, TimeRange.entries.associateWith { r -> TopStats.get(r).artists.map { it.uri } })
        }.getOrNull()
    }
    val (range, n) = rank ?: return
    val text = when (range) {
        TimeRange.SHORT_TERM -> R.string.artist_top_short
        TimeRange.MEDIUM_TERM -> R.string.artist_top_medium
        TimeRange.LONG_TERM -> R.string.artist_top_long
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(16.dp, 10.dp, 16.dp, 0.dp)
            .clip(RoundedCornerShape(50)).background(AppText.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AppAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(text, n), color = AppText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Friend activity, as the official feed shows it: each followed person sharing their listening,
 * their latest track and where it was played from, "Now" for the past 15 minutes. Tapping the
 * person opens their profile, tapping the row plays the track in its context.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FriendActivityScreen(navController: NavController) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<Result<List<FriendActivity>>?>(null) }
    LaunchedEffect(attempt) {
        state = runCatching { MyspotiRuntime.api.users.friendActivity() }
    }
    val now = remember(state) { System.currentTimeMillis() }
    val sc = com.music.spotui.ui.theme.SystemColors.scheme
    Column(Modifier.fillMaxSize().background(sc.surface).statusBarsPadding()) {
        // Opened from Home's header (no longer a tab): back to it; refresh at the other end.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(sc.surfaceContainerHigh).clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center) {
                Icon(com.music.spotui.ui.dots.DesignIcons.Back, stringResource(R.string.accessibility_navigate_back), tint = sc.onSurface, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(48.dp).clip(CircleShape).background(sc.surfaceContainerHigh).clickable { state = null; attempt++ },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Refresh, stringResource(R.string.friends_refresh), tint = sc.onSurface, modifier = Modifier.size(22.dp))
            }
        }
        // Shrinks to fit: "Freundesaktivität" was cut to "Freundesakti…".
        val titleStyle = androidx.compose.material3.MaterialTheme.typography.displayMedium
        com.music.spotui.ui.components.FitText(
            stringResource(R.string.friends_title), color = sc.onSurface, maxLines = 1,
            fontSize = titleStyle.fontSize, minSize = 26.sp, fontWeight = titleStyle.fontWeight, fontFamily = titleStyle.fontFamily,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp))
        val s = state
        val friends = s?.getOrNull()
        val nowList = friends.orEmpty().filter { it.isNowPlaying(now) }
        val earlier = friends.orEmpty().filterNot { it.isNowPlaying(now) }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = com.music.spotui.ui.components.bottomBarsSpace()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                s == null -> item { com.music.spotui.ui.components.SkeletonList(count = 6, round = true) }
                friends == null -> item {
                    com.music.spotui.ui.components.ErrorRetry(stringResource(R.string.friends_failed)) { state = null; attempt++ }
                }
                friends.isEmpty() -> item {
                    Text(stringResource(R.string.friends_empty), color = sc.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
                }
                else -> {
                    if (nowList.isNotEmpty()) {
                        item(key = "h-now") { FriendsHeader(stringResource(R.string.friends_listening_now), live = true) }
                        items(nowList, key = { it.user.uri }) { f -> FriendRow(f, now, navController) }
                    }
                    if (earlier.isNotEmpty()) {
                        item(key = "h-earlier") { FriendsHeader(stringResource(R.string.whats_new_section_earlier), live = false) }
                        items(earlier, key = { it.user.uri }) { f -> FriendRow(f, now, navController) }
                    }
                }
            }
        }
    }
}

/** "Listening now" (a coral dot before it) or "Earlier", in the design's section style. */
@Composable
private fun FriendsHeader(text: String, live: Boolean) {
    val sc = com.music.spotui.ui.theme.SystemColors.scheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp)) {
        if (live) { Box(Modifier.size(10.dp).clip(CircleShape).background(sc.coral)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = sc.primary, fontFamily = com.music.spotui.ui.theme.FlexDisplay, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * A friend as a card: their avatar with the track's cover in halftone on its corner, their name,
 * "Track · Artist", where or when, and a round play button. Tapping the avatar or the name opens
 * the profile; the card or the button plays the track in its context.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun FriendRow(f: FriendActivity, now: Long, navController: NavController) {
    val sc = com.music.spotui.ui.theme.SystemColors.scheme
    val track = f.track
    val openProfile = { navController.navigate(profileRoute(f.user.uri.substringAfterLast(':'), f.user.name)) }
    val play = { if (track != null) SongPlayer.playSong(track.uri, contextUri = f.contextUri) else openProfile() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(sc.surfaceContainer)
            .clickable { play() }.padding(10.dp),
    ) {
        Box(Modifier.size(56.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = openProfile)) {
                com.music.spotui.ui.components.ProfileAvatar(url = f.user.imageUrl, name = f.user.name, size = 48.dp)
            }
            f.trackImageUrl?.let { cover ->
                Box(Modifier.align(Alignment.BottomEnd).size(26.dp).clip(RoundedCornerShape(8.dp)).background(sc.surfaceContainer).padding(2.dp)) {
                    com.music.spotui.ui.dots.HalftoneCover(spotifyImageUrl(cover.replaceFirst("http://", "https://")), Modifier.fillMaxSize(),
                        RoundedCornerShape(6.dp), cells = 10f)
                }
            }
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(f.user.name, color = sc.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = openProfile))
            if (track != null) Text(listOf(track.name, track.artists.joinToString(", ") { it.name }).filter { it.isNotBlank() }.joinToString(" · "),
                color = sc.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val whenText = when {
                f.isNowPlaying(now) -> stringResource(R.string.friends_listening_now)
                f.timestampMs > 0 -> android.text.format.DateUtils.getRelativeTimeSpanString(f.timestampMs, now,
                    android.text.format.DateUtils.MINUTE_IN_MILLIS, android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString()
                else -> null
            }
            val where = f.contextName ?: track?.album?.name
            val line = listOfNotNull(where?.takeIf { it.isNotBlank() }, whenText).joinToString(" · ")
            if (line.isNotEmpty()) Text(line, color = sc.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track != null) Box(
            Modifier.padding(start = 8.dp).size(44.dp).clip(CircleShape).background(sc.surface).clickable { play() },
            contentAlignment = Alignment.Center,
        ) { Icon(com.music.spotui.ui.dots.DesignIcons.Play, stringResource(R.string.content_description_play_button), tint = sc.onSurface, modifier = Modifier.size(20.dp)) }
    }
}
