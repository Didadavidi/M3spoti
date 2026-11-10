package com.music.spotui.ui.screens

import com.music.spotui.ui.theme.tone
import androidx.compose.material.icons.outlined.History as ProfileHistoryIcon
import androidx.compose.material.icons.outlined.Extension as ProfileExtensionIcon
import androidx.compose.material.icons.outlined.Info as ProfileInfoIcon
import androidx.compose.material.icons.outlined.Star as ProfileStarIcon
import androidx.compose.material.icons.outlined.SwitchAccount as ProfileSwitchIcon
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight as ProfileChevronIcon
import androidx.compose.material.icons.automirrored.outlined.Logout as ProfileLogoutIcon
import com.music.spotui.ui.theme.soft
import com.music.spotui.ui.theme.onSoft
import com.music.spotui.ui.theme.card
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.pink
import com.music.spotui.ui.theme.onPink
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
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
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
import com.music.spotui.ui.theme.GridBackground
import com.music.spotui.ui.viewmodel.HomeViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController){

    val homeViewModel : HomeViewModel = viewModel()
    // An old feed is read again when Home shows again (see HomeViewModel.refreshIfStale).
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { homeViewModel.refreshIfStale() }
    val home by homeViewModel.home.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
    ) {
        val feed = (home as? Response.Success)?.data
        val hasFeed = feed != null && feed.sections.isNotEmpty()
        // The feed fades in over the placeholder instead of replacing it in one frame.
        com.music.spotui.ui.components.LoadedFade(!hasFeed) { waiting ->
        when {
            // Preferred: the real personalized Spotify home feed.
            !waiting && feed != null -> {
                HomeFeedContent(navController, feed, homeViewModel.facet.value) { homeViewModel.selectFacet(it) }
            }

            // No feed to show (loading, empty chip, error): keep the chips on screen
            // so the user can always switch back to "Tout".
            else -> androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                HomeHeaderRow(navController, homeViewModel.facet.value) { homeViewModel.selectFacet(it) }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        // Fading out as the feed fades in: still the placeholder.
                        home is Response.Loading || hasFeed -> Loader(SkeletonKind.Home)
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
    // With podcasts off, their shows and episodes leave the feed (and a section left empty goes).
    // Kept between recompositions: a new list for every one of them would restart the rows below.
    val hideDj = com.music.spotui.data.preferences.HideDjPref.hidden
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
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
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
    // The list scrolls on under the status bar: the page's colour behind its icons, not the shelves.
    com.music.spotui.ui.theme.StatusBarScrim(page, solid = true)
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
        // Spotify's home chips. Once one is picked, as in Spotify: a close chip, the picked one,
        // and joined to it the chips it opens (Following: only what the user follows).
        val labels = mapOf(
            "music-chip" to stringResource(R.string.recents_content_filter_music),
            "podcasts-chip" to stringResource(R.string.ad_category_title_podcasts),
            "audiobooks-chip" to stringResource(R.string.audiobooks_title),
        )
        if (facet.isEmpty()) {
            HomePill(stringResource(R.string.filter_chip_all_label), selected = true) {}
            HomePill(labels.getValue("music-chip")) { onFacet("music-chip") }
            if (com.music.spotui.data.api.Features.PODCASTS) {
                HomePill(labels.getValue("podcasts-chip")) { onFacet("podcasts-chip") }
                HomePill(labels.getValue("audiobooks-chip")) { onFacet("audiobooks-chip") }
            }
        } else {
            val parent = com.music.spotui.data.entity.HomeSubChips.parentOf(facet) ?: facet
            val subs = com.music.spotui.data.entity.HomeSubChips.byParent[parent].orEmpty()
            val s = com.music.spotui.ui.theme.SystemColors.scheme
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(16.dp)).background(s.surfaceContainerHigh).clickable { onFacet("") },
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = stringResource(R.string.filter_chip_all_label),
                    tint = s.onSurface, modifier = Modifier.size(18.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HomePill(labels[parent].orEmpty(), selected = true, shape = if (subs.isEmpty()) RoundedCornerShape(16.dp) else JoinedStart) { onFacet(parent) }
                subs.forEachIndexed { i, (id, label) ->
                    val shown = remember(id) { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
                    androidx.compose.animation.AnimatedVisibility(
                        visibleState = shown,
                        enter = androidx.compose.animation.expandHorizontally(expandFrom = Alignment.Start) + androidx.compose.animation.fadeIn(),
                    ) {
                        HomePill(label, selected = facet == id, shape = if (i == subs.lastIndex) JoinedEnd else RoundedCornerShape(6.dp)) { onFacet(id) }
                    }
                }
            }
        }
    }
}

/** The left half of two pills joined (a picked chip and the one it opened), and the right one. */
private val JoinedStart = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50, topEndPercent = 15, bottomEndPercent = 15)
private val JoinedEnd = RoundedCornerShape(topStartPercent = 15, bottomStartPercent = 15, topEndPercent = 50, bottomEndPercent = 50)

