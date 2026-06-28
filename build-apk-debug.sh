#!/usr/bin/env bash
set -e
if [ -f ./gradlew ]; then
  ./gradlew assembleDebug
else
  gradle assembleDebug
fi
