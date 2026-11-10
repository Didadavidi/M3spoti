package com.music.spotui.ui.screens

import androidx.compose.foundation.clickable
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.deep
import com.music.spotui.ui.theme.onDeep
import com.music.spotui.ui.theme.headerTint
import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppAccent

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.navigationBarsPadding
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.data.local.LocalMusic
import com.music.spotui.data.local.LocalTrack
import com.music.spotui.di.LocalPlayer
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.theme.AppBackground

/**
 * "Local Files": the device's own audio files, played by [LocalPlayer], in the design of an album
 * page: the wallpaper colours' dot field behind a halftone disc, the title big in the primary
 * colour, the actions as chips (play, shuffle, folders), a pill to find a song, and the songs as
 * one rounded card group with their covers in halftone.
 */
@Composable
fun LocalFilesScreen(navController: NavController) {
    val context = LocalContext.current
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    var granted by remember { mutableStateOf(LocalMusic.hasPermission(context)) }
    var askedOnce by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<List<LocalTrack>?>(null) }
    var find by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok; askedOnce = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = LocalMusic.hasPermission(context) }
    LaunchedEffect(granted) { if (granted) tracks = LocalMusic.tracks(context, refresh = true) }

    val np by SongPlayer.nowPlaying
    val playingLocal = LocalPlayer.active.value
    var adding by remember { mutableStateOf<LocalTrack?>(null) }
    adding?.let { t -> AddLocalToPlaylistSheet(t, onDismiss = { adding = null }) }
    var choosingFolders by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (choosingFolders) LocalFoldersSheet(
        onChanged = { scope.launch { tracks = LocalMusic.tracks(context) } },
        onDismiss = { choosingFolders = false },
    )

    Box(Modifier.fillMaxSize().background(s.surface)) {
        when {
            !granted -> EmptyState(
                title = stringResource(R.string.local_files_empty_section_title),
                subtitle = stringResource(R.string.local_files_empty_section_subtitle),
                button = stringResource(if (askedOnce) R.string.local_files_empty_section_button_go_to_settings else R.string.local_files_empty_section_dialog_permission_rationale_accept),
            ) {
                if (askedOnce) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                else launcher.launch(LocalMusic.permission)
            }
            tracks?.isEmpty() == true -> EmptyState(
                title = stringResource(R.string.local_files_empty_section_no_files_title),
                subtitle = stringResource(R.string.local_files_none_subtitle),
                button = null,
            ) {}
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item(key = "head") {
                    Box(Modifier.fillMaxWidth()) {
                        // The dot field runs on behind the title, as on an album page.
                        Box(Modifier.fillMaxWidth().height(360.dp)) {
                            com.music.spotui.ui.dots.DotFieldBackground(
                                field = com.music.spotui.ui.dots.DotField.PAGE_HEADER,
                                inkA = s.primary, inkB = s.coral, step = 8f, animate = playingLocal,
                            )
                        }
                        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                            Spacer(Modifier.height(64.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                val disc = com.music.spotui.ui.dots.DotShapes.cookie(10, 0.1f)
                                com.music.spotui.ui.dots.HalftoneCover(null, Modifier.size(168.dp), disc, cells = 28f, animate = playingLocal, fallbackSeed = "local-files")
                                Box(Modifier.size(64.dp).clip(CircleShape).background(s.primary), contentAlignment = Alignment.Center) {
                                    Icon(com.music.spotui.ui.dots.DesignIcons.Library, null, tint = s.onPrimary, modifier = Modifier.size(30.dp))
                                }
                            }
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                                com.music.spotui.ui.components.FitText(
                                    stringResource(R.string.local_files_title), fontSize = 36.sp, minSize = 24.sp, fontWeight = FontWeight.Black,
                                    maxLines = 2, color = s.primary, fontFamily = com.music.spotui.ui.theme.FlexDisplay,
                                )
                                val n = tracks?.size ?: 0
                                Text(pluralStringResource(R.plurals.local_files_count, n, n), color = s.onSurface, fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                            }
                            // The actions as the design's chips: play (morphing), shuffle, the folders shown.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                            ) {
                                val playing = playingLocal && SongPlayer.isPlaying()
                                com.music.spotui.ui.dots.MorphPlayButton(
                                    playing = playing,
                                    onClick = {
                                        when {
                                            playing -> SongPlayer.pause()
                                            playingLocal -> SongPlayer.play()
                                            else -> tracks?.let { if (it.isNotEmpty()) LocalPlayer.play(it, 0) }
                                        }
                                    },
                                    container = s.primary, content = s.onPrimary,
                                    width = 56.dp, playingRadius = 28.dp, pausedRadius = 18.dp, iconSize = 24.dp,
                                    label = stringResource(if (playing) R.string.content_description_pause_button else R.string.content_description_play_button),
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(s.coral)
                                        .pressable(0.92f) { tracks?.let { if (it.isNotEmpty()) LocalPlayer.play(it, it.indices.random(), shuffle = true) } },
                                ) {
                                    Icon(com.music.spotui.ui.dots.DesignIcons.Shuffle, stringResource(R.string.play_mode_item_shuffle_title),
                                        tint = s.onCoral, modifier = Modifier.size(22.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(50.dp).clip(RoundedCornerShape(25.dp)).background(s.surfaceContainerHigh)
                                        .pressable(0.95f) { choosingFolders = true }.padding(start = 16.dp, end = 20.dp),
                                ) {
                                    Icon(com.music.spotui.ui.dots.DesignIcons.Filter, null, tint = s.onSurface, modifier = Modifier.size(20.dp))
                                    Text(stringResource(R.string.local_folders_button), color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        // The back button over the dots, round in the deep colour.
                        com.music.spotui.ui.theme.StatusBarScrim(s.surface)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp).size(52.dp).clip(CircleShape)
                                .background(s.deep()).pressable { navController.navigateUp() },
                        ) {
                            Icon(com.music.spotui.ui.dots.DesignIcons.Back, stringResource(R.string.accessibility_navigate_back), tint = s.onDeep(), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                item(key = "find") { LocalFindField(find, { find = it }, stringResource(R.string.local_files_find_hint)) }
                val shown = tracks.orEmpty().filter { com.music.spotui.ui.components.matchesFind(find, it.title, it.artist, it.album) }
                if (shown.isEmpty() && find.isNotBlank()) item(key = "find-empty") {
                    com.music.spotui.ui.components.FindInListEmpty(stringResource(R.string.assisted_curation_search_empty_view_no_result_title, find.trim()))
                }
                itemsIndexed(shown, key = { _, t -> t.id }) { i, t ->
                    LocalTrackRow(
                        t, first = i == 0, last = i == shown.lastIndex,
                        current = playingLocal && np.uri == "local:${t.id}",
                        // The whole folder plays from this song, even when the list is filtered.
                        onClick = { tracks?.let { all -> LocalPlayer.play(all, all.indexOf(t).coerceAtLeast(0)) } },
                        onAdd = { adding = t },
                    )
                }
                item { Spacer(Modifier.height(com.music.spotui.ui.components.bottomBarsSpace())) }
            }
        }
        if (!granted || tracks?.isEmpty() == true) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp).size(52.dp).clip(CircleShape)
                .background(s.deep()).pressable { navController.navigateUp() },
        ) {
            Icon(com.music.spotui.ui.dots.DesignIcons.Back, stringResource(R.string.accessibility_navigate_back), tint = s.onDeep(), modifier = Modifier.size(22.dp))
        }
    }
}

/** The pill to find a song in the list, as the design's search field. */
@Composable
private fun LocalFindField(query: String, onQuery: (String) -> Unit, hint: String) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp)
            .clip(RoundedCornerShape(26.dp)).background(s.surfaceContainerHigh).padding(horizontal = 18.dp),
    ) {
        Icon(com.music.spotui.ui.dots.DesignIcons.Search, null, tint = s.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(start = 12.dp)) {
            if (query.isEmpty()) Text(hint, color = s.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            androidx.compose.foundation.text.BasicTextField(
                query, onQuery, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(s.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) Icon(com.music.spotui.ui.dots.DesignIcons.Close, stringResource(R.string.bottom_actions_close), tint = s.onSurfaceVariant,
            modifier = Modifier.size(32.dp).clip(CircleShape).pressable { onQuery("") }.padding(6.dp))
    }
}

/**
 * One file in the songs' card group (big corners at the group's ends, small between): its cover
 * in halftone (the design's art when the file has none), title and artist, and "add to playlist".
 */
@Composable
private fun LocalTrackRow(t: LocalTrack, first: Boolean, last: Boolean, current: Boolean, onClick: () -> Unit, onAdd: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val big = 26.dp
    val small = 6.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.5.dp)
            .clip(RoundedCornerShape(if (first) big else small, if (first) big else small, if (last) big else small, if (last) big else small))
            .background(s.surfaceContainerHigh).pressable(0.98f, onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        com.music.spotui.ui.dots.HalftoneCover(t.artUri.toString(), Modifier.size(52.dp), RoundedCornerShape(16.dp), cells = 12f, fallbackSeed = t.title)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            if (current) Text(
                stringResource(R.string.home_now_playing).uppercase(), color = s.primary, fontSize = 12.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontFamily = com.music.spotui.ui.theme.FlexDisplay,
            )
            Text(t.title, color = if (current) s.primary else s.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                fontFamily = com.music.spotui.ui.theme.FlexTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.artist.isNotBlank()) Text(t.artist, color = s.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(s.surface).pressable(0.92f, onClick = onAdd),
        ) {
            Icon(com.music.spotui.ui.dots.DesignIcons.Plus, stringResource(R.string.context_menu_add_to_playlist), tint = s.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

/** No permission yet, or no files: a halftone disc, the title big, a line, and the one action. */
@Composable
private fun EmptyState(title: String, subtitle: String, button: String?, onClick: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            com.music.spotui.ui.dots.HalftoneCover(null, Modifier.size(150.dp), com.music.spotui.ui.dots.DotShapes.cookie(10, 0.1f), cells = 24f, fallbackSeed = "local-files")
            Box(Modifier.size(58.dp).clip(CircleShape).background(s.primary), contentAlignment = Alignment.Center) {
                Icon(com.music.spotui.ui.dots.DesignIcons.Library, null, tint = s.onPrimary, modifier = Modifier.size(28.dp))
            }
        }
        Text(title, color = s.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
            fontFamily = com.music.spotui.ui.theme.FlexDisplay, lineHeight = 30.sp, modifier = Modifier.padding(top = 24.dp))
        Text(subtitle, color = s.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        if (button != null) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = 24.dp).height(52.dp).pressable(0.96f, onClick = onClick)
                .clip(RoundedCornerShape(26.dp)).background(s.primary).padding(horizontal = 28.dp),
        ) {
            Text(button, color = s.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}


/** The phone's music folders, each shown in Local Files or left out (WhatsApp audio…). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LocalFoldersSheet(onChanged: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var folders by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var version by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { folders = LocalMusic.folders(context) }
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    com.music.spotui.ui.components.LookSheet(onDismissRequest = onDismiss, containerColor = s.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.local_folders_title), color = s.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Black,
                fontFamily = com.music.spotui.ui.theme.FlexDisplay, modifier = Modifier.padding(horizontal = 20.dp))
            Text(stringResource(R.string.local_folders_detail), color = s.onSurfaceVariant, fontSize = 14.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp))
            LazyColumn {
                items(folders.size) { i ->
                    val (folder, count) = folders[i]
                    @Suppress("UNUSED_EXPRESSION") version
                    val shown = !com.music.spotui.data.local.LocalFolders.isExcluded(context, folder)
                    // One card per folder, the design's switch on it; the whole card toggles.
                    val big = 22.dp
                    val small = 6.dp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.5.dp)
                            .clip(RoundedCornerShape(if (i == 0) big else small, if (i == 0) big else small,
                                if (i == folders.lastIndex) big else small, if (i == folders.lastIndex) big else small))
                            .background(s.surfaceContainerHigh)
                            .clickable {
                                com.music.spotui.data.local.LocalFolders.set(context, folder, !shown)
                                version++
                                onChanged()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            val name = folder.trimEnd('/').substringAfterLast('/').ifBlank { folder.ifBlank { "/" } }
                            Text(name, color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = com.music.spotui.ui.theme.FlexTitle,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("$folder · ${pluralStringResource(R.plurals.local_files_count, count, count)}", color = s.onSurfaceVariant,
                                fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        DesignSwitch(shown)
                    }
                }
            }
        }
    }
}
