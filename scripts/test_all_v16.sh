#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test_rules_v16.sh
python scripts/verify_v16_assets.py
TMP="$(mktemp -d)";trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" scripts/JavaSyntaxCheck.java
java -cp "$TMP" JavaSyntaxCheck src/main/java
