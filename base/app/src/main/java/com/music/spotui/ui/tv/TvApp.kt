/*
 * The shell, Home shelves and list page began as Apple Music TV's
 * (https://github.com/JObersi10/apple-music-tv), GPL-3.0, android/app/src/main/java/com/applemusicktv/ui/
 * AppShell.kt, screens/HomeScreen.kt and screens/PlaylistDetailScreen.kt at 7e5c594.
 * Modified for Myspoti, 2026-11; redrawn in the M3spoti TV design, 2026-11.
 *
 * Changes: Myspoti's own data (Home feed, Your Library, search, lists) and engine; one stack of
 * lists over the open tab instead of a navigation graph; no onboarding, video, radio, settings
 * or update checks (those stay on the phone).
 */
package com.music.spotui.ui.tv

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import app.myspoti.engine.R
import com.music.spotui.data.api.Api
import com.music.spotui.data.api.Features
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.ContextPageModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.entity.withoutDj
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.ArtistChooser
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.theme.FlexTitle
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.onPink
import com.music.spotui.ui.theme.pink
import com.music.spotui.ui.viewmodel.HomeViewModel
import com.music.spotui.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android TV: a version of the app made for the remote. A floating bar (Search, Home, Library,
 * and the player while something plays) over the page; a list opens over its tab and Back
 * closes it; a song played goes to the full-screen player. Settings, extensions and downloads
 * stay on the phone; playback, extensions and Connect are the same engine.
 */
