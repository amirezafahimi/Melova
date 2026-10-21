"""Step 4 — human ratings: create rating batches for teachers, then analyse them.

Two task types:
  quality          one clip; the rater scores
                     G R B A S (GRBAS voice-quality scale, 0–3 each:
                     Grade/overall hoarseness, Roughness, Breathiness, Asthenia/weakness, Strain)
                     power   (1–7, projection/resonance — NOT loudness)
                     overall (1–7)
  expression_pair  two clips of the SAME prompt; the rater picks which one has more
                   expression/feeling (a / b / equal) and how sure they are (1–3).

Built-in quality control:
  * every clip is rated by --raters-per-clip different raters (default 2);
  * ~10% of each rater's quality tasks are hidden repeats (test-retest consistency);
  * a rater never rates their own recordings (raters.csv → singer_id column);
  * same-singer "neutral" vs "expressive" pairs are mixed in as sanity checks.

Usage:
    python -m melova_dataset.ratings make --root . --batch round1
    # ... raters fill in their CSV files ...
    python -m melova_dataset.ratings analyze --root . --batch round1
"""

from __future__ import annotations

import argparse
import csv
import itertools
import json
import random
from collections import defaultdict
from pathlib import Path

import numpy as np

QUALITY_FIELDS = ["G", "R", "B", "A", "S", "power", "overall"]
QUALITY_RANGES = {"G": (0, 3), "R": (0, 3), "B": (0, 3), "A": (0, 3), "S": (0, 3), "power": (1, 7), "overall": (1, 7)}
PAIR_FIELDS = ["more_expressive", "confidence"]
RATER_COLUMNS = ["task_id", "task_type", "audio_a", "audio_b", *QUALITY_FIELDS, *PAIR_FIELDS, "notes"]


def _read_manifest(root: Path) -> list[dict]:
    path = root / "data" / "manifest.jsonl"
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def _read_raters(root: Path) -> list[dict]:
    path = root / "metadata" / "raters.csv"
    with path.open(encoding="utf-8-sig", newline="") as handle:
        rows = [r for r in csv.DictReader(handle) if r.get("rater_id", "").strip()]
    return [r for r in rows if r.get("active", "yes").strip().lower() in {"yes", "1", "true", ""}]


# --------------------------------------------------------------------------- make


def make_batch(
    root: Path,
    batch: str,
    raters_per_clip: int = 2,
    repeat_fraction: float = 0.1,
    max_pairs_per_prompt: int = 40,
    seed: int = 7,
) -> Path:
    rng = random.Random(seed)
    clips = [c for c in _read_manifest(root) if c["status"] == "ok"]
    raters = _read_raters(root)
    if len(raters) < raters_per_clip:
        raise SystemExit(f"Need at least {raters_per_clip} active raters in metadata/raters.csv")

    out_dir = root / "data" / "ratings" / batch
    if out_dir.exists() and any(out_dir.glob("*.csv")):
        raise SystemExit(f"{out_dir} already has rater files — choose a new --batch name")
    out_dir.mkdir(parents=True, exist_ok=True)

    own = {r["rater_id"]: (r.get("singer_id") or "").strip() for r in raters}
    load: dict[str, int] = defaultdict(int)
    assignments: dict[str, list[dict]] = defaultdict(list)
    key: list[dict] = []
    counter = itertools.count(1)

    def assign(task: dict, singers: set[str], n: int) -> None:
        eligible = [r["rater_id"] for r in raters if own[r["rater_id"]] not in singers]
        rng.shuffle(eligible)
        eligible.sort(key=lambda rid: load[rid])
        for rid in eligible[:n]:
            task_id = f"{batch}-{next(counter):05d}"
            assignments[rid].append({**task, "task_id": task_id})
            key.append({**task, "task_id": task_id, "rater_id": rid})
            load[rid] += 1

    # quality tasks
    for clip in clips:
        assign(
            {"task_type": "quality", "clip_a": clip["clip_id"], "clip_b": "", "repeat_of": ""},
            {clip["singer_id"]},
            raters_per_clip,
        )

    # expression pairs, per prompt
    by_prompt: dict[str, list[dict]] = defaultdict(list)
    for clip in clips:
        by_prompt[clip["prompt_id"]].append(clip)
    for prompt_clips in by_prompt.values():
        sanity, cross = [], []
        for a, b in itertools.combinations(prompt_clips, 2):
            if a["singer_id"] == b["singer_id"]:
                if {a["condition"], b["condition"]} == {"neutral", "expressive"}:
                    sanity.append((a, b))
            else:
                cross.append((a, b))
        rng.shuffle(cross)
        for a, b in sanity + cross[: max(0, max_pairs_per_prompt - len(sanity))]:
            if rng.random() < 0.5:
                a, b = b, a
            assign(
                {
                    "task_type": "expression_pair",
                    "clip_a": a["clip_id"],
                    "clip_b": b["clip_id"],
                    "repeat_of": "",
                    "sanity": "yes" if a["singer_id"] == b["singer_id"] else "",
                },
                {a["singer_id"], b["singer_id"]},
                raters_per_clip,
            )

    # hidden repeats + shuffle + write
    paths = {c["clip_id"]: c["path"] for c in clips}
    for rid, tasks in assignments.items():
        quality = [t for t in tasks if t["task_type"] == "quality"]
        for original in rng.sample(quality, int(round(len(quality) * repeat_fraction))):
            task_id = f"{batch}-{next(counter):05d}"
            repeat = {**original, "task_id": task_id, "repeat_of": original["task_id"]}
            tasks.append(repeat)
            key.append({**repeat, "rater_id": rid})
        rng.shuffle(tasks)
        with (out_dir / f"{rid}.csv").open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=RATER_COLUMNS)
            writer.writeheader()
            for t in tasks:
                writer.writerow(
                    {
                        "task_id": t["task_id"],
                        "task_type": t["task_type"],
                        "audio_a": paths[t["clip_a"]],
                        "audio_b": paths.get(t["clip_b"], ""),
                    }
                )

    with (out_dir / "_key.jsonl").open("w", encoding="utf-8") as handle:
        for row in key:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")

    print(f"Batch '{batch}' -> {out_dir}")
    for rid in sorted(assignments):
        n_q = sum(t["task_type"] == "quality" for t in assignments[rid])
        n_p = len(assignments[rid]) - n_q
        print(f"  {rid}: {n_q} quality + {n_p} pair tasks")
    print("Keep _key.jsonl private: it reveals which tasks are repeats/sanity checks.")
    return out_dir


