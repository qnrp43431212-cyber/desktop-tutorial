#!/usr/bin/env python3
"""Fast CI verification that the intended Java hotfix was actually pushed."""
from pathlib import Path
import hashlib

root = Path(__file__).resolve().parents[1]
file = root / "src/main/java/kr/murimlife/npc/dialogue/DisputeService.java"
src = file.read_text(encoding="utf-8")
assert "final int choice = i;" in src, "HOTFIX MISSING: lambda loop variable capture was not updated. Replace DisputeService.java in the repository root/src/.../npc/dialogue directory."
assert '"/murim dispute choose "+choice' in src, "HOTFIX MISSING: Dispute command still references the mutable variable."
assert '"/murim dispute choose "+i' not in src, "HOTFIX MISSING: Old lambda loop variable is still used."
version = (root / "gradle.properties").read_text(encoding="utf-8")
assert "mod_version=0.1.26.3-github-actions-hotfix-alpha" in version, "Wrong source version: expected v1.26.3."
print("PASS: v1.26.3 compiler hotfix is present in checkout")
print("DisputeService.java SHA-256:", hashlib.sha256(file.read_bytes()).hexdigest())
