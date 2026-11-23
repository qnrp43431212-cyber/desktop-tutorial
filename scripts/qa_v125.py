#!/usr/bin/env python3
"""v1.25 static audit and succession exhaustion model, NOT a full Minecraft/NeoForge run."""
from pathlib import Path
from collections import Counter
import struct
import hashlib,json
R=Path(__file__).resolve().parents[1]
P=json.loads((R/'src/main/resources/data/murimlife/cult/cult_roster_524.json').read_text(encoding='utf-8'))
people=P['people'];seats=P['seats'];assert len(people)==524 and len(seats)==12
assert all(seats[i]['min_realm']==(6 if i<4 else 5 if i<8 else 4) for i in range(12))
assert Counter(p['cult'] for p in people)=={'마교':262,'혈교':262}
for attr in ('id','skin','name_ko','technique'):
 assert len({p[attr] for p in people})==524, 'duplicate '+attr
assert all('혈마' not in p['technique'] and '천마' not in p['technique'] for p in people)
base=json.loads((R/'src/main/resources/assets/murimlife/rules/martial_catalog.json').read_text())
assert not ({p['technique'] for p in people} & {a['name'] for a in base['martial_arts']})
legacy_names=set()
for file in ['named_leaders_16.json','named_faction_members_330.json','procedural_name_bank.json']:
  data=json.loads((R/'src/main/resources/data/murimlife/npc'/file).read_text())
  for value in data.values():
   if isinstance(value,list):legacy_names.update(a.get('name_ko','') for a in value if isinstance(a,dict))
legacy_names.update(n['name'] for n in json.loads((R/'src/main/resources/murimnpc/legacy_profiles_96.json').read_text())['profiles'])
assert not (legacy_names & {p['name_ko'] for p in people})
tex=R/'src/main/resources/assets/murimlife/textures/entity/wuxia'
assert len(list(tex.glob('skin_*.png')))==966
hashes=[]
for p in people:
 path=tex/f'skin_{p["skin"]:03d}.png'
 assert struct.unpack('>II',path.read_bytes()[16:24])==(64,64)
 hashes.append(hashlib.sha256(path.read_bytes()).hexdigest())
assert len(set(hashes))==524
assert set(p['skin'] for p in people)==set(range(442,966))
for cult in ('마교','혈교'):
 group=[p for p in people if p['cult']==cult]
 assert len(group)==262 and all(group[s]['realm']>=seats[s]['min_realm'] for s in range(12))
 # Same seat-shift algorithm as server: eligible subordinate inherits senior rank;
 # the next unique reserve takes the lowest vacated slot, with no reused identity.
 roster=list(group[:12]); idx=12
 retired=set()
 for count in range(250):
  victim=roster[0];assert victim['id'] not in retired
  retired.add(victim['id'])
  vacancy=0
  while True:
   eligible=next((i for i in range(vacancy+1,12)
       if roster[i] is not None and roster[i]['realm']>=seats[vacancy]['min_realm']),None)
   if eligible is None:break
   roster[vacancy]=roster[eligible];roster[eligible]=None;vacancy=eligible
  if vacancy==11 and idx<len(group):roster[11]=group[idx];idx+=1
  assert len([x for x in roster if x is not None])==12,(cult,count)
  assert len({x['id'] for x in roster})==12
 assert len(retired)==250 and idx==262
print('PASS: 24 active, 500 reserve, 524 unique names/techniques/skins; 500 sequential high-seat successions')
basejava=R/'src/main/java/kr/murimlife'
required={
 'cult/CultRoster.java':['murimlife_cult_roster_v125','promoteAfterDeath','onDeath','onJoin','setDirty','Member'],
 'cult/CultDesign.java':['cult_roster_524.json','Map.copyOf','new Person'],
 'MurimCombat.java':['cultManual:','p.realm<demonic.realm()','CultDesign.all()'],
 'economy/SectMembershipCommands.java':['CultRankRules.eligible'],
 'economy/NpcSectRoster.java':['CultRankRules.eligible'],
 'MurimCommands.java':['Commands.literal("breakthrough")','CultCommands.register'],
 'MurimLife.java':['CultRoster::onDeath','CultRoster::onJoin'],
}
for fname,needles in required.items():
 s=(basejava/fname).read_text()
 assert all(needle in s for needle in needles),(fname,needles)
report={'version':'1.25.0','catalog_total':524,'initial_great_demons':24,'future_name_templates':500,
        'unique_techniques':524,'added_skin_textures':524,'total_wuxia_skins':966,
        'tested_successions':500,'neoforge_compile':'NOT_VERIFIED','minecraft_execution':'NOT_VERIFIED'}
(R/'qa/v125_cult_succession.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False))
