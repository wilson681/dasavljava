#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$PROJECT_DIR/scripts/common.sh"
resolve_java_tools

"$PROJECT_DIR/scripts/compile.sh"

TEST_CLASS_DIR="$PROJECT_DIR/build/test-classes"
rm -rf "$TEST_CLASS_DIR"
mkdir -p "$TEST_CLASS_DIR"

TEST_SOURCES=()
while IFS= read -r source_file; do
  TEST_SOURCES+=("$source_file")
done < <(find "$PROJECT_DIR/test" -name '*.java' -print | sort)

if [[ ${#TEST_SOURCES[@]} -eq 0 ]]; then
  echo "No Java test files found under $PROJECT_DIR/test" >&2
  exit 1
fi

"$JAVAC_BIN" --release 14 -encoding UTF-8 \
  -cp "$PROJECT_DIR/build/classes" \
  -d "$TEST_CLASS_DIR" \
  "${TEST_SOURCES[@]}"

"$JAVA_BIN" \
  -cp "$PROJECT_DIR/build/classes:$TEST_CLASS_DIR" \
  adt.AVLTreeTest
