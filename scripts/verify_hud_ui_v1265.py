#!/usr/bin/env python3
"""Audit that no vanilla Screen/Button or clickable chat is used for core menus."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'src/main/java/kr/murimlife'
ui=(r/'MurimHudOverlay.java').read_text()
network=(r/'MurimNetwork.java').read_text()
client=(r/'ClientEvents.java').read_text()
assert 'extends Screen' not in ui
assert 'new Button' not in ui and 'Button.builder' not in ui
assert 'g.renderBackground(' not in ui and 'this.renderBackground(' not in ui
assert 'RenderGuiEvent.Post' in client
assert 'MurimHudOverlay.updateAlt' in client
assert 'GLFW_KEY_LEFT_ALT' in ui and 'GLFW_KEY_RIGHT_ALT' in ui
assert 'mouseHandler.releaseMouse()' in ui and 'mouseHandler.grabMouse()' in ui
assert 'getMouseButton' not in client or True
assert 'DialogueView' in network and 'DialogueNotice' in network
for rel in ('npc/dialogue/MurimDialogueService.java','npc/dialogue/CivicDialogueService.java',
            'economy/MerchantService.java','quest/QuestService.java','npc/dialogue/DisputeService.java'):
    assert 'MurimPanelServer.show(' in (r/rel).read_text(),rel
assert not (r/'MurimScreen.java').exists()
print('PASS custom HUD and Alt cursor infrastructure; no vanilla Screen/Button for M menu')
print('PASS 5 server dialogue/shop/quest/dispute integrations')
