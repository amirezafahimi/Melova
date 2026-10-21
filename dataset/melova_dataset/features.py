"""Step 2 — acoustic features per clip (voice quality, power, vibrato, dynamics).

These are NOT labels. They serve three purposes:
  1. input features for the models, next to WavLM / MERT embeddings;
  2. pre-screening ("these 200 clips look hoarse — have the teachers hear them first");
  3. a sanity baseline: a trained model should beat a simple model on these numbers.

Features (all computed on voiced frames only):
  jitter_local_pct, shimmer_local_pct, hnr_db  — Praat (parselmouth); most reliable on sustained vowels
  cpp_db            — cepstral peak prominence (own implementation; compare within this dataset only)
  spr_db            — singing power ratio: spectral peak 2–4 kHz minus peak 0–2 kHz (higher = more "ring")
  alpha_ratio_db    — energy 1–5 kHz minus energy 50 Hz–1 kHz (spectral tilt)
  level_mean_dbfs, dynamic_range_db, level_std_db
  vibrato_rate_hz, vibrato_extent_cents, vibrato_note_fraction
  f0_median_hz, f0_range_st, voiced_fraction

Usage:
    python -m melova_dataset.features --root .            # all clips with status ok
    python -m melova_dataset.features --root . --all      # include clips flagged for review
    python -m melova_dataset.features --file some.wav     # quick look at one file
"""

from __future__ import annotations

import argparse
import csv
import json
import math
from pathlib import Path

import numpy as np

from melova_dataset.audio import TARGET_SR, load_mono

F0_FLOOR = 65.0
F0_CEILING = 1100.0
STEP_S = 0.01
MIN_VIBRATO_NOTE_S = 0.8

try:
    import parselmouth
    from parselmouth.praat import call as praat_call
except ImportError:  # features that need Praat are left empty
    parselmouth = None


def _nan() -> float:
    return float("nan")


def pitch_track(signal: np.ndarray, sr: int) -> tuple[np.ndarray, np.ndarray]:
    """Return (times_s, f0_hz) at 10 ms steps; unvoiced frames are 0."""
    if parselmouth is not None:
        sound = parselmouth.Sound(signal, sampling_frequency=sr)
        pitch = sound.to_pitch_ac(time_step=STEP_S, pitch_floor=F0_FLOOR, pitch_ceiling=F0_CEILING)
        return np.asarray(pitch.xs()), np.asarray(pitch.selected_array["frequency"])
    import pyworld

    f0, times = pyworld.harvest(signal, sr, f0_floor=F0_FLOOR, f0_ceil=F0_CEILING, frame_period=STEP_S * 1000)
    return times, f0


def praat_voice_quality(signal: np.ndarray, sr: int) -> dict[str, float]:
    if parselmouth is None:
        return {"jitter_local_pct": _nan(), "shimmer_local_pct": _nan(), "hnr_db": _nan()}
    sound = parselmouth.Sound(signal, sampling_frequency=sr)
    points = praat_call(sound, "To PointProcess (periodic, cc)", F0_FLOOR, F0_CEILING)
    jitter = praat_call(points, "Get jitter (local)", 0, 0, 0.0001, 0.02, 1.3)
    shimmer = praat_call([sound, points], "Get shimmer (local)", 0, 0, 0.0001, 0.02, 1.3, 1.6)
    harmonicity = sound.to_harmonicity_cc(time_step=STEP_S, minimum_pitch=F0_FLOOR)
    values = harmonicity.values[harmonicity.values > -199]
    return {
        "jitter_local_pct": float(jitter) * 100 if np.isfinite(jitter) else _nan(),
        "shimmer_local_pct": float(shimmer) * 100 if np.isfinite(shimmer) else _nan(),
        "hnr_db": float(np.mean(values)) if values.size else _nan(),
    }


def _voiced_frames(signal: np.ndarray, sr: int, times: np.ndarray, f0: np.ndarray, n_fft: int) -> np.ndarray:
    """Hann-windowed frames centred on voiced pitch-track times."""
    half = n_fft // 2
    padded = np.pad(signal, (half, half))
    centres = (times[f0 > 0] * sr).astype(int)
    if centres.size == 0:
        return np.empty((0, n_fft))
    idx = centres[:, None] + np.arange(n_fft)[None, :]
    idx = np.clip(idx, 0, len(padded) - 1)
    return padded[idx] * np.hanning(n_fft)[None, :]


