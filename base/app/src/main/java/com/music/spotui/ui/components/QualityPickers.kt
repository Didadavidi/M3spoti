package com.music.spotui.ui.components

import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppAccent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.spotui.ui.screens.settingsCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.pages.ImmersiveChoiceRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.DownloadQuality
import com.music.spotui.data.preferences.StreamingQuality
import com.music.spotui.data.preferences.getDownloadQuality
import com.music.spotui.data.preferences.getStreamingQuality
import com.music.spotui.data.preferences.setDownloadQuality
import com.music.spotui.data.preferences.setStreamingQuality

/** The streaming levels of the Web Player, one choice; saved and applied at once. */
@Composable
fun StreamingQualityPicker(modifier: Modifier = Modifier, cellular: Boolean = false, title: Int = R.string.quality_streaming_title) {
    val ctx = LocalContext.current
    var current by remember(cellular) { mutableStateOf(getStreamingQuality(ctx, cellular)) }
    QualityGroup(
        title = stringResource(title),
        detail = stringResource(R.string.quality_streaming_detail),
        options = listOf(
            Option(StreamingQuality.AUTO, R.string.quality_streaming_auto, R.string.quality_streaming_auto_detail, Icons.Outlined.Autorenew),
            Option(StreamingQuality.LOW, R.string.quality_streaming_low, R.string.quality_streaming_low_detail, Icons.Outlined.DataUsage),
            Option(StreamingQuality.NORMAL, R.string.quality_streaming_normal, R.string.quality_streaming_normal_detail, Icons.Outlined.GraphicEq),
            Option(StreamingQuality.HIGH, R.string.quality_streaming_high, R.string.quality_streaming_high_detail, Icons.Outlined.HighQuality),
        ),
        selected = current,
        onSelect = { current = it; setStreamingQuality(ctx, it, cellular) },
        modifier = modifier,
    )
}

/** Downloads: off, FLAC CD quality or FLAC hi-res; saved at once. */
@Composable
fun DownloadQualityPicker(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var current by remember { mutableStateOf(getDownloadQuality(ctx)) }
    QualityGroup(
        title = stringResource(R.string.quality_download_title),
        detail = stringResource(R.string.quality_download_detail),
        options = listOf(
            Option(DownloadQuality.OFF, R.string.quality_download_off, R.string.quality_download_off_detail, Icons.Outlined.Block),
            Option(DownloadQuality.SMALL, R.string.quality_download_small, R.string.quality_download_small_detail, Icons.Outlined.Compress),
            Option(DownloadQuality.CD, R.string.quality_download_cd, R.string.quality_download_cd_detail, Icons.Outlined.Album),
            Option(DownloadQuality.HI_RES, R.string.quality_download_hires, R.string.quality_download_hires_detail, Icons.Outlined.HighQuality),
            Option(DownloadQuality.ASK, R.string.quality_download_ask, R.string.quality_download_ask_detail, Icons.AutoMirrored.Outlined.HelpOutline),
        ),
        selected = current,
        onSelect = { current = it; setDownloadQuality(ctx, it) },
        modifier = modifier,
    )
}

private class Option<T>(val value: T, val label: Int, val detail: Int, val icon: ImageVector? = null)

@Composable
private fun <T> QualityGroup(
    title: String, detail: String, options: List<Option<T>>, selected: T,
    onSelect: (T) -> Unit, modifier: Modifier = Modifier,
) {
    // Immersive: the same choices as its Settings pages, a grouped list with a check.
    if (com.music.spotui.ui.navigation.isImmersive()) {
        Column(modifier.fillMaxWidth().padding(horizontal = 20.dp).selectableGroup()) {
            Text(title, style = Immersive.Headline, modifier = Modifier.padding(top = 4.dp))
            Text(detail, style = Immersive.Caption.copy(color = Color.White.copy(alpha = 0.72f)), modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            Column(Modifier.fillMaxWidth().clip(Immersive.Card).background(Immersive.Raised.copy(alpha = 0.92f))) {
                options.forEachIndexed { i, o ->
                    ImmersiveChoiceRow(stringResource(o.label), selected = o.value == selected, detail = stringResource(o.detail), onClick = { onSelect(o.value) })
                    if (i < options.lastIndex) Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(Immersive.Separator))
                }
            }
        }
        return
    }
    // The design's settings cards: a shaped icon badge, the choice and what it means, a round
    // check at the end (was a plain radio list among the cards).
    Column(modifier.fillMaxWidth().selectableGroup()) {
        Text(title, color = AppText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 26.dp, end = 20.dp, top = 4.dp))
        Text(detail, color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 26.dp, end = 20.dp, bottom = 6.dp))
        options.forEachIndexed { i, o ->
            val on = o.value == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().settingsCard()
                    .heightIn(min = 64.dp)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(o.value) })
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                o.icon?.let { com.music.spotui.ui.screens.SettingsBadge(it, i) }
                Column(Modifier.weight(1f).padding(start = if (o.icon != null) 14.dp else 4.dp, end = 10.dp)) {
                    Text(stringResource(o.label), color = AppText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text(stringResource(o.detail), color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                DesignCheck(on)
            }
        }
    }
}

