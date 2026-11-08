package com.music.spotui.ui.navigation

import app.myspoti.engine.R
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppShade

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.spotui.ui.components.MiniPlayer

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding

/**
 * What re-tapping the tab you are already on does in the official app: Home and Library
 * scroll their list back to the top without reloading anything, Search focuses its field.
 * The bar only sees the tap; the list state and the text field live inside the screens,
 * so the tap is published here and each screen collects its own event.
 */
object TabReselect {
    // replay = 0 + a one-slot buffer: a tap is never held back, and a tap that lands while
    // the screen is not composed is simply dropped rather than replayed on the next visit.
    private val _home = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    private val _library = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    private val _search = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    val home: kotlinx.coroutines.flow.SharedFlow<Unit> = _home
    val library: kotlinx.coroutines.flow.SharedFlow<Unit> = _library
    val search: kotlinx.coroutines.flow.SharedFlow<Unit> = _search

    fun emit(route: String) {
        when (route) {
            Routes.Home.route -> _home.tryEmit(Unit)
            Routes.Library.route -> _library.tryEmit(Unit)
            Routes.Search.route -> _search.tryEmit(Unit)
        }
    }
}

@Composable
fun MainBottomNavigation(navController: NavHostController) {

    // Visibility follows the screen actually shown (not flags each screen sets,
    // which raced during transitions and left the mini player over the full one).
    val shownRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val fullScreen = shownRoute == Routes.Player.route || shownRoute == Routes.Queue.route
    val hasTrack = com.music.spotui.di.Graph.currentSongState.title.value.isNotBlank()
    // No motion of its own: the bar sits above the pages, so sliding it while the
    // player rises (or collapses behind it) made one gesture look like two.
    AnimatedVisibility(
        visible = !fullScreen,
        enter = androidx.compose.animation.EnterTransition.None,
        exit = androidx.compose.animation.ExitTransition.None,
        content = {
            val scheme = com.music.spotui.ui.theme.SystemColors.scheme
            val large = com.music.spotui.ui.components.windowSize().large
            Column(Modifier.fillMaxWidth().then(if (large) Modifier.navigationBarsPadding() else Modifier)) {
                AnimatedVisibility(
                    visible = hasTrack,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    content = {
                        // Large windows: a capped, centred card, not a bar across a desktop window.
                        Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                            Box(if (large) Modifier.widthIn(max = com.music.spotui.ui.components.WindowSize.MaxMiniPlayerWidth) else Modifier) {
                                MiniPlayer()
                            }
                        }
                    }
                )
                val tabs = rememberTabNavigation(navController)
                // Large windows: the tabs are in the rail at the side (MainNavigationRail).
                if (!large) {
                    // The design's bar: a solid band in the card colour down to the screen's edge,
                    // three tabs, the selected one's icon on a filled pill above a bold label.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(scheme.surfaceContainerHigh)
                            .navigationBarsPadding()
                            .height(NavBarHeight)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Top,
                    ) {
                        TabItems.forEach { item ->
                            NavBarTab(
                                item = item,
                                selected = tabs.selected == item.route,
                                onClick = { tabs.open(item) },
                            )
                        }
                    }
                }
            }
        }
    )
}

// Friend activity opens from Home's header (as in Spotify), not from a tab.
/** The tabs, in the order an extension's look sets (`ui.order.nav`) or the app's own. */
private val TabItems: List<Routes>
    get() {
        val tabs = mapOf("home" to Routes.Home, "search" to Routes.Search, "library" to Routes.Library)
        return com.music.spotui.extensions.ExtensionLook.navOrder?.mapNotNull { tabs[it] }?.takeIf { it.size == tabs.size }
            ?: listOf(Routes.Home, Routes.Search, Routes.Library)
    }

/** The tab shown (also on its sub-pages) and what a tap on a tab does; shared by the bar and the rail. */
private class TabNavigation(private val navController: NavHostController, private val state: androidx.compose.runtime.MutableState<String>, private val currentRoute: String?) {
    val selected: String get() = state.value

    fun open(item: Routes) {
        // Standard bottom-tab navigation: each tab keeps its
        // state (no reload, scroll kept) when you come back.
        if (item.route == state.value) {
            if (currentRoute == item.route) {
                // Already on the tab's own page: nothing to reload, only
                // the re-tap shortcut (scroll to top, or focus search).
                TabReselect.emit(item.route)
            } else if (!navController.popBackStack(item.route, inclusive = false)) {
                // On one of its sub-pages: back to its root page first.
                navController.navigate(item.route) { launchSingleTop = true }
            }
        } else {
            state.value = item.route
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                // Home always opens Home itself (its scroll kept), not the
                // page last left open from it (a playlist, an artist…).
                restoreState = item.route != Routes.Home.route
            }
        }
    }
}

@Composable
private fun rememberTabNavigation(navController: NavHostController): TabNavigation {
    val navStack by navController.currentBackStackEntryAsState()
    val currentRoute = navStack?.destination?.route
    // The tab the user is in, even on one of its sub-pages (album, artist…).
    val selectedTab = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(Routes.Home.route) }
    if (currentRoute in TabItems.map { it.route }) selectedTab.value = currentRoute!!
    return TabNavigation(navController, selectedTab, currentRoute)
}

/**
 * Large windows (tablets, foldables, DeX): the three tabs in a rail down the left side instead
 * of a phone bar stretched across the window. Hidden with the bar on the full-screen routes.
 */
@Composable
fun MainNavigationRail(navController: NavHostController) {
    val shownRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    if (shownRoute == Routes.Player.route || shownRoute == Routes.Queue.route) return
    val tabs = rememberTabNavigation(navController)
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(96.dp)
            .background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabItems.forEach { item ->
            NavBarTab(item = item, selected = tabs.selected == item.route, onClick = { tabs.open(item) })
        }
    }
}

private val NavBarHeight = 72.dp

private fun Routes.designIcon() = when (this) {
    Routes.Home -> com.music.spotui.ui.dots.DesignIcons.Home
    Routes.Search -> com.music.spotui.ui.dots.DesignIcons.Explore
    else -> com.music.spotui.ui.dots.DesignIcons.Library
}

@Composable
private fun Routes.designLabel(): String = when (this) {
    Routes.Search -> stringResource(R.string.nav_explore)
    else -> if (labelRes != 0) stringResource(labelRes) else label
}

/**
 * One tab: the icon on a 66×34 pill (filled with the primary colour when selected, springing in),
 * the label under it, black-bold when selected.
 */
@Composable
private fun NavBarTab(
    item: Routes,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = com.music.spotui.ui.theme.SystemColors.scheme
    val label = item.designLabel()
    val pill by androidx.compose.animation.animateColorAsState(if (selected) scheme.primary else Color.Transparent, label = "navPill")
    val pillWidth by androidx.compose.animation.core.animateDpAsState(if (selected) 66.dp else 40.dp, com.music.spotui.ui.dots.bouncy(), label = "navPillWidth")
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = label,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(pillWidth, 34.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(17.dp)).background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.designIcon(),
                contentDescription = null,
                tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        androidx.compose.material3.Text(
            text = label,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
