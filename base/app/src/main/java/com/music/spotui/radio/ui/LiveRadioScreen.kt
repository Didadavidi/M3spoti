package com.music.spotui.radio.ui

import androidx.compose.foundation.border
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.pink
import com.music.spotui.ui.theme.onPink
import com.music.spotui.ui.theme.deep
import com.music.spotui.ui.theme.onDeep
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.radio.RadioFavoritesStore
import com.music.spotui.radio.RadioPlayer
import com.music.spotui.radio.RadioStation
import com.music.spotui.ui.components.FindInListField
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.headerTint

/**
 * The Live Radio screen of the Classic look ([ImmersiveLiveRadioScreen] is the Immersive one):
 * search the radio-browser.info directory, browse a handful of popular genres, and keep
 * favorites (see [LiveRadioState]). Deliberately its own small screen rather than reusing Library's
 * list/grid machinery — stations have no album, no artist, no duration, so little of that
 * machinery would apply.
 */
@Composable
fun LiveRadioScreen(navController: NavController) {
    val state = rememberLiveRadioState()
    val favorites = RadioFavoritesStore.list()
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    var searching by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(s.surface).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.music.spotui.ui.components.bottomBarsSpace()),
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(s.surfaceContainerHigh).pressable { navController.navigateUp() },
                        contentAlignment = Alignment.Center) {
                        Icon(com.music.spotui.ui.dots.DesignIcons.Back, stringResource(R.string.accessibility_navigate_back), tint = s.onSurface, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(if (searching) s.primary else s.surfaceContainerHigh)
                        .pressable { searching = !searching; if (!searching) state.query = "" },
                        contentAlignment = Alignment.Center) {
                        Icon(com.music.spotui.ui.dots.DesignIcons.Search, stringResource(R.string.live_radio_search_hint),
                            tint = if (searching) s.onPrimary else s.onSurface, modifier = Modifier.size(22.dp))
                    }
                }
                Text(stringResource(R.string.live_radio_title), color = s.onSurface, style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 14.dp))
                if (searching || state.query.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    FindInListField(state.query, { state.query = it }, stringResource(R.string.live_radio_search_hint), Modifier.fillMaxWidth())
                }
            }
        }

        if (favorites.isNotEmpty() && state.browsing) {
            item { SectionTitle(stringResource(R.string.live_radio_favorites)) }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                ) { favorites.forEachIndexed { i, st -> FavoriteCard(st, i) } }
            }
        }

        if (state.genres.isNotEmpty()) item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                RadioChip(stringResource(R.string.filter_chip_all_label), state.genre.isBlank()) { if (state.genre.isNotBlank()) state.pick(state.genre) }
                for (g in state.genres) RadioChip(g.replaceFirstChar { it.uppercase() }, g == state.genre) { state.pick(g) }
            }
        }

        if (!state.browsing) item {
            SectionTitle(
                if (state.query.isNotBlank()) stringResource(R.string.live_radio_search_results)
                else state.genre.replaceFirstChar { it.uppercase() },
            )
        }

        // Said even over the curated picks: what follows them is missing.
        if (state.error) item {
            Text(stringResource(R.string.live_radio_error), color = s.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp))
        }
        val results = state.results
        when {
            state.loading && results == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = s.primary)
                }
            }
            results?.isEmpty() == true && !state.error -> item {
                Text(stringResource(R.string.live_radio_no_results), color = s.onSurfaceVariant, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
            }
            else -> itemsIndexed(results.orEmpty(), key = { _, st -> st.stationUuid }) { i, st ->
                StationRow(st, i)
            }
        }
    }
}

/** What a station shows in big type: a frequency in its name ("Radio 98.4" → 98.4), else its initials. */
internal fun stationMark(name: String): String {
    Regex("""\b(\d{2,3}[.,]\d)\b""").find(name)?.let { return it.groupValues[1].replace(',', '.') }
    val words = name.split(Regex("""[\s\-_/|]+""")).filter { it.isNotBlank() && it.first().isLetterOrDigit() }
    return words.take(3).joinToString("") { it.first().uppercase() }.ifEmpty { name.take(2).uppercase() }
}

