package com.music.spotui.ui.components

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer

/**
 * The song in a picture-in-picture window ([com.music.spotui.data.preferences.SongPipPref], off by
 * default): asked for on Discord by people whose phone kills apps in the background, with the
 * lyrics and the playback buttons (the window's own actions: previous, play/pause, next).
 */
object SongPip {
    private const val ACTION = "app.myspoti.PIP_CONTROL"
    private const val EXTRA = "what"

    /** A card a little taller than wide, like the one shared on Discord. */
    // Landscape, as the design's window: the cover's dots across, the title and the wave at its foot.
    private val RATIO = Rational(16, 10)

    fun wanted(): Boolean =
        com.music.spotui.data.preferences.SongPipPref.on && SongPlayer.isPlaying() && Graph.currentSongState.title.value.isNotBlank()

    fun params(c: Context): PictureInPictureParams {
        val playing = Graph.currentSongState.playingState.value
        return PictureInPictureParams.Builder()
            .setAspectRatio(RATIO)
            .setActions(
                listOf(
                    action(c, 0, app.myspoti.host.android.R.drawable.msp_ic_previous, R.string.pip_previous, "prev"),
                    if (playing) action(c, 1, app.myspoti.host.android.R.drawable.msp_ic_pause, R.string.pip_pause, "toggle")
                    else action(c, 1, app.myspoti.host.android.R.drawable.msp_ic_play, R.string.pip_play, "toggle"),
                    action(c, 2, app.myspoti.host.android.R.drawable.msp_ic_next, R.string.pip_next, "next"),
                ),
            )
            .build()
    }

    private fun action(c: Context, code: Int, icon: Int, label: Int, what: String): RemoteAction {
        val intent = Intent(ACTION).setPackage(c.packageName).putExtra(EXTRA, what)
        val pending = PendingIntent.getBroadcast(c, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = c.getString(label)
        return RemoteAction(Icon.createWithResource(c, icon), text, text, pending)
    }

    /** Listens to the window's buttons while it is shown; returns what [Activity.unregisterReceiver] takes. */
    fun listen(activity: Activity): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getStringExtra(EXTRA)) {
                    "prev" -> SongPlayer.previous()
                    "next" -> SongPlayer.next()
                    "toggle" -> if (SongPlayer.isPlaying()) SongPlayer.pause() else SongPlayer.play()
                }
            }
        }
        ContextCompat.registerReceiver(activity, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        return receiver
    }
}

/**
 * What the song's picture-in-picture window shows, in the design: the cover printed in halftone
 * across the whole window (moving while it plays), the album colour fading in from the foot, the
 * title wide with the artist, the wavy progress, and the line being sung on the album-colour pill
 * when the song has lyrics. The previous / play / next buttons are Android's own, over it.
 */
@Composable
fun PipSong() {
    val song = Graph.currentSongState
    val title = song.title.value
    val artist = song.singer.value
    val playing = song.playingState.value
    val album = com.music.spotui.ui.theme.ExpressiveColor.scheme
    val (line, _) = com.music.spotui.ui.screens.rememberPipLyricLines(title, artist)
    val frame = com.music.spotui.ui.components.rememberFrameClock(playing, fps = 4)
    Box(Modifier.fillMaxSize().background(album.surface)) {
        com.music.spotui.ui.dots.HalftoneCover(
            song.coverUri.value.takeIf { it.isNotBlank() }, Modifier.fillMaxSize(), androidx.compose.ui.graphics.RectangleShape,
            cells = 30f, animate = playing, fallbackSeed = title,
        )
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to Color.Transparent, 0.45f to album.surface.copy(alpha = 0.35f), 1f to album.surface.copy(alpha = 0.95f))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp)) {
            if (line != null) Text(
                line, color = album.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp)).background(album.primary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text(title, color = album.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1,
                fontFamily = com.music.spotui.ui.theme.FlexWide, overflow = TextOverflow.Ellipsis)
            Text(artist, color = album.onSurface.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            com.music.spotui.ui.dots.WavyProgress(
                progress = {
                    frame.value
                    val d = com.music.spotui.di.SongPlayer.getDuration()
                    if (d > 0) (com.music.spotui.di.SongPlayer.getCurrentPosition().toFloat() / d).coerceIn(0f, 1f) else 0f
                },
                color = album.primary, track = album.surfaceContainerHigh,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(12.dp),
                amplitude = 2.dp, wavelength = 10.dp, stroke = 3.dp, animate = playing,
            )
        }
    }
}
