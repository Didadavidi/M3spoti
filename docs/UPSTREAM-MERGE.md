# Merging a new Myspoti release into M3spoti

M3spoti is a redesign fork of Myspoti (https://github.com/kota-zo/myspoti).
Upstream ships the features. M3spoti keeps its own design on top.
This guide is how Myspoti 2.4.0 was merged. Use it for 2.5.0 and later.
Replace the version numbers below with the new ones.

## The idea in one minute

- Take upstream's diff between the last release we imported and the new tag.
- Apply it. Most files apply cleanly (for 2.4.0: 563 of 595).
- Where files conflict, keep the M3spoti design and wire upstream's new feature into it.
- Redraw the new upstream screens in the M3spoti design.
- Add the new texts in all 11 languages. Test, check on a phone, merge.

## Things that never change

- The Android project is in `base/`.
- App ids are `app.m3spoti`, `app.m3spoti.stable` and `app.m3spoti.dev`. Never upstream's ids.
- APKs are named `M3spoti-*`. This includes any new build variant upstream adds.
- README, CHANGELOG and CI stay M3spoti's.
- Design texts live in `values/myspoti_strings.xml` (English) and `values-<lang>/design_strings.xml`.
- The design system lives in `ui/dots` (HalftoneCover, DotShapes, DesignIcons, WavyProgress, MorphPlayButton) and `ui/theme` (SystemColors, ExpressiveColor, Type.kt with FlexWide, FlexDisplay, FlexTitle).
- `main` is protected. Changes go in through a pull request.
- The TV design lives on its own `tv` branch. It is merged separately.

## Steps

1. Prepare a branch.
   Start from an up-to-date `main`. Create a branch such as `merge-myspoti-2.5.0`.
   Do not work on `main` or `tv`.

2. Fetch the upstream tag.
   Add upstream as a remote if it is missing: `git remote add upstream https://github.com/kota-zo/myspoti`.
   Then run `git fetch upstream --tags`.

3. Find the last imported upstream commit.
   For 2.4.0 it was 8e8898a (the Myspoti 2.3.0 import).
   For 2.5.0 it is upstream's own tag `v2.4.0`: M3spoti holds everything up to it.
   The diff to apply is then `git diff v2.4.0 v2.5.0` (upstream's tags, after the fetch above).
   Write the new base commit in the merge commit message, so the next merge can find it.

4. Apply the diff.
   Run `git diff <old-upstream-commit> v2.5.0 | git apply -3`.
   The `-3` makes git do a three-way merge and leave conflict markers.
   List the conflicts with `git status` and `git diff --name-only --diff-filter=U`.

5. Resolve the conflicts with the rules in the table below.
   The rule of thumb is: the design wins, the feature gets wired in.
   If upstream adds a setting that the design already covers (accent picker, solid nav bar), leave it out.
   Write down every decision. They go into the merge commit message.

6. Redraw the new screens.
   Upstream's new screens arrive in upstream's look. Redraw each one in the M3spoti design.
   Use the pieces from `ui/dots` and `ui/theme`: halftone covers, deep hero cards, round-badge rows, chips, the three Flex fonts.
   For 2.4.0 this was Welcome, Login help, Accounts, What's New, Friend activity, Live Radio, the radio player, Transitions/AutoMix, SponsorBlock and Song PiP.
   If a feature does not exist on the service side (for example, Spotify has no "share my listening" switch), leave the control out. Do not fake it.
   Do this in a second commit, named like "The 2.5.0 screens in the M3spoti design".

7. Add the texts.
   Add every new string to English in `values/myspoti_strings.xml`.
   Add it to all 11 languages: English, de, es, fr, it, el, pl, ru, zh, pt, and pt-BR/pt-PT (both use `values-pt`).
   Each goes in `values-<lang>/design_strings.xml`.
   Keep texts short. German and Russian run long and get cut.

8. Build and test.
   Build from `base/` with Gradle and run the unit tests.
   Check that the APK is named `M3spoti-*` and the app id is M3spoti's.
   Known failure on Windows only: `RadioFavoritesStoreTest` fails because `File.renameTo` cannot replace an existing file. This is upstream code. Ignore it on Windows, but it must pass elsewhere.
   For 2.4.0, 1,397 unit tests passed.

9. Check on a phone.
   Install the debug build and walk through every new or redrawn screen.
   Check light and dark theme, a long language (German), and landscape for Song PiP.
   Fix what you find in a third commit, named like "Fixes from the check on the phone".
   For 2.4.0 this found a cut title, a card that said too much, and unreadable colours on a deep card.

10. Update the changelog.
    Add the new version to the M3spoti CHANGELOG. Say what changed in plain words.
    Do not copy upstream's changelog over ours.

11. Open a pull request to `main`.
    Push the branch and open the PR. Do not push to `main`.
    Use the merge commit message as the PR text.
    Merge only when the checks below are done.

12. Do the `tv` branch.
    After `main` has the merge, merge `main` into `tv` (or repeat steps 4 to 11 there).
    The TV screens are their own design. Redraw any new upstream TV screens in it.
    Open a separate pull request for it.

## Files that usually conflict, and what wins

| File or area | What wins |
| --- | --- |
| `SettingsScreen.kt` | The design header and chip tabs stay. Upstream's new tabs and pages are wired in. Settings the design already covers stay out. |
| Home screen | The design stays. New upstream buttons become round buttons in the top bar. New filters join the chip row. |
| Album page, queue | Our look stays. Take upstream's new behaviour (Move button, drag handle, stable keys) and draw it in our chips and cards. |
| Explore | Our pill field stays. Add upstream's new input (voice search). |
| `ui/theme` (colours, Type.kt) | Our system and album colours and fonts stay. Upstream's new look options apply on top. |
| `strings.xml` and `values-*` | Our texts win (for example the support popup). Upstream's new strings are added. |
| `build.gradle.kts` | M3spoti ids and `M3spoti-*` APK names win, also for any new build type. Take upstream's new dependencies and version numbers. |
| README, CHANGELOG, CI workflows | Always ours. |
| Everything else, including new files | Take upstream's. Then redraw anything the user sees. |

## Checks before merging

- [ ] No conflict markers left: `git grep -n '^<<<<<<<\|^>>>>>>>'` finds nothing.
- [ ] App ids are `app.m3spoti`, `app.m3spoti.stable`, `app.m3spoti.dev`. Search for `kota` and `myspoti` ids in `build.gradle.kts` and the manifest.
- [ ] APK names start with `M3spoti-`.
- [ ] README, CHANGELOG and CI are still M3spoti's.
- [ ] Every new string exists in all 11 languages.
- [ ] Unit tests pass (apart from the known Windows-only failure).
- [ ] The app builds and starts on a phone.
- [ ] Every new screen uses the M3spoti design, in light and dark.
- [ ] The merge commit message says which upstream commit was imported and lists the conflict decisions.
- [ ] The change goes through a pull request, not a push to `main`.
- [ ] A plan for the `tv` branch is in place.
