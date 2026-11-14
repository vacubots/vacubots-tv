"""Turn a recording into a theme sound: mono, 22050 Hz, loudness-normalised Ogg in pages/sounds/, and bump its version in
manifest.json so the TVs fetch it.   python tools/make_sound.py INPUT NAME [--start S] [--seconds N]"""
import argparse, json, subprocess, sys
from pathlib import Path

NAMES = ["wind", "rain", "surf", "water", "crickets", "deep_wind", "crowd", "piano", "traffic", "meadow", "snowfield"]
OUT = Path(__file__).resolve().parent.parent / "pages" / "sounds"


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("input"); p.add_argument("name", choices=NAMES)
    p.add_argument("--start", type=float, default=0); p.add_argument("--seconds", type=float, default=90)
    a = p.parse_args(argv)
    OUT.mkdir(parents=True, exist_ok=True)
    dest = OUT / f"{a.name}.ogg"
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-ss", str(a.start), "-t", str(a.seconds), "-i", a.input, "-ac", "1", "-ar", "22050",
                    "-af", "loudnorm=I=-20:TP=-3:LRA=7", "-c:a", "libvorbis", "-q:a", "3", str(dest)], check=True)
    mf = OUT / "manifest.json"
    man = json.loads(mf.read_text() or "{}")
    man[a.name] = {"file": dest.name, "v": man.get(a.name, {}).get("v", 0) + 1}
    mf.write_text(json.dumps(man, indent=2) + "\n")
    print(f"{dest} ({dest.stat().st_size // 1024} KB), version {man[a.name]['v']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
