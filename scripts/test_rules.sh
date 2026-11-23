#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/rules
javac -encoding UTF-8 -d build/rules rules_engine/src/kr/murimlife/rules/*.java
java -cp build/rules kr.murimlife.rules.RulesSelfTest
python3 scripts/validate_assets.py
