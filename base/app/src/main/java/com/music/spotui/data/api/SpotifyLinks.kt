package com.music.spotui.data.api

/**
 * Every shape a Spotify link comes in, reduced to one Spotify uri:
 * - web links: `https://open.spotify.com/intl-de/album/{id}?si=…` (also www./play.spotify.com),
 * - app links: `spotify:album:{id}`, `spotify:user:{name}:playlist:{id}`,
 * - profiles: `…/user/{id}` → `spotify:user:{id}`,
 * - Liked Songs: `…/collection/tracks`, `spotify:collection(:tracks)` → `spotify:collection:tracks`,
 * anywhere inside a longer text (a message shared with "Share", "Listen to this: https://…").
 * Short links (spotify.link, spoti.fi) carry no id: InviteLinks resolves them first.
 */
object SpotifyLinks {
    private const val ID = "[A-Za-z0-9]{22}"
    private val WEB = Regex("""(?:open|www|play)\.spotify\.com/(?:intl-[A-Za-z-]+/)?(track|album|playlist|artist|show|episode)/($ID)""")
    private val WEB_USER = Regex("""(?:open|www|play)\.spotify\.com/(?:intl-[A-Za-z-]+/)?user/([^/?#\s"'<>]+)(/collection)?""")
    private val WEB_LIKED = Regex("""(?:open|www|play)\.spotify\.com/(?:intl-[A-Za-z-]+/)?collection/tracks""")
    private val APP = Regex("""spotify:(track|album|playlist|artist|show|episode):($ID)""")
    private val APP_USER_PLAYLIST = Regex("""spotify:user:[^:\s]+:playlist:($ID)""")
    private val APP_LIKED = Regex("""spotify:(?:user:[^:\s]+:)?collection(?::tracks)?\b""")
    private val APP_USER = Regex("""spotify:user:([^:\s"'<>]+)""")

    /** The Spotify uri of the first link in [text], or null when there is none. */
    fun uriIn(text: String): String? {
        val t = text.trim()
        WEB.find(t)?.let { return "spotify:${it.groupValues[1]}:${it.groupValues[2]}" }
        if (WEB_LIKED.containsMatchIn(t)) return LIKED
        WEB_USER.find(t)?.let { m ->
            return if (m.groupValues[2].isNotEmpty()) LIKED else "spotify:user:${java.net.URLDecoder.decode(m.groupValues[1], "UTF-8")}"
        }
        APP_USER_PLAYLIST.find(t)?.let { return "spotify:playlist:${it.groupValues[1]}" }
        APP.find(t)?.let { return "spotify:${it.groupValues[1]}:${it.groupValues[2]}" }
        if (APP_LIKED.containsMatchIn(t)) return LIKED
        APP_USER.find(t)?.let { return "spotify:user:${it.groupValues[1]}" }
        return null
    }

    /** A spotify.link / spoti.fi short link in [text] (resolved by its redirect), or null. */
    fun shortLinkIn(text: String): String? = Regex("""https?://(?:spotify\.link|spoti\.fi)/[^\s"'<>]+""").find(text)?.value

    /**
     * Opens a Spotify uri the way a tapped link should: a list, artist, podcast, profile or Liked
     * Songs on its page; a song on its album's page, playing there; an episode plays.
     * App.kt does the opening (it holds the navigation), MainActivity and InviteLinks call this.
     */
    fun open(uri: String) {
        if (uri.startsWith("spotify:episode:")) {
            com.music.spotui.di.SongPlayer.playSong("episode:" + uri.substringAfterLast(':'))
            return
        }
        InviteLinks.page.value = uri
    }

    /** Liked Songs, whichever way the link spelled it. */
    const val LIKED = "spotify:collection:tracks"
}
