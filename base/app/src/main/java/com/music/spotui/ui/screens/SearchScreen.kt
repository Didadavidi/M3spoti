package com.music.spotui.ui.screens

import com.music.spotui.ui.components.secondaryClick
import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted

import kotlinx.coroutines.launch
import com.music.spotui.ui.components.SearchClearButton
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.components.launchVoiceSearch
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.music.spotui.ui.components.SongOptionsSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import app.myspoti.engine.R
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.categoryRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.navigation.showRoute
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppPalette
import com.music.spotui.ui.viewmodel.SearchViewModel


@Composable
fun SearchScreen(navController: NavController) {
    val searchViewModel : SearchViewModel = viewModel()
    val results by searchViewModel.results.collectAsState()

    // Results are live search hits (or empty); never gate the search UI on them.
    val searchResults = (results as? Response.Success)?.data ?: SearchResults()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
    ) {
        SumUpSearchScreen(navController = navController, searchResults, searchViewModel,
            loading = results is Response.Loading, failed = results is Response.Error)
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun SumUpSearchScreen(
    navController: NavController,
    results: SearchResults,
    searchViewModel: SearchViewModel,
    loading: Boolean = false,
    failed: Boolean = false,
) {
    val context = LocalContext.current

    val text = searchViewModel.query
    // Recents are the *items opened from results* (songs/artists/albums), not the
    // typed queries, and only appear once the user taps into the search bar.
    var searchFocused by remember { mutableStateOf(false) }
    var recents by remember {
        mutableStateOf(com.music.spotui.data.preferences.getRecentItems(context).filter { com.music.spotui.data.api.Features.PODCASTS || it.type != "show" })
    }
    var menuSong by remember { mutableStateOf<SongsModel?>(null) }
    // Re-tapping the Search tab while already on it focuses the field (official behaviour)
    // and shows the recents again; no query is re-run. The field lives in the sticky bar,
    // so a focus requester is handed down to it.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val searchFieldFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        com.music.spotui.ui.navigation.TabReselect.search.collect {
            // Focus first: the keyboard should be up as soon as the tab is tapped.
            // Not attached yet (screen being torn down): the tap is simply dropped.
            runCatching { searchFieldFocus.requestFocus() }
            if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
                listState.animateScrollToItem(0)
            }
        }
    }
    // Spotify's filter chips: Top (mixed), or one kind of result.
    val filter = searchViewModel.filter
    menuSong?.let { sel ->
        SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            onDismiss = { menuSong = null },
        )
    }

    // A tap on a result puts the keyboard away; the query and the list stay as they are.
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val dismissKeyboard = { keyboard?.hide(); focusManager.clearFocus() }
    val recordRecent: (com.music.spotui.data.preferences.RecentItem) -> Unit = { item ->
        dismissKeyboard()
        com.music.spotui.data.preferences.addRecentItem(context, item)
        recents = com.music.spotui.data.preferences.getRecentItems(context).filter { com.music.spotui.data.api.Features.PODCASTS || it.type != "show" }
    }
    // One relevance-mixed list (songs, artists, albums interleaved) instead of
    // separate type sections — matches how most music apps present search.
    val mixed = remember(results, filter) { filterSearchResults(results, filter) }
    // Profiles are searched only when their chip is on (Spotify keeps them out of Top).
    val profiles by androidx.compose.runtime.produceState(emptyList<app.myspoti.api.MediaCard>(), text, filter) {
        value = emptyList()
        if (filter != SearchFilter.Profiles || text.isBlank()) return@produceState
        kotlinx.coroutines.delay(250)
        value = runCatching { app.myspoti.host.android.MyspotiRuntime.api.search.users(text.trim(), limit = 30).items }.getOrDefault(emptyList())
    }
    val suggestions by androidx.compose.runtime.produceState(emptyList<String>(), text) {
        value = emptyList()
        if (text.isBlank()) return@produceState
        kotlinx.coroutines.delay(300)
        value = runCatching { app.myspoti.host.android.MyspotiRuntime.api.search.suggestions(text.trim(), 6) }.getOrDefault(emptyList())
            .filter { !it.equals(text.trim(), ignoreCase = true) }
    }

    LazyColumn(
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.music.spotui.ui.components.bottomBarsSpace()),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
            .statusBarsPadding()

    ){
        item{
            SearchTopBar()
        }
        stickyHeader {
            SearchStickyBar(
                text,
                onFocusChange = { searchFocused = it },
                focusRequester = searchFieldFocus,
            ) {
                searchViewModel.search(it)
            }
        }

        if (text.isBlank()) {
            if (searchFocused && recents.isNotEmpty()) {
                // ── Recent searches: the items the user opened (Spotify-style),
                // shown only once the search bar is focused ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 16.dp, 16.dp, 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.assisted_curation_search_search_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppText,
                        )
                        Text(
                            stringResource(R.string.chat_content_picker_clear_recent_searches),
                            color = AppMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.pressable(0.96f) {
                                com.music.spotui.data.preferences.clearRecentItems(context)
                                recents = emptyList()
                                // Spotify's own list too, so other devices forget them as well.
                                clearSpotifyRecents()
                            },
                        )
                    }
                }
                items(recents.size) { i ->
                    val item = recents[i]
                    RecentItemRow(
                        item = item,
                        onClick = {
                            when (item.type) {
                                "song" -> {
                                    val songUrl = item.songUrl.ifBlank {
                                        SongPlayer.buildSpotifyPlayQuery(item.spotifyTrackId, item.name, item.singer)
                                    }.let { savedUrl ->
                                        if (
                                            item.spotifyTrackId.isNotBlank() &&
                                            !savedUrl.startsWith("spotify:track:")
                                        ) {
                                            SongPlayer.buildSpotifyPlayQuery(item.spotifyTrackId, item.name, item.singer)
                                        } else {
                                            savedUrl
                                        }
                                    }
                                    val song = SongsModel(
                                        item.songId, item.name, item.songAlbum, item.singer,
                                        item.image, songUrl, item.spotifyTrackId,
                                        explicit = item.explicit,
                                        durationMs = item.durationMs,
                                    )
                                    searchViewModel.startRadioFromSong(song)
                                    SongPlayer.playSong(song.url, context, songs = listOf(song), alone = true)
                                    searchViewModel.updateSongState(
                                        song.coverUri, song.title, song.singer, true, song.id, 0, song.album)
                                }
                                "artist" -> navController.navigate(artistRoute(item.name, item.key.takeIf { it != item.name }.orEmpty()))
                                "album" -> navController.navigate(albumRoute(item.name, item.singer, item.key.takeIf { it != item.name }.orEmpty()))
                                "show" -> navController.navigate(showRoute(item.key, item.name))
                                "playlist" -> navController.navigate(playlistRoute(item.key, item.name))
                            }
                        },
                        onRemove = {
                            com.music.spotui.data.preferences.removeRecentItem(context, item)
                            recents = com.music.spotui.data.preferences.getRecentItems(context).filter { com.music.spotui.data.api.Features.PODCASTS || it.type != "show" }
                        },
                    )
                }
            } else {
                // ── Spotify-style "Browse all" category grid ──
                item {
                    // Real Spotify opens a genre *catalogue* (a page of curated
                    // playlists) rather than running a keyword song search.
                    BrowseAllSection { genre, title ->
                        if (genre == NEW_RELEASES_GENRE) navController.navigate(com.music.spotui.ui.navigation.Routes.NewReleases.route)
                        else navController.navigate(categoryRoute(genre, title))
                    }
                }
            }
        } else {
            // Spotify's completions for what is typed: one tap searches it.
            if (suggestions.isNotEmpty()) item {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    items(suggestions.size) { i ->
                        val q = suggestions[i]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(tone(0xFF2A2A2A))
                                .pressable(0.96f) { searchViewModel.search(q) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Search, null, tint = AppMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(q, color = AppText, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item { SearchFilterChips(filter) { searchViewModel.filter = it } }
            // Loading, nothing found and a failure each say what they are (never an empty page).
            if (filter != SearchFilter.Profiles && mixed.isEmpty() && results.shows.isEmpty() && results.episodes.isEmpty()) item {
                SearchStatus(text, loading, failed) { searchViewModel.retry() }
            }
            if (filter == SearchFilter.Profiles) {
                items(profiles.size) { i ->
                    val u = profiles[i]
                    SearchProfileRow(u.title, u.images.firstOrNull()?.url) {
                        dismissKeyboard()
                        navController.navigate(com.music.spotui.ui.navigation.profileRoute(u.uri.substringAfterLast(':'), u.title))
                    }
                }
            }
            items(mixed.size) { i ->
                when (val row = mixed[i]) {
                    is SearchRow.Song -> com.music.spotui.ui.components.SwipeToQueue(row.song) {
                        SearchSongRow(row.song, searchViewModel, onPlayed = {
                            recordRecent(row.song.toRecentItem())
                        }, onLongClick = { menuSong = row.song })
                    }
                    is SearchRow.Artist -> SearchArtistRow(row.artist) {
                        recordRecent(com.music.spotui.data.preferences.RecentItem(
                            type = "artist",
                            key = row.artist.id.ifBlank { row.artist.name },
                            name = row.artist.name,
                            image = row.artist.coverUri,
                        ))
                        navController.navigate(artistRoute(row.artist.name, row.artist.id))
                    }
                    is SearchRow.Album -> SearchAlbumRow(row.album) {
                        recordRecent(com.music.spotui.data.preferences.RecentItem(
                            type = "album",
                            key = row.album.spotifyId.ifBlank { row.album.name },
                            name = row.album.name,
                            singer = row.album.artists,
                            image = row.album.coverUri,
                        ))
                        navController.navigate(albumRoute(row.album.name, row.album.artists, row.album.spotifyId))
                    }
                    is SearchRow.Playlist -> SearchPlaylistRow(row.playlist) {
                        recordRecent(com.music.spotui.data.preferences.RecentItem(
                            type = "playlist",
                            key = row.playlist.spotifyId,
                            name = row.playlist.name,
                            singer = row.playlist.subtitle,
                            image = row.playlist.coverUri,
                        ))
                        navController.navigate(playlistRoute(row.playlist.spotifyId, row.playlist.name))
                    }
                }
            }
            // ── Podcasts: shows (→ detail) then individual episodes (→ play) ──
            if (com.music.spotui.data.api.Features.PODCASTS && (filter == SearchFilter.Top || filter == SearchFilter.Podcasts)) {
            if (results.shows.isNotEmpty()) {
                item { SearchSectionHeader(stringResource(R.string.assisted_curation_search_filter_podcasts)) }
                items(results.shows.size) { i ->
                    val show = results.shows[i]
                    SearchShowRow(show) {
                        recordRecent(com.music.spotui.data.preferences.RecentItem(
                            type = "show",
                            key = show.id,
                            name = show.name,
                            singer = show.publisher,
                            image = show.coverUri,
                        ))
                        navController.navigate(showRoute(show.id, show.name))
                    }
                }
            }
            if (results.episodes.isNotEmpty()) {
                item { SearchSectionHeader(stringResource(R.string.assisted_curation_search_filter_episodes)) }
                items(results.episodes.size) { i ->
                    val ep = results.episodes[i]
                    SearchSongRow(ep, searchViewModel, onPlayed = {
                        recordRecent(ep.toRecentItem())
                    }, onLongClick = { menuSong = ep })
                }
            }
            // What extensions add for this search (search.provide), under Spotify's results.
            if (filter != SearchFilter.Profiles) item {
                com.music.spotui.extensions.ExtensionBlocks(
                    app.myspoti.extensions.Hook.SEARCH, app.myspoti.extensions.Surface.SEARCH,
                    kotlinx.serialization.json.buildJsonObject { put("query", kotlinx.serialization.json.JsonPrimitive(text.trim())) }.toString(),
                    delayMs = 400,
                )
            }
            }
        }

    }
}

/** A single row in the search results: a track, an artist, or an album. */
sealed class SearchRow {
    data class Song(val song: SongsModel) : SearchRow()
    data class Artist(val artist: com.music.spotui.data.entity.ArtistsModel) : SearchRow()
    data class Album(val album: com.music.spotui.data.entity.AlbumsModel) : SearchRow()
    data class Playlist(val playlist: com.music.spotui.data.entity.LibraryEntry) : SearchRow()
}

/** The search filter chips (official labels). Podcasts shows shows + episodes. */
@Composable
private fun SearchStatus(query: String, loading: Boolean, failed: Boolean, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp, 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            loading -> com.music.spotui.ui.components.SkeletonList(count = 7, modifier = Modifier.padding(top = 0.dp))
            failed -> com.music.spotui.ui.components.ErrorRetry(stringResource(R.string.search_failed), Modifier.padding(0.dp), onRetry)
            else -> Text(stringResource(R.string.assisted_curation_search_empty_view_no_result_title, query.trim()),
                color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

enum class SearchFilter(@androidx.annotation.StringRes val label: Int) {
    Top(R.string.assisted_curation_search_filter_top),
    Songs(R.string.assisted_curation_search_filter_songs),
    Artists(R.string.assisted_curation_search_filter_artists),
    Albums(R.string.assisted_curation_search_filter_albums),
    Playlists(R.string.assisted_curation_search_filter_playlists),
    Podcasts(R.string.assisted_curation_search_filter_podcasts),
    Profiles(R.string.search_filter_profiles),
}

private fun filterSearchResults(results: SearchResults, filter: SearchFilter): List<SearchRow> = when (filter) {
    SearchFilter.Top -> mixSearchResults(results)
    SearchFilter.Songs -> results.songs.map { SearchRow.Song(it) }
    SearchFilter.Artists -> results.artists.map { SearchRow.Artist(it) }
    SearchFilter.Albums -> results.albums.map { SearchRow.Album(it) }
    SearchFilter.Playlists -> results.playlists.map { SearchRow.Playlist(it) }
    SearchFilter.Podcasts, SearchFilter.Profiles -> emptyList()
}

@Composable
private fun SearchFilterChips(selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        val filters = SearchFilter.entries.filter { it != SearchFilter.Podcasts || com.music.spotui.data.api.Features.PODCASTS }
        items(filters.size) { i ->
            val f = filters[i]
            val on = f == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(42.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (on) com.music.spotui.ui.theme.SystemColors.scheme.primary else com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh)
                    .pressable(0.96f) { onSelect(if (on && f != SearchFilter.Top) SearchFilter.Top else f) }
                    .padding(horizontal = 18.dp),
            ) {
                Text(stringResource(f.label), color = if (on) com.music.spotui.ui.theme.SystemColors.scheme.onPrimary else AppText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/**
 * Interleaves the three result types into one list, weighted toward songs
 * (2 songs per artist+album cycle) so the list reads as mixed rather than
 * grouped, while songs — the most common search intent — stay prominent.
 */
private fun mixSearchResults(results: SearchResults): List<SearchRow> {
    val songs = results.songs.iterator()
    val artists = results.artists.iterator()
    val albums = results.albums.iterator()
    // A few playlists in the mix; the Playlists chip lists them all.
    val playlists = results.playlists.take(5).iterator()
    val out = ArrayList<SearchRow>()
    while (songs.hasNext() || artists.hasNext() || albums.hasNext() || playlists.hasNext()) {
        repeat(2) { if (songs.hasNext()) out += SearchRow.Song(songs.next()) }
        if (artists.hasNext()) out += SearchRow.Artist(artists.next())
        if (albums.hasNext()) out += SearchRow.Album(albums.next())
        if (playlists.hasNext()) out += SearchRow.Playlist(playlists.next())
    }
    return out
}

/** Maps a tapped search-result song to a persisted recent item. */
private fun SongsModel.toRecentItem() = com.music.spotui.data.preferences.RecentItem(
    type = "song",
    key = spotifyTrackId.ifBlank { url },
    name = title,
    singer = singer,
    image = coverUri,
    songId = id,
    songAlbum = album,
    songUrl = url,
    spotifyTrackId = spotifyTrackId,
    explicit = explicit,
    durationMs = durationMs,
)

/** A recent item row (song/artist/album/show the user opened), with remove (x). */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun RecentItemRow(
    item: com.music.spotui.data.preferences.RecentItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pressable(0.96f) { onClick() }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh).padding(10.dp),
    ) {
        com.music.spotui.ui.dots.HalftoneCover(item.image, Modifier.size(56.dp), if (item.type == "artist") androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(18.dp), cells = 14f)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 8.dp),
        ) {
            Text(text = item.name, color = AppText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            val subtitle = when (item.type) {
                "song" -> stringResource(R.string.assisted_curation_search_song) + " • ${item.singer}"
                "artist" -> stringResource(R.string.artist_subtitle)
                "album" -> stringResource(R.string.album) + " • ${item.singer}"
                "show" -> stringResource(R.string.album_type_podcast) + (if (item.singer.isNotBlank()) " • ${item.singer}" else "")
                "playlist" -> stringResource(R.string.assisted_curation_search_playlist) + (if (item.singer.isNotBlank()) " • ${item.singer}" else "")
                else -> ""
            }
            Text(text = subtitle, color = AppMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = Icons.Default.Close,
            tint = AppMuted,
            modifier = Modifier.pressable(0.96f) { onRemove() }
                .size(20.dp)
                ,
            contentDescription = stringResource(R.string.aligned_curation_first_save_default_row_remove_action_description),
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun SearchSongRow(
    song: SongsModel,
    searchViewModel: SearchViewModel,
    onPlayed: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isLiked = com.music.spotui.di.rememberTrackSaved(song.spotifyTrackId)
    val currentPlayingIndicatorColor =
        if (song.id == searchViewModel.currentSongId.value) Color(AppPalette.toArgb()) else AppText

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh).padding(10.dp)
            // A mouse's right click opens the same menu as the long press (DeX, tablets).
            .secondaryClick(onLongClick)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = {
                    onPlayed()
                    // Start a radio from the tapped track (queue = this song + Spotify
                    // recommendations) rather than queuing the whole search list.
                    searchViewModel.startRadioFromSong(song)
                    SongPlayer.playSong(song.url, context, songs = listOf(song), alone = true)
                    searchViewModel.updateSongState(
                        song.coverUri,
                        song.title,
                        song.singer,
                        true,
                        song.id,
                        0,
                        song.album,
                    )
                },
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
            com.music.spotui.ui.dots.HalftoneCover(song.coverUri, Modifier.padding(end = 12.dp).size(56.dp), RoundedCornerShape(18.dp), cells = 14f)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, color = currentPlayingIndicatorColor, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.explicit) { com.music.spotui.ui.screens.ExplicitBadge(); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)) }
                    Text(text = stringResource(R.string.assisted_curation_search_song) + " • ${song.singer}", color = AppMuted, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }

com.music.spotui.ui.screens.AddButton(song = { song }, liked = isLiked, size = 22.dp) {
                    // Spotify library, per track (not the now-playing like state).
                    com.music.spotui.di.SongPlayer.setTrackSaved(song.spotifyTrackId, true)
        }
    }
}

@Composable
fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        color = AppText,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp),
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchShowRow(show: com.music.spotui.data.entity.PodcastModel, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pressable(0.96f) { onClick() }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh).padding(10.dp),
    ) {
        com.music.spotui.ui.dots.HalftoneCover(show.coverUri, Modifier.padding(end = 12.dp).size(56.dp), RoundedCornerShape(18.dp), cells = 14f)
        Column {
            Text(text = show.name, color = AppText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.album_type_podcast) + (if (show.publisher.isNotBlank()) " • ${show.publisher}" else ""),
                color = AppMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchArtistRow(artist: com.music.spotui.data.entity.ArtistsModel, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pressable(0.96f) { onClick() }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh).padding(10.dp),
    ) {
        com.music.spotui.ui.dots.HalftoneCover(artist.coverUri, Modifier.padding(end = 12.dp).size(56.dp), androidx.compose.foundation.shape.CircleShape, cells = 14f)
        Column {
            Text(text = artist.name, color = AppText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(text = stringResource(R.string.artist_subtitle), color = AppMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchAlbumRow(album: com.music.spotui.data.entity.AlbumsModel, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pressable(0.96f) { onClick() }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh).padding(10.dp),
    ) {
        com.music.spotui.ui.dots.HalftoneCover(album.coverUri, Modifier.padding(end = 12.dp).size(56.dp), RoundedCornerShape(18.dp), cells = 14f)
        Column {
            Text(text = album.name, color = AppText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(text = stringResource(R.string.album) + " • ${album.artists}", color = AppMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchPlaylistRow(playlist: com.music.spotui.data.entity.LibraryEntry, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pressable(0.96f) { onClick() }.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh).padding(10.dp),
    ) {
        com.music.spotui.ui.dots.HalftoneCover(playlist.coverUri, Modifier.padding(end = 12.dp).size(56.dp), RoundedCornerShape(18.dp), cells = 14f)
        Column {
            Text(text = playlist.name, color = AppText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(text = stringResource(R.string.assisted_curation_search_playlist) +
                (if (playlist.subtitle.isNotBlank()) " • ${playlist.subtitle}" else ""),
                color = AppMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

/** Genre key of the "New releases" tile, opened in its own screen. */
const val NEW_RELEASES_GENRE = "new-releases"

/** One "Browse all" tile: Spotify's own category (title, colour, artwork). */
private data class BrowseTile(val key: String, val title: String, val color: Color, val image: String)

@Composable
fun BrowseAllSection(onCategoryClick: (genre: String, title: String) -> Unit) {
    val newReleases = stringResource(R.string.browse_new_releases)
    // Spotify's real categories (the Web Player's "Browse all"), New releases first.
    val tiles by androidx.compose.runtime.produceState(initialValue = emptyList<BrowseTile>()) {
        val categories = runCatching { app.myspoti.host.android.MyspotiRuntime.api.home.categories() }.getOrDefault(emptyList())
        value = listOf(BrowseTile(NEW_RELEASES_GENRE, newReleases, Color(0xFFE8115B), "")) +
            categories.filter { com.music.spotui.data.api.Features.PODCASTS || !it.title.contains("odcast", ignoreCase = true) }.map { c ->
                val color = runCatching { Color(android.graphics.Color.parseColor(c.color ?: "#333333")) }.getOrDefault(tone(0xFF333333))
                BrowseTile(c.uri, c.title, color, c.images.firstOrNull()?.url.orEmpty())
            }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.browse_all),
            style = MaterialTheme.typography.headlineSmall,
            color = AppText,
            modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 10.dp),
        )
        // Large windows: more tiles per row (two on phones).
        val cols = com.music.spotui.ui.components.windowSize().columns(2, 220.dp)
        tiles.chunked(cols).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { t ->
                    BrowseCategoryTile(t.title, t.color, t.image, Modifier.weight(1f)) { onCategoryClick(t.key, t.title) }
                }
                // Keep the tiles' width on a last row that is not full.
                repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BrowseCategoryTile(
    name: String,
    color: Color,
    cover: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // The category's own colour pulled toward the card colour (so the grid sits in the
    // wallpaper's palette), its artwork printed in halftone in a cookie, tilted, at the corner.
    val scheme = com.music.spotui.ui.theme.SystemColors.scheme
    val base = scheme.surfaceContainerHigh
    val tonal = remember(color, base) { androidx.compose.ui.graphics.lerp(color, base, if (com.music.spotui.ui.theme.SystemColors.dark) 0.55f else 0.62f) }
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(tonal)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
    ) {
        // A tile with no artwork of its own (New releases) gets the design's banded art from its name.
        com.music.spotui.ui.dots.HalftoneCover(
            cover,
            Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 14.dp).rotate(18f).size(76.dp),
            com.music.spotui.ui.dots.DotShapes.cookie(),
            cells = 14f,
            fallbackSeed = name,
        )
        Text(
            text = name,
            color = scheme.onSurface,
            // A word too long for the tile is hyphenated ("Neuerschei-nungen"), not cut anywhere.
            style = MaterialTheme.typography.titleMedium.copy(
                hyphens = androidx.compose.ui.text.style.Hyphens.Auto,
                lineBreak = androidx.compose.ui.text.style.LineBreak.Heading,
            ),
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp).padding(end = 40.dp),
        )
    }
}

@Composable
fun SearchTopBar() {
    Row(horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 14.dp, 20.dp, 6.dp)
    ) {
        Text(text = stringResource(R.string.nav_explore), style = MaterialTheme.typography.displayMedium, color = AppText)
        //Icon(imageVector = Icons.Default.Person, contentDescription = "", tint = Color.White)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStickyBar(
    text: String,
    onFocusChange: (Boolean) -> Unit = {},
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onTextChange: (String) -> Unit,
) {

    // The field is always attached to a requester, so the clear button can put the cursor back in it.
    val ownFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val fieldFocus = focusRequester ?: ownFocus
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // The design's pill field, then voice search (the Google app's "Search with your voice") beside it.
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .height(56.dp)
                .background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh)
                .padding(18.dp, 0.dp)
        ){
            Icon(
                com.music.spotui.ui.dots.DesignIcons.Search,
                tint = AppMuted,
                modifier = Modifier.size(22.dp),
                contentDescription = "")

            TextField(
                enabled = true,
                // The rest of the bar, so the clear button sits at its end.
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(fieldFocus)
                    .onFocusChanged { onFocusChange(it.isFocused) },
                value = text,
                textStyle = TextStyle.Default.copy(fontSize = 16.sp, color = AppText, fontWeight = FontWeight(500)),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = AppText,
                    unfocusedTextColor = AppText,
                    cursorColor = com.music.spotui.ui.theme.AppAccent,
                ),
                singleLine = true,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = AppMuted,
                        text = stringResource(R.string.search_header_field_hint)
                    )
                },
                trailingIcon = if (text.isEmpty()) null else {
                    { SearchClearButton(fieldFocus, AppMuted) { onTextChange("") } }
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh)
                .pressable(0.9f) { launchVoiceSearch(context) },
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = stringResource(R.string.voice_search),
                tint = AppText,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}


/** A user found by the Profiles chip: round picture, name, "Profile". */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SearchProfileRow(name: String, image: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().pressable(0.98f, onClick = onClick).padding(16.dp, 8.dp),
    ) {
        com.bumptech.glide.integration.compose.GlideImage(
            transition = CrossFade,
            model = image, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            failure = com.bumptech.glide.integration.compose.placeholder(R.drawable.placeholder),
            modifier = Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, color = AppText, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(stringResource(R.string.recently_played_user_profile), color = AppMuted, fontSize = 13.sp)
        }
    }
}

/** Spotify's own recent searches, cleared with the app's list. */
private fun clearSpotifyRecents() {
    val runtime = app.myspoti.host.android.MyspotiRuntime
    runtime.scope.launch { runCatching { runtime.api.search.clearRecent() } }
}
