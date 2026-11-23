#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" scripts/JavaParseOnly.java scripts/JavaDuplicateCheck.java
java -cp "$TMP" JavaParseOnly src/main/java
java -cp "$TMP" JavaDuplicateCheck src/main/java
javac -encoding UTF-8 -d "$TMP" src/main/java/kr/murimlife/npc/combat/{ClashRules,MotionBlend,WeaponTactics}.java \
    src/main/java/kr/murimlife/npc/dialogue/DialogueRules.java \
    src/testkit/java/MurimV121PureTests.java src/testkit/java/MurimV122PureTests.java
java -cp "$TMP" MurimV121PureTests
java -cp "$TMP" MurimV122PureTests
python3 scripts/validate_assets.py
python3 scripts/verify_v16_assets.py
python3 scripts/npc_static_qa.py
python3 scripts/npc_v120_qa.py
python3 scripts/qa_v121.py
python3 scripts/qa_v122.py
