package com.music.spotui.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.pink
import com.music.spotui.ui.theme.onPink

import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.myspoti.engine.R
import kotlinx.coroutines.launch
import com.music.spotui.ui.components.DownloadQualityPicker
import com.music.spotui.ui.components.StreamingQualityPicker
import com.music.spotui.ui.theme.AppBackground

/**
 * Settings, on one page: sound quality (streaming and downloads), the download source,
 * data and battery, display, language, and the player's own repair tools.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding()) {
        // The design's header: a rounded-square back button, then the title big and wide.
        val scheme = com.music.spotui.ui.theme.SystemColors.scheme
        Box(
            Modifier.padding(start = 16.dp, top = 12.dp).size(52.dp).clip(RoundedCornerShape(18.dp)).background(scheme.surfaceContainerHigh)
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center,
        ) { Icon(com.music.spotui.ui.dots.DesignIcons.Back, contentDescription = stringResource(R.string.accessibility_navigate_back), tint = scheme.onSurface, modifier = Modifier.size(22.dp)) }
        Text(
            stringResource(R.string.settings_title), color = scheme.onSurface,
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        )
        // One tab per group, so the page is no longer one long list.
        var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
        val tabs = listOf(R.string.settings_section_playback, R.string.settings_section_downloads, R.string.settings_tab_appearance,
            R.string.extensions_title, R.string.settings_tab_about)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            tabs.forEachIndexed { i, title ->
                DesignChip(
                    stringResource(title),
                    if (tab == i) scheme.primary else scheme.surfaceContainerHigh,
                    if (tab == i) scheme.onPrimary else scheme.onSurface,
                ) { tab = i }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            // The first source of each order, when it is an extension announcing settings in that tab.
            val registry by com.music.spotui.extensions.Extensions.registry.collectAsStateWithLifecycle()
            // A source picked in the player for this session leads too ([com.music.spotui.extensions.SessionSource]).
            val session by com.music.spotui.extensions.Extensions.sessionSource.collectAsStateWithLifecycle()
            if (tab == 0) {
            LinksRow()
            Section(R.string.settings_section_playback)
            // An extension above Spotify: its settings, and Spotify's quality (kept, still used for what Spotify plays) leaves.
            val ext = remember(registry, session) { com.music.spotui.extensions.Extensions.sectionPlacement(app.myspoti.extensions.SettingSection.PLAYBACK) }
            ext?.let { ExtensionSectionSettings(it) }
            if (ext?.hidesAppQuality != true) {
                StreamingQualityPicker(title = R.string.quality_streaming_wifi_title)
                StreamingQualityPicker(cellular = true, title = R.string.quality_streaming_cell_title)
            }
            var canvasWifiOnly by remember { mutableStateOf(com.music.spotui.data.preferences.isCanvasWifiOnly(ctx)) }
            OptionRow(
                label = stringResource(R.string.settings_autoplay_title),
                detail = stringResource(R.string.settings_autoplay_detail),
                checked = com.music.spotui.data.preferences.AutoplayPref.on,
                onChecked = { com.music.spotui.data.preferences.AutoplayPref.set(ctx, it) },
            )
            OptionRow(
                label = stringResource(R.string.myspoti_shuffle_title),
                detail = stringResource(R.string.myspoti_shuffle_detail),
                checked = com.music.spotui.data.preferences.AppShufflePref.on,
                onChecked = { com.music.spotui.data.preferences.AppShufflePref.set(ctx, it) },
            )
            CrossfadeRow()
            // The local player's equalizer, speed and silence skipping (never Spotify's own songs).
            Column(
                Modifier.fillMaxWidth().settingsCard().clickable { navController.navigate(com.music.spotui.ui.navigation.Routes.Equalizer.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(stringResource(R.string.equalizer_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.equalizer_settings_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            var keepPlaying by remember { mutableStateOf(app.myspoti.host.android.PlaybackService.keepsPlayingOnClose(ctx)) }
            OptionRow(
                label = stringResource(R.string.settings_keep_playing_title),
                detail = stringResource(R.string.settings_keep_playing_detail),
                checked = keepPlaying,
                onChecked = { keepPlaying = it; app.myspoti.host.android.PlaybackService.setKeepsPlayingOnClose(ctx, it) },
            )
            var headsetSkip by remember { mutableStateOf(app.myspoti.host.android.PlaybackService.skipsOnEpisodes(ctx)) }
            OptionRow(
                label = stringResource(R.string.settings_headset_skip_title),
                detail = stringResource(R.string.settings_headset_skip_detail),
                checked = headsetSkip,
                onChecked = { headsetSkip = it; app.myspoti.host.android.PlaybackService.setSkipsOnEpisodes(ctx, it) },
            )
            // Lyrics: word by word (karaoke) first, or line by line with Spotify's first.
            var wordSync by remember { mutableStateOf(com.music.spotui.data.preferences.isLyricsWordSync(ctx)) }
            OptionRow(
                label = stringResource(R.string.lyrics_word_sync_title),
                detail = stringResource(if (wordSync) R.string.lyrics_word_sync_on else R.string.lyrics_word_sync_off),
                checked = wordSync,
                onChecked = {
                    wordSync = it
                    com.music.spotui.data.preferences.setLyricsWordSync(ctx, it)
                    com.music.spotui.data.api.LyricsApi.wordSync = it
                    com.music.spotui.data.api.LyricsApi.forgetAll()
                },
            )
            // Songs an extension streams, played by Spotify too (unheard): counted there, seen by Connect.
            if (com.music.spotui.extensions.Extensions.sources(app.myspoti.extensions.Action.AUDIO_SOURCE).isNotEmpty())
                OptionRow(
                    label = stringResource(R.string.settings_connect_mirror_title),
                    detail = stringResource(R.string.settings_connect_mirror_detail),
                    checked = com.music.spotui.data.preferences.ConnectMirrorPref.on,
                    onChecked = { com.music.spotui.data.preferences.ConnectMirrorPref.set(ctx, it) },
                )
            // Downloaded songs, played from their files online, played by Spotify too (unheard), as an extension's.
            OptionRow(
                label = stringResource(R.string.settings_download_sync_title),
                detail = stringResource(R.string.settings_download_sync_detail),
                checked = com.music.spotui.data.preferences.DownloadSyncPref.on,
                onChecked = { com.music.spotui.data.preferences.DownloadSyncPref.set(ctx, it) },
            )
            // Songs marked "Not interested": skipped by lists, listed here to be shown again.
            Column(
                Modifier.fillMaxWidth().settingsCard().clickable { navController.navigate(com.music.spotui.ui.navigation.Routes.HiddenSongs.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(stringResource(R.string.hidden_songs_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.hidden_songs_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Divider()
            SourcesBlock(app.myspoti.extensions.Action.AUDIO_SOURCE, navController)

            Section(R.string.settings_section_data)
            OptionRow(
                label = stringResource(R.string.energy_saver_title),
                detail = stringResource(R.string.energy_saver_summary),
                checked = energy.manualSaver,
                onChecked = app.myspoti.power.EnergySettings::setSaver,
            )
            OptionRow(
                label = stringResource(R.string.data_saver_title),
                detail = stringResource(R.string.data_saver_summary),
                checked = com.music.spotui.data.DataSaver.enabled,
                onChecked = com.music.spotui.data.DataSaver::set,
            )
            // Battery optimization off = music and Connect keep going with the screen off.
            var exempt by remember { androidx.compose.runtime.mutableStateOf(app.myspoti.power.BatteryExemption.isExempt(ctx)) }
            androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                exempt = app.myspoti.power.BatteryExemption.isExempt(ctx)
            }
            OptionRow(
                label = stringResource(R.string.background_playback_title),
                detail = stringResource(if (exempt) R.string.background_playback_on else R.string.background_playback_off),
                checked = exempt,
                onChecked = {
                    if (exempt) ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    else app.myspoti.power.BatteryExemption.request(ctx)
                },
            )
            // Samsung's "Sleeping apps" is a second list: the app has to be in Never sleeping apps too.
            if (app.myspoti.power.BatteryExemption.isSamsung()) Column(
                Modifier.fillMaxWidth().clickable { app.myspoti.power.BatteryExemption.openAppSettings(ctx) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.background_playback_open), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.background_playback_samsung), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            }
            if (tab == 1) {
            Section(R.string.settings_section_downloads)
            // Its role quality takes the place of the app's (kept, and still passed to every extension as `quality`).
            val ext = remember(registry) { com.music.spotui.extensions.Extensions.sectionPlacement(app.myspoti.extensions.SettingSection.DOWNLOADS) }
            if (ext?.hidesAppQuality != true) DownloadQualityPicker()
            ext?.let { ExtensionSectionSettings(it) }
            // Where downloads come from: the download-source extensions, in the order they are asked.
            if (com.music.spotui.extensions.Extensions.sources(app.myspoti.extensions.Action.DOWNLOAD_SOURCE).isNotEmpty())
                SourcesBlock(app.myspoti.extensions.Action.DOWNLOAD_SOURCE, navController)
            else Column(
                Modifier.fillMaxWidth().settingsCard().clickable { navController.navigate(com.music.spotui.ui.navigation.Routes.Extensions.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(stringResource(R.string.downloads_source_row_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        if (com.music.spotui.extensions.Extensions.hasDownloadSource()) R.string.downloads_source_row_on
                        else R.string.downloads_source_row_off,
                    ),
                    color = AppMuted, fontSize = 12.sp,
                )
            }
            if (com.music.spotui.data.offline.OfflineStore.tracks.isNotEmpty()) RemoveDownloadsRow()
            }
            if (tab == 2) {
            ColoursSection()

            Section(R.string.settings_section_display)
            OptionRow(
                label = stringResource(R.string.settings_amoled_theme),
                detail = stringResource(R.string.settings_amoled_theme_subtitle),
                checked = energy.blackPreference,
                onChecked = app.myspoti.power.EnergySettings::setBlackBackground,
            )
            OptionRow(
                label = stringResource(R.string.settings_music_video_title),
                detail = stringResource(R.string.settings_music_video_detail),
                checked = com.music.spotui.data.preferences.MusicVideoPref.on,
                onChecked = { com.music.spotui.data.preferences.MusicVideoPref.set(ctx, it) },
            )
            OptionRow(
                label = stringResource(R.string.settings_player_source_title),
                detail = stringResource(R.string.settings_player_source_detail),
                checked = com.music.spotui.data.preferences.PlayerSourceLinePref.on,
                onChecked = { com.music.spotui.data.preferences.PlayerSourceLinePref.set(ctx, it) },
            )
            OptionRow(
                label = stringResource(R.string.settings_hide_dj_title),
                detail = stringResource(R.string.settings_hide_dj_detail),
                checked = com.music.spotui.data.preferences.HideDjPref.on,
                onChecked = { com.music.spotui.data.preferences.HideDjPref.set(ctx, it) },
            )
            OrientationRow()
            LanguageRow()
            }
            if (tab == 3) {
            ExtensionSettingsRows(navController)
            // Third-party extensions: lyrics, a song's audio, scripts reacting to the music.
            Column(
                Modifier.fillMaxWidth().settingsCard().clickable { navController.navigate(com.music.spotui.ui.navigation.Routes.Extensions.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(stringResource(R.string.extensions_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.extensions_row_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            }
            if (tab == 4) {
            Section(R.string.settings_section_app)
            AboutRow()
            // A18: the launch check can be turned off; the manual check below keeps working.
            OptionRow(
                label = stringResource(R.string.settings_auto_update_title),
                detail = stringResource(R.string.settings_auto_update_detail),
                checked = com.music.spotui.data.preferences.AutoUpdateCheckPref.on,
                onChecked = { com.music.spotui.data.preferences.AutoUpdateCheckPref.set(ctx, it) },
            )
            // The hidden Web Player stuck or crashed: rebuild it without leaving the app.
            Column(
                Modifier.fillMaxWidth().clickable {
                    app.myspoti.host.android.MyspotiRuntime.host.reload()
                    // Reported: nothing said the restart happened.
                    android.widget.Toast.makeText(ctx, ctx.getString(R.string.restart_player_done), android.widget.Toast.LENGTH_SHORT).show()
                }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.restart_player_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.restart_player_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            // The report a tester hands us. The engine's log is otherwise reachable only through
            // adb logcat, which the person reporting the bug cannot run: the label is a literal
            // rather than a string resource so this change stays out of the shared string files.
            Column(
                Modifier.fillMaxWidth().clickable {
                    val file = runCatching { app.myspoti.host.android.Logbook.export(ctx) }.getOrNull()
                    if (file != null) shareDiagnosticReport(ctx, file)
                    else android.widget.Toast.makeText(ctx, ctx.getString(R.string.diagnostic_export_failed), android.widget.Toast.LENGTH_SHORT).show()
                }.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.diagnostic_export_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.diagnostic_export_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            }
            // Clear of the mini player and the navigation bar, as every other page (96 dp left the
            // last rows, "Restart the player" among them, under the mini player).
            Spacer(Modifier.height(com.music.spotui.ui.components.bottomBarsSpace()))
        }
    }
}

/**
 * The installed extensions that declare settings (manifest `settings`), each one opening its page,
 * where they are edited. Nothing when none has any.
 */