object Tv {
    /** A TV (leanback) device, or a phone or tablet in TV mode. */
    fun isTv(context: Context): Boolean =
        (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || debugTv

    /**
     * Debug builds only: `adb shell setprop debug.m3spoti.tv 1` (then restart the app) shows the TV
     * version on a phone or an emulator, to check the TV screens without a TV image.
     */
    private val debugTv: Boolean by lazy {
        app.myspoti.engine.BuildConfig.DEBUG && runCatching {
            ProcessBuilder("getprop", "debug.m3spoti.tv").start().inputStream.bufferedReader().readText().trim() == "1"
        }.getOrDefault(false)
    }
}

@Composable
fun TvApp() {
    // Once, after the first sign-in on this TV: who's listening.
    val context = LocalContext.current
    var youreIn by remember { mutableStateOf(TvYoureIn.needed(context)) }
    if (youreIn) { TvYoureInScreen { youreIn = false }; return }
    var tab by remember { mutableStateOf(TvTab.Home) }
    // The tab the player was opened from, for Back.
    var from by remember { mutableStateOf(TvTab.Home) }
    val pages = remember { mutableStateListOf<TvPage>() }
    fun select(t: TvTab) {
        if (t == TvTab.NowPlaying && tab != TvTab.NowPlaying) from = tab
        pages.clear()
        tab = t
    }
    val open: (TvPage) -> Unit = { pages += it }
    val played: () -> Unit = { select(TvTab.NowPlaying) }

    BackHandler(enabled = pages.isNotEmpty()) { pages.removeAt(pages.lastIndex) }
    BackHandler(enabled = pages.isEmpty() && tab == TvTab.NowPlaying) { tab = from }
    BackHandler(enabled = pages.isEmpty() && (tab == TvTab.Library || tab == TvTab.Search)) { tab = TvTab.Home }

    // The bar is on the tabs themselves; a list, an artist and the player fill the screen.
    TvFrame(tab, bar = pages.isEmpty() && tab != TvTab.NowPlaying, onSelect = ::select) {
        when {
            // The player's artist opens over the tab it was opened from.
            tab == TvTab.NowPlaying -> TvNowPlaying(onArtist = { id, name -> tab = from; pages.clear(); pages += TvPage.Artist(id, name) })
            pages.isNotEmpty() -> when (val p = pages.last()) {
                is TvPage.Songs -> TvSongs(p.entry, open, played)
                is TvPage.Artist -> TvArtist(p, open, played)
                is TvPage.Show -> TvShow(p, played)
            }
            tab == TvTab.Home -> TvHome(open)
            tab == TvTab.Library -> TvLibrary(open, onSearch = { select(TvTab.Search) }, onHome = { select(TvTab.Home) })
            else -> TvSearch(open, played)
        }
    }
}

/** The page under the floating bar; [bar] off, the page has the whole screen. */
@Composable
internal fun TvFrame(tab: TvTab, bar: Boolean, onSelect: (TvTab) -> Unit, page: @Composable () -> Unit) {
    val ui = tvScheme
    Box(Modifier.fillMaxSize().background(ui.surface)) {
        Box(Modifier.fillMaxSize().padding(top = if (bar) 96.dp else 0.dp)) { page() }
        if (bar) TvTopBar(tab, onSelect, Modifier.align(Alignment.TopCenter))
    }
}

/** Home: Spotify's shelves, the first one as wide halftone cards, the rest as covers in the design's shapes. */
@Composable
private fun TvHome(onOpen: (TvPage) -> Unit) {
    val vm: HomeViewModel = viewModel()
    val home by vm.home.collectAsState()
    val feed = (home as? Response.Success)?.data
    val ui = tvScheme
    Box(Modifier.fillMaxSize()) {
        TvDots(ui.primary, ui.coral)
        when {
            feed == null && home is Response.Loading -> TvShelfSkeleton()
            feed == null -> TvRetry { vm.retry() }
            else -> TvHomeShelves(feed.greeting, feed.sections, onOpen)
        }
    }
}

/** Home's shelves, the first one in wide cards; an untitled one is named [greeting]. */
@Composable
internal fun TvHomeShelves(greeting: String, feedSections: List<HomeSection>, onOpen: (TvPage) -> Unit) {
    val likedName = stringResource(R.string.collection_liked_songs_title)
    val homeName = stringResource(R.string.navigationbar_musicappitems_home_title)
    val sections = remember(feedSections, likedName) {
        feedSections.withoutDj()
            .map { s -> s.copy(items = s.items.filter { it.page(likedName) != null }) }
            .filter { it.items.isNotEmpty() }
    }
    val first = remember { FocusRequester() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        itemsIndexed(sections, key = { i, s -> "$i:${s.title}" }) { i, section ->
            val lead = i == 0
            Column(Modifier.fillMaxWidth()) {
                TvShelfTitle(section.title.ifBlank { greeting.ifBlank { homeName } }, Modifier.padding(start = 48.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    itemsIndexed(section.items, key = { j, it -> "$j:${it.name}:${it.imageUrl}" }) { j, item ->
                        val focus = if (lead && j == 0) Modifier.focusRequester(first) else Modifier
                        val page = item.page(likedName) ?: return@itemsIndexed
                        if (lead) TvWideCard(item.name, item.subtitleText(), item.imageUrl, focus) { onOpen(page) }
                        else TvCoverCard(item.name, item.subtitleText(), item.imageUrl, 170.dp, focus,
                            shape = DotShapes.coverShapeFor(item.name + item.imageUrl, artist = item is HomeItem.Artist)) { onOpen(page) }
                    }
                }
            }
        }
        item(key = "support") { TvSupport() }
    }
    LaunchedEffect(sections.isNotEmpty()) { if (sections.isNotEmpty()) runCatching { delay(80); first.requestFocus() } }
}

/** The page a Home card opens, or null for one the TV cannot open (a card with no id, a single episode). */
private fun HomeItem.page(likedName: String): TvPage? = when (this) {
    is HomeItem.Album -> if (id.isBlank()) null else TvPage.Songs(LibraryEntry(id, name, artists.ifBlank { subtitle }, imageUrl, isPlaylist = false, uri = "spotify:album:$id"))
    is HomeItem.Playlist -> when {
        id == Api.HomeCache.LIKED_SONGS_ID -> TvPage.Songs(LibraryEntry("collection", likedName, "", imageUrl, isPlaylist = true, uri = LIKED))
        id.isNotBlank() -> TvPage.Songs(LibraryEntry(id, name, subtitle, imageUrl, isPlaylist = true, uri = "spotify:playlist:$id"))
        else -> null
    }
    is HomeItem.Artist -> if (id.isBlank()) null else TvPage.Artist(id, name)
    is HomeItem.Show -> if (id.isBlank() || isEpisode || !Features.PODCASTS) null else TvPage.Show(id, name)
}

private fun HomeItem.subtitleText(): String = when (this) {
    is HomeItem.Album -> artists.ifBlank { subtitle }
    is HomeItem.Playlist -> subtitle
    is HomeItem.Show -> subtitle
    is HomeItem.Artist -> ""
}

private enum class LibraryFilter { All, Playlists, Albums, Artists }

private fun LibraryEntry.matches(f: LibraryFilter) = when (f) {
    LibraryFilter.All -> true
    LibraryFilter.Playlists -> folderUri.isNotEmpty() || ":playlist:" in uri || uri == LIKED
    LibraryFilter.Albums -> ":album:" in uri
    LibraryFilter.Artists -> ":artist:" in uri
}

/**
 * Your Library: the big title, filter chips, then the covers in a grid (Liked Songs first; a
 * folder opens its own grid). Nothing saved: the empty page, which points to Search and Home.
 */
@Composable
private fun TvLibrary(onOpen: (TvPage) -> Unit, onSearch: () -> Unit, onHome: () -> Unit) {
    val context = LocalContext.current
    var folder by remember { mutableStateOf<LibraryEntry?>(null) }
    var entries by remember { mutableStateOf<List<LibraryEntry>?>(null) }
    var saved by remember { mutableIntStateOf(-1) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(LibraryFilter.All) }
    BackHandler(enabled = folder != null) { folder = null }
    LaunchedEffect(folder, attempt) {
        failed = false
        val f = folder
        val flow = if (f == null) Graph.api.getLibraryRoot() else Graph.api.getLibraryFolder(f.folderUri)
        when (val r = flow.first { it !is Response.Loading }) {
            is Response.Success -> {
                val liked = LibraryEntry("collection", context.getString(R.string.collection_liked_songs_title), "", Api.HomeCache.LIKED_COVER, isPlaylist = true, uri = LIKED)
                val own = r.data.filter { it.folderUri.isNotEmpty() || it.opens() }.filterNot { it.uri == LIKED }
                if (f == null) saved = own.size
                entries = (if (f == null) listOf(liked) else emptyList()) + own
            }
            else -> failed = true
        }
    }
    val list = entries
    val ui = tvScheme
    val first = remember { FocusRequester() }
    when {
        failed && list == null -> TvRetry { attempt++ }
        list == null -> TvShelfSkeleton()
        folder == null && saved == 0 -> TvMessage(
            stringResource(R.string.tv_empty_library_title), stringResource(R.string.tv_empty_library_body), { TvEmptyArt() },
            stringResource(R.string.tv_go_to_search) to DesignIcons.Search, onSearch,
            stringResource(R.string.tv_browse_home) to DesignIcons.Home, onHome, focus = first,
        )
        else -> {
            val shown = remember(list, filter) { list.filter { it.matches(filter) } }
            Box(Modifier.fillMaxSize()) {
                TvDots(ui.primary, ui.coral)
                LazyVerticalGrid(GridCells.Fixed(5), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
                        Column {
                            TvTitle(folder?.name ?: stringResource(R.string.tv_your_library))
                            if (folder == null) Row(Modifier.padding(top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LibraryFilter.entries.forEach { f ->
                                    TvChip(stringResource(when (f) {
                                        LibraryFilter.All -> R.string.tv_filter_all
                                        LibraryFilter.Playlists -> R.string.assisted_curation_search_filter_playlists
                                        LibraryFilter.Albums -> R.string.assisted_curation_search_filter_albums
                                        LibraryFilter.Artists -> R.string.assisted_curation_search_filter_artists
                                    }), f == filter) { filter = f }
                                }
                            }
                        }
                    }
                    gridItemsIndexed(shown, key = { _, it -> it.uri.ifEmpty { it.folderUri.ifEmpty { it.spotifyId } } }) { i, e ->
                        val artist = ":artist:" in e.uri
                        TvCoverCard(e.name, e.subtitle, e.coverUri, 150.dp, if (i == 0) Modifier.focusRequester(first) else Modifier,
                            shape = if (artist) CircleShape else DotShapes.Squircle) {
                            if (e.folderUri.isNotEmpty()) folder = e else onOpen(e.page())
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(list, folder, saved) { if (list != null) runCatching { delay(80); first.requestFocus() } }
}

/**
 * Search: the pill field (the TV's own keyboard opens on it) with voice search beside it, then the
 * songs in two columns with the typed part in coral, and shelves of artists, albums, playlists
 * and (podcasts on) shows. Nothing found: what to do next, and a few genres to try.
 */
@Composable
private fun TvSearch(onOpen: (TvPage) -> Unit, onPlayed: () -> Unit) {
    val context = LocalContext.current
    val ui = tvScheme
    var query by remember { mutableStateOf("") }
    var asked by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<SearchResults?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val field = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(asked, attempt) {
        if (asked.isBlank()) return@LaunchedEffect
        failed = false
        when (val r = Graph.api.searchEverything(asked).first { it !is Response.Loading }) {
            is Response.Success -> found = r.data
            else -> failed = true
        }
    }
    fun ask(q: String) { query = q; asked = q.trim(); keyboard?.hide() }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val said = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == Activity.RESULT_OK && !said.isNullOrBlank()) ask(said)
    }
    LaunchedEffect(Unit) { runCatching { delay(80); field.requestFocus() } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 48.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val shape = RoundedCornerShape(28.dp)
            var focused by remember { mutableStateOf(false) }
            Row(
                Modifier.weight(1f).height(56.dp).tvFocus(shape, ui.surface, ui.coral, scale = 1.02f, own = false, onFocus = { focused = it })
                    .clip(shape).background(if (focused) ui.surfaceContainerHigh else ui.surfaceContainerLow).padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DesignIcons.Search, null, tint = ui.onSurface, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) {
                    BasicTextField(query, { query = it }, singleLine = true,
                        textStyle = TextStyle(color = ui.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(ui.coral),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { ask(query) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(field))
                }
                if (query.isEmpty()) TvText(stringResource(R.string.tv_search_hint), ui.onSurfaceVariant, 16.sp, FontWeight.SemiBold)
            }
            TvRoundButton(Icons.Outlined.Mic, stringResource(R.string.voice_search), ui.surfaceContainerLow, ui.onSurface, iconSize = 26.dp) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                runCatching { voice.launch(intent) }
                    .onFailure { Toast.makeText(context, context.getString(R.string.voice_search_unavailable), Toast.LENGTH_SHORT).show() }
            }
        }
        Spacer(Modifier.height(18.dp))
        val f = found
        when {
            failed -> TvRetry { attempt++ }
            f == null -> Unit
            f.isEmpty() -> TvNoResults(asked, onClear = { query = ""; asked = ""; found = null; runCatching { field.requestFocus() } },
                onEdit = { runCatching { field.requestFocus() }; keyboard?.show() }, onGenre = ::ask)
            else -> TvSearchResults(f, asked, onOpen) { s -> play(s, null, listOf(s)); onPlayed() }
        }
    }
}

private fun SearchResults.isEmpty() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && (!Features.PODCASTS || shows.isEmpty())

/** Nothing found for [asked]: clear or edit the search, or try one of a few genres. */
@Composable
internal fun TvNoResults(asked: String, onClear: () -> Unit, onEdit: () -> Unit, onGenre: (String) -> Unit) {
    val ui = tvScheme
    val first = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
        TvTitle(stringResource(R.string.tv_no_results, asked), size = 38.sp, lines = 2)
        Spacer(Modifier.height(10.dp))
        TvText(stringResource(R.string.tv_no_results_body), ui.onSurfaceVariant, 20.sp, FontWeight.Medium)
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(stringResource(R.string.tv_clear_search), DesignIcons.Close, ui.primary, ui.onPrimary, Modifier.focusRequester(first), onClick = onClear)
            TvButton(stringResource(R.string.tv_edit_search), Icons.Default.Edit, ui.surfaceContainerHigh, ui.onSurface, onClick = onEdit)
        }
        TvShelfTitle(stringResource(R.string.tv_try_genre), Modifier.padding(top = 32.dp, bottom = 16.dp))
        // The design's five badge colours, square and round in turn.
        val looks = listOf(
            ui.coral to ui.onCoral, ui.primary to ui.onPrimary, Color(0xFFFFE15A) to Color(0xFF2B2200),
            ui.pink to ui.onPink, Color(0xFFA6D2FF) to Color(0xFF002A5C),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(R.string.tv_genre_pop, R.string.tv_genre_chill, R.string.tv_genre_jazz, R.string.tv_genre_rock, R.string.tv_genre_focus)
                .forEachIndexed { i, id ->
                    val name = stringResource(id)
                    val (bg, fg) = looks[i]
                    val shape = RoundedCornerShape(if (i % 2 == 0) 18.dp else 28.dp)
                    Box(Modifier.width(120.dp).height(56.dp).tvFocus(shape, ui.surface, ui.coral, onClick = { onGenre(name) }).clip(shape).background(bg),
                        contentAlignment = Alignment.Center) { TvText(name, fg, 20.sp, FontWeight.Black, family = FlexTitle) }
                }
        }
    }
    LaunchedEffect(asked) { runCatching { delay(80); first.requestFocus() } }
}

@Composable
internal fun TvSearchResults(f: SearchResults, asked: String, onOpen: (TvPage) -> Unit, onSong: (SongsModel) -> Unit) {
    val ui = tvScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (f.songs.isNotEmpty()) item("songs") {
            Column(Modifier.padding(horizontal = 48.dp)) {
                TvShelfTitle(stringResource(R.string.assisted_curation_search_filter_songs), Modifier.padding(bottom = 12.dp))
                val songs = f.songs.take(8)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    listOf(songs.take(4), songs.drop(4)).forEach { part ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            part.forEach { s -> TvSearchSong(s, asked) { onSong(s) } }
                        }
                    }
                }
            }
        }
        val artists = f.artists.filter { it.id.isNotBlank() }
        if (artists.isNotEmpty()) item("artists") {
            TvShelf(stringResource(R.string.assisted_curation_search_filter_artists)) {
                itemsIndexed(artists, key = { i, a -> "$i:${a.id}" }) { _, a ->
                    TvCoverCard(a.name, "", a.coverUri, 150.dp, shape = CircleShape) { onOpen(TvPage.Artist(a.id, a.name)) }
                }
            }
        }
        val albums = f.albums.filter { it.spotifyId.isNotBlank() }
        if (albums.isNotEmpty()) item("albums") {
            TvShelf(stringResource(R.string.assisted_curation_search_filter_albums)) {
                itemsIndexed(albums, key = { i, a -> "$i:${a.spotifyId}" }) { _, a ->
                    TvCoverCard(a.name, a.artists, a.coverUri, 150.dp) {
                        onOpen(TvPage.Songs(LibraryEntry(a.spotifyId, a.name, a.artists, a.coverUri, isPlaylist = false, uri = "spotify:album:${a.spotifyId}")))
                    }
                }
            }
        }
        if (f.playlists.isNotEmpty()) item("playlists") {
            TvShelf(stringResource(R.string.assisted_curation_search_filter_playlists)) {
                itemsIndexed(f.playlists, key = { i, e -> "$i:${e.uri}" }) { _, e ->
                    TvCoverCard(e.name, e.subtitle, e.coverUri, 150.dp) { onOpen(TvPage.Songs(e)) }
                }
            }
        }
        val shows = if (Features.PODCASTS) f.shows.filter { it.id.isNotBlank() } else emptyList()
        if (shows.isNotEmpty()) item("shows") {
            TvShelf(stringResource(R.string.assisted_curation_search_filter_podcasts)) {
                itemsIndexed(shows, key = { i, sh -> "$i:${sh.id}" }) { _, sh ->
                    TvCoverCard(sh.name, sh.publisher, sh.coverUri, 150.dp) { onOpen(TvPage.Show(sh.id, sh.name)) }
                }
            }
        }
    }
}

