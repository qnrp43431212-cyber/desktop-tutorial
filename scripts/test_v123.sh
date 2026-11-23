#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test_v122.sh
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
javac -encoding UTF-8 -d "$T" src/main/java/kr/murimlife/economy/CurrencyRules.java src/testkit/java/MurimV123PureTests.java
java -cp "$T" MurimV123PureTests
python3 scripts/qa_v123.py
