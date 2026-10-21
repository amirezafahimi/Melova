"""Step 3 — synthetic errors with exact frame-level labels.

Takes clean clips from good singers, analyses them with the WORLD vocoder
(f0 + spectral envelope + aperiodicity), injects controlled errors, and
re-synthesises. Because we change the f0 curve ourselves, every frame's label
(how many cents off, which error type) is exact — no hand labelling needed.

Pitch errors (per note):
  offset      — whole note sharp/flat by 15–80 cents
  drift       — long note sinks (or rises) towards its end
  scoop       — note starts from below/above and slides in late
  overshoot   — passes the target on arrival, then settles (damped oscillation)
  wobble      — unstable, slow random wandering around the note
  wrong_note  — whole note a semitone (±100) or a quarter-tone/koron (±50) off

Voice-quality errors (over a region or the whole clip, level 1–3):
  breathy     — more aperiodic (noise) energy, weaker high harmonics
  rough       — cycle irregularity: f0 jitter + amplitude shimmer

IMPORTANT: a "clean.wav" re-synthesis is written for every clip. Train on
clean.wav vs vNN.wav — never on the original recording vs vNN.wav — otherwise
the model learns "vocoder sound = error" instead of the error itself.

Strain (فشار) and expression (حس) are NOT simulated: they can't be faked
convincingly and must come from human ratings.

Usage:
    python -m melova_dataset.synth_errors --root . --variants 6
    python -m melova_dataset.synth_errors --file clip.wav --out /tmp/demo --variants 4
"""

from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import asdict, dataclass, field
from pathlib import Path

import numpy as np
import pyworld

from melova_dataset.audio import TARGET_SR, load_mono, write_wav

FRAME_MS = 5.0
F0_FLOOR = 65.0
F0_CEILING = 1100.0

PITCH_OPS = ("offset", "drift", "scoop", "overshoot", "wobble", "wrong_note")
QUALITY_OPS = ("breathy", "rough")


@dataclass
class Note:
    start: int  # frame index, inclusive
    end: int  # frame index, exclusive
    center_cents: float

    @property
    def frames(self) -> int:
        return self.end - self.start

    @property
    def duration_ms(self) -> float:
        return self.frames * FRAME_MS


@dataclass
class ErrorEvent:
    type: str
    start_ms: float
    end_ms: float
    params: dict = field(default_factory=dict)


@dataclass
class WorldAnalysis:
    f0: np.ndarray
    sp: np.ndarray
    ap: np.ndarray
    sr: int


def analyse(signal: np.ndarray, sr: int) -> WorldAnalysis:
    x = np.ascontiguousarray(signal, dtype=np.float64)
    f0, times = pyworld.harvest(x, sr, f0_floor=F0_FLOOR, f0_ceil=F0_CEILING, frame_period=FRAME_MS)
    sp = pyworld.cheaptrick(x, f0, times, sr)
    ap = pyworld.d4c(x, f0, times, sr)
    return WorldAnalysis(f0=f0, sp=sp, ap=ap, sr=sr)


def synthesize(f0: np.ndarray, sp: np.ndarray, ap: np.ndarray, sr: int) -> np.ndarray:
    return pyworld.synthesize(
        np.ascontiguousarray(f0), np.ascontiguousarray(sp), np.ascontiguousarray(ap), sr, frame_period=FRAME_MS
    )


# --------------------------------------------------------------------------- notes


def to_cents(f0: np.ndarray) -> np.ndarray:
    cents = np.full(f0.shape, np.nan)
    voiced = f0 > 0
    cents[voiced] = 1200 * np.log2(f0[voiced] / 440.0)
    return cents


