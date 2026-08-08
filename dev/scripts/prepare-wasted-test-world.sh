#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ARCHIVE="${1:-/mnt/data/WASTED V0.5 Release.zip}"
DEST="$ROOT/dev/worlds/WASTED_TEST"
TMP="$ROOT/dev/worlds/.extract"

if [[ ! -f "$ARCHIVE" ]]; then
  echo "WASTED archive not found: $ARCHIVE" >&2
  exit 1
fi

rm -rf "$DEST" "$TMP"
mkdir -p "$TMP"
unzip -q "$ARCHIVE" -d "$TMP"
SRC="$TMP/WASTED V0.5 Release"
if [[ ! -f "$SRC/level.dat" ]]; then
  echo "Unexpected WASTED archive layout: level.dat not found" >&2
  exit 1
fi
mv "$SRC" "$DEST"
rm -rf "$TMP"

echo "Prepared disposable test world: $DEST"
echo "Original archive remains untouched."