@Composable
private fun ExtensionSettingsRows(navController: NavController) {
    val registry by com.music.spotui.extensions.Extensions.registry.collectAsStateWithLifecycle()
    val withSettings = registry.extensions.mapNotNull { e ->
        com.music.spotui.extensions.Extensions.manifest(e.id)?.settings?.takeIf { it.isNotEmpty() }?.let { e to it }
    }
    if (withSettings.isEmpty()) return
    Section(R.string.extensions_title)
    withSettings.forEach { (e, settings) ->
        Column(
            Modifier.fillMaxWidth().settingsCard().clickable { navController.navigate(installedRoute(e.id)) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(e.name, color = AppText, fontSize = 16.sp)
            Text(settings.joinToString { it.label }, color = AppMuted, fontSize = 12.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
    Divider()
}

/** The order a source action asks extensions in ([SourceOrderList]); nothing without any such extension. */
@Composable
private fun SourcesBlock(action: app.myspoti.extensions.Action, navController: NavController) {
    val registry by com.music.spotui.extensions.Extensions.registry.collectAsStateWithLifecycle()
    val audio = action == app.myspoti.extensions.Action.AUDIO_SOURCE
    if (remember(registry) { com.music.spotui.extensions.Extensions.sources(action).isEmpty() }) return
    Section(if (audio) R.string.sources_audio_title else R.string.sources_download_title)
    SourceOrderList(action, spotifyLast = audio, look = SourceLook.Classic, onOpen = { navController.navigate(installedRoute(it)) })
    Text(stringResource(if (audio) R.string.sources_audio_footer else R.string.sources_download_footer), color = AppMuted, fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    if (audio) Divider()
}

@Composable
internal fun Section(title: Int) {
    Text(stringResource(title), color = com.music.spotui.ui.theme.SystemColors.scheme.primary, fontSize = 17.sp, fontWeight = FontWeight.Black,
        fontFamily = com.music.spotui.ui.theme.FlexDisplay, modifier = Modifier.padding(start = 26.dp, end = 20.dp, top = 20.dp, bottom = 8.dp))
}

/** A settings row's card: rounded, in the card colour, a little apart from the next. */
internal fun Modifier.settingsCard(): Modifier = this
    .padding(horizontal = 16.dp, vertical = 3.dp)
    .clip(RoundedCornerShape(20.dp))
    .background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh)

/**
 * The design's switch: a 60×36 track outlined (off) or filled (on) in the primary colour, its
 * thumb growing and springing across, a check in it when on.
 */
@Composable
internal fun DesignSwitch(checked: Boolean, enabled: Boolean = true) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val x by androidx.compose.animation.core.animateDpAsState(if (checked) 26.dp else 6.dp, com.music.spotui.ui.dots.bouncy(), label = "switchX")
    val d by androidx.compose.animation.core.animateDpAsState(if (checked) 26.dp else 18.dp, com.music.spotui.ui.dots.bouncy(), label = "switchD")
    Box(
        Modifier.size(60.dp, 36.dp).alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(18.dp))
            .background(if (checked) s.primary else s.surfaceContainerHigh)
            .border(3.dp, if (checked) s.primary else s.outline, RoundedCornerShape(18.dp)),
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = x - 3.dp).size(d).clip(CircleShape)
                .background(if (checked) s.onPrimary else s.outline),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(com.music.spotui.ui.dots.DesignIcons.Check, null, tint = s.primary, modifier = Modifier.size(16.dp))
        }
    }
}

