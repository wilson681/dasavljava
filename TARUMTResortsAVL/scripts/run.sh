#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$PROJECT_DIR/scripts/common.sh"
resolve_java_tools

"$PROJECT_DIR/scripts/compile.sh"
"$JAVA_BIN" -cp "$PROJECT_DIR/build/classes" main.Main "$@"
