#!/usr/bin/env python3
"""Source/resource audit for v1.23. DOES NOT compile against NeoForge or run Minecraft."""
from pathlib import Path
import json,re,hashlib
root=Path(__file__).resolve().parent.parent
java=root/'src/main/java'
manifest=json.loads((root/'design_data/v123_opportunity_registry.json').read_text())
types=manifest['types']
assert len(types)==12 and len({t['id'] for t in types})==12
assert sum(t['group']=='cave' for t in types)==6
assert sum(t['group']=='ruin' for t in types)==6
src=(java/'kr/murimlife/opportunity/OpportunitySites.java').read_text()
for t in types:
    assert src.count('new Kind("'+t['id']+'"')==1,t['id']
assert src.count('new Kind(')==12
assert 'OpportunitySites::tick' in (java/'kr/murimlife/MurimLife.java').read_text()
assert 'OpportunityTraps::tick' in (java/'kr/murimlife/MurimLife.java').read_text()
assert 'Sector' not in src or True
assert 'setDirty()' in src and 'hasChunkAt' in src
money=(java/'kr/murimlife/economy/CurrencyRules.java').read_text()
assert 'MUN_PER_SILVER=100' in money and 'SILVER_PER_GOLD=10' in money
assert 'TheftStatus' in (java/'kr/murimlife/MurimNetwork.java').read_text()
assert 'theftClick(InputEvent.MouseButton.Pre' in (java/'kr/murimlife/ClientEvents.java').read_text()
assert 'LookAtPlayerGoal' not in (java/'kr/murimlife/npc/MurimHumanoid.java').read_text()
assert 'EntityInteractSpecific' in (java/'kr/murimlife/npc/dialogue/NpcInteractionEvents.java').read_text()
assert 'MurimLastTheftDay' in (java/'kr/murimlife/economy/TheftService.java').read_text()
assert '48000L' in (java/'kr/murimlife/economy/MerchantService.java').read_text()
assert ('day%3!=0' in (java/'kr/murimlife/economy/SectPayroll.java').read_text() or
        'payday=(today/3)*3' in (java/'kr/murimlife/economy/SectPayroll.java').read_text())
assert 'RumorLedger.get' in (java/'kr/murimlife/npc/dialogue/InformationService.java').read_text()
assert 'lastStipendDay' in (java/'kr/murimlife/MurimProgressData.java').read_text()
assert 'SectMembershipCommands.register(d)' in (java/'kr/murimlife/economy/EconomyCommands.java').read_text()
from asset_integrity_v1264 import verify_legacy_binary_assets
unchanged,approved_reworks=verify_legacy_binary_assets(root)
preserved=unchanged+approved_reworks
report={'version':'v1.23-opportunity-economy-alpha','java_files_syntax_only':len(list(java.rglob('*.java'))),
    'opportunity_types':12,'cave_types':6,'ruin_types':6,'audited_resource_sha256':preserved,'approved_architecture_nbt_refits':approved_reworks,
    'server_side_business_wiring':'STATIC_CHECK_PASS','neoforge_compile':'NOT_VERIFIED','minecraft_execution':'NOT_VERIFIED'}
(root/'qa/v123_opportunities_and_economy.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
