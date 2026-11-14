# Theme sounds (TV app)

The VacuBots TV app downloads these from https://vacubots.github.io/vacubots-tv/sounds/ and plays them, looped, behind the wallpaper themes.
A theme with no sound file here (or no internet) is silent. There is no built-in fallback sound.

Swap a sound without an app release: replace the file, raise its `v` in `manifest.json`, push to main (the workflow publishes the files to GitHub Pages).

Files must be Ogg Vorbis, **mono, 22050 Hz**, 30-120 s, already loudness-normalised. Make one with:

    python tools/make_sound.py INPUT.wav wind     # (the script is still in the website repository, and still writes into its theme folder: copy the result here) writes pages/sounds/wind.ogg and bumps manifest.json

To add a sound for another theme: make the file with a new name, add that name to `names` and the theme id to `forTheme` in androidtv/.../Ambience.kt, and add the name to NAMES in tools/make_sound.py.
Current sounds: meadow (Spring Meadow), snowfield (Snowy Pink Sky).

Only use CC0 or commercially-usable licences, and record each in CREDITS.md.
