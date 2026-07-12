#!/usr/bin/env bash

# 找出真正可用的 JDK。macOS 的 /usr/bin/java 有时只是会弹安装提示的 stub，
# 所以除了检查文件存在，也会实际执行 -version。
resolve_java_tools() {
  local candidate_home=""

  if [[ -n "${JAVA_HOME:-}" ]] \
      && [[ -x "$JAVA_HOME/bin/javac" ]] \
      && [[ -x "$JAVA_HOME/bin/java" ]] \
      && "$JAVA_HOME/bin/javac" -version >/dev/null 2>&1 \
      && "$JAVA_HOME/bin/java" -version >/dev/null 2>&1; then
    candidate_home="$JAVA_HOME"
  elif command -v javac >/dev/null 2>&1 \
      && command -v java >/dev/null 2>&1 \
      && "$(command -v javac)" -version >/dev/null 2>&1 \
      && "$(command -v java)" -version >/dev/null 2>&1; then
    JAVAC_BIN="$(command -v javac)"
    JAVA_BIN="$(command -v java)"
    export JAVAC_BIN JAVA_BIN
    return
  elif [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/javac" ]]; then
    # 当前 Mac 已有 Android Studio 内置 JDK，不需要为了本项目重复安装 Java。
    candidate_home="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi

  if [[ -z "$candidate_home" ]]; then
    echo "No working JDK found." >&2
    echo "Install/configure JDK 14+ approved by your tutor, then set JAVA_HOME." >&2
    exit 1
  fi

  JAVAC_BIN="$candidate_home/bin/javac"
  JAVA_BIN="$candidate_home/bin/java"
  export JAVAC_BIN JAVA_BIN
}
