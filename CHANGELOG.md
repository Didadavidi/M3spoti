# Changelog

M3spoti's changes. Everything up to Myspoti 2.4.0, which M3spoti is built on, is in
[Orpheon's changelog](https://github.com/kota-zo/Orpheon/blob/main/CHANGELOG.md) (Myspoti is now called Orpheon)
(a copy of its detailed history: [docs/CHANGELOG-full.md](docs/CHANGELOG-full.md)).

## 2.4.0-beta.3

**Changed**
- Myspoti is now called Orpheon: the support window, the donate button and the credits in Settings
  say so, and the credit opens Orpheon's page.

**Fixed**
- The update check keeps working if the GitHub repository is renamed again (it asks GitHub by the
  repository's number, not its name).
- The source code link in About opens M3spoti's repository.

## 2.4.0-beta.2

**New**
- Look settings: covers always as dots, always as waves, or the original artwork; backgrounds as
  the moving dots or a still gradient that is lighter on the battery.
- Springy buttons across the app: they squeeze under the finger and bounce back.
- Player: previous and next push each other as you press them; a soft glow of the cover's colour
  under the dots; pulled down, the player follows the finger as a card and glides into the mini player.
- Lyrics: words light up one by one with a little hop, the lines around trail in a light wave, a
  soft tick on each new line (can be turned off in the lyrics menu), duet lines on the right are
  mirrored, and previous / next sit beside play/pause.
- Local Files and the profile sheet in the M3spoti design.
- Credits in Settings > Info: Myspoti, PixelPlayer (the inspiration for the shapes and the springy
  buttons) and PixelMusic.

**Fixed**
- Spotify links to a song open its album page and play there (they played the song alone).
- spotify.link / spoti.fi short links to songs, albums and artists open (they did nothing).
- The player's colours follow the cover again after the screen is rebuilt.
- What's AutoMix? says which songs it can blend.

## Unreleased (based on Myspoti 2.4.0)

**Myspoti 2.4.0, in the new design**
- Everything new in Myspoti 2.4.0: AutoMix, new-release notices and What's New, romanized lyrics,
  several accounts, Friends, Jam by link or QR code, Live Radio, voice search, SponsorBlock for
  podcasts, playlist reordering, speed-ups and fixes.
- Its screens drawn in the design: the welcome and sign-in help, the account list, What's New,
  Friend activity, Live Radio and its player, the Transitions page with the "What's AutoMix?" sheet,
  SponsorBlock, and song picture-in-picture in landscape with the halftone cover.
- Settings in the design's seven tabs (Playback, Sources, Lyrics, Downloads, Look, About,
  Accounts) with a search; Look adds the system font.
- Home gets What's New and Friends buttons in its top bar; Explore gets voice search beside the field.
- The sign-in screens say M3spoti.

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
