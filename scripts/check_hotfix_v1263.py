#!/usr/bin/env python3
"""Fast CI verification that the intended Java hotfix was actually pushed."""
from pathlib import Path
import hashlib

root = Path(__file__).resolve().parents[1]
file = root / "src/main/java/kr/murimlife/npc/dialogue/DisputeService.java"
src = file.read_text(encoding="utf-8")
assert "MurimPanelServer.option(" in src, "New HUD dispute menu missing"
assert '"/murim dispute choose "+i' in src, "Missing command menu binding."
assert "withClickEvent" not in src, "Dispute menu still uses clickable chat."
version = (root / "gradle.properties").read_text(encoding="utf-8")
assert "mod_version=0.1.26.5-wuxia-hud-alpha" in version, "Wrong source version: expected v1.26.5."
print("PASS: v1.26.5 custom HUD and dispute menu are present in checkout")
print("DisputeService.java SHA-256:", hashlib.sha256(file.read_bytes()).hexdigest())
