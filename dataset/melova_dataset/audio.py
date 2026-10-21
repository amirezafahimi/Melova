"""Audio loading / writing helpers shared by every dataset step."""

from __future__ import annotations

import shutil
import subprocess
import tempfile
from math import gcd
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import resample_poly

TARGET_SR = 44_100
AUDIO_EXTENSIONS = {".wav", ".flac", ".ogg", ".mp3", ".m4a", ".aac", ".opus", ".aiff", ".aif"}


def load_mono(path: str | Path, target_sr: int = TARGET_SR) -> tuple[np.ndarray, int]:
    """Load any audio file as float64 mono at ``target_sr``.

    Formats libsndfile can't read (m4a from iPhone voice memos, aac, opus) are
    decoded through ffmpeg when it is installed.
    """
    path = Path(path)
    try:
        data, sr = sf.read(str(path), dtype="float64", always_2d=True)
    except (RuntimeError, sf.LibsndfileError):
        data, sr = _decode_with_ffmpeg(path)
    mono = data.mean(axis=1)
    return resample(mono, sr, target_sr), target_sr


def _decode_with_ffmpeg(path: Path) -> tuple[np.ndarray, int]:
    if shutil.which("ffmpeg") is None:
        raise RuntimeError(
            f"Cannot decode {path.name}: install ffmpeg (brew install ffmpeg) or convert it to WAV."
        )
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "decoded.wav"
        subprocess.run(
            ["ffmpeg", "-nostdin", "-loglevel", "error", "-i", str(path), "-f", "wav", str(out)],
            check=True,
        )
        data, sr = sf.read(str(out), dtype="float64", always_2d=True)
    return data, sr


def resample(signal: np.ndarray, source_sr: int, target_sr: int) -> np.ndarray:
    if source_sr == target_sr:
        return signal
    divisor = gcd(source_sr, target_sr)
    return resample_poly(signal, target_sr // divisor, source_sr // divisor)


def write_wav(path: str | Path, signal: np.ndarray, sr: int = TARGET_SR) -> None:
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(path), np.clip(signal, -1.0, 1.0), sr, subtype="PCM_24")


def frame_rms_db(signal: np.ndarray, sr: int, frame_ms: float = 30.0, hop_ms: float = 10.0) -> np.ndarray:
    frame = max(1, int(sr * frame_ms / 1000))
    hop = max(1, int(sr * hop_ms / 1000))
    if len(signal) < frame:
        signal = np.pad(signal, (0, frame - len(signal)))
    count = 1 + (len(signal) - frame) // hop
    idx = np.arange(frame)[None, :] + hop * np.arange(count)[:, None]
    rms = np.sqrt(np.mean(signal[idx] ** 2, axis=1))
    return 20.0 * np.log10(np.maximum(rms, 1e-10))
