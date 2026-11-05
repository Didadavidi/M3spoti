package com.music.spotui.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyLinksTest {
    private val id = "4uLU6hMCjMI75M1A2tKUQC"

    @Test fun webLinks() {
        assertEquals("spotify:track:$id", SpotifyLinks.uriIn("https://open.spotify.com/track/$id?si=abc123"))
        assertEquals("spotify:album:$id", SpotifyLinks.uriIn("https://open.spotify.com/intl-de/album/$id"))
        assertEquals("spotify:playlist:$id", SpotifyLinks.uriIn("https://play.spotify.com/playlist/$id"))
        assertEquals("spotify:artist:$id", SpotifyLinks.uriIn("https://www.spotify.com/artist/$id"))
        assertEquals("spotify:episode:$id", SpotifyLinks.uriIn("https://open.spotify.com/intl-pt-BR/episode/$id"))
    }

    @Test fun appLinks() {
        assertEquals("spotify:track:$id", SpotifyLinks.uriIn("spotify:track:$id"))
        assertEquals("spotify:playlist:$id", SpotifyLinks.uriIn("spotify:user:someone:playlist:$id"))
        assertEquals("spotify:show:$id", SpotifyLinks.uriIn("spotify:show:$id"))
    }

    @Test fun profilesAndLiked() {
        assertEquals("spotify:user:max.mustermann", SpotifyLinks.uriIn("https://open.spotify.com/user/max.mustermann?si=x"))
        assertEquals("spotify:user:someone", SpotifyLinks.uriIn("spotify:user:someone"))
        assertEquals(SpotifyLinks.LIKED, SpotifyLinks.uriIn("https://open.spotify.com/collection/tracks"))
        assertEquals(SpotifyLinks.LIKED, SpotifyLinks.uriIn("spotify:collection:tracks"))
        assertEquals(SpotifyLinks.LIKED, SpotifyLinks.uriIn("spotify:user:someone:collection"))
    }

    @Test fun insideSharedText() {
        assertEquals("spotify:track:$id", SpotifyLinks.uriIn("Hör dir das an: https://open.spotify.com/track/$id?si=1 🔥"))
        assertNull(SpotifyLinks.uriIn("nur ein Text ohne Link"))
        assertNull(SpotifyLinks.uriIn("https://example.com/track/$id"))
        assertEquals("https://spotify.link/AbC123", SpotifyLinks.shortLinkIn("schau mal https://spotify.link/AbC123 !"))
    }
}
