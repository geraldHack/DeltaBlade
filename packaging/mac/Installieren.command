#!/bin/bash
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/DeltaBlade.app"
DEST="/Applications/DeltaBlade.app"

if [[ ! -d "$SRC" ]]; then
  osascript -e 'display dialog "DeltaBlade.app nicht gefunden.\nBitte das DMG geöffnet lassen und Installieren.command darin starten." buttons {"OK"} default button "OK" with icon stop'
  exit 1
fi

CHOICE="$(osascript -e 'button returned of (display dialog "DeltaBlade nach Programme kopieren und starten?" buttons {"Abbrechen","Installieren"} default button "Installieren")')" || true
if [[ "$CHOICE" != "Installieren" ]]; then
  exit 0
fi

if [[ "$SRC" != "$DEST" ]]; then
  rm -rf "$DEST"
  ditto "$SRC" "$DEST"
fi

xattr -cr "$DEST" || true
xattr -d com.apple.quarantine "$DEST" 2>/dev/null || true
cd "$HOME"
echo "DeltaBlade startet …"
exec "$DEST/Contents/MacOS/DeltaBlade"