def _moving_average(values: np.ndarray, width: int) -> np.ndarray:
    width = max(1, width) | 1
    padded = np.pad(values, width // 2, mode="edge")
    return np.convolve(padded, np.ones(width) / width, mode="valid")


def segment_notes(f0: np.ndarray, split_cents: float = 70.0, min_note_ms: float = 80.0) -> list[Note]:
    """Rough note segmentation on a contour smoothed enough to ignore vibrato."""
    cents = to_cents(f0)
    notes: list[Note] = []
    voiced = np.append(f0 > 0, False)
    start = None
    for i, is_voiced in enumerate(voiced):
        if is_voiced and start is None:
            start = i
        elif not is_voiced and start is not None:
            notes.extend(_split_run(cents, start, i, split_cents, min_note_ms))
            start = None
    return notes


def _split_run(cents: np.ndarray, start: int, end: int, split_cents: float, min_note_ms: float) -> list[Note]:
    smooth = _moving_average(cents[start:end], int(200 / FRAME_MS))
    min_frames = int(min_note_ms / FRAME_MS)
    hold = int(30 / FRAME_MS)
    boundaries = [0]
    anchor = smooth[0]
    i = 1
    while i < len(smooth):
        if abs(smooth[i] - anchor) > split_cents and np.all(
            np.abs(smooth[i : i + hold] - anchor) > split_cents
        ):
            boundaries.append(i)
            anchor = smooth[min(i + hold, len(smooth) - 1)]
            i += hold
            continue
        # follow slow movement inside a note
        anchor = 0.98 * anchor + 0.02 * smooth[i]
        i += 1
    boundaries.append(len(smooth))

    notes = []
    for a, b in zip(boundaries[:-1], boundaries[1:]):
        if b - a < min_frames and notes:
            notes[-1] = Note(notes[-1].start, start + b, notes[-1].center_cents)
            continue
        notes.append(Note(start + a, start + b, float(np.nanmedian(cents[start + a : start + b]))))
    return [n for n in notes if n.frames >= min_frames]


# --------------------------------------------------------------------------- pitch ops


def _ramp_envelope(n: int, ramp_frames: int) -> np.ndarray:
    env = np.ones(n)
    r = min(ramp_frames, n // 2)
    if r > 0:
        shape = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, r))
        env[:r] = shape
        env[-r:] = shape[::-1]
    return env


def op_offset(note: Note, rng: np.random.Generator, **_) -> tuple[np.ndarray, dict]:
    cents = float(rng.uniform(15, 80) * rng.choice([-1, 1]))
    return cents * _ramp_envelope(note.frames, int(25 / FRAME_MS)), {"cents": round(cents, 1)}


def op_drift(note: Note, rng: np.random.Generator, **_) -> tuple[np.ndarray, dict]:
    begin = float(rng.uniform(0.3, 0.6))
    amount = float(rng.uniform(20, 60) * (-1 if rng.random() < 0.8 else 1))
    t = np.linspace(0, 1, note.frames)
    curve = np.clip((t - begin) / (1 - begin), 0, 1) * amount
    return curve, {"cents_at_end": round(amount, 1), "starts_at": round(begin, 2)}


def op_scoop(note: Note, rng: np.random.Generator, **_) -> tuple[np.ndarray, dict]:
    amount = float(rng.uniform(60, 250) * (-1 if rng.random() < 0.8 else 1))
    tau_ms = float(rng.uniform(60, 200))
    t_ms = np.arange(note.frames) * FRAME_MS
    return amount * np.exp(-t_ms / tau_ms), {"start_cents": round(amount, 1), "tau_ms": round(tau_ms)}


def op_overshoot(note: Note, rng: np.random.Generator, previous: Note | None = None, **_) -> tuple[np.ndarray, dict]:
    if previous is not None and abs(note.center_cents - previous.center_cents) > 30:
        direction = float(np.sign(note.center_cents - previous.center_cents))
    else:
        direction = float(rng.choice([-1, 1]))
    amount = float(rng.uniform(40, 120))
    freq = float(rng.uniform(5, 8))
    tau_ms = float(rng.uniform(60, 140))
    t_s = np.arange(note.frames) * FRAME_MS / 1000
    curve = direction * amount * np.exp(-t_s * 1000 / tau_ms) * np.sin(2 * np.pi * freq * t_s + np.pi / 2)
    curve -= curve[0] * np.exp(-t_s * 1000 / 15)  # start smoothly from the arriving pitch
    return curve, {"peak_cents": round(direction * amount, 1), "freq_hz": round(freq, 1), "tau_ms": round(tau_ms)}


def op_wobble(note: Note, rng: np.random.Generator, **_) -> tuple[np.ndarray, dict]:
    std = float(rng.uniform(12, 35))
    noise = rng.standard_normal(note.frames + 200)
    smooth = _moving_average(noise, int(rng.uniform(150, 300) / FRAME_MS))[100 : 100 + note.frames]
    smooth = (smooth - smooth.mean()) / (smooth.std() + 1e-9) * std
    return smooth * _ramp_envelope(note.frames, int(30 / FRAME_MS)), {"std_cents": round(std, 1)}


def op_wrong_note(note: Note, rng: np.random.Generator, **_) -> tuple[np.ndarray, dict]:
    size = float(rng.choice([50, 100]))
    cents = size * float(rng.choice([-1, 1]))
    return cents * _ramp_envelope(note.frames, int(30 / FRAME_MS)), {
        "cents": cents,
        "kind": "quarter_tone" if size == 50 else "semitone",
    }


