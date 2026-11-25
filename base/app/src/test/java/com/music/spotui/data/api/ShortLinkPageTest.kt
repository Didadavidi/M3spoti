package com.music.spotui.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** spotify.link answers with a page, not a redirect: the real link is read from it. */
class ShortLinkPageTest {
    private val id = "4uLU6hMCjMI75M1A2tKUQC"
    private val short = "https://spotify.link/AbCdEfGh"

    @Test fun songInThePage() {
        val page = """<html><head><meta property="og:url" content="https://open.spotify.com/track/$id?si=x"></head>
            <a class="secondary-action" href="https:\/\/open.spotify.com\/track\/$id?si=x&amp;utm_source=copy-link">Open</a></html>"""
        val target = InviteLinks.linkInPage(short, page)
        assertEquals("spotify:track:$id", SpotifyLinks.uriIn(target!!))
    }

    @Test fun albumArtistAndProfile() {
        assertEquals("spotify:album:$id", SpotifyLinks.uriIn(InviteLinks.linkInPage(short, "x open.spotify.com/intl-de/album/$id y")!!))
        assertEquals("spotify:artist:$id", SpotifyLinks.uriIn(InviteLinks.linkInPage(short, "\"https://open.spotify.com/artist/$id\"")!!))
        assertEquals("spotify:user:someone", SpotifyLinks.uriIn(InviteLinks.linkInPage(short, "href=\"https://open.spotify.com/user/someone?si=1\"")!!))
    }

    @Test fun invitationWinsOverOtherLinks() {
        val page = "open.spotify.com/track/$id ... open.spotify.com/playlist/$id?si=a&amp;pt=TOKEN123"
        val invite = InviteLinks.parse(InviteLinks.linkInPage(short, page)!!)
        assertEquals(id, invite?.playlistId)
        assertEquals("TOKEN123", invite?.token)
    }

    @Test fun aRedirectThatLandedIsTakenAsItIs() {
        val final = "https://open.spotify.com/album/$id?si=b"
        assertEquals(final, InviteLinks.linkInPage(final, ""))
    }

    @Test fun aDeadLinkSentToSpotifysHomeOpensNothingFromThatPage() {
        assertNull(InviteLinks.linkInPage("https://open.spotify.com/", "featured: open.spotify.com/playlist/$id"))
    }

    @Test fun noLinkNoTarget() {
        assertNull(InviteLinks.linkInPage(short, "<html>Spotify – Web Player</html>"))
    }
}
