# Old notes about the TV app (copied from the website repository's HANDOVER.md on 14 November 2026)

The app moved out of vacubots-price-tracker into this repository. These notes are kept as they were written, for history. Some of them describe the
old setup (builds that wrote into the website theme, the committed debug key); the new setup is in the README and the workflow in this repository.

## From "Found, not changed"
- Android TV app: the build now signs with a private key when the repo secrets exist (see "TV app private signing key" below); until then it still uses the committed debug key. The updater still does not check a hash of the APK.

### TV app private signing key (one-time, on your own computer)
**Easiest way (8 Nov 2026):** double-click `androidtv/make-signing-key.command` on the Mac (right-click, Open the first time). It needs Java (Temurin 21 from adoptium.net), makes a random password, makes the key at `~/vacubots-release.keystore` (trash an old one first), and walks through the four GitHub secrets one at a time. Claude cannot save repo secrets from its session, so this has to be done by the owner. Afterwards: have Claude rebuild the app, uninstall the old app on the TV, install the new APK once. Still open: the updater does not check a hash of the downloaded APK.

Manual steps:
1. `keytool -genkeypair -v -keystore vacubots-release.keystore -alias vacubots -keyalg RSA -keysize 2048 -validity 10000` (keep the file and passwords safe, never commit them).
2. In the repo's Settings, Secrets and variables, Actions, add: `VB_KEYSTORE_B64` (the output of `base64 -i vacubots-release.keystore`), `VB_KEYSTORE_PASSWORD`, `VB_KEY_ALIAS` (`vacubots`), `VB_KEY_PASSWORD`.
3. Android will not update an app over one signed with a different key, so the TV must uninstall the old app and install the new one once. After that, updates work as before.

### TV app (`androidtv/`, package com.vacubots.tv)
- It only compiles in GitHub Actions. A merge to main builds, publishes the APK and writes `theme/assets/apk/v<code>.json`, then starts a theme push. The site keeps serving a stale `version.json` (it showed code 8), so the app looks for `v<code>.json` first. TVs update a few minutes after the theme push finishes; run the update check in Settings, or close and reopen the app.
- **Build budget: one build per batch of changes.** Do not dispatch a branch test build and then merge (that builds twice and posts two APKs to Discord). A failed release build publishes nothing, so merging directly is safe. Do not ship every note as its own version. Changes to the TV page (`theme/page-tv-watch.hbs`) or the website need no app build: merge, then run the theme push (`theme-push.yml`, `dry_run=false`).
- The editor wants "merge" to just happen: no asking for PR numbers, no asking permission to do the obvious.
- State on 11 Nov: version 2.91 (code 107). 2.89 moved to target/compile API 36 (AGP 8.9.2, Gradle 8.11.1) and videos then stuck on the logo with a slow app; 2.90 went back to target 34 and it was still broken, so API 36 was not the cause. 2.91 stops the wallpaper video when another screen opens (`onStop`), on the theory that the 4K loop held the streamer's video decoder. Videos started playing again on 11 Nov after 2.91 and a TV-page push that shows YouTube's error code on screen (a grey line near the bottom, and the page stops skipping after 6 errors in a row), but it was never established which of the two fixed it, or whether the TV had simply not yet loaded the new app/page. If videos stick on the logo again, read that grey line first: it names the YouTube error. compileSdk and AGP stay at 36 / 8.9.2.
- The TV page's "hold the first frame so the title can be cleared" attempt (pausing YouTube's player) left videos stuck on black and was removed. The page is now the same as the version that worked, apart from the Start pill only appearing outside the TV app. If a title delay is wanted again, hold the splash before the video loads at all; never pause the player.
- Look and feel decided with the editor: the animated outlines take their colours from the wallpaper (`Glow` object in BrowseActivity.kt); the rainbow colour bar under the menu is gone, replaced by a soft dark gradient from the top; the menu follows Google's 5% safe area; Settings panels have a drop shadow and a dimmed backdrop; the ThievingGinger page uses green moving outlines and shows video titles exactly as written (brand casing like MOVA, LiDAX, iRobot, ECOVACS is house style: never change letter case).