PITCH_OP_FUNCS = {
    "offset": (op_offset, 250),
    "drift": (op_drift, 600),
    "scoop": (op_scoop, 200),
    "overshoot": (op_overshoot, 250),
    "wobble": (op_wobble, 400),
    "wrong_note": (op_wrong_note, 200),
}


# --------------------------------------------------------------------------- quality ops


def _region(n_frames: int, voiced: np.ndarray, rng: np.random.Generator) -> tuple[int, int]:
    """Whole clip (40%) or a random 0.6–2.5 s region starting on a voiced frame."""
    if rng.random() < 0.4 or n_frames * FRAME_MS < 1500:
        return 0, n_frames
    length = int(rng.uniform(600, 2500) / FRAME_MS)
    candidates = np.flatnonzero(voiced[: max(1, n_frames - length)])
    start = int(rng.choice(candidates)) if candidates.size else 0
    return start, min(n_frames, start + length)


def apply_breathy(world: WorldAnalysis, level: int, start: int, end: int) -> None:
    mix = {1: 0.15, 2: 0.3, 3: 0.5}[level]
    tilt_db = {1: 2.0, 2: 4.0, 3: 6.0}[level]
    freqs = np.linspace(0, world.sr / 2, world.sp.shape[1])
    weight = mix * (0.3 + 0.7 * np.clip(freqs / 4000, 0, 1))
    env = _ramp_envelope(end - start, int(40 / FRAME_MS))[:, None]
    ap = world.ap[start:end]
    world.ap[start:end] = np.clip(ap + (1 - ap) * weight[None, :] * env, 0, 1)
    tilt = 10 ** (-(tilt_db * np.clip(freqs / 5000, 0, 1.5)) / 10)
    world.sp[start:end] *= 1 + (tilt[None, :] - 1) * env


def apply_rough(world: WorldAnalysis, level: int, start: int, end: int, rng: np.random.Generator) -> None:
    jitter_cents = {1: 8.0, 2: 16.0, 3: 30.0}[level]
    shimmer_db = {1: 0.7, 2: 1.4, 3: 2.5}[level]
    n = end - start
    env = _ramp_envelope(n, int(40 / FRAME_MS))
    voiced = world.f0[start:end] > 0
    jitter = rng.standard_normal(n) * jitter_cents * env
    world.f0[start:end][voiced] *= 2 ** (jitter[voiced] / 1200)
    shimmer = _moving_average(rng.standard_normal(n), 2) * shimmer_db * env
    world.sp[start:end] *= (10 ** (shimmer / 10))[:, None]


# --------------------------------------------------------------------------- variants


def _seed(*parts: object) -> int:
    return int(hashlib.sha1("|".join(map(str, parts)).encode()).hexdigest()[:8], 16)


def make_variant(base: WorldAnalysis, notes: list[Note], rng: np.random.Generator) -> tuple[WorldAnalysis, dict]:
    """Return a modified copy plus labels. Recipes: 50% pitch, 25% quality, 25% both."""
    world = WorldAnalysis(base.f0.copy(), base.sp.copy(), base.ap.copy(), base.sr)
    n = len(world.f0)
    voiced = base.f0 > 0
    offset_cents = np.zeros(n)
    pitch_type = np.array([""] * n, dtype=object)
    breathy_level = np.zeros(n, dtype=int)
    rough_level = np.zeros(n, dtype=int)
    events: list[ErrorEvent] = []

    roll = rng.random()
    do_pitch = roll < 0.75
    do_quality = roll >= 0.5

    if do_pitch:
        wanted = int(rng.integers(1, 4))
        order = rng.permutation(len(notes))
        used = 0
        for idx in order:
            if used >= wanted:
                break
            note = notes[idx]
            if np.any(offset_cents[note.start : note.end] != 0):
                continue
            eligible = [op for op, (_, min_ms) in PITCH_OP_FUNCS.items() if note.duration_ms >= min_ms]
            if not eligible:
                continue
            op = str(rng.choice(eligible))
            func, _ = PITCH_OP_FUNCS[op]
            previous = notes[idx - 1] if idx > 0 and notes[idx - 1].end >= note.start - int(150 / FRAME_MS) else None
            curve, params = func(note, rng, previous=previous)
            offset_cents[note.start : note.end] = curve
            pitch_type[note.start : note.end] = op
            events.append(ErrorEvent(op, note.start * FRAME_MS, note.end * FRAME_MS, params))
            used += 1

    if do_quality or not events:
        op = str(rng.choice(QUALITY_OPS))
        level = int(rng.integers(1, 4))
        start, end = _region(n, voiced, rng)
        if op == "breathy":
            apply_breathy(world, level, start, end)
            breathy_level[start:end] = level
        else:
            apply_rough(world, level, start, end, rng)
            rough_level[start:end] = level
        events.append(ErrorEvent(op, start * FRAME_MS, end * FRAME_MS, {"level": level}))

    world.f0[voiced] *= 2 ** (offset_cents[voiced] / 1200)
    offset_cents[~voiced] = 0
    pitch_type[~voiced] = ""

    labels = {
        "frame_ms": FRAME_MS,
        "events": [asdict(e) for e in sorted(events, key=lambda e: e.start_ms)],
        "frames": {
            "voiced": voiced.astype(int).tolist(),
            "offset_cents": np.round(offset_cents, 1).tolist(),
            "pitch_error_type": pitch_type.tolist(),
            "breathy_level": breathy_level.tolist(),
            "rough_level": rough_level.tolist(),
        },
    }
    return world, labels


