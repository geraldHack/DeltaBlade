#!/bin/bash
set -euo pipefail

VERSION="${1:?version}"
DEST="${2:?dest dir}"
BASE="${3:?project dir}"
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"
INPUT="$BASE/target/jpackage-input"
WORK="$BASE/target/dist-arch"
NAME="DeltaBlade-$VERSION-arch-x64"

if [[ ! -d "$INPUT" ]]; then
  echo "missing jpackage input: $INPUT" >&2
  exit 1
fi

rm -rf "$WORK"
mkdir -p "$WORK" "$DEST"

"$JPACKAGE" \
  --name DeltaBlade \
  --dest "$WORK" \
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

cat > "$WORK/LIESMICH.txt" <<EOF
DeltaBlade für Arch / andere Distros ohne .deb

Ordner entpacken und starten:

  tar -xzf $NAME.tar.gz
  ./DeltaBlade/bin/DeltaBlade

Musik: XDG-Musikordner/DeltaBlade (oft ~/Musik/DeltaBlade).
MP3 läuft über JavaSound, ohne GStreamer.
EOF

tar -C "$WORK" -czf "$DEST/$NAME.tar.gz" DeltaBlade LIESMICH.txt
echo "Packaged: $NAME.tar.gz"
