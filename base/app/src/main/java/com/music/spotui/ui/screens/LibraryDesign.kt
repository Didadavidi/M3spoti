package com.music.spotui.ui.screens

import com.music.spotui.ui.components.PressScale
import com.music.spotui.ui.components.squeezeButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.spotui.data.api.Api
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.theme.SystemColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Library entries that play as a Spotify list (not the app's folders, files or shows). */
internal fun LibraryEntry.playable(): Boolean = !isFolder && when {
    spotifyId == Api.HomeCache.LIKED_SONGS_ID -> true
    spotifyId.startsWith(LOCAL_PLAYLIST_PREFIX) || spotifyId.startsWith(Api.HomeCache.SHOW_PREFIX) -> false
    spotifyId in setOf(Api.HomeCache.LOCAL_FILES_ID, Api.HomeCache.DOWNLOADS_ID, Api.HomeCache.EPISODES_ID, Api.HomeCache.NEW_EPISODES_ID) -> false
    else -> spotifyId.isNotBlank()
}

/**
 * "Shuffle all": one of the lists shown, picked at random, played on shuffle from a random song
 * (Spotify then keeps shuffling within that list).
 */
internal suspend fun shuffleLibrary(entries: List<LibraryEntry>) {
    val pick = entries.filter { it.playable() }.randomOrNull() ?: return
    val api = Graph.api
    val flow = when {
        pick.spotifyId == Api.HomeCache.LIKED_SONGS_ID -> api.getLikedPage()
        pick.isPlaylist -> api.getPlaylistPage(pick.spotifyId)
        else -> api.getAlbumPage(pick.spotifyId)
    }
    val page = (withContext(Dispatchers.IO) { flow.first { it !is Response.Loading } } as? Response.Success)?.data ?: return
    val songs = page.tracks.filter { it.spotifyTrackId.isNotBlank() }
    val first = songs.randomOrNull() ?: return
    val state = Graph.currentSongState
    SongPlayer.setShuffle(true); state.shuffle.value = true
    state.updateQueue(page.tracks)
    SongPlayer.registerContextName(page.uri, page.name)
    if (page.type == "liked") SongPlayer.registerLikedSongs(page.tracks.map { it.url })
    SongPlayer.playSong(first.url, contextUri = page.uri, songs = page.tracks)
    state.updateSongState(first.coverUri, first.title, first.singer, true, first.id, 0, first.album)
}

/** A design chip: a 40 dp rounded rectangle, optionally with an icon, in the given colours. */
@Composable
internal fun DesignChip(
    label: String,
    container: Color,
    content: Color,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .height(42.dp)
            // Springs under the finger, its corners squeezing while held.
            .squeezeButton(container, 16.dp, 9.dp, PressScale.ICON, onClick = onClick)
            .padding(horizontal = if (icon != null) 16.dp else 18.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
        Text(label, color = content, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** A round or rounded-square header button in the card colour (the design's top-right actions). */
@Composable
internal fun HeaderButton(icon: ImageVector, label: String, round: Boolean = true, filled: Boolean = false, onClick: () -> Unit) {
    val s = SystemColors.scheme
    Box(
        // The spring first, so the whole button bounces, not only its icon.
        Modifier.size(52.dp).pressable(0.92f, onClick = onClick).clip(if (round) CircleShape else RoundedCornerShape(18.dp))
            .background(if (filled) s.primaryContainer else s.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (filled) s.onPrimaryContainer else s.onSurface, modifier = Modifier.size(22.dp)) }
}

/** "Jump back in": the first lists of the library as three big covers in three shapes. */
@Composable
internal fun LibraryJumpBackIn(entries: List<LibraryEntry>, navController: NavController, title: String, open: (LibraryEntry) -> Unit) {
    if (entries.isEmpty()) return
    val s = SystemColors.scheme
    val shapes = listOf(DotShapes.cookie(), DotShapes.Arch, DotShapes.Circle)
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = s.onSurface, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            entries.take(3).forEachIndexed { i, e ->
                Column(Modifier.weight(1f).pressable(0.96f) { open(e) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    LibraryEntryCover(e, Modifier.fillMaxWidth().aspectRatio(1f).clip(shapes[i]), cells = 20f)
                    Text(
                        e.name, color = s.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            repeat(3 - entries.size.coerceAtMost(3)) { Box(Modifier.weight(1f)) }
        }
    }
}