def generate_for_clip(signal: np.ndarray, sr: int, clip_id: str, out_dir: Path, variants: int) -> list[dict]:
    base = analyse(signal, sr)
    notes = segment_notes(base.f0)
    clean = synthesize(base.f0, base.sp, base.ap, sr)
    target_rms = float(np.sqrt(np.mean(clean**2)) + 1e-12)
    out_dir.mkdir(parents=True, exist_ok=True)
    write_wav(out_dir / "clean.wav", clean, sr)

    entries = []
    for v in range(1, variants + 1):
        rng = np.random.default_rng(_seed(clip_id, v))
        world, labels = make_variant(base, notes, rng)
        audio = synthesize(world.f0, world.sp, world.ap, sr)
        audio *= target_rms / (np.sqrt(np.mean(audio**2)) + 1e-12)
        name = f"v{v:02d}"
        write_wav(out_dir / f"{name}.wav", audio, sr)
        labels.update({"clip_id": clip_id, "variant": name, "clean": "clean.wav", "audio": f"{name}.wav"})
        (out_dir / f"{name}.json").write_text(json.dumps(labels, ensure_ascii=False), encoding="utf-8")
        entries.append(
            {
                "clip_id": clip_id,
                "variant": name,
                "clean": str(out_dir / "clean.wav"),
                "audio": str(out_dir / f"{name}.wav"),
                "labels": str(out_dir / f"{name}.json"),
                "events": labels["events"],
            }
        )
    return entries


def run(root: Path, variants: int, singer_levels: set[str]) -> None:
    manifest = root / "data" / "manifest.jsonl"
    records = [json.loads(line) for line in manifest.read_text(encoding="utf-8").splitlines() if line.strip()]
    out_root = root / "data" / "synthetic"
    all_entries = []
    for record in records:
        level = record.get("singer", {}).get("level", "")
        if record["status"] != "ok" or record["condition"] not in {"normal", "expressive"}:
            continue
        if singer_levels and level not in singer_levels:
            continue
        signal, sr = load_mono(root / record["path"], TARGET_SR)
        entries = generate_for_clip(signal, sr, record["clip_id"], out_root / record["clip_id"], variants)
        for entry in entries:
            for key in ("clean", "audio", "labels"):
                entry[key] = str(Path(entry[key]).relative_to(root))
        all_entries.extend(entries)
        print(f"  {record['clip_id']}: {len(entries)} variants")
    out = root / "data" / "synthetic_manifest.jsonl"
    with out.open("w", encoding="utf-8") as handle:
        for entry in all_entries:
            handle.write(json.dumps(entry, ensure_ascii=False) + "\n")
    print(f"{len(all_entries)} synthetic variants -> {out}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--variants", type=int, default=6)
    parser.add_argument(
        "--levels",
        default="advanced,professional",
        help="only use clips from singers with these levels (comma separated, empty = all)",
    )
    parser.add_argument("--file", type=Path, help="process a single audio file instead of the manifest")
    parser.add_argument("--out", type=Path, help="output folder for --file")
    args = parser.parse_args()
    if args.file:
        signal, sr = load_mono(args.file, TARGET_SR)
        out = args.out or Path("synthetic_demo")
        entries = generate_for_clip(signal, sr, args.file.stem, out, args.variants)
        for entry in entries:
            print(entry["variant"], json.dumps(entry["events"], ensure_ascii=False))
        return
    levels = {x.strip() for x in args.levels.split(",") if x.strip()}
    run(args.root.resolve(), args.variants, levels)


if __name__ == "__main__":
    main()
