"""Step 1 — turn raw recordings into clean, uniformly named clips + a manifest.

Expected raw layout (see README_FA.md):

    data/raw/<singer_id>/<session_id>/<prompt_id>__<condition>__<take>.<ext>

e.g. ``data/raw/S001/2026-10-20/P05__normal__1.m4a``. ``take`` may be omitted.

Every clip is converted to mono 44.1 kHz / 24-bit WAV, silence at both ends is
trimmed, and quality checks (clipping, noise, level) are recorded. Loudness is
NOT normalised on purpose: absolute level carries information for the "power"
dimension, so the measured level is stored instead.

Usage:
    python -m melova_dataset.ingest --root .
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sys
from dataclasses import asdict, dataclass, field
from pathlib import Path

import numpy as np

from melova_dataset.audio import AUDIO_EXTENSIONS, TARGET_SR, frame_rms_db, load_mono, write_wav

CONDITIONS = {
    "normal",      # ordinary performance
    "neutral",     # deliberately without expression (بی‌حس)
    "expressive",  # maximum expression (با حداکثر حس)
    "soft",        # quiet voice
    "full",        # full / projected voice (not shouting)
    "tired",       # recorded naturally at the end of a long lesson/day
    "offpitch",    # deliberately slightly off (only for advanced singers)
}

CLIP_THRESHOLD = 0.999
MAX_CLIPPED_FRACTION = 0.001
MIN_SNR_DB = 25.0
MIN_LEVEL_DBFS = -45.0
MIN_DURATION_S = 0.5
TRIM_MARGIN_S = 0.15
HOP_MS = 10.0

_SAFE = re.compile(r"[^A-Za-z0-9\-]+")


@dataclass
class ClipRecord:
    clip_id: str
    singer_id: str
    session_id: str
    prompt_id: str
    condition: str
    take: int
    path: str
    source_path: str
    source_sha1: str
    sample_rate: int
    duration_s: float
    level_dbfs_p90: float
    noise_floor_dbfs: float
    snr_db: float
    clipped_fraction: float
    status: str
    flags: list[str] = field(default_factory=list)
    singer: dict = field(default_factory=dict)
    prompt: dict = field(default_factory=dict)


def parse_filename(stem: str) -> tuple[str, str, int]:
    parts = stem.split("__")
    if len(parts) not in (2, 3):
        raise ValueError(
            f"'{stem}': expected <prompt_id>__<condition>__<take>, e.g. P05__normal__1"
        )
    prompt_id, condition = parts[0], parts[1].lower()
    take = int(parts[2]) if len(parts) == 3 else 1
    if condition not in CONDITIONS:
        raise ValueError(f"'{stem}': unknown condition '{condition}'. Allowed: {sorted(CONDITIONS)}")
    return prompt_id, condition, take


def read_table(path: Path, key: str) -> dict[str, dict]:
    if not path.exists():
        raise FileNotFoundError(f"Missing metadata file: {path}")
    with path.open(encoding="utf-8-sig", newline="") as handle:
        rows = [row for row in csv.DictReader(handle) if row.get(key, "").strip()]
    return {row[key].strip(): {k: (v or "").strip() for k, v in row.items()} for row in rows}


def sha1_of(path: Path) -> str:
    digest = hashlib.sha1()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def analyse_and_trim(signal: np.ndarray, sr: int) -> tuple[np.ndarray, dict]:
    clipped_fraction = float(np.mean(np.abs(signal) >= CLIP_THRESHOLD))
    db = frame_rms_db(signal, sr, hop_ms=HOP_MS)
    noise_floor = float(np.percentile(db, 10))
    level = float(np.percentile(db, 90))
    snr = level - noise_floor

    threshold = max(noise_floor + 0.3 * snr, level - 40.0)
    active = np.flatnonzero(db > threshold)
    hop = int(sr * HOP_MS / 1000)
    margin = int(sr * TRIM_MARGIN_S)
    if active.size:
        start = max(0, active[0] * hop - margin)
        end = min(len(signal), (active[-1] + 1) * hop + int(sr * 0.03) + margin)
        trimmed = signal[start:end]
    else:
        trimmed = signal

    return trimmed, {
        "clipped_fraction": round(clipped_fraction, 6),
        "noise_floor_dbfs": round(noise_floor, 2),
        "level_dbfs_p90": round(level, 2),
        "snr_db": round(snr, 2),
    }


def quality_flags(stats: dict, duration_s: float) -> list[str]:
    flags = []
    if stats["clipped_fraction"] > MAX_CLIPPED_FRACTION:
        flags.append("clipped")
    if stats["snr_db"] < MIN_SNR_DB:
        flags.append("noisy")
    if stats["level_dbfs_p90"] < MIN_LEVEL_DBFS:
        flags.append("too_quiet")
    if duration_s < MIN_DURATION_S:
        flags.append("too_short")
    return flags


def ingest(root: Path, force: bool = False) -> list[ClipRecord]:
    raw_dir = root / "data" / "raw"
    clips_dir = root / "data" / "clips"
    singers = read_table(root / "metadata" / "singers.csv", "singer_id")
    prompts = read_table(root / "metadata" / "prompts.csv", "prompt_id")

    records: list[ClipRecord] = []
    errors: list[str] = []
    seen_hashes: dict[str, str] = {}

    files = sorted(p for p in raw_dir.rglob("*") if p.suffix.lower() in AUDIO_EXTENSIONS)
    for source in files:
        rel = source.relative_to(raw_dir)
        if len(rel.parts) != 3:
            errors.append(f"{rel}: must be <singer_id>/<session_id>/<file>")
            continue
        singer_id, session_id, _ = rel.parts
        try:
            prompt_id, condition, take = parse_filename(source.stem)
        except ValueError as exc:
            errors.append(str(exc))
            continue
        singer = singers.get(singer_id)
        if singer is None:
            errors.append(f"{rel}: singer '{singer_id}' not in metadata/singers.csv")
            continue
        if not singer.get("consent_id"):
            errors.append(f"{rel}: singer '{singer_id}' has no consent_id — skipped (no consent, no data)")
            continue
        prompt = prompts.get(prompt_id)
        if prompt is None:
            errors.append(f"{rel}: prompt '{prompt_id}' not in metadata/prompts.csv")
            continue

        digest = sha1_of(source)
        if digest in seen_hashes:
            errors.append(f"{rel}: duplicate of {seen_hashes[digest]} — skipped")
            continue
        seen_hashes[digest] = str(rel)

        clip_id = "_".join(_SAFE.sub("-", x) for x in (singer_id, session_id, prompt_id, condition, str(take)))
        out_path = clips_dir / f"{clip_id}.wav"

        signal, sr = load_mono(source, TARGET_SR)
        trimmed, stats = analyse_and_trim(signal, sr)
        duration = len(trimmed) / sr
        flags = quality_flags(stats, duration)
        if force or not out_path.exists():
            write_wav(out_path, trimmed, sr)

        records.append(
            ClipRecord(
                clip_id=clip_id,
                singer_id=singer_id,
                session_id=session_id,
                prompt_id=prompt_id,
                condition=condition,
                take=take,
                path=str(out_path.relative_to(root)),
                source_path=str(source.relative_to(root)),
                source_sha1=digest,
                sample_rate=sr,
                duration_s=round(duration, 3),
                status="review" if flags else "ok",
                flags=flags,
                singer={k: v for k, v in singer.items() if k not in {"name", "phone", "email"}},
                prompt=prompt,
                **stats,
            )
        )

    manifest = root / "data" / "manifest.jsonl"
    manifest.parent.mkdir(parents=True, exist_ok=True)
    with manifest.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(asdict(record), ensure_ascii=False) + "\n")

    ok = sum(r.status == "ok" for r in records)
    print(f"Ingested {len(records)} clips ({ok} ok, {len(records) - ok} need review) -> {manifest}")
    if errors:
        print(f"\n{len(errors)} file(s) skipped:", file=sys.stderr)
        for line in errors:
            print(f"  - {line}", file=sys.stderr)
    return records


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", type=Path, default=Path("."), help="dataset folder (contains data/ and metadata/)")
    parser.add_argument("--force", action="store_true", help="re-write clips that already exist")
    args = parser.parse_args()
    ingest(args.root.resolve(), force=args.force)


if __name__ == "__main__":
    main()
