package com.music.spotui.ui.screens

import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppAccent

import com.music.spotui.ui.components.SkeletonKind
import com.music.spotui.ui.components.pressable

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Extension
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
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.ArtistsModel
import com.music.spotui.data.api.Api
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.withLikedFirst
import com.music.spotui.data.entity.withoutDj
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.ui.components.Loader
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppPalette
import com.music.spotui.ui.viewmodel.HomeViewModel
import java.time.LocalTime


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController){

    val homeViewModel : HomeViewModel = viewModel()
    val home by homeViewModel.home.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
    ) {
        val feed = (home as? Response.Success)?.data

        when {
            // Preferred: the real personalized Spotify home feed.
            feed != null && feed.sections.isNotEmpty() -> {
                HomeFeedContent(navController, feed, homeViewModel.facet.value) { homeViewModel.selectFacet(it) }
            }

            // No feed to show (loading, empty chip, error): keep the chips on screen
            // so the user can always switch back to "Tout".
            else -> androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                HomeHeaderRow(navController, homeViewModel.facet.value) { homeViewModel.selectFacet(it) }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        home is Response.Loading -> Loader(SkeletonKind.Home)
                        else -> androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (home is Response.Error) stringResource(R.string.home_error) else stringResource(R.string.home_empty),
                                color = AppText,
                                textAlign = TextAlign.Center,
                            )
                            if (home is Response.Error) androidx.compose.material3.TextButton(onClick = { homeViewModel.retry() }) {
                                Text(stringResource(R.string.home_retry), color = AppAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Albums and playlists start loading under the finger (see pressHint). */
internal fun prefetchHomeItem(item: HomeItem) {
    when (item) {
        is HomeItem.Album -> if (item.id.isNotBlank()) ContextViewModel.prefetch("album", item.id)
        is HomeItem.Playlist ->
            if (item.id == Api.HomeCache.LIKED_SONGS_ID) ContextViewModel.prefetch("liked", "")
            else if (item.id.isNotBlank()) ContextViewModel.prefetch("playlist", item.id)
        else -> {}
    }
}

internal fun onHomeItemClick(navController: NavController, item: HomeItem) {
    when (item) {
        is HomeItem.Album -> navController.navigate(albumRoute(item.name, item.artists.ifBlank { item.subtitle }, item.id))
        is HomeItem.Artist -> navController.navigate(artistRoute(item.name, item.id))
        is HomeItem.Show ->
            if (item.isEpisode) com.music.spotui.di.SongPlayer.playSong("episode:${item.id}")
            else navController.navigate(com.music.spotui.ui.navigation.showRoute(item.id, item.name))
        // Load the real playlist content by its Spotify id (daily mixes, etc).
        is HomeItem.Playlist ->
            if (item.id == Api.HomeCache.LIKED_SONGS_ID) navController.navigate(Routes.Liked.route)
            else if (item.id.isNotBlank()) navController.navigate(playlistRoute(item.id, item.name))
            else navController.navigate(albumRoute(item.name))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun HomeFeedContent(navController: NavController, feed: HomeFeedModel, facet: String = "", onFacet: (String) -> Unit = {}) {
    // Mirror open.spotify.com exactly: sections render in the order the feed
    // returns them. The 2-column "shortcuts" grid is only used for the UNTITLED
    // section the web home starts with — if the feed leads with a titled section
    // ("Jump back in", "Made For …"), it renders as a titled carousel first,
    // not force-squeezed into the grid.
    // Podcasts are off for now: their shows and episodes leave the feed.
    // Kept between recompositions: a new list for every one of them would restart the rows below.
    val hideDj = com.music.spotui.data.preferences.HideDjPref.on
    val sections = remember(feed, hideDj) {
        val all = if (com.music.spotui.data.api.Features.PODCASTS) feed.sections
        else feed.sections.map { s -> s.copy(items = s.items.filterNot { it is HomeItem.Show }) }.filter { it.items.isNotEmpty() }
        if (hideDj) all.withoutDj() else all
    }
    val gridSection = remember(sections) { sections.firstOrNull()?.takeIf { it.title.isBlank() } }
    val carousels = remember(sections, gridSection) { if (gridSection != null) sections.drop(1) else sections }
    val carouselKeys = remember(carousels) { uniqueKeys(carousels.map { it.title }) }
    // Liked Songs leads the shortcuts when Spotify's grid does not carry it.
    val likedName = stringResource(R.string.collection_liked_songs_title)
    val shortcuts = remember(gridSection, likedName) { gridSection?.items?.withLikedFirst(likedName) }
    // The shortcuts at the top load in the background, ready before a tap.
    androidx.compose.runtime.LaunchedEffect(shortcuts?.map { it.name }) {
        val top = shortcuts.orEmpty().mapNotNull { item ->
            when (item) {
                is HomeItem.Album -> item.id.takeIf { it.isNotBlank() }?.let { "album" to it }
                is HomeItem.Playlist -> if (item.id == Api.HomeCache.LIKED_SONGS_ID) "liked" to "" else item.id.takeIf { it.isNotBlank() }?.let { "playlist" to it }
                else -> null
            }
        }
        if (top.isNotEmpty()) ContextViewModel.prefetchSoon(top)
    }

    // Re-tapping the Home tab while on Home scrolls this list back to its top (official
    // behaviour): an animation over the existing items, so nothing is fetched again.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.music.spotui.ui.navigation.TabReselect.home.collect {
            if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // The hero stands for Spotify's first mix; the rest of its shelf rings it as covers.
    val hero = remember(carousels) { pickHeroMix(carousels) }
    val page = com.music.spotui.ui.theme.SystemColors.scheme.surface
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = com.music.spotui.ui.components.bottomBarsSpace()),
        modifier = Modifier
            .fillMaxSize()
            .background(page)
    ) {
        item(key = "top") {
            Column(Modifier.statusBarsPadding()) {
                HomeHeaderRow(navController, facet, onFacet)
            }
        }
        hero?.let { (mix, others) -> item(key = "hero") { HomeHero(navController, mix, others) } }
        item(key = "keep") { HomeKeepListening(navController, shortcuts.orEmpty()) }
        shortcuts?.takeIf { it.isNotEmpty() }?.let { tiles ->
            item(key = "shortcuts") { HomeShelf(navController, stringResource(R.string.home_jump_back_in), tiles) }
        }
        items(carousels.size, key = { carouselKeys[it] }) { i ->
            HomeShelf(navController, carousels[i].title, carousels[i].items)
        }
        // Sections from extensions (home.sections), after Spotify's own, each under its extension's name.
        item { com.music.spotui.extensions.ExtensionBlocks(app.myspoti.extensions.Hook.HOME, app.myspoti.extensions.Surface.HOME, "{}") }
    }
}

/**
 * The top of Home: the design's bar (profile, Search, Settings), then Spotify's music/podcast
 * filters when podcasts are on (the design has none, and with podcasts off there is nothing to filter).
 */
@Composable
private fun HomeHeaderRow(navController: NavController, facet: String, onFacet: (String) -> Unit) {
    var showProfile by remember { androidx.compose.runtime.mutableStateOf(false) }
    if (showProfile) ProfileSheet(onOpen = { route -> showProfile = false; navController.navigate(route) }) { showProfile = false }
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.music.spotui.data.api.ProfileCache.ensure(context)
    }
    HomeTopBar(navController) { showProfile = true }
    if (com.music.spotui.data.api.Features.PODCASTS) Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
    ) {
        // Spotify's home chips; tapping the selected one goes back to "All".
        HomePill(stringResource(R.string.filter_chip_all_label), selected = facet.isEmpty()) { onFacet("") }
        HomePill(stringResource(R.string.recents_content_filter_music), selected = facet == "music-chip") { onFacet("music-chip") }
        HomePill(stringResource(R.string.ad_category_title_podcasts), selected = facet == "podcasts-chip") { onFacet("podcasts-chip") }
        HomePill(stringResource(R.string.audiobooks_title), selected = facet == "audiobooks-chip") { onFacet("audiobooks-chip") }
    }
}

@Composable
private fun HomePill(label: String, selected: Boolean = false, onClick: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) s.primary else s.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(text = label, color = if (selected) s.onPrimary else s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** A key per entry that stays with it while the others around it change: [raw], with a counter on repeats. */
private fun uniqueKeys(raw: List<String>): List<String> {
    val seen = HashMap<String, Int>()
    return raw.map { k ->
        val n = (seen[k] ?: 0) + 1
        seen[k] = n
        if (n == 1) k else "$k#$n"
    }
}

/** Profile sheet behind the avatar: account name and sign-out (in the Web Player). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
private fun ProfileSheet(onOpen: (route: String) -> Unit = {}, onDismiss: () -> Unit) {
    val name = com.music.spotui.data.api.ProfileCache.name.orEmpty()
    val avatar = com.music.spotui.data.api.ProfileCache.imageUrl
    // Opened in full: half-open, the sheet and its list both moved under the finger.
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = tone(0xFF282828),
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().then(Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars))
                .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(bottom = 16.dp),
        ) {
            // Name and picture open your public profile, as in Spotify.
            val myId = com.music.spotui.data.api.ProfileCache.id
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
                .clickable(enabled = myId != null) { myId?.let { onOpen(com.music.spotui.ui.navigation.profileRoute(it, name)) } }
                .padding(horizontal = 20.dp, vertical = 8.dp)) {
                com.music.spotui.ui.components.ProfileAvatar(url = avatar, name = name, size = 48.dp)
                Text(name.ifBlank { stringResource(R.string.spotify_account) }, color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 14.dp))
                if (com.music.spotui.data.api.SupportReminder.level != com.music.spotui.data.api.SupportReminder.Level.NONE)
                    com.music.spotui.ui.components.SupporterBadge(Modifier.padding(start = 10.dp))
            }
            if (myId != null) Text(stringResource(R.string.view_profile), color = AppMuted, fontSize = 13.sp,
                modifier = Modifier.padding(start = 82.dp, bottom = 4.dp))
            androidx.compose.material3.HorizontalDivider(color = tone(0xFF3E3E3E), modifier = Modifier.padding(vertical = 8.dp))
            // Sound quality, downloads, data, battery, display: one page.
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { onOpen(com.music.spotui.ui.navigation.Routes.Settings.route) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.settings_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.settings_detail), color = AppMuted, fontSize = 12.sp)
            }
            // In-app recap: every listen on this phone, Spotify, downloads and local files.
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { onOpen(com.music.spotui.ui.navigation.Routes.Recap.route) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.recap_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.recap_detail), color = AppMuted, fontSize = 12.sp)
            }
            // The account's own listening history, as Spotify keeps it: it belongs beside the
            // recap (the local counter) because both answer "what did I listen to".
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { onOpen(com.music.spotui.ui.navigation.Routes.Recent.route) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.recents_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.recents_detail), color = AppMuted, fontSize = 12.sp)
            }
            // What the followed people are listening to (the official Friend Activity feed).
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { onOpen(com.music.spotui.ui.navigation.Routes.Friends.route) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.friends_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.friends_detail), color = AppMuted, fontSize = 12.sp)
            }
            // Third-party extensions (lyrics, audio, scripts): installed, enabled, repositories.
            val extRegistry by com.music.spotui.extensions.Extensions.registry.collectAsState()
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(com.music.spotui.ui.navigation.Routes.Extensions.route) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.extensions_title), color = AppText, fontSize = 16.sp)
                    val installed = extRegistry.extensions
                    Text(
                        if (installed.isEmpty()) stringResource(R.string.extensions_row_detail)
                        else stringResource(R.string.extensions_row_count, installed.size, installed.count { it.enabled }),
                        color = AppMuted, fontSize = 12.sp,
                    )
                }
                androidx.compose.material3.Icon(Icons.Filled.Extension, null, tint = AppMuted,
                    modifier = Modifier.padding(start = 12.dp).size(22.dp))
            }
            // The Spotify account page (plan, privacy, devices), in the app's signed-in browser.
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable {
                    onDismiss()
                    com.music.spotui.ui.screens.AccountBrowser.open(com.music.spotui.di.Graph.app, com.music.spotui.ui.screens.AccountBrowser.ACCOUNT_URL)
                }.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.account_page_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.account_page_detail), color = AppMuted, fontSize = 12.sp)
            }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            // Donations (Ko-fi or GitHub Sponsors): always here, never in the way.
            var support by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (support) com.music.spotui.ui.components.SupportSheet(onDismiss = { support = false })
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { support = true }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.support_title), color = com.music.spotui.ui.theme.AppAccent, fontSize = 16.sp)
                Text(stringResource(R.string.support_row_detail), color = AppMuted, fontSize = 12.sp)
            }
            // About: version, Discord, source code and Ko-fi.
            var about by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (about) com.music.spotui.ui.components.AboutSheet(onDismiss = { about = false })
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { about = true }.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.creator_biography_section_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.about_detail), color = AppMuted, fontSize = 12.sp)
            }
            var perks by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (perks) com.music.spotui.ui.components.SupporterSheet(onDismiss = { perks = false })
            if (com.music.spotui.data.api.SupportReminder.PERKS_ENABLED) androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().clickable { perks = true }.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.supporter_perks_title), color = AppText, fontSize = 16.sp)
                Text(stringResource(R.string.supporter_perks_detail), color = AppMuted, fontSize = 12.sp)
            }
            Text(
                stringResource(R.string.connect_device_menu_logout),
                color = AppText, fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        com.music.spotui.di.SessionActions.logout(com.music.spotui.di.Graph.app)
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
    }
}