/** The card colours, in turn (coral, deep, the soft primary, pink, a light green as in the design). */
@Composable
internal fun stationColors(index: Int): Pair<Color, Color> {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val list = listOf(
        s.coral to s.onCoral,
        // The deep colour reads in the light theme; in the dark one it sank into the card.
        if (com.music.spotui.ui.theme.SystemColors.dark) s.primary to s.onPrimary else s.deep() to s.onDeep(),
        s.primaryContainer to s.onPrimaryContainer,
        s.pink to s.onPink,
        s.tertiaryContainer to s.onTertiaryContainer,
    )
    return list[index % list.size]
}

/** A favorite: a tall card in its colour, "● LIVE", the station's mark set big and narrow, its name at the foot. */
@Composable
private fun FavoriteCard(station: RadioStation, index: Int) {
    val (bg, fg) = stationColors(index)
    Column(
        Modifier.size(width = 136.dp, height = 156.dp).pressable(0.96f) { RadioPlayer.play(station) }
            .clip(RoundedCornerShape(28.dp)).background(bg).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(com.music.spotui.ui.theme.SystemColors.scheme.error))
            Text(stringResource(R.string.live_radio_live).uppercase(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Black,
                letterSpacing = 1.sp, modifier = Modifier.padding(start = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(stationMark(station.name), color = fg, fontFamily = com.music.spotui.ui.theme.FlexCondensed, fontWeight = FontWeight.Bold,
            fontSize = 56.sp, lineHeight = 52.sp, maxLines = 1)
        Text(station.name, color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, lineHeight = 16.sp,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RadioChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Box(
        Modifier.height(40.dp).clip(RoundedCornerShape(16.dp))
            .then(if (selected) Modifier.background(s.primary) else Modifier.border(2.dp, s.outlineVariant, RoundedCornerShape(16.dp)))
            .pressable(0.95f, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) s.onPrimary else s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = com.music.spotui.ui.theme.SystemColors.scheme.primary, fontFamily = com.music.spotui.ui.theme.FlexDisplay,
        fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 8.dp))
}

/**
 * A station as a card: its mark on a tile in its colour (the dancing bars while it plays), its
 * name and "country · genre", and the favourite heart (coral and filled when kept).
 */
@Composable
private fun StationRow(station: RadioStation, index: Int) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    // The station currently loaded in LocalPlayer (playing or paused on), by its synthetic song
    // id — the same signal the mini player, full player and notification already show.
    val currentId by com.music.spotui.di.Graph.currentSongState.songId
    val current = currentId == RadioPlayer.songIdFor(station)
    // Bars only while it is actually playing (not merely loaded/paused), same as any other row.
    val playing = current && com.music.spotui.di.Graph.currentSongState.playingState.value
    val fav = RadioFavoritesStore.favorites.containsKey(station.stationUuid)
    val (bg, fg) = stationColors(index)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).pressable(0.97f) { RadioPlayer.play(station) }
            .clip(RoundedCornerShape(22.dp))
            .background(if (current) s.primaryContainer.copy(alpha = 0.6f) else s.surfaceContainer).padding(10.dp),
    ) {
        Box(Modifier.size(width = 64.dp, height = 52.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
            if (playing) com.music.spotui.ui.immersive.components.ImmersivePlayingBars(Modifier.align(Alignment.Center))
            else Text(stationMark(station.name), color = fg, fontFamily = com.music.spotui.ui.theme.FlexCondensed, fontWeight = FontWeight.Bold,
                fontSize = 30.sp, lineHeight = 30.sp, maxLines = 1)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(station.name, color = if (current) s.primary else s.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = stationSubtitle(station)
            if (subtitle.isNotBlank()) Text(subtitle, color = s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(44.dp).clip(if (fav) RoundedCornerShape(14.dp) else CircleShape)
                .then(if (fav) Modifier.background(s.coral) else Modifier.background(s.surface))
                .pressable(0.9f) { RadioFavoritesStore.toggle(station) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (fav) com.music.spotui.ui.dots.DesignIcons.Heart else com.music.spotui.ui.dots.DesignIcons.HeartOutline,
                contentDescription = stringResource(if (fav) R.string.live_radio_unfavorite else R.string.live_radio_favorite),
                tint = if (fav) s.onCoral else s.onSurface, modifier = Modifier.size(20.dp),
            )
        }
    }
}
