#!/bin/bash

set -e

if [ $# -ne 1 ]; then
    echo "Usage: $0 <path-to-svg>"
    exit 1
fi

SVG="$1"

if [ ! -f "$SVG" ]; then
    echo "Error: SVG file not found: $SVG"
    exit 1
fi

PNG="icon_2048.png"
ICONSET="BluePad.iconset"
ICNS="BluePad.icns"

echo "Exporting SVG to PNG..."
inkscape "$SVG" \
    --export-type=png \
    --export-filename="$PNG" \
    -w 2048 -h 2048

rm -rf "$ICONSET"
mkdir -p "$ICONSET"

echo "Generating iconset..."

sips -z 16 16     "$PNG" --out "$ICONSET/icon_16x16.png" >/dev/null
sips -z 32 32     "$PNG" --out "$ICONSET/icon_16x16@2x.png" >/dev/null

sips -z 32 32     "$PNG" --out "$ICONSET/icon_32x32.png" >/dev/null
sips -z 64 64     "$PNG" --out "$ICONSET/icon_32x32@2x.png" >/dev/null

sips -z 128 128   "$PNG" --out "$ICONSET/icon_128x128.png" >/dev/null
sips -z 256 256   "$PNG" --out "$ICONSET/icon_128x128@2x.png" >/dev/null

sips -z 256 256   "$PNG" --out "$ICONSET/icon_256x256.png" >/dev/null
sips -z 512 512   "$PNG" --out "$ICONSET/icon_256x256@2x.png" >/dev/null

sips -z 512 512   "$PNG" --out "$ICONSET/icon_512x512.png" >/dev/null
sips -z 1024 1024 "$PNG" --out "$ICONSET/icon_512x512@2x.png" >/dev/null

echo "Generating ICNS..."
iconutil -c icns "$ICONSET" -o "$ICNS"

echo "Done."
echo "Created:"
echo "  $ICONSET"
echo "  $ICNS"
