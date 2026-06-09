#!/usr/bin/env bash
exec python3 "$(dirname "$0")/../backend/scripts/align_syllables.py" "$@"
