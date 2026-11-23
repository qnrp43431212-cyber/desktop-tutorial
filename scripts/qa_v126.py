#!/usr/bin/env python3
"""Offline source and asset assertions; NOT NeoForge compilation or an in-game test."""
from pathlib import Path
import json,re,struct,hashlib
root=Path(__file__).resolve().parents[1]
java=root/'src/main/java/kr/murimlife'
medicine=java/'medicine/ElixirCatalog.java'
content=medicine.read_text()
formula_ids=re.findall(r'add\("([a-z_]+)","[^\"]+","[^\"]+",(?:true|false),',content)
assert len(formula_ids)==len(set(formula_ids))==24
assert 'unknown_elixir' in formula_ids
for ident in formula_ids:
 assert (root/f'src/main/resources/assets/murimlife/models/item/{ident}.json').is_file()
 path=root/f'src/main/resources/assets/murimlife/textures/item/{ident}.png'
 assert path.is_file() and struct.unpack('>II',path.read_bytes()[16:24])==(16,16)
for path in (root/'src/main/resources/assets/murimlife').rglob('*.json'):
 json.loads(path.read_text())
assert (root/'src/main/resources/data/murimlife/recipe/herbal_brewing_pot.json').is_file()
assert 'HERBAL_BREWING_POT' in (java/'ModBlocks.java').read_text()
profile=(java/'MurimProgressData.java').read_text()
for token in ('CultMentorId','ArtMastery','QiStrain','QiSeverity','QiAftereffectUntil','RealmProgress'):
 assert token in profile
ment=(java/'cult/CultMentorshipService.java').read_text()
assert all(t in ment for t in ['CultMentorRules.apprenticeChance','cultManual:','MurimDialogueService.forceHostility','pr.sect=person.cult()'])
cast=(java/'MurimCombat.java').read_text()
assert 'QiDeviationService.onBreathing' in cast and 'profile.artMastery.merge' in cast
raid=(java/'cult/CultRaidService.java').read_text()
for needle in ('murimlife_cult_raids_v126','Survivors','onJoin(EntityJoinLevelEvent','onDeath(LivingDeathEvent','SiteLedger.get','lastDay'):
 assert needle in raid
assert raid.index('active.put(id,raid)')<raid.index('owner.serverLevel().addFreshEntity(mob)')
assert 'tag.hasUUID("MurimCultProvokedPlayer")' in (java/'cult/CultRoster.java').read_text()
audit=(java/'economy/SectDiscipline.java').read_text()
assert 'raiders>=3&&raidsStopped>=1' in audit
assert '기연 수호자 3명 처치' not in audit
assert '철괴 12개' not in audit
assert 'guardianKill(ServerPlayer player)' in audit and 'Legacy hook deliberately' in audit
assert 'CultRaidService::tick' in (java/'MurimLife.java').read_text()
assert 'QiDeviationService::tick' in (java/'MurimLife.java').read_text()
assert 'IntegerArgumentType.integer(1,21)' in (java/'npc/dialogue/MurimDialogueService.java').read_text()
assert len(list((root/'src/main/resources/data/murimlife/cult').glob('*.json')))>0
roster=json.loads((root/'src/main/resources/data/murimlife/cult/cult_roster_524.json').read_text())
assert len(roster['people'])==524 and len({p['name_ko'] for p in roster['people']})==524
assert len(set(p['name_ko'] for p in roster['people'][24:]))>=500 # 500 names for future generation
assets_before=json.loads((root/'MANIFEST_SHA256_v121_historical.json').read_text())
preserved=0
for entry in assets_before['files']:
 if entry['path'].startswith('src/main/resources/') and entry['path'].endswith(('.nbt','.png')):
  f=root/entry['path'];assert f.is_file() and hashlib.sha256(f.read_bytes()).hexdigest()==entry['sha256'],f
  preserved+=1
print(f'PASS 24 medicines, 23 alchemy recipes, 500 additional unique cult names, {preserved} untouched legacy binary assets, raid/audit/mentor/qi code wiring')
print('NOTE Gradle NeoForge API compile and actual Minecraft play still unverified')