/** A song found: its halftone cover, the title with the typed part in coral, the artist and the length. */
@Composable
private fun TvSearchSong(s: SongsModel, asked: String, onClick: () -> Unit) {
    val ui = tvScheme
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().height(64.dp).tvFocus(shape, ui.surface, ui.coral, scale = 1.03f, onClick = onClick).clip(shape)
            .background(ui.surfaceContainerLow).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvArt(s.coverUri, s.title, Modifier.size(48.dp), RoundedCornerShape(12.dp), cells = 14f)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            val title = remember(s.title, asked, ui.coral) {
                buildAnnotatedString {
                    val n = if (asked.isNotBlank() && s.title.startsWith(asked, ignoreCase = true)) asked.length else 0
                    withStyle(SpanStyle(color = ui.coral, fontWeight = FontWeight.Black)) { append(s.title.take(n)) }
                    append(s.title.drop(n))
                }
            }
            Text(title, color = ui.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TvText(s.singer, ui.onSurfaceVariant, 16.sp, FontWeight.Medium)
        }
        if (s.durationMs > 0) TvText(formatMs(s.durationMs.toLong()), ui.onSurfaceVariant, 16.sp, FontWeight.SemiBold, Modifier.padding(end = 8.dp))
    }
}

/** A list's page: its cover, name, Play, Shuffle and Save on the left, its songs on the right, in the cover's colours. */
@Composable
private fun TvSongs(entry: LibraryEntry, onOpen: (TvPage) -> Unit, onPlayed: () -> Unit) {
    val vm: PlayerViewModel = viewModel()
    var page by remember(entry) { mutableStateOf<ContextPageModel?>(null) }
    var failed by remember(entry) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(entry, attempt) {
        failed = false
        val id = entry.uri.substringAfterLast(':')
        val flow = when {
            entry.uri == LIKED -> Graph.api.getLikedPage()
            ":album:" in entry.uri -> Graph.api.getAlbumPage(id)
            else -> Graph.api.getPlaylistPage(id)
        }
        // The pages come in as they are read (a big list): each one shown, the last one kept.
        flow.collect { r ->
            when (r) {
                is Response.Success -> page = r.data
                is Response.Loading -> Unit
                else -> if (page == null) failed = true
            }
        }
    }
    val p = page
    when {
        failed && p == null -> TvRetry { attempt++ }
        p == null -> TvShelfSkeleton(rows = 2)
        else -> TvSongsPage(entry, p, onOpen,
            onPlay = { p.tracks.firstOrNull()?.let { vm.updateShuffleState(false); play(it, p.uri, p.tracks); onPlayed() } },
            onShuffle = { if (p.tracks.isNotEmpty()) { vm.updateShuffleState(true); play(p.tracks.random(), p.uri, p.tracks); onPlayed() } },
            onSong = { s -> play(s, p.uri, p.tracks); onPlayed() })
    }
}