/** A shaped icon badge (circle, square, arch, leaf in turn, in the design's four colours). */
@Composable
internal fun SettingsBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, index: Int) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val colors = listOf(
        s.primary to s.onPrimary,
        s.coral to s.onCoral,
        s.primaryContainer to s.onPrimaryContainer,
        s.pink to s.onPink,
    )
    val (bg, fg) = colors[index % colors.size]
    Box(Modifier.size(44.dp).clip(com.music.spotui.ui.dots.DotShapes.badges[index % 4]).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
    }
}

/** Settings, Links: whether Android opens Spotify links here; a tap goes to where that is allowed. */
@Composable
private fun LinksRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val on = com.music.spotui.data.preferences.rememberLinksEnabled()
    Section(R.string.settings_links)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().settingsCard()
            .clickable { com.music.spotui.data.preferences.LinkHandling.openSettings(ctx) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SettingsBadge(androidx.compose.material.icons.Icons.Outlined.Link, 3)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(stringResource(R.string.links_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(stringResource(if (on) R.string.links_on else R.string.links_off), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        DesignSwitch(on)
    }
}

/** Settings, Colours: where the album and the wallpaper colours apply, and light or dark. */
@Composable
private fun ColoursSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Section(R.string.settings_colours)
    OptionRow(
        label = stringResource(R.string.settings_album_colours),
        detail = stringResource(R.string.settings_album_colours_detail),
        checked = com.music.spotui.ui.theme.ColorPrefs.albumColours,
        icon = androidx.compose.material.icons.Icons.Outlined.Album, badge = 0,
        onChecked = { com.music.spotui.ui.theme.ColorPrefs.setAlbumColours(ctx, it) },
    )
    OptionRow(
        label = stringResource(R.string.settings_system_colours),
        detail = stringResource(R.string.settings_system_colours_detail),
        checked = com.music.spotui.ui.theme.ColorPrefs.systemColours,
        icon = androidx.compose.material.icons.Icons.Outlined.Palette, badge = 1,
        onChecked = { com.music.spotui.ui.theme.ColorPrefs.setSystemColours(ctx, it) },
    )
    val mode = com.music.spotui.ui.theme.ColorPrefs.mode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().settingsCard()
            .clickable {
                val next = com.music.spotui.ui.theme.ThemeMode.entries.let { it[(it.indexOf(mode) + 1) % it.size] }
                com.music.spotui.ui.theme.ColorPrefs.setMode(ctx, next)
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SettingsBadge(androidx.compose.material.icons.Icons.Outlined.PhoneAndroid, 2)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(stringResource(R.string.settings_theme), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(stringResource(when (mode) {
                com.music.spotui.ui.theme.ThemeMode.SYSTEM -> R.string.settings_theme_system
                com.music.spotui.ui.theme.ThemeMode.LIGHT -> R.string.settings_theme_light
                com.music.spotui.ui.theme.ThemeMode.DARK -> R.string.settings_theme_dark
            }), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Divider() = Spacer(Modifier.height(8.dp))

/**
 * "Crossfade": the length of the fade at the end of a song. The label says what it is: the two
 * songs never overlap (the page plays one media element), so this is a fade, not a true
 * crossfade. The default is 0: off, as before.
 */
@Composable
private fun CrossfadeRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pref = com.music.spotui.data.preferences.CrossfadePref
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.settings_crossfade_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_crossfade_detail),
            color = AppMuted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            for (v in listOf(0, 3, 6, 9, 12)) {
                val selected = pref.seconds == v
                Text(
                    if (v == 0) stringResource(R.string.settings_crossfade_off)
                    else stringResource(R.string.settings_crossfade_seconds, v),
                    color = if (selected) com.music.spotui.ui.theme.AppOnAccent else AppText, fontSize = 14.sp,
                    modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(if (selected) com.music.spotui.ui.theme.AppAccent else tone(0xFF2A2A2A))
                        .clickable { pref.set(ctx, v) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Screen rotation: portrait (default), automatic or landscape. */
@Composable
private fun OrientationRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pref = com.music.spotui.data.preferences.OrientationPref
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.settings_rotation_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            for ((value, label) in listOf(
                pref.PORTRAIT to R.string.settings_rotation_portrait,
                pref.AUTO to R.string.settings_rotation_auto,
                pref.LANDSCAPE to R.string.settings_rotation_landscape,
            )) {
                val selected = pref.mode == value
                Text(
                    stringResource(label),
                    color = if (selected) com.music.spotui.ui.theme.AppOnAccent else AppText, fontSize = 14.sp,
                    modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(if (selected) com.music.spotui.ui.theme.AppAccent else tone(0xFF2A2A2A))
                        .clickable { pref.set(ctx, value) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** "App language": Automatic (French or English) or any language with Spotify's labels. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LanguageRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val current = remember { com.music.spotui.data.AppLanguage.current(ctx) }
    var picking by remember { androidx.compose.runtime.mutableStateOf(false) }
    val auto = stringResource(R.string.language_auto)
    fun label(tag: String) = if (tag == com.music.spotui.data.AppLanguage.SYSTEM) auto else com.music.spotui.data.AppLanguage.nativeName(tag)
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth().clickable { picking = true }.padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(stringResource(R.string.settings_item_app_language_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Text(label(current), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (picking) androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { picking = false },
        containerColor = tone(0xFF282828),
    ) {
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            item {
                Text(stringResource(R.string.settings_item_app_language_title), color = AppText, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Text(stringResource(R.string.language_partial_note), color = AppMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
            }
            items(com.music.spotui.data.AppLanguage.AVAILABLE.size) { i ->
                val tag = com.music.spotui.data.AppLanguage.AVAILABLE[i]
                val selected = tag == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                        picking = false
                        if (!selected) {
                            com.music.spotui.data.AppLanguage.apply(ctx, tag)
                            // Screens take the new language when the activity is rebuilt.
                            generateSequence(ctx) { (it as? android.content.ContextWrapper)?.baseContext }
                                .filterIsInstance<android.app.Activity>().firstOrNull()?.recreate()
                        }
                    }.padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(label(tag), color = if (selected) com.music.spotui.ui.theme.AppAccent else AppText, fontSize = 16.sp)
                        if (tag == com.music.spotui.data.AppLanguage.SYSTEM)
                            Text(stringResource(R.string.language_auto_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (selected) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Check, null, tint = com.music.spotui.ui.theme.AppAccent)
                }
            }
        }
    }
}

/** "Remove all downloads", with the space they take; asks first, like the official app. */
@Composable
private fun RemoveDownloadsRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var ask by remember { androidx.compose.runtime.mutableStateOf(false) }
    val size = android.text.format.Formatter.formatShortFileSize(ctx, com.music.spotui.data.offline.OfflineStore.totalBytes())
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth().clickable { ask = true }.padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(stringResource(R.string.settings_item_remove_downloads_title), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.downloads_size, size), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (ask) androidx.compose.material3.AlertDialog(
        onDismissRequest = { ask = false },
        containerColor = tone(0xFF282828),
        title = { Text(stringResource(R.string.settings_storage_remove_downloads_dialog_title), color = AppText) },
        text = { Text(stringResource(R.string.playlist_entity_remove_download_dialog_description), color = AppMuted) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                ask = false
                com.music.spotui.data.offline.OfflineDownloader.removeAll()
            }) { Text(stringResource(R.string.playlist_entity_remove_download_dialog_positive_button), color = com.music.spotui.ui.theme.AppAccent) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { ask = false }) {
                Text(stringResource(R.string.add_to_playlist_button_cancel), color = AppText)
            }
        },
    )
}



@Composable
internal fun OptionRow(label: String, detail: String, checked: Boolean,
    enabled: Boolean = true, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, badge: Int = 0,
    onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().settingsCard().heightIn(min = 66.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChecked)
            .padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (icon != null) { SettingsBadge(icon, badge); Spacer(Modifier.width(14.dp)) }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(label, color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(detail, color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        DesignSwitch(checked, enabled)
    }
}

/**
 * The installed version with a refresh button: it asks GitHub right now (no cache), shows the
 * answer and the time of the last check. A newer release opens the update card on tap.
 */
@Composable
private fun AboutRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val check = com.music.spotui.data.api.rememberUpdateCheckState(ctx)
    // Also what "Later" put aside: the card in the app offers it again from here.
    val live = com.music.spotui.updateRelease.value
    val known = live ?: remember { com.music.spotui.data.api.UpdateChecker.known(ctx) }
    Row(
        Modifier.fillMaxWidth()
            .then(if (known != null) Modifier.clickable { com.music.spotui.updateRelease.value = known } else Modifier)
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_about_version, app.myspoti.engine.BuildConfig.VERSION_NAME), color = AppText, fontSize = 16.sp)
            if (known != null) {
                Text(stringResource(R.string.settings_update_available, known.version), color = com.music.spotui.ui.theme.AppAccent, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                Text(stringResource(R.string.settings_update_available_detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            check.message?.takeIf { known == null || check.isError }?.let {
                Text(it, color = if (check.isError) androidx.compose.material3.MaterialTheme.colorScheme.error else AppText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text(check.lastCheckedText, color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (check.checking) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp, color = AppMuted)
        } else {
            androidx.compose.material3.IconButton(onClick = { scope.launch { check.run() } }) {
                androidx.compose.material3.Icon(Icons.Default.Refresh, stringResource(R.string.settings_update_refresh), tint = AppMuted)
            }
        }
    }
}

/**
 * Hands the exported report to the share sheet. The file goes through the app's FileProvider;
 * if the provider refuses the path (external storage absent), the report's own path is shared
 * instead, so the tester still has something to send.
 */
internal fun shareDiagnosticReport(context: android.content.Context, file: java.io.File) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Myspoti diagnostic report")
    }
    val uri = runCatching {
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
    if (uri != null) {
        send.type = "application/octet-stream"
        send.putExtra(android.content.Intent.EXTRA_STREAM, uri)
        send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        send.putExtra(android.content.Intent.EXTRA_TEXT, "Myspoti diagnostic report: ${file.absolutePath}")
    }
    context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}
