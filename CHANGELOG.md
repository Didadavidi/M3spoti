# Changelog

M3spoti's changes. Everything up to Myspoti 2.3.0, which M3spoti is built on, is in
[Myspoti's changelog](https://github.com/kota-zo/myspoti/blob/main/CHANGELOG.md)
(a copy of its detailed history: [docs/CHANGELOG-full.md](docs/CHANGELOG-full.md)).

## Unreleased (based on Myspoti 2.3.0)

**A new design**
- Halftone covers everywhere: each cover becomes two-colour dots or wavy lines, whichever suits the
  picture, in shaped frames (cookies, pills, arches). They move only while music plays, never in
  battery saver or with animations turned off.
- New Home (mix hero, Keep listening, shelves), Explore, Library (big title, chips, shaped grid),
  album and playlist pages with "Tracks | About" tabs and a track search, a new player (morphing
  play button, wavy progress bar, swipe the cover to skip, pull down to close), queue, lyrics and
  settings.
- Two colour systems: your wallpaper's colours, light or dark as your phone, for the app; the
  playing cover's colours for the player, queue and lyrics.
- Roboto Flex type throughout.
- A new logo: an M printed in dots, with the supporter colours as variants and a themed icon.
- The Immersive look and its switch are gone; the classic screens remain underneath the new design.

**Spotify links**
- `open.spotify.com` links (also `intl-xx` ones), `spotify:` links, `spotify.link` short links and
  text shared to the app open the right page; a song opens its album and plays there.
- A prompt offers to make Android open Spotify links here, and Settings → Links shows the status.

**Languages**
- The new design is translated into all 11 languages: English, German, French, Spanish, Italian,
  Portuguese, Brazilian Portuguese, Greek, Russian, Polish and Chinese (Simplified).

**Fixes found on a phone**
- Status bar icons stay readable on the always-dark player pages and over halftone headers; lists
  no longer scroll under the clock.
- The cover in the player changes when a song is skipped or ends.
- The play button's ring follows its shape when paused.
- Closing the queue opened from the player goes back to the player.
- Long titles fit (Library) or are hyphenated (Explore tiles); lyrics show a note on pauses instead of
  an empty line; dates use your language's format.

**Under the hood**
- Its own app ids (`app.m3spoti`, `app.m3spoti.stable`, `app.m3spoti.dev`): it installs next to the
  official Myspoti instead of clashing with it.
- Debug builds are signed with one public project key, so a build from any machine installs over the
  previous one.
- The support popup asks you to support Myspoti's developer, who did the real work.
- CI only builds and tests; there are no signed releases yet.
