#!/usr/bin/env bash

set -euo pipefail

SOURCE_PNG="${1:-}"
OUTPUT_DIR="${2:-appx}"

if [[ -z "$SOURCE_PNG" ]]; then
    echo "Usage: $0 <icon.png> [output-directory]"
    exit 1
fi

if [[ ! -f "$SOURCE_PNG" ]]; then
    echo "Error: File not found: $SOURCE_PNG"
    exit 1
fi

if [[ "${SOURCE_PNG,,}" != *.png ]]; then
    echo "Error: Input must be a .png file"
    exit 1
fi

if ! command -v magick >/dev/null 2>&1; then
    echo "Error: ImageMagick (magick) not found in PATH"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

generate_logo() {
    local width="$1"
    local height="$2"
    local name="$3"

    magick "$SOURCE_PNG" \
        -filter Lanczos \
        -resize "${width}x${height}" \
        -background none \
        -gravity center \
        -extent "${width}x${height}" \
        "$OUTPUT_DIR/${name}.png"

    echo "Generated ${name}.png (${width}x${height})"
}

# Required AppX/MSIX assets
generate_logo 44  44  "Square44x44Logo"
generate_logo 150 150 "Square150x150Logo"

# Additional Microsoft recommended assets
generate_logo 50  50  "StoreLogo"
generate_logo 310 150 "Wide310x150Logo"

echo
echo "Assets written to: $OUTPUT_DIR"

echo
echo "Generated:"
find "$OUTPUT_DIR" -type f -name "*.png" | sort
