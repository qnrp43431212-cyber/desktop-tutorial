#!/usr/bin/env python3
"""MURIM LIFE v1.19 NPC preparation QA; no Minecraft runtime required."""
import json,sys
from pathlib import Path
from npc_parse_nbt import load
BASE=Path(__file__).resolve().parents[1]
RES=BASE/'src/main/resources'
def read(file):return json.loads((RES/'murimnpc'/file).read_text(encoding='utf8'))
def floor(n):
 return any(s in n for s in ('planks','bricks','stone','terracotta','concrete','dirt','gravel','grass_block','log','polished','tile','quartz','mud','tuff','sandstone','wool','moss_block','clay','carpet','glass')) and not any(s in n for s in ('stairs','slab','wall','fence','pane','glass_pane','button','pressure_plate','trapdoor','door','sign','flower_pot'))
def air(n):return n in ('minecraft:air','minecraft:cave_air','minecraft:void_air')
def check():
 roles=read('npc_role_catalog.json')['roles'];roleids={r['id'] for r in roles}
 assert len(roles)==len(roleids)==87
 assert all(r['schedule'] and r['label_ko'] and r['daily_activity'] for r in roles)
 profile=read('legacy_profiles_96.json')['profiles']
 assert len(profile)==96 and len({p['name'] for p in profile})==96
 skinroot=RES/'assets/murimlife/textures/entity/npc'
 assert all((skinroot/p['skin']).is_file() for p in profile)
 worldsites={x['id']:x for x in json.loads((RES/'murimworldgen/sites.json').read_text())['sites']}
 manifest=read('npc_site_spawn_candidates.json');assert len(manifest['sites'])==38==len(worldsites)
 checked=0
 for site in manifest['sites']:
  original=worldsites[site['site_id']];assert len(site['pieces'])==len(original['pieces'])
  for piece in site['pieces']:
   base=original['pieces'][piece['piece_index']];assert base['template']==piece['template']
   ns,rid=piece['template'].split(':',1)
   obj=load(RES/'data'/ns/'structure'/f'{rid}.nbt')
   blocks={tuple(b['pos']):obj['palette'][b['state']]['Name'] for b in obj['blocks']}
   for x,y,z in piece['candidate_spawn_offsets']:
    px=x-base['x'];py=y-base['y'];pz=z-base['z']
    assert 0<=px<obj['size'][0] and 1<=py<obj['size'][1]-1 and 0<=pz<obj['size'][2]
    assert floor(blocks.get((px,py-1,pz),'minecraft:air'))
    assert air(blocks.get((px,py,pz),'minecraft:air')) and air(blocks.get((px,py+1,pz),'minecraft:air'))
    checked+=1
   assert all(x in roleids for x in piece['possible_roles'])
 assert checked==manifest['candidate_count']==864
 natural=read('npc_natural_jigsaw_candidates.json')['templates'];assert len(natural)==17
 for n in natural:
  ns,rid=n['template'].split(':',1)
  obj=load(RES/'data'/ns/'structure'/f'{rid}.nbt')
  assert all(0<=x<obj['size'][0] and 1<=y<obj['size'][1]-1 and 0<=z<obj['size'][2]
             for x,y,z in n['local_unrotated_candidates'])
  assert all(x in roleids for x in n['allowed_roles'])
 assert not read('npc_spawn_policy.json')['enabled']
 print(f'NPC QA PASS: {len(roles)} roles, 96 profiles, {len(worldsites)} world sites, {checked} candidate anchors, {len(natural)} jigsaw templates. Runtime spawn OFF.')
if __name__=='__main__':check()