# --------------------------------------------------------------------------- analyze


def krippendorff_alpha_interval(units: dict[str, list[float]]) -> float:
    """Krippendorff's alpha (interval metric) for units rated by ≥2 raters."""
    pairable = [np.asarray(v, dtype=float) for v in units.values() if len(v) >= 2]
    if len(pairable) < 2:
        return float("nan")
    n_total = sum(len(v) for v in pairable)
    observed = sum(((v[:, None] - v[None, :]) ** 2).sum() / (len(v) - 1) for v in pairable) / n_total
    allv = np.concatenate(pairable)
    expected = ((allv[:, None] - allv[None, :]) ** 2).sum() / (n_total * (n_total - 1))
    return float(1 - observed / expected) if expected > 0 else float("nan")


def bradley_terry(comparisons: list[tuple[str, str, float]], iterations: int = 200) -> dict[str, float]:
    """MM-algorithm Bradley–Terry; ties count half. Returns log-strength (mean 0)."""
    items = sorted({x for a, b, _ in comparisons for x in (a, b)})
    index = {x: i for i, x in enumerate(items)}
    wins = np.full(len(items), 0.5)  # weak prior keeps unbeaten / never-winning items finite
    pair_counts: dict[tuple[int, int], float] = defaultdict(float)
    for a, b, a_score in comparisons:
        ia, ib = index[a], index[b]
        wins[ia] += a_score
        wins[ib] += 1 - a_score
        pair_counts[(min(ia, ib), max(ia, ib))] += 1
    strength = np.ones(len(items))
    for _ in range(iterations):
        denom = 1.0 / (strength + 1.0)  # prior: one virtual game vs. a strength-1 opponent
        for (i, j), n in pair_counts.items():
            d = n / (strength[i] + strength[j])
            denom[i] += d
            denom[j] += d
        strength = wins / denom
        strength /= np.exp(np.mean(np.log(strength)))
    return {x: float(np.log(strength[index[x]])) for x in items}


def _to_float(value: str) -> float | None:
    try:
        return float(str(value).strip().replace("٫", "."))
    except ValueError:
        return None


