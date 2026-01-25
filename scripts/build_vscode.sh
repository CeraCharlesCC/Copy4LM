#!/usr/bin/env bash
set -euo pipefail

./gradlew --no-daemon :common:jsProductionLibraryDistribution

npm --prefix vscode install
npm --prefix vscode run build
npm --prefix vscode run package
