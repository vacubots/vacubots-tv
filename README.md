# VacuBots TV

The VacuBots TV app for Google TV and Android TV.

- `androidtv/`: the app (Kotlin). See `androidtv/README.md` for the remote keys and how to install it on a TV.
- `pages/`: the wallpaper videos and sounds the app downloads. They are published with the app.
- `.github/workflows/build.yml`: builds the app, signs it with our private key, and publishes it to GitHub Pages at https://vacubots.github.io/vacubots-tv/ (the APK, the update notes the app reads, the videos and the sounds).

The app still shows the VacuBots TV pages from vacubots.com; those pages and the website are in a separate repository.

Signing: run `androidtv/make-signing-key.command` once on the Mac. The key is never committed. Android will not update an app signed with a different key, so losing the key means reinstalling the app on every TV.