def analyze_batch(root: Path, batch: str) -> dict:
    out_dir = root / "data" / "ratings" / batch
    key = {
        row["task_id"]: row
        for row in map(json.loads, (out_dir / "_key.jsonl").read_text(encoding="utf-8").splitlines())
    }
    answers: dict[str, dict] = {}
    problems: list[str] = []
    for path in sorted(out_dir.glob("*.csv")):
        with path.open(encoding="utf-8-sig", newline="") as handle:
            for row in csv.DictReader(handle):
                task = key.get(row["task_id"])
                if task is None:
                    problems.append(f"{path.name}: unknown task {row['task_id']}")
                    continue
                answers[row["task_id"]] = row

    # quality
    per_dim: dict[str, dict[str, list[float]]] = {f: defaultdict(list) for f in QUALITY_FIELDS}
    retest: dict[str, list[float]] = defaultdict(list)
    done_quality = 0
    for task_id, row in answers.items():
        task = key[task_id]
        if task["task_type"] != "quality":
            continue
        values = {}
        for f in QUALITY_FIELDS:
            v = _to_float(row.get(f, ""))
            lo, hi = QUALITY_RANGES[f]
            if v is not None and not lo <= v <= hi:
                problems.append(f"{task_id}: {f}={v} outside {lo}–{hi}")
                v = None
            values[f] = v
        if all(v is None for v in values.values()):
            continue
        done_quality += 1
        if task["repeat_of"]:
            first = answers.get(task["repeat_of"], {})
            for f in QUALITY_FIELDS:
                a, b = _to_float(first.get(f, "")), values[f]
                if a is not None and b is not None:
                    retest[f].append(abs(a - b))
            continue
        for f, v in values.items():
            if v is not None:
                per_dim[f][task["clip_a"]].append(v)

    # pairs
    comparisons: list[tuple[str, str, float]] = []
    sanity_hits, sanity_total = 0, 0
    pair_votes: dict[tuple[str, str], list[float]] = defaultdict(list)
    manifest = {c["clip_id"]: c for c in _read_manifest(root)}
    for task_id, row in answers.items():
        task = key[task_id]
        if task["task_type"] != "expression_pair":
            continue
        choice = str(row.get("more_expressive", "")).strip().lower()
        score = {"a": 1.0, "b": 0.0, "equal": 0.5, "=": 0.5}.get(choice)
        if score is None:
            continue
        a, b = task["clip_a"], task["clip_b"]
        comparisons.append((a, b, score))
        pair_votes[tuple(sorted((a, b)))].append(score if a < b else 1 - score)
        if task.get("sanity") == "yes" and score != 0.5:
            sanity_total += 1
            expressive = a if manifest[a]["condition"] == "expressive" else b
            winner = a if score == 1.0 else b
            sanity_hits += winner == expressive

    multi = [v for v in pair_votes.values() if len(v) >= 2]
    pair_agreement = float(np.mean([v[0] == v[1] for v in multi])) if multi else float("nan")
    bt = bradley_terry(comparisons) if comparisons else {}

    # write labels
    with (out_dir / "labels_quality.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["clip_id", *QUALITY_FIELDS, "n_raters"])
        for clip_id in sorted({c for d in per_dim.values() for c in d}):
            means = [np.mean(per_dim[f][clip_id]) if per_dim[f].get(clip_id) else "" for f in QUALITY_FIELDS]
            n = max(len(per_dim[f].get(clip_id, [])) for f in QUALITY_FIELDS)
            writer.writerow([clip_id, *[round(m, 3) if m != "" else "" for m in means], n])
    with (out_dir / "labels_expression.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["clip_id", "prompt_id", "bt_log_strength", "n_comparisons"])
        counts = defaultdict(int)
        for a, b, _ in comparisons:
            counts[a] += 1
            counts[b] += 1
        for clip_id, s in sorted(bt.items(), key=lambda kv: -kv[1]):
            writer.writerow([clip_id, manifest.get(clip_id, {}).get("prompt_id", ""), round(s, 4), counts[clip_id]])

    report = {
        "quality_tasks_answered": done_quality,
        "pair_tasks_answered": len(comparisons),
        "agreement_alpha": {f: round(krippendorff_alpha_interval(per_dim[f]), 3) for f in QUALITY_FIELDS},
        "retest_mean_abs_diff": {f: round(float(np.mean(v)), 3) for f, v in retest.items() if v},
        "pair_agreement": round(pair_agreement, 3) if np.isfinite(pair_agreement) else None,
        "sanity_expressive_win_rate": round(sanity_hits / sanity_total, 3) if sanity_total else None,
        "problems": problems[:50],
    }
    (out_dir / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print(
        "\nHow to read: alpha ≥ 0.67 usable, ≥ 0.8 good. Low alpha on a dimension means the raters "
        "don't share a definition yet — calibrate together before collecting more."
    )
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)
    mk = sub.add_parser("make", help="create rating files for each rater")
    mk.add_argument("--root", type=Path, default=Path("."))
    mk.add_argument("--batch", required=True)
    mk.add_argument("--raters-per-clip", type=int, default=2)
    mk.add_argument("--repeat-fraction", type=float, default=0.1)
    mk.add_argument("--max-pairs-per-prompt", type=int, default=40)
    mk.add_argument("--seed", type=int, default=7)
    an = sub.add_parser("analyze", help="agreement report + label files")
    an.add_argument("--root", type=Path, default=Path("."))
    an.add_argument("--batch", required=True)
    args = parser.parse_args()
    if args.cmd == "make":
        make_batch(
            args.root.resolve(), args.batch, args.raters_per_clip, args.repeat_fraction, args.max_pairs_per_prompt, args.seed
        )
    else:
        analyze_batch(args.root.resolve(), args.batch)


if __name__ == "__main__":
    main()