/** The design's round choice mark: an outlined ring, or a filled disc with a check. */
@Composable
private fun DesignCheck(on: Boolean) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Box(
        Modifier.size(30.dp).clip(CircleShape)
            .background(if (on) s.primary else Color.Transparent)
            .border(3.dp, if (on) s.primary else s.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) androidx.compose.material3.Icon(com.music.spotui.ui.dots.DesignIcons.Check, null, tint = s.onPrimary, modifier = Modifier.size(18.dp))
    }
}

/**
 * "Ask each time": a download asks its quality first, once for a whole list. The
 * question is drawn by [DownloadAskHost], at the root, so it outlives the menu that
 * started the download.
 */
object DownloadAsk {
    internal var pending by mutableStateOf<((DownloadQuality) -> Unit)?>(null)
    /** No download extension is installed: the pointer to the Extensions store is shown instead of a download that cannot run. */
    internal var needSource by mutableStateOf(false)
    /** Route the pointer's button asks the app to open (taken by [com.music.spotui.App]). */
    val openRoute = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Runs [start] now with the Settings quality (null), or after the user picked one. */
    fun start(context: android.content.Context, start: (DownloadQuality?) -> Unit) {
        if (com.music.spotui.data.preferences.isDownloadsEnabled(context) && !com.music.spotui.extensions.Extensions.hasDownloadSource()) {
            needSource = true
            return
        }
        if (getDownloadQuality(context) == DownloadQuality.ASK) pending = { start(it) } else start(null)
    }
}

@Composable
fun DownloadAskHost() {
    // A download extension was installed meanwhile: the pointer to the store has done its job.
    val hasSource = com.music.spotui.extensions.Extensions.hasDownloadSource()
    LaunchedEffect(hasSource) { if (hasSource) DownloadAsk.needSource = false }
    if (DownloadAsk.needSource) NeedSourceDialog()
    val action = DownloadAsk.pending ?: return
    val choices = listOf(
        Option(DownloadQuality.SMALL, R.string.quality_download_small, R.string.quality_download_small_detail),
        Option(DownloadQuality.CD, R.string.quality_download_cd, R.string.quality_download_cd_detail),
        Option(DownloadQuality.HI_RES, R.string.quality_download_hires, R.string.quality_download_hires_detail),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { DownloadAsk.pending = null },
        containerColor = tone(0xFF282828),
        title = { Text(stringResource(R.string.quality_download_title), color = AppText) },
        text = {
            Column {
                for (o in choices) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .selectable(selected = false, role = Role.Button, onClick = {
                                DownloadAsk.pending = null
                                action(o.value)
                            })
                            .padding(vertical = 8.dp),
                    ) {
                        Text(stringResource(o.label), color = AppText, fontSize = 16.sp)
                        Text(stringResource(o.detail), color = AppMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { DownloadAsk.pending = null }) {
                Text(stringResource(R.string.add_to_playlist_button_cancel), color = AppText)
            }
        },
    )
}

/** Downloads come from extensions: say so where the tap happened, and lead to the store. */
@Composable
private fun NeedSourceDialog() {
    val immersive = com.music.spotui.ui.navigation.isImmersive()
    val close = { DownloadAsk.needSource = false }
    val ink = if (immersive) Immersive.Text else AppText
    val accent = if (immersive) Immersive.Accent else AppAccent
    androidx.compose.material3.AlertDialog(
        onDismissRequest = close,
        containerColor = if (immersive) Color(0xFF2C2C2E) else tone(0xFF282828),
        shape = if (immersive) androidx.compose.foundation.shape.RoundedCornerShape(20.dp) else androidx.compose.material3.AlertDialogDefaults.shape,
        title = { Text(stringResource(R.string.downloads_need_source_title), color = ink) },
        text = { Text(stringResource(R.string.downloads_need_source_detail), color = if (immersive) Immersive.TextSecondary else AppMuted, fontSize = 14.sp) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                close()
                DownloadAsk.openRoute.tryEmit(com.music.spotui.ui.navigation.Routes.Extensions.route)
            }) { Text(stringResource(R.string.setup_ext_browse), color = accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = close) { Text(stringResource(R.string.add_to_playlist_button_cancel), color = ink) }
        },
    )
}