@Composable
internal fun TvSongsPage(entry: LibraryEntry, p: ContextPageModel, onOpen: (TvPage) -> Unit, onPlay: () -> Unit, onShuffle: () -> Unit, onSong: (SongsModel) -> Unit) {
    val context = LocalContext.current
    val cover = p.coverUri.ifBlank { entry.coverUri }
    // The cover's own colours, as the phone's player takes them: its first ink seeds the album scheme.
    val art by produceState(com.music.spotui.ui.dots.cachedHalftone(cover), cover) {
        value = com.music.spotui.ui.dots.cachedHalftone(cover)
        if (value == null && cover.isNotBlank()) value = com.music.spotui.ui.dots.loadHalftone(context, cover)
    }
    val fallback = tvScheme.primary
    // Album colours off: the design's own colours, as on the phone.
    val ui = tvScheme
    val albumColours = remember { runCatching { com.music.spotui.ui.theme.ColorPrefs.albumColours }.getOrDefault(true) }
    val al = remember(art?.inkA, fallback, albumColours) { if (albumColours) com.music.spotui.ui.theme.albumSchemeOf(art?.inkA ?: fallback.toArgb()) else ui }
    val playingUri = SongPlayer.nowPlaying.value.uri
    val listUri = p.uri.ifBlank { entry.uri }
    val canSave = entry.uri != LIKED && listUri.startsWith("spotify:")
    val saved by produceState(false, listUri) { if (canSave) value = runCatching { SongPlayer.libraryContains(listOf(listUri)) }.getOrDefault(emptySet()).contains(listUri) }
    var savedNow by remember(saved) { mutableStateOf(saved) }
    var menuFor by remember { mutableStateOf<SongsModel?>(null) }
    menuFor?.let { s -> TvSongMenu(s, al, onOpen) { menuFor = null } }
    val playFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(al.surface)) {
        TvDots(al.primary, al.tertiaryContainer)
        Row(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 27.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.width(272.dp)) {
                TvArt(cover, p.name.ifBlank { entry.name }, Modifier.size(190.dp), DotShapes.Squircle, cells = 26f)
                Spacer(Modifier.height(12.dp))
                TvTitle(p.name.ifBlank { entry.name }, size = 34.sp, color = al.onSurface, lines = 2)
                val count = pluralStringResource(R.plurals.add_to_playlist_subtitle_songs_count, p.tracks.size, p.tracks.size.toString())
                val owner = p.ownerName.ifBlank { entry.subtitle }
                TvText(listOf(owner, count).filter { it.isNotBlank() }.joinToString(" · "), al.onSurfaceVariant, 16.sp, FontWeight.SemiBold, Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(16.dp))
                TvButton(stringResource(R.string.immersive_player_play), DesignIcons.Play, al.primary, al.onPrimary,
                    Modifier.fillMaxWidth().focusRequester(playFocus), gap = al.surface, onClick = onPlay)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton(stringResource(R.string.immersive_shell_shuffle), DesignIcons.Shuffle, al.surfaceContainerHigh, al.onSurface,
                        Modifier.weight(1f), gap = al.surface, onClick = onShuffle)
                    if (canSave) TvRoundButton(
                        if (savedNow) DesignIcons.Heart else DesignIcons.HeartOutline,
                        stringResource(if (savedNow) R.string.tv_remove_list else R.string.tv_save_list),
                        al.tertiaryContainer, al.onTertiaryContainer, shape = RoundedCornerShape(18.dp), gap = al.surface,
                    ) { savedNow = !savedNow; SongPlayer.setSaved(listUri, savedNow) }
                }
            }
            Column(Modifier.weight(1f)) {
                val minutes = (p.tracks.sumOf { it.durationMs.toLong() } / 60_000L).toInt()
                val count = pluralStringResource(R.plurals.add_to_playlist_subtitle_songs_count, p.tracks.size, p.tracks.size.toString())
                TvText(if (minutes > 0) "$count · ${stringResource(R.string.tv_minutes, minutes)}" else count, al.onSurfaceVariant, 16.sp, FontWeight.Bold,
                    Modifier.padding(bottom = 8.dp))
                LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp, start = 10.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(p.tracks, key = { i, s -> "$i:${s.url}" }) { i, s ->
                        TvListSong(s, i + 1, current = playingUri.isNotBlank() && (s.url == playingUri || (s.spotifyTrackId.isNotBlank() && playingUri.endsWith(s.spotifyTrackId))), al, onClick = { onSong(s) }, onMore = { menuFor = s })
                    }
                }
            }
        }
    }
    LaunchedEffect(p.tracks.isNotEmpty()) { if (p.tracks.isNotEmpty()) runCatching { delay(80); playFocus.requestFocus() } }
}

