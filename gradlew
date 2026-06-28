#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="${GRADLE_VERSION:-8.7}"
GRADLE_BASE="$HOME/.gradle/manual"
GRADLE_HOME="$GRADLE_BASE/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_BASE"
  TMP_ZIP="${TMPDIR:-/tmp}/gradle-$GRADLE_VERSION-bin.zip"
  echo "Baixando Gradle $GRADLE_VERSION..."
  curl -sSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$TMP_ZIP"
  rm -rf "$GRADLE_HOME"
  unzip -q "$TMP_ZIP" -d "$GRADLE_BASE"
fi

exec "$GRADLE_BIN" "$@"
