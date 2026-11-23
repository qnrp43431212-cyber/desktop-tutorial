#!/usr/bin/env python3
"""v1.26.4 structure audit: data-level safety only, not rendered Minecraft QA."""
from pathlib import Path
from collections import Counter
import hashlib,json,sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'scripts'))
from asset_integrity_v1264 import verify_legacy_binary_assets
import nbt_codec

approved=json.loads((ROOT/'ARCHITECTURE_V1264_MANIFEST.json').read_text(encoding='utf8'))
unchanged,approved_old=verify_legacy_binary_assets(ROOT)
assert len(approved['modified_nbt'])==approved['modified_files']>=300
all_files=sorted((ROOT/'src/main/resources/data').rglob('*.nbt'))
assert len(all_files)==approved['total_structures']==635
problems=[];counts=Counter();seen=set();bedparts=0
modified={x['path'] for x in approved['modified_nbt']}
for i,p in enumerate(all_files):
    sha=hashlib.sha256(p.read_bytes()).hexdigest()
    if sha in seen:continue
    seen.add(sha)
    d=nbt_codec.load(p);size=d['size'];pal=d['palette'];blocks=d['blocks']
    mapping={tuple(b['pos']):pal[b['state']] for b in blocks}
    if len(mapping)!=len(blocks):problems.append((p.name,'duplicate position'))
    for xyz,palette in mapping.items():
        x,y,z=xyz;n=palette.get('Name','');props=palette.get('Properties',{})
        if x<0 or x>=size[0] or y<0 or y>=size[1] or z<0 or z>=size[2]:problems.append((p.name,xyz,'out of bounds'))
        if n.endswith('_door'):
            if props.get('half')=='lower':
                u=mapping.get((x,y+1,z),{})
                if u.get('Name')!=n or u.get('Properties',{}).get('half')!='upper':problems.append((p.name,xyz,'broken door upper'))
            elif props.get('half')=='upper':
                lower=mapping.get((x,y-1,z),{})
                if lower.get('Name')!=n or lower.get('Properties',{}).get('half')!='lower':problems.append((p.name,xyz,'broken door lower'))
        if n.endswith('_bed'):
            counts['bed_parts']+=1
        if n.endswith('_fence') and str(p.relative_to(ROOT)) in modified:
            if not all(d in props for d in ('north','east','south','west')):problems.append((p.name,xyz,'fence state missing'))
            for direction,dx,dz in [('north',0,-1),('east',1,0),('south',0,1),('west',-1,0)]:
                nb=mapping.get((x+dx,y,z+dz),{}).get('Name','')
                if nb.endswith('_fence') and props.get(direction)!='true':problems.append((p.name,xyz,'fence disconnected '+direction))
        counts['total_unique_blocks']+=1
    if (i+1)%100==0:print('scanned',i+1,flush=True)
assert not problems,problems[:20]
print('PASS',len(all_files),'NBT files,',len(seen),'distinct structures,',len(modified),'edited copies;',unchanged,'historical binary files still byte-identical')
print('PASS door pairs, fence adjacencies, dimensions and positions; test type: OFFLINE STATIC ONLY')
