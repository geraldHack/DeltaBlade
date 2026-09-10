#!/bin/bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  exit 0
fi

DIST="${1:-}"
if [[ -z "$DIST" || ! -d "$DIST" ]]; then
  echo "No dist directory: ${DIST:-<missing>}"
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DMG="$(find "$DIST" -maxdepth 1 -name '*.dmg' -print | head -n 1)"
if [[ -z "$DMG" ]]; then
  echo "No DMG in $DIST"
  exit 0
fi

WORKDIR="$(mktemp -d /tmp/deltablade-dmg.XXXXXX)"
MOUNT="$WORKDIR/mnt"
RW="$WORKDIR/rw.dmg"
OUT="$WORKDIR/out.dmg"
mkdir -p "$MOUNT"
trap 'hdiutil detach "$MOUNT" -force >/dev/null 2>&1 || true; rm -rf "$WORKDIR"' EXIT

hdiutil convert "$DMG" -format UDRW -o "$RW" >/dev/null
# Grow a bit so HINWEIS + script fit on a packed image.
CURRENT_KB="$(hdiutil resize -limits "$RW" | awk 'NR==1 {print int($2)}')"
if [[ -n "$CURRENT_KB" && "$CURRENT_KB" -gt 0 ]]; then
  hdiutil resize -size "$((CURRENT_KB + 2048))k" "$RW" >/dev/null
fi

hdiutil attach "$RW" -mountpoint "$MOUNT" -nobrowse >/dev/null
cp "$SCRIPT_DIR/HINWEIS.txt" "$MOUNT/HINWEIS.txt"
cp "$SCRIPT_DIR/Installieren.command" "$MOUNT/Installieren.command"
chmod a+x "$MOUNT/Installieren.command"
hdiutil detach "$MOUNT" -force >/dev/null

hdiutil convert "$RW" -format UDZO -imagekey zlib-level=9 -o "$OUT" >/dev/null
mv -f "$OUT" "$DMG"
echo "Injected macOS helper files into $(basename "$DMG")"
