"""End-to-end checks on synthetic 'sung' tones (no real recordings needed)."""

from __future__ import annotations

import csv
import json
from pathlib import Path

import numpy as np
import pytest

from melova_dataset import features, ingest, ratings, synth_errors
from melova_dataset.audio import TARGET_SR, load_mono, write_wav

SR = TARGET_SR


def sung_tone(notes_hz, note_s=0.8, vibrato_hz=0.0, vibrato_cents=0.0, pad_s=0.5, seed=0):
    """Harmonic 'voice' with a vowel-like envelope, optional vibrato, silence around it."""
    rng = np.random.default_rng(seed)
    f0 = np.concatenate([np.full(int(note_s * SR), hz) for hz in notes_hz])
    t = np.arange(len(f0)) / SR
    if vibrato_hz:
        f0 = f0 * 2 ** (vibrato_cents * np.sin(2 * np.pi * vibrato_hz * t) / 1200)
    phase = 2 * np.pi * np.cumsum(f0) / SR
    voice = sum((0.8 ** k) * np.sin(k * phase) for k in range(1, 25))
    voice *= np.minimum(1, np.minimum(t, t[-1] - t) / 0.03)
    voice = 0.3 * voice / np.max(np.abs(voice))
    silence = np.zeros(int(pad_s * SR))
    signal = np.concatenate([silence, voice, silence])
    return signal + rng.standard_normal(len(signal)) * 1e-4


@pytest.fixture()
def dataset_root(tmp_path: Path) -> Path:
    (tmp_path / "metadata").mkdir()
    (tmp_path / "metadata" / "singers.csv").write_text(
        "singer_id,level,consent_id\nS001,professional,C1\nS002,advanced,C2\nS003,beginner,\n", encoding="utf-8"
    )
    (tmp_path / "metadata" / "prompts.csv").write_text("prompt_id,type\nP01,phrase\n", encoding="utf-8")
    (tmp_path / "metadata" / "raters.csv").write_text(
        "rater_id,role,singer_id,active\nR01,teacher,S001,yes\nR02,teacher,,yes\nR03,teacher,,yes\n", encoding="utf-8"
    )
    melody = [220.0, 246.94, 261.63, 293.66]
    for s, singer in enumerate(("S001", "S002", "S003")):
        for cond, seed in (("normal", 1), ("neutral", 2), ("expressive", 3)):
            write_wav(tmp_path / f"data/raw/{singer}/sess1/P01__{cond}__1.wav", sung_tone(melody, seed=10 * s + seed), SR)
    write_wav(tmp_path / "data/raw/S001/sess1/badname.wav", sung_tone([220.0]), SR)
    return tmp_path


def test_ingest_trims_and_enforces_consent(dataset_root: Path):
    records = ingest.ingest(dataset_root)
    assert {r.singer_id for r in records} == {"S001", "S002"}  # S003 has no consent
    assert len(records) == 6
    for r in records:
        assert r.status == "ok", r.flags
        assert 3.2 <= r.duration_s <= 3.2 + 2 * ingest.TRIM_MARGIN_S + 0.1  # silence removed
    manifest = (dataset_root / "data/manifest.jsonl").read_text(encoding="utf-8").splitlines()
    assert len(manifest) == 6


def test_vibrato_feature_detects_rate_and_extent():
    signal = sung_tone([220.0], note_s=2.0, vibrato_hz=5.5, vibrato_cents=50)
    feats = features.extract(signal, SR)
    assert feats["vibrato_rate_hz"] == pytest.approx(5.5, abs=0.6)
    assert feats["vibrato_extent_cents"] == pytest.approx(50, abs=15)
    assert feats["f0_median_hz"] == pytest.approx(220, rel=0.02)
    assert feats["hnr_db"] > 20


def test_synthetic_offsets_match_labels(tmp_path: Path):
    signal = sung_tone([220.0, 261.63, 329.63, 293.66], note_s=0.9)
    entries = synth_errors.generate_for_clip(signal, SR, "demo", tmp_path, variants=8)
    clean, _ = load_mono(tmp_path / "clean.wav")
    f0_clean = synth_errors.analyse(clean, SR).f0
    checked = 0
    for entry in entries:
        labels = json.loads(Path(entry["labels"]).read_text())
        offsets = np.array(labels["frames"]["offset_cents"])
        if not np.any(np.abs(offsets) > 10):
            continue
        audio, _ = load_mono(entry["audio"])
        f0 = synth_errors.analyse(audio, SR).f0
        n = min(len(f0), len(f0_clean), len(offsets))
        mask = (f0[:n] > 0) & (f0_clean[:n] > 0) & (np.abs(offsets[:n]) > 10)
        measured = 1200 * np.log2(f0[:n][mask] / f0_clean[:n][mask])
        assert np.median(np.abs(measured - offsets[:n][mask])) < 5
        checked += 1
    assert checked >= 3


def test_ratings_roundtrip(dataset_root: Path):
    ingest.ingest(dataset_root)
    out = ratings.make_batch(dataset_root, "r1", raters_per_clip=2, repeat_fraction=0.2)
    key = [json.loads(x) for x in (out / "_key.jsonl").read_text().splitlines()]
    # R01 is singer S001 and must never rate S001's clips
    for row in key:
        if row["rater_id"] == "R01":
            assert "S001" not in row["clip_a"] and "S001" not in row.get("clip_b", "")
    assert any(row.get("sanity") == "yes" for row in key)

    # Fill answers: everyone agrees, expressive always wins.
    manifest = {json.loads(x)["clip_id"]: json.loads(x) for x in (dataset_root / "data/manifest.jsonl").read_text().splitlines()}
    by_task = {row["task_id"]: row for row in key}
    score_of = {"neutral": 1, "normal": 2, "expressive": 3}
    for path in out.glob("R*.csv"):
        with path.open(encoding="utf-8-sig") as handle:
            rows = list(csv.DictReader(handle))
        for row in rows:
            task = by_task[row["task_id"]]
            a = manifest[task["clip_a"]]
            if task["task_type"] == "quality":
                level = score_of[a["condition"]]
                row.update({"G": 3 - level, "R": 0, "B": 3 - level, "A": 0, "S": 0, "power": level * 2, "overall": level * 2})
            else:
                b = manifest[task["clip_b"]]
                sa, sb = score_of[a["condition"]], score_of[b["condition"]]
                row.update({"more_expressive": "a" if sa > sb else "b" if sb > sa else "equal", "confidence": 3})
        with path.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=rows[0].keys())
            writer.writeheader()
            writer.writerows(rows)

    report = ratings.analyze_batch(dataset_root, "r1")
    assert report["agreement_alpha"]["power"] == pytest.approx(1.0)
    assert report["sanity_expressive_win_rate"] == 1.0
    assert all(v == 0 for v in report["retest_mean_abs_diff"].values())
    with (out / "labels_expression.csv").open() as handle:
        ranking = [row["clip_id"] for row in csv.DictReader(handle)]
    assert "expressive" in ranking[0] and "neutral" in ranking[-1]


def test_krippendorff_alpha_reference_values():
    perfect = {"u1": [1, 1], "u2": [3, 3], "u3": [2, 2]}
    assert ratings.krippendorff_alpha_interval(perfect) == pytest.approx(1.0)
    noisy = {"u1": [1, 3], "u2": [3, 1], "u3": [2, 2]}
    assert ratings.krippendorff_alpha_interval(noisy) < 0
