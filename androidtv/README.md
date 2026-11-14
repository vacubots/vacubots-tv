# VacuBots TV for Google TV

A full-screen WebView on `https://www.vacubots.com/tv/?tv=1`. The page plays through YouTube's own embedded
player (creators keep their views); the app keeps the screen on, goes full screen, adds `VacuBotsTV` to the user
agent, and turns remote keys into the page's keys. The page's TV mode lives in `theme/page-tv-watch.hbs`
(`html.tv-mode`).

## Remote
| Key | Does |
| --- | --- |
| Left / Right | rewind / fast-forward 10 seconds |
| Down | list of what is coming up (pick any video or feed) |
| Up | show the controls |
| Channel and media next/previous | next / previous video |
| OK, Play/Pause | pause and resume |
| Back | leave the app |

## Build
Actions > "Build and publish VacuBots TV" > Run workflow. The APK is on the run's page under Artifacts
(`VacuBotsTV-apk`), and on main it is also published at https://vacubots.github.io/vacubots-tv/ for the in-app updater.
It is signed with our private key (repository secrets; `make-signing-key.command` makes them) and the build stops without it.
Fine for sideloading onto our own TVs, not for Google Play.

## Install on a Google TV
1. On the TV: Settings > System > About, tap "Android TV OS build" seven times for Developer options, then allow
   "Install unknown apps" for the app you will use to open the file (for example Downloader, or Send Files to TV).
2. Get `VacuBotsTV.apk` onto the TV (Send Files to TV from a phone or computer, or a USB stick) and open it.
   With a computer: turn on USB/Network debugging, then `adb connect <tv-ip>` and `adb install VacuBotsTV.apk`.
3. "VacuBots TV" appears in the app row.

## Changing it
The start address is `START_URL` in `MainActivity.kt`. The page the app shows is on the website, in the website repository
(vacubots-price-tracker): its TV mode is `theme/page-tv-watch.hbs` and `html.tv-mode` in `theme/assets/css/screen.css`, and
changes to the page go live with the theme. The app itself only needs rebuilding when `androidtv/` changes.
The app's own downloads (updates, wallpaper videos, sounds) come from `pages/` here, published to
https://vacubots.github.io/vacubots-tv/ by the workflow.