def spectral_features(signal: np.ndarray, sr: int, times: np.ndarray, f0: np.ndarray) -> dict[str, float]:
    n_fft = 2048
    frames = _voiced_frames(signal, sr, times, f0, n_fft)
    if len(frames) < 5:
        return {"cpp_db": _nan(), "spr_db": _nan(), "alpha_ratio_db": _nan()}

    spectrum = np.abs(np.fft.rfft(frames, axis=1)) ** 2 + 1e-12
    freqs = np.fft.rfftfreq(n_fft, 1 / sr)

    # Cepstral peak prominence, per frame then averaged.
    log_spec = 10 * np.log10(spectrum)
    cepstrum = np.abs(np.fft.irfft(log_spec, axis=1))[:, : n_fft // 2]
    ceps_db = 20 * np.log10(cepstrum + 1e-12)
    quefrency = np.arange(n_fft // 2) / sr
    search = (quefrency >= 1 / F0_CEILING) & (quefrency <= 1 / F0_FLOOR)
    fit_range = (quefrency >= 0.001) & (quefrency <= 1 / F0_FLOOR)
    cpps = []
    q_fit = quefrency[fit_range]
    for row in ceps_db:
        slope, intercept = np.polyfit(q_fit, row[fit_range], 1)
        peak_idx = np.flatnonzero(search)[np.argmax(row[search])]
        cpps.append(row[peak_idx] - (slope * quefrency[peak_idx] + intercept))

    ltas = 10 * np.log10(spectrum.mean(axis=0))

    def band_peak(lo: float, hi: float) -> float:
        return float(ltas[(freqs >= lo) & (freqs < hi)].max())

    def band_energy(lo: float, hi: float) -> float:
        power = spectrum.mean(axis=0)[(freqs >= lo) & (freqs < hi)].sum()
        return float(10 * np.log10(power))

    return {
        "cpp_db": float(np.median(cpps)),
        "spr_db": band_peak(2000, 4000) - band_peak(0, 2000),
        "alpha_ratio_db": band_energy(1000, 5000) - band_energy(50, 1000),
    }


def dynamics(signal: np.ndarray, sr: int, times: np.ndarray, f0: np.ndarray) -> dict[str, float]:
    frames = _voiced_frames(signal, sr, times, f0, int(sr * 0.03))
    if len(frames) < 5:
        return {"level_mean_dbfs": _nan(), "dynamic_range_db": _nan(), "level_std_db": _nan()}
    # Hann window loses ~4.3 dB of RMS; compensate so values read as dBFS.
    db = 10 * np.log10(np.mean(frames**2, axis=1) / 0.375 + 1e-12)
    return {
        "level_mean_dbfs": float(np.mean(db)),
        "dynamic_range_db": float(np.percentile(db, 95) - np.percentile(db, 5)),
        "level_std_db": float(np.std(db)),
    }


def voiced_runs(f0: np.ndarray, min_frames: int) -> list[tuple[int, int]]:
    runs, start = [], None
    for i, value in enumerate(np.append(f0, 0)):
        if value > 0 and start is None:
            start = i
        elif value <= 0 and start is not None:
            if i - start >= min_frames:
                runs.append((start, i))
            start = None
    return runs


def vibrato(f0: np.ndarray) -> dict[str, float]:
    """Vibrato rate / extent on sustained voiced runs (≥ 0.8 s)."""
    rates, extents, with_vibrato = [], [], 0
    runs = voiced_runs(f0, int(MIN_VIBRATO_NOTE_S / STEP_S))
    for start, end in runs:
        cents = 1200 * np.log2(f0[start:end] / np.median(f0[start:end]))
        window = int(0.35 / STEP_S) | 1  # remove slow melodic movement (~>3 Hz passes)
        trend = np.convolve(np.pad(cents, window // 2, mode="edge"), np.ones(window) / window, mode="valid")
        residual = cents - trend
        spectrum = np.abs(np.fft.rfft(residual * np.hanning(len(residual)))) ** 2
        freqs = np.fft.rfftfreq(len(residual), STEP_S)
        band = (freqs >= 3.5) & (freqs <= 9.0)
        if not band.any():
            continue
        peak = np.argmax(np.where(band, spectrum, 0))
        band_share = spectrum[band].sum() / max(spectrum[(freqs > 0.5)].sum(), 1e-12)
        if band_share > 0.5:
            with_vibrato += 1
            rates.append(freqs[peak])
            extents.append(math.sqrt(2) * float(np.std(residual)))
    return {
        "vibrato_rate_hz": float(np.median(rates)) if rates else _nan(),
        "vibrato_extent_cents": float(np.median(extents)) if extents else _nan(),
        "vibrato_note_fraction": with_vibrato / len(runs) if runs else _nan(),
    }


def extract(signal: np.ndarray, sr: int) -> dict[str, float]:
    times, f0 = pitch_track(signal, sr)
    voiced = f0[f0 > 0]
    features: dict[str, float] = {
        "duration_s": len(signal) / sr,
        "voiced_fraction": float(np.mean(f0 > 0)) if f0.size else _nan(),
        "f0_median_hz": float(np.median(voiced)) if voiced.size else _nan(),
        "f0_range_st": float(12 * np.log2(np.percentile(voiced, 95) / np.percentile(voiced, 5)))
        if voiced.size > 10
        else _nan(),
    }
    features.update(praat_voice_quality(signal, sr))
    features.update(spectral_features(signal, sr, times, f0))
    features.update(dynamics(signal, sr, times, f0))
    features.update(vibrato(f0))
    return {k: (round(v, 4) if isinstance(v, float) and np.isfinite(v) else v) for k, v in features.items()}


def run(root: Path, include_review: bool) -> Path:
    manifest = root / "data" / "manifest.jsonl"
    records = [json.loads(line) for line in manifest.read_text(encoding="utf-8").splitlines() if line.strip()]
    rows = []
    for record in records:
        if record["status"] != "ok" and not include_review:
            continue
        signal, sr = load_mono(root / record["path"], TARGET_SR)
        row = {k: record[k] for k in ("clip_id", "singer_id", "prompt_id", "condition")}
        row.update(extract(signal, sr))
        rows.append(row)
        print(f"  {record['clip_id']}")
    out = root / "data" / "features.csv"
    if rows:
        with out.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
            writer.writeheader()
            writer.writerows(rows)
    print(f"Features for {len(rows)} clips -> {out}")
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--all", action="store_true", help="include clips whose status is 'review'")
    parser.add_argument("--file", type=Path, help="print features of a single audio file and exit")
    args = parser.parse_args()
    if args.file:
        signal, sr = load_mono(args.file, TARGET_SR)
        print(json.dumps(extract(signal, sr), indent=2))
        return
    run(args.root.resolve(), include_review=args.all)


if __name__ == "__main__":
    main()
