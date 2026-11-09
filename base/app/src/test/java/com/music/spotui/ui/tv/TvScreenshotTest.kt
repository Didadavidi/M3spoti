/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.tv

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.ArtistOverviewModel
import com.music.spotui.data.entity.ArtistTrackUi
import com.music.spotui.data.entity.ArtistsModel
import com.music.spotui.data.entity.ContextPageModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.immersive.player.ImmersivePlayerActions
import com.music.spotui.ui.immersive.player.ImmersivePlayerState
import com.music.spotui.ui.immersive.player.ImmersiveRepeat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The TV screens drawn on the computer at 1920×1080, with made-up data and no network: the covers
 * stay as placeholders. Run with `./gradlew :app:testDebugUnitTest --tests '*TvScreenshotTest*'`;
 * the images land in app/build/outputs/roborazzi/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land-television-xhdpi", application = Application::class)
class TvScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shoot(name: String, content: @Composable () -> Unit) {
        // The player never idles (its clock, the marquee, the playhead): time is moved by hand.
        rule.mainClock.autoAdvance = false
        rule.setContent(content)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/tv-$name.png")
    }

    private fun song(i: Int, title: String, artist: String) =
        SongsModel(i, title, "Album", artist, "", "spotify:track:$i", durationMs = 180_000 + i * 7_000)

    private val songs = listOf(
        song(1, "Midnight City", "M83"), song(2, "Breathe Deeper", "Tame Impala"), song(3, "Nightcall", "Kavinsky"),
        song(4, "Instant Crush", "Daft Punk, Julian Casablancas"), song(5, "Electric Feel", "MGMT"), song(6, "Redbone", "Childish Gambino"),
        song(7, "The Less I Know the Better", "Tame Impala"), song(8, "Kids", "MGMT"), song(9, "Genesis", "Justice"), song(10, "Odd Look", "Kavinsky, The Weeknd"),
    )

    private fun playlists(n: Int, prefix: String) = List(n) { HomeItem.Playlist("$prefix ${it + 1}", "", "Mix of the week", id = "pl$prefix$it") }

    @Test fun home() = shoot("home") {
        TvFrame(TvTab.Home, bar = true, onSelect = {}) {
            TvHomeShelves("Good evening", listOf(
                HomeSection("", playlists(6, "Daily Mix")),
                HomeSection("Made for you", playlists(9, "Discover")),
                HomeSection("Recently played", playlists(9, "Radio")),
            )) {}
        }
    }

    @Test fun list() = shoot("list") {
        val entry = LibraryEntry("pl1", "Night Drive", "Myspoti", "", isPlaylist = true, uri = "spotify:playlist:pl1")
        TvFrame(TvTab.Library, bar = false, onSelect = {}) {
            TvSongsPage(entry, ContextPageModel("spotify:playlist:pl1", "playlist", "Night Drive", "", ownerName = "Myspoti", tracks = songs), {}, {}, {}, {})
        }
    }

    @Test fun search() = shoot("search") {
        TvFrame(TvTab.Search, bar = true, onSelect = {}) {
            TvSearchResults(SearchResults(
                songs = songs.take(8),
                artists = listOf("M83", "Tame Impala", "Kavinsky", "MGMT", "Justice", "Daft Punk", "Air").mapIndexed { i, n -> ArtistsModel(n, "", id = "a$i") },
                albums = List(8) { AlbumsModel(it, "M83", "", "Album ${it + 1}", "2011", spotifyId = "al$it") },
            ), "Mid", {}, {})
        }
    }

    @Test fun noResults() = shoot("no-results") {
        TvFrame(TvTab.Search, bar = true, onSelect = {}) { TvNoResults("zxqv lights", {}, {}, {}) }
    }

    @Test fun offline() = shoot("offline") {
        TvFrame(TvTab.Home, bar = true, onSelect = {}) { TvRetry {} }
    }

    @Test fun emptyLibrary() = shoot("empty-library") {
        TvFrame(TvTab.Library, bar = true, onSelect = {}) {
            TvMessage("Your library is empty", "Save albums, playlists and artists and they'll show up here.", { TvEmptyArt() },
                "Go to Search" to com.music.spotui.ui.dots.DesignIcons.Search, {}, "Browse Home" to com.music.spotui.ui.dots.DesignIcons.Home, {},
                focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() })
        }
    }

    @Test fun welcome() = shoot("welcome") { TvWelcome {} }

    @Test fun loginHelp() = shoot("login-help") { TvLoginHelp {} }

    @Test fun artist() = shoot("artist") {
        TvFrame(TvTab.Search, bar = false, onSelect = {}) {
            TvArtistPage(ArtistOverviewModel(
                id = "a1", name = "Tame Impala", monthlyListeners = 31_245_871,
                topTracks = songs.map { ArtistTrackUi(it) },
                popularReleases = List(8) { AlbumsModel(it, "Tame Impala", "", "Release ${it + 1}", "2020", spotifyId = "r$it") },
                relatedArtists = listOf("MGMT", "Pond", "Unknown Mortal Orchestra", "Mac DeMarco").mapIndexed { i, n -> ArtistsModel(n, "", id = "r$i") },
            ), {}, {})
        }
    }

    @Test fun player() = shoot("player") {
        TvFrame(TvTab.NowPlaying, bar = false, onSelect = {}) {
            TvPlayerScreen(
                ImmersivePlayerState(title = "Breathe Deeper", artist = "Tame Impala", playing = true, positionMs = { 74_000L }, durationMs = 372_000L,
                    shuffle = true, repeatMode = ImmersiveRepeat.ALL, liked = true, contextName = "Night Drive"),
                ImmersivePlayerActions(),
                com.music.spotui.ui.theme.albumSchemeOf(0xFFE0663A.toInt()),
                onArtist = {}, onDevices = {},
            )
        }
    }

    @Test fun devices() = shoot("devices") {
        TvDevicesList(listOf(
            SongPlayer.ConnectDevice("1", "Salon TV", "TV", isSelf = true, isActive = false),
            SongPlayer.ConnectDevice("2", "Pixel 9", "SMARTPHONE", isSelf = false, isActive = true),
            SongPlayer.ConnectDevice("3", "Kitchen speaker", "CAST_AUDIO", isSelf = false, isActive = false),
        )) {}
    }
}
