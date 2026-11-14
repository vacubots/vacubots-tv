# Theme videos (TV app)

The VacuBots TV app downloads these from https://vacubots.github.io/vacubots-tv/videos/ and plays them, silently and looped, behind a theme.
`<theme id>.mp4` replaces that theme's still picture and moving effects. With no file (or no internet) the theme looks as it always did.

Add or swap one without an app release:

    python tools/make_video.py clip.mp4 meadow     # (the script is still in the website repository, and still writes into its theme folder: copy the result here) copies/converts to pages/videos/meadow.mp4 and bumps manifest.json

The script also makes `<theme id>-1080.mp4`, a 1080p copy the app downloads on a screen narrower than 2560 px (most TVs give apps a 1080p screen, where a 4K video only costs decoding time).

Then push to main: the workflow publishes the files to GitHub Pages. Keep clips 8-20 s, 3840x2160 (4K) H.264 at about 14 Mbps (about 15 MB for 9 seconds), and make the end flow into the start.
Credits: see CREDITS.md.
