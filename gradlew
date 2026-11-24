#!/bin/sh
set -eu
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi
printf '%s\n' 'Gradle is not installed. Install Gradle 8.9, Android SDK Platform 35, and Build Tools, then run ./gradlew assembleDebug.' >&2
exit 1
