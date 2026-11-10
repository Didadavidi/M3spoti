# Releasing M3spoti

A release is one tag. Push it, and GitHub Actions (`.github/workflows/android.yml`, job `release`)
tests the app, builds every APK signed with M3spoti's release key, turns the in-app update check on
against this repository, and publishes the release with the APKs and notes from `CHANGELOG.md`.

## Once: give GitHub the key

The key never goes into the repository. GitHub keeps it as encrypted secrets that only the release
job can read. In the repository: **Settings → Secrets and variables → Actions → New repository
secret**, three times:

| Name | Value |
|---|---|
| `M3SPOTI_KEYSTORE_BASE64` | The key file as text. In PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Users\David\M3spoti-Key\m3spoti-release.jks")) \| Set-Clipboard`, then paste. |
| `M3SPOTI_KEYSTORE_PASSWORD` | The key's password. |
| `M3SPOTI_KEY_ALIAS` | `m3spoti` |

Without them the release job stops at "Signing key" with a clear error; nothing is published.

## Each release

1. Write the version's section in `CHANGELOG.md` (`## 2.4.0-beta.2`, or `## 2.4.0`); it becomes the
   release description. Without one, the `## Unreleased` section is used.
2. Get it into `main` (a pull request).
3. Tag `main` and push the tag:

   ```bash
   git tag v2.4.0-beta.2
   git push origin v2.4.0-beta.2
   ```

4. Watch the **Actions** tab; the release appears under **Releases** when the job is green.

## Versions

- `vX.Y.Z-beta.N` is a **pre-release** for the beta app (`app.m3spoti`, "M3spoti Beta"). Betas count
  up: `beta.1`, `beta.2`… A beta must be newer than every earlier tag (`tools/check_tag.py`).
- `vX.Y.Z` is a **release** for the stable app (`app.m3spoti.stable`, "M3spoti"). It installs next
  to the beta as a second app.
- The beta app only offers pre-releases, the stable app only releases.

## What a release carries

`M3spoti-Beta-<version>-ARMv8.apk` (64-bit phones), `-ARMv7.apk` (32-bit) and `-universal.apk`
(both). Stable releases drop `-Beta`. The app's update check
picks the one for its phone by these names (`UpdateChecker.apkAsset`), so keep them.

## The TV design

The new TV screens live on the `tv` branch, not on `main`, so a release from `main` carries no TV
APK. The TV preview releases are built by hand from `tv` (`assembleRelease collectApks`, then its
`-TV.apk`).

## The key

`C:\Users\David\M3spoti-Key` (the `.jks` and `keystore.properties`) signs local release builds
too. Keep a backup: an app signed with a lost key can never be updated, only reinstalled.
