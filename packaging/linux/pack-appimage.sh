#!/bin/bash
set -euo pipefail

VERSION="${1:?version}"
DEST="${2:?dest dir}"
BASE="${3:?project dir}"
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"
INPUT="$BASE/target/jpackage-input"
APP_SRC="$BASE/target/dist-arch/DeltaBlade"
WORK="$BASE/target/appimage"
APPDIR="$WORK/DeltaBlade.AppDir"
TOOLS="$WORK/tools"
NAME="DeltaBlade-$VERSION-linux-appimage-x64.AppImage"
TOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"

if [[ ! -d "$APP_SRC" ]]; then
  if [[ ! -d "$INPUT" ]]; then
    echo "missing jpackage input: $INPUT" >&2
    exit 1
  fi
  mkdir -p "$BASE/target/dist-arch"
  "$JPACKAGE" \
    --name DeltaBlade \
    --dest "$BASE/target/dist-arch" \
    --type app-image \
    --app-version "$VERSION" \
    --copyright DeltaBlade \
    --description "Arcade shooter" \
    --input "$INPUT" \
    --vendor DeltaBlade \
    --main-class deltablade.DeltaBladeApp \
    --main-jar "deltablade-$VERSION.jar" \
    --icon "$BASE/packaging/icons/deltablade.png" \
    --java-options -Dprism.order=es2,sw \
    --java-options -Dprism.metal=false \
    --java-options --enable-native-access=ALL-UNNAMED \
    --java-options -Xms128m \
    --java-options -Xmx512m
fi

if [[ ! -x "$APP_SRC/bin/DeltaBlade" ]]; then
  echo "missing app image launcher: $APP_SRC/bin/DeltaBlade" >&2
  exit 1
fi

rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr/lib" "$APPDIR/usr/bin" "$TOOLS" "$DEST"
cp -a "$APP_SRC" "$APPDIR/usr/lib/DeltaBlade"
cp "$BASE/packaging/icons/deltablade.png" "$APPDIR/deltablade.png"
ln -sf deltablade.png "$APPDIR/.DirIcon"

cat > "$APPDIR/AppRun" <<'EOF'
#!/bin/bash
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
export APPDIR="$HERE"
cd "${HOME:-/tmp}"
exec "$HERE/usr/lib/DeltaBlade/bin/DeltaBlade" "$@"
EOF
chmod +x "$APPDIR/AppRun"

cat > "$APPDIR/usr/bin/DeltaBlade" <<'EOF'
#!/bin/bash
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")/../.."
exec "$HERE/usr/lib/DeltaBlade/bin/DeltaBlade" "$@"
EOF
chmod +x "$APPDIR/usr/bin/DeltaBlade"

cat > "$APPDIR/DeltaBlade.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=DeltaBlade
Comment=Arcade shooter
Exec=DeltaBlade
Icon=deltablade
Categories=Game;
Terminal=false
StartupWMClass=DeltaBlade
EOF

TOOL="$TOOLS/appimagetool-x86_64.AppImage"
if [[ ! -x "$TOOL" ]]; then
  curl -fsSL -o "$TOOL" "$TOOL_URL"
  chmod +x "$TOOL"
fi

export ARCH=x86_64
export APPIMAGE_EXTRACT_AND_RUN=1
"$TOOL" --no-appstream "$APPDIR" "$DEST/$NAME"
chmod +x "$DEST/$NAME"
echo "Packaged: $NAME"
