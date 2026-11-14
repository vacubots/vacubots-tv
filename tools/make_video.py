"""Turn a video into a theme wallpaper: 4K (3840x2160) H.264, silent, in pages/videos/<theme>.mp4, plus a 1080p copy
(<theme>-1080.mp4, about a third of the size) for the many TVs that give apps a 1080p screen: a 4K video there is decoded and then
shrunk, which costs a lot and looks no sharper, and some TV chips cannot decode 4K H.264 at all. The app picks the right one. The
version in manifest.json is bumped so the TVs fetch it. A file that is already 4K H.264 with no audio is copied as it is. Anything smaller is scaled up to 4K (with a little sharpening and film grain, so it does not look soft on a 4K TV), but the real fix for a soft clip is a sharper source.
   python tools/make_video.py INPUT.mp4 THEME_ID      (THEME_ID is the theme's id in Wallpapers.kt, e.g. meadow, pinksnow)
Make the clip loop first (the end flowing into the start): the app plays it over and over."""
import argparse, json, shutil, subprocess, sys
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "pages" / "videos"


def probe(path: str) -> dict:
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=codec_type,codec_name,width,height", "-of", "json", path],
                       capture_output=True, text=True, check=True)
    return {s["codec_type"]: s for s in json.loads(r.stdout)["streams"]}


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("input"); p.add_argument("theme")
    a = p.parse_args(argv)
    OUT.mkdir(parents=True, exist_ok=True)
    dest = OUT / f"{a.theme}.mp4"
    s = probe(a.input)
    v = s.get("video", {})
    if v.get("codec_name") == "h264" and v.get("width", 0) >= 3840 and "audio" not in s:
        shutil.copyfile(a.input, dest)
    else:
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", a.input, "-an", "-vf", "scale=3840:2160:flags=lanczos,unsharp=5:5:0.7:5:5:0.0,noise=alls=3:allf=t,fps=24",
                        "-c:v", "libx264", "-crf", "16", "-maxrate", "14M", "-bufsize", "28M", "-preset", "slow", "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(dest)], check=True)
    small = OUT / f"{a.theme}-1080.mp4"
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(dest), "-an", "-vf", "scale=1920:1080:flags=lanczos", "-c:v", "libx264", "-profile:v", "high", "-level", "4.1",
                    "-crf", "22", "-maxrate", "5M", "-bufsize", "10M", "-preset", "slow", "-pix_fmt", "yuv420p", "-r", "24", "-movflags", "+faststart", str(small)], check=True)
    mf = OUT / "manifest.json"
    man = json.loads(mf.read_text() if mf.exists() else "{}")
    n = man.get(a.theme, {}).get("v", 0) + 1
    man[a.theme] = {"file": dest.name, "file1080": small.name, "v": n, "v1080": n}   # v1080 is the 1080p copy's own version, so an app that cannot use it never re-fetches the 4K file for nothing
    mf.write_text(json.dumps(man, indent=2) + "\n")
    print(f"{dest} ({dest.stat().st_size // 1024} KB) and {small.name} ({small.stat().st_size // 1024} KB), version {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
