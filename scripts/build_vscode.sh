#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
exec ./gradlew --no-daemon :vscode:vscodeTypecheck :vscode:vscodeTest stageVscodeVsix