/** A song of a list: its number, title and length on a soft album-colour row, and its ⋯ menu. */
@Composable
private fun TvListSong(s: SongsModel, number: Int, current: Boolean, al: androidx.compose.material3.ColorScheme, onClick: () -> Unit, onMore: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.weight(1f).height(60.dp).tvFocus(shape, al.surface, tvScheme.coral, scale = 1.03f, onClick = onClick).clip(shape)
                .background(if (current) al.surfaceContainer else al.surfaceContainer.copy(alpha = 0.55f)).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvText("$number", al.onSurfaceVariant, 18.sp, FontWeight.Bold, Modifier.width(30.dp))
            TvText(s.title, if (current) al.primary else al.onSurface, 18.sp, FontWeight.ExtraBold, Modifier.weight(1f))
            if (s.durationMs > 0) TvText(formatMs(s.durationMs.toLong()), al.onSurfaceVariant, 16.sp, FontWeight.SemiBold, Modifier.padding(start = 12.dp))
        }
        TvRoundButton(DesignIcons.More, stringResource(R.string.tv_more), al.surfaceContainer.copy(alpha = 0.55f), al.onSurface,
            size = 48.dp, gap = al.surface, iconSize = 22.dp, onClick = onMore)
    }
}

/** The ⋯ of a song: add it to the queue, or open its artist. */
@Composable
private fun TvSongMenu(s: SongsModel, al: androidx.compose.material3.ColorScheme, onOpen: (TvPage) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val vm: PlayerViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.width(420.dp).clip(RoundedCornerShape(28.dp)).background(al.surfaceContainer).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TvText(s.title, al.onSurface, 22.sp, FontWeight.Black, family = FlexTitle)
            TvText(s.singer, al.onSurfaceVariant, 16.sp, FontWeight.Medium, Modifier.padding(bottom = 6.dp))
            TvButton(stringResource(R.string.add_to_queue_action_content_description), DesignIcons.AddToQueue, al.primary, al.onPrimary,
                Modifier.fillMaxWidth().focusRequester(first), gap = al.surfaceContainer) {
                if (vm.addToQueue(s)) Toast.makeText(context, context.getString(R.string.tv_added_to_queue), Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            TvButton(stringResource(R.string.tv_go_to_artist), DesignIcons.Profile, al.surfaceContainerHigh, al.onSurface,
                Modifier.fillMaxWidth(), gap = al.surfaceContainer) {
                scope.launch {
                    val a = ArtistChooser.artistsOf(s.spotifyTrackId, s.singer).firstOrNull { it.uri.isNotBlank() }
                    onDismiss()
                    if (a != null) onOpen(TvPage.Artist(a.uri.substringAfterLast(':'), a.name))
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { delay(80); first.requestFocus() } }
}

/** A page that did not load: offline or not, the reason and Try again. */
@Composable
internal fun TvRetry(onRetry: () -> Unit) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    val online = remember { tvOnline(context) }
    TvMessage(
        stringResource(if (online) R.string.tv_load_failed_title else R.string.tv_offline_title),
        stringResource(if (online) R.string.tv_load_failed_body else R.string.tv_offline_body),
        { TvOfflineArt() },
        stringResource(R.string.retry_button) to DesignIcons.Repeat, onRetry, focus = focus,
    )
    LaunchedEffect(Unit) { runCatching { delay(80); focus.requestFocus() } }
}

private const val LIKED = "spotify:collection:tracks"

/** A Library row the TV opens: a playlist, an album, an artist or (podcasts on) a show. */
private fun LibraryEntry.opens() = ":playlist:" in uri || ":album:" in uri || ":artist:" in uri || uri == LIKED ||
    (Features.PODCASTS && ":show:" in uri)

/** The page a Library row opens. */
private fun LibraryEntry.page(): TvPage = when {
    ":artist:" in uri -> TvPage.Artist(uri.substringAfterLast(':'), name)
    ":show:" in uri -> TvPage.Show(uri.substringAfterLast(':'), name)
    else -> TvPage.Songs(this)
}

internal fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun play(song: SongsModel, contextUri: String?, songs: List<SongsModel>) {
    SongPlayer.playSong(song.url, contextUri = contextUri, songs = songs)
    Graph.currentSongState.updateSongState(song.coverUri, song.title, song.singer, true, song.id, songs.indexOf(song).coerceAtLeast(0), song.album)
}
