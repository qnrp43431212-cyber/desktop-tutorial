#!/usr/bin/env python3
"""v1.20 reproducible static QA. Run from MDK root: python scripts/npc_v120_qa.py"""
import json, csv, sys, re, zipfile
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from npc_parse_nbt import load
ROOT=Path(__file__).resolve().parent.parent
D=ROOT/'src/main/resources/data/murimlife/npc'
R=ROOT/'src/main/resources/murimnpc'
OUT=ROOT/'npc_qa_v120';OUT.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
legacy=read(R/'legacy_profiles_96.json')['profiles']
roles=read(R/'npc_role_catalog.json')['roles']
jobs=read(D/'civilian_professions_30.json')['professions']
leaders=read(D/'named_leaders_15.json')['leaders']
members=read(D/'named_faction_members_330.json')['members']
bank=read(D/'procedural_name_bank.json')['names']
seats=read(D/'arena_spectator_seats.json')['seats']
roster_sites={x['site_id'] for x in read(R/'npc_faction_rosters.json')['sites']}
assert len(legacy)==96
assert len(roles)==87 and len({r['id'] for r in roles})==87
assert len(jobs)==30 and len({j['id'] for j in jobs})==30
assert {j['id'] for j in jobs} <= {r['id'] for r in roles}
assert all(j['persistent'] for j in jobs)
assert len(leaders)==15 and len({x['faction_id'] for x in leaders})==15
assert {x['faction_id'] for x in leaders} <= roster_sites
assert len(members)==330 and len({x['npc_key'] for x in members})==330
assert len({x['faction_id'] for x in members})==15
assert all(sum(m['faction_id']==f['faction_id'] for m in members)==22 for f in leaders)
assert len(bank)>=1400
seen={}
for kind,rows in [('legacy',legacy),('leaders',leaders),('members',members),('bank',bank)]:
 for item in rows:
  name=item.get('name_ko',item.get('name'))
  assert name and name not in seen,(kind,name,seen.get(name))
  seen[name]=kind
  if kind=='leaders': assert item['name_zh'] and item['pinyin']
assert len({tuple(s['offset']) for s in seats})==len(seats)
assert len(seats)==444 and all(sum(s['quarter']==q for s in seats)==111 for q in range(4))
structure=ROOT/'src/main/resources/data/murimlife/structure/martial_tournament_arena.nbt'
n=load(structure); palette=n['palette'];blocks={tuple(b['pos']):palette[b['state']]['Name'] for b in n['blocks']}
for s in seats:
 x,y,z=s['offset']
 assert blocks.get((x,y-1,z))=='minecraft:spruce_stairs'
 assert blocks.get((x,y,z),'minecraft:air')=='minecraft:air'
 assert blocks.get((x,y+1,z),'minecraft:air')=='minecraft:air'
assert n['size']==[55,17,55]
javafiles=list((ROOT/'src/main/java').rglob('*.java'))
src='\n'.join(p.read_text(encoding='utf-8') for p in javafiles)
assert src.count('@Mod(')==1
assert 'NeoForge.EVENT_BUS.addListener(NpcResidentService::onTick)' in src
assert 'NeoForge.EVENT_BUS.addListener(NpcArenaService::onTick)' in src
assert 'NpcAdminCommands.register(disp)' in src
assert 'mod_id=murimlife' in (ROOT/'gradle.properties').read_text(encoding='utf-8')
policy=read(R/'npc_spawn_policy.json')
assert 'ALL named residents remain saved' in policy['unload_policy']
assert policy['default_max_site_active']==22
assert policy['tournament_crowd']['default_visible']==48
# Generate summaries and curated review CSVs.
for filename,rows,fields in [
 ('resident_professions_30.csv',jobs,['id','name_ko','area','day_work','interaction','persistent']),
 ('faction_leaders_15.csv',leaders,['faction_id','faction_ko','name_ko','name_zh','pinyin','title_ko','persistent']),
 ('faction_members_330.csv',members,['faction_id','faction_ko','npc_key','name_style','role','name_ko','name_zh','pinyin']),
]:
 with (OUT/filename).open('w',encoding='utf-8-sig',newline='') as f:
  w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows({key:r.get(key,'') for key in fields} for r in rows)
report={
 'source_version':'0.1.20-residents-arena-alpha',
 'one_mod_id':'murimlife','npc_catalog_total_roles':len(roles),
 'civilian_professions':len(jobs),'curated_unique_leaders':len(leaders),
 'curated_unique_faction_members':len(members),'legacy_profiles':len(legacy),
 'additional_nonconflicting_name_bank':len(bank),'distinct_ko_names_total':len(seen),
 'arena_nbt_55x55_stair_seat_candidates':len(seats),'arena_quadrant_counts':[sum(s['quarter']==q for s in seats) for q in range(4)],
 'default_live_crowd':48,'configurable_max_crowd':80,
 'java_files_parsed_syntax_only':len(javafiles),
 'actual_entity_prototype':'minecraft:villager NBT-stored; actual NeoForge compile and Minecraft not tested',
 'neo_full_build':'NOT TESTED','in_game_behavior':'NOT TESTED',
 'distinct_civilian_occupational_AI':'DATA ONLY (roles planned, not distinct AI)',
 'tournament':'COMMAND-TRIGGERED ALPHA (NOT official timed tournament)',
 'assertions':'PASS'
}
(OUT/'npc_v120_static_qa.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
