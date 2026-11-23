#!/usr/bin/env python3
"""Reference-driven Minecraft Chinese/wuxia refit, not a generic real-world reskin.

Important: not a world-save editor; changes only StructureTemplate resource NBT.
To avoid arbitrary artifacts, roof tiles are changed to stairs only on verified
visible outer slope cells. Window frames replace wall infill, never panes/doors.
"""
from __future__ import annotations
import collections,hashlib,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from architecture_refit_v1264 import Template,ROOT

DATA=ROOT/'src/main/resources/data'
ROOFS={
 'minecraft:dark_prismarine':'minecraft:dark_prismarine_stairs',
 'minecraft:deepslate_tiles':'minecraft:deepslate_tile_stairs',
 'minecraft:polished_blackstone_bricks':'minecraft:polished_blackstone_brick_stairs',
 'minecraft:nether_bricks':'minecraft:nether_brick_stairs',
 'minecraft:dark_oak_planks':'minecraft:dark_oak_stairs',
 'minecraft:spruce_planks':'minecraft:spruce_stairs',
 'minecraft:waxed_oxidized_cut_copper':'minecraft:waxed_oxidized_cut_copper_stairs'
}
WALL=('white_terracotta','smooth_sandstone','calcite','light_gray_terracotta','gray_terracotta','polished_diorite','polished_andesite')
MAIN_KINDS=('_gate','_main','_dorm','_inn','_archive','_hall','_residence')

def timber(t):
    from collections import Counter
    c=Counter(t.name(p) for p in t.at)
    return 'minecraft:stripped_dark_oak_log' if c['minecraft:dark_oak_planks']>c['minecraft:spruce_planks'] else 'minecraft:stripped_spruce_log'

def decorate_windows(t):
    # Decorative mullions for exposed 2x1 openings in exterior infill.
    if not any(k in t.path.stem for k in MAIN_KINDS):return
    coords=[];wood=timber(t)
    for (x,y,z),record in list(t.at.items()):
        name=t.name((x,y,z))
        if 'glass_pane' not in name and 'stained_glass' not in name:continue
        if y<3 or y>t.size[1]-5:continue
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            # An external face must have 2 unobstructed outward blocks.
            if t.name((x+dx,y,z+dz)) not in ('minecraft:air','minecraft:cave_air'):continue
            if t.name((x+2*dx,y,z+2*dz)) not in ('minecraft:air','minecraft:cave_air'):continue
            # Within the wall's tangent axis, make both window sides real framed panels.
            tx,tz=(0,1) if dx else (1,0)
            sides=[]
            for sign in (-1,1):
                a=(x+tx*sign,y,z+tz*sign)
                b=(a[0],y+1,a[2])
                if all(any(w in t.name(p) for w in WALL) for p in (a,b)):
                    sides.append((a,b))
            if not sides:continue
            coords.append((sides,wood))
            break
    changes=0
    for sides,wood in coords:
        for pts in sides:
            for p in pts:
                if any(w in t.name(p) for w in WALL) and t.put(p,wood,{'axis':'y'},only_air=False,reason='minecraft_window_mullion'):
                    changes+=1
    return changes

def roof_eave_stairs(t):
    # Typical Minecraft Chinese roofs are expressed in narrow stepped stair courses.
    # Change only *visible* outermost roof course; keep all solid undersides intact.
    if not any(k in t.path.stem for k in ('_gate','_main','_inn','_hall')):return
    candidates=[]
    family_count=collections.Counter(t.name(p) for p in t.at if p[1]>7)
    roof_name=max((n for n in family_count if n in ROOFS and n not in ('minecraft:dark_oak_planks','minecraft:spruce_planks')),
                  key=lambda n:family_count[n],default=None)
    if roof_name is None or family_count[roof_name]<80:return
    for (x,y,z),_ in list(t.at.items()):
        if t.name((x,y,z))!=roof_name:continue
        if y<8 or y>t.size[1]-5:continue
        if t.name((x,y+1,z)) not in ('minecraft:air','minecraft:cave_air'):continue
        # Select only the outer edge of a sloped roof course, not the ridge or the full tile surface.
        facing=None
        for dx,dz,f in ((0,-1,'north'),(0,1,'south'),(-1,0,'west'),(1,0,'east')):
            outside=t.name((x+dx,y,z+dz))
            inside=t.name((x-dx,y,z-dz))
            below=t.name((x,y-1,z))
            if outside not in ('minecraft:air','minecraft:cave_air'):continue
            if inside!=roof_name:continue
            if below not in ('minecraft:air','minecraft:cave_air',roof_name):continue
            facing=f;break
        if facing:candidates.append(((x,y,z),facing))
    if len(candidates)<12:return
    # Pitched roof corners are left solid for coherent meeting at hips.
    candidates_set={p for p,f in candidates}
    for pos,facing in candidates:
        x,y,z=pos
        if sum((x+dx,y,z+dz) in candidates_set for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)))<1:continue
        t.put(pos,ROOFS[roof_name],{'facing':facing,'half':'bottom','shape':'straight','waterlogged':'false'},
              only_air=False,reason='minecraft_roof_edge_stairs')


def main():
    if (ROOT/'ARCHITECTURE_V1265_MANIFEST.json').exists():
        print('v1.26.5 already patched; new transformations intentionally skipped.')
        return
    paths=sorted(DATA.rglob('*.nbt'))
    # Deduplicate aliases by payload first; identical data is mirrored across resource namespaces.
    grouped=collections.defaultdict(list)
    for p in paths:grouped[hashlib.sha256(p.read_bytes()).hexdigest()].append(p)
    results=[];changed=collections.Counter();errors=[]
    for copies in grouped.values():
        p=copies[0]
        try:
            template=Template(p)
            decorate_windows(template)
            roof_eave_stairs(template)
            after=template.commit()
            if after:
                new_payload=p.read_bytes()
                for alias in copies[1:]:alias.write_bytes(new_payload)
                changed.update(template.changes)
                results.extend(str(a.relative_to(ROOT)) for a in copies)
        except Exception as exc:
            errors.append((str(p),repr(exc)))
    report={'input_nbt':len(paths),'unique_input':len(grouped),'updated_files':len(results),'block_changes':dict(changed),
            'updated_paths':results,'errors':errors,
            'sources':['https://www.bilibili.com/video/BV1HP4y1i7BC/',
                       'https://www.bilibili.com/video/BV1xyRKYzEJ7/',
                       'https://www.bilibili.com/video/BV1qL4y187bt/']}
    (ROOT/'MINECRAFT_WUXIA_REFIT_V1265_REPORT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k!='updated_paths'},ensure_ascii=False))
    if errors:raise SystemExit(1)
if __name__=='__main__':main()
