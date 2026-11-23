#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
find rules_engine/src -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d "$TMP"
java -cp "$TMP" kr.murimlife.rules.RulesSelfTest
java -cp "$TMP" kr.murimlife.v16.MurimRuleTests