@Composable
private fun HomePill(label: String, selected: Boolean = false, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), onClick: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
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

@Composable
private fun HomeFeedSection(navController: NavController, section: HomeSection) {
    Text(
        text = section.title,
        color = AppText,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier.padding(16.dp, 20.dp, 16.dp, 4.dp),
    )
    val keys = remember(section) { uniqueKeys(section.items.map { it.name + "|" + it.imageUrl }) }
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp, 0.dp)) {
        items(section.items.size, key = { keys[it] }) { i ->
            HomeFeedCard(section.items[i]) { onHomeItemClick(navController, section.items[i]) }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun HomeFeedCard(item: HomeItem, onClick: () -> Unit) {
    val isArtist = item is HomeItem.Artist
    val subtitle = when (item) {
        is HomeItem.Album -> item.subtitle
        is HomeItem.Playlist -> item.subtitle
        is HomeItem.Artist -> stringResource(R.string.artist_subtitle)
        is HomeItem.Show -> item.subtitle
    }
    Column(
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.pressable(0.96f, onPressHint = { prefetchHomeItem(item) }) { onClick() }
            .width(150.dp)
            .padding(6.dp)
            ,
    ) {
        Box {
            GlideImage(
                transition = CrossFade,
                modifier = Modifier
                    .size(150.dp)
                    .clip(if (isArtist) CircleShape else RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                model = item.imageUrl,
                loading = placeholder(R.drawable.placeholder),
                failure = placeholder(R.drawable.placeholder),
                contentDescription = "",
            )
            if (item is HomeItem.Show && !item.isEpisode) {
                com.music.spotui.ui.components.NewEpisodeDot(item.id, AppAccent, Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.name,
            color = AppText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
        )
        // Always two lines tall: a long artist mix no longer makes one card taller than its row.
        Text(
            text = subtitle,
            color = AppMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            minLines = 2,
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
        )
    }
}

/** The bell beside the avatar: opens "What's New". */
@Composable
private fun WhatsNewBell(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(end = 10.dp, top = 10.dp, bottom = 10.dp)
            .size(34.dp),
    ) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Outlined.Notifications,
            contentDescription = stringResource(R.string.whats_new_title),
            tint = AppText,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** One row of the profile sheet: badge, title, one-line detail, and a trailing icon (a chevron by default). */
private class ProfileRow(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val detail: String?,
    val badgeBg: Color,
    val badgeFg: Color,
    val onClick: () -> Unit,
)

/** A rounded group of rows: large outer corners, small inner ones (as Settings does). */
@Composable
private fun ProfileGroup(rows: List<ProfileRow>, firstBadge: Int) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val big = 26.dp
    val small = 6.dp
    val badges = com.music.spotui.ui.dots.DotShapes.badges
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEachIndexed { i, r ->
            val top = if (i == 0) big else small
            val bottom = if (i == rows.lastIndex) big else small
            val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(shape).background(s.card)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = r.onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    Modifier.size(44.dp).clip(badges[(firstBadge + i) % badges.size]).background(r.badgeBg),
                    contentAlignment = Alignment.Center,
                ) { Icon(r.icon, contentDescription = null, tint = r.badgeFg, modifier = Modifier.size(22.dp)) }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(r.title, color = s.onSurface, fontFamily = com.music.spotui.ui.theme.FlexTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (r.detail != null) Text(r.detail, color = s.onSurfaceVariant, fontSize = 12.sp, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ProfileChevronIcon, contentDescription = null,
                    tint = s.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Profile sheet behind the avatar: account name and sign-out (in the Web Player). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
private fun ProfileSheet(onOpen: (route: String) -> Unit = {}, onDismiss: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val name = com.music.spotui.data.api.ProfileCache.name.orEmpty()
    val avatar = com.music.spotui.data.api.ProfileCache.imageUrl
    // Opened in full: half-open, the sheet and its list both moved under the finger.
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = s.surface,
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
                com.music.spotui.ui.components.ProfileAvatar(url = avatar, name = name, size = 72.dp)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(name.ifBlank { stringResource(R.string.spotify_account) }, color = s.onSurface,
                        fontFamily = com.music.spotui.ui.theme.FlexDisplay, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 30.sp,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (com.music.spotui.data.api.SupportReminder.level != com.music.spotui.data.api.SupportReminder.Level.NONE)
                        com.music.spotui.ui.components.SupporterBadge(Modifier.padding(top = 6.dp))
                    if (myId != null) Text(stringResource(R.string.view_profile), color = s.onSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50)).background(s.soft)
                            .padding(horizontal = 14.dp, vertical = 6.dp))
                }
            }
            val outline = androidx.compose.material.icons.Icons.Outlined
            val di = com.music.spotui.ui.dots.DesignIcons
            // Third-party extensions (lyrics, audio, scripts): installed, enabled, repositories.
            val extRegistry by com.music.spotui.extensions.Extensions.registry.collectAsState()
            val installed = extRegistry.extensions
            // Donations (Ko-fi or GitHub Sponsors): always here, never in the way.
            var support by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (support) com.music.spotui.ui.components.SupportSheet(onDismiss = { support = false })
            // About: version, Discord, source code and Ko-fi.
            var about by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (about) com.music.spotui.ui.components.AboutSheet(onDismiss = { about = false })
            var perks by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (perks) com.music.spotui.ui.components.SupporterSheet(onDismiss = { perks = false })
            // The other Spotify accounts kept on the phone: switch, add, remove.
            var accounts by remember { androidx.compose.runtime.mutableStateOf(false) }
            if (accounts) com.music.spotui.ui.components.AccountsSheet(onDismiss = { accounts = false }, onLeave = { accounts = false; onDismiss() })

            Spacer(Modifier.height(8.dp))
            ProfileGroup(listOf(
                // Sound quality, downloads, data, battery, display: one page.
                ProfileRow(di.Tune, stringResource(R.string.settings_title), stringResource(R.string.settings_detail), s.soft, s.onSoft) {
                    onOpen(com.music.spotui.ui.navigation.Routes.Settings.route)
                },
                // In-app recap: every listen on this phone, Spotify, downloads and local files.
                ProfileRow(di.Wave, stringResource(R.string.recap_title), stringResource(R.string.recap_detail), s.pink, s.onPink) {
                    onOpen(com.music.spotui.ui.navigation.Routes.Recap.route)
                },
                // The account's own listening history, as Spotify keeps it: beside the recap (the local counter).
                ProfileRow(outline.ProfileHistoryIcon, stringResource(R.string.recents_title), stringResource(R.string.recents_detail), s.soft, s.onSoft) {
                    onOpen(com.music.spotui.ui.navigation.Routes.Recent.route)
                },
                ProfileRow(
                    outline.ProfileExtensionIcon, stringResource(R.string.extensions_title),
                    if (installed.isEmpty()) stringResource(R.string.extensions_row_detail)
                    else stringResource(R.string.extensions_row_count, installed.size, installed.count { it.enabled }),
                    s.pink, s.onPink,
                ) { onOpen(com.music.spotui.ui.navigation.Routes.Extensions.route) },
            ), firstBadge = 0)
            ProfileGroup(buildList {
                // The Spotify account page (plan, privacy, devices), in the app's signed-in browser.
                add(ProfileRow(di.Profile, stringResource(R.string.account_page_title), stringResource(R.string.account_page_detail), s.soft, s.onSoft) {
                    onDismiss()
                    com.music.spotui.ui.screens.AccountBrowser.open(com.music.spotui.di.Graph.app, com.music.spotui.ui.screens.AccountBrowser.ACCOUNT_URL)
                })
                add(ProfileRow(di.Heart, stringResource(R.string.support_title), stringResource(R.string.support_row_detail), s.coral, s.onCoral) { support = true })
                add(ProfileRow(outline.ProfileInfoIcon, stringResource(R.string.creator_biography_section_title), stringResource(R.string.about_detail), s.pink, s.onPink) { about = true })
                if (com.music.spotui.data.api.SupportReminder.PERKS_ENABLED)
                    add(ProfileRow(outline.ProfileStarIcon, stringResource(R.string.supporter_perks_title), stringResource(R.string.supporter_perks_detail), s.soft, s.onSoft) { perks = true })
            }, firstBadge = 1)
            ProfileGroup(listOf(
                ProfileRow(outline.ProfileSwitchIcon, stringResource(R.string.accounts_switch_row), com.music.spotui.ui.components.switchAccountDetail(), s.soft, s.onSoft) {
                    accounts = true
                },
                ProfileRow(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ProfileLogoutIcon, stringResource(R.string.connect_device_menu_logout), null,
                    s.surfaceContainerHighest, s.onSurface) {
                    onDismiss()
                    com.music.spotui.di.SessionActions.logout(com.music.spotui.di.Graph.app)
                },
            ), firstBadge = 2)
        }
    }
}


