#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$PROJECT_DIR/scripts/common.sh"
resolve_java_tools

CLASS_DIR="$PROJECT_DIR/build/classes"
rm -rf "$CLASS_DIR"
mkdir -p "$CLASS_DIR"

SOURCES=()
while IFS= read -r source_file; do
  SOURCES+=("$source_file")
done < <(find "$PROJECT_DIR/src" -name '*.java' -print | sort)

if [[ ${#SOURCES[@]} -eq 0 ]]; then
  echo "No Java source files found under $PROJECT_DIR/src" >&2
  exit 1
fi

"$JAVAC_BIN" --release 14 -encoding UTF-8 -d "$CLASS_DIR" "${SOURCES[@]}"
echo "Compiled ${#SOURCES[@]} source files into $CLASS_DIR"
