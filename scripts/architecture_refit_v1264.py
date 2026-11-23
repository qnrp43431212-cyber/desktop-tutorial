#!/usr/bin/env python3
# MURIM LIFE v1.26.4 — reproducible structural/interior refit of Vanilla structure NBT.
# Safe to run in the source project: idempotent by scanning existing placements.
# No external packages required. Avoids editing existing world save data.
from __future__ import annotations
import collections
import gzip
import hashlib
import io
import json
import struct
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import nbt_codec

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data'
AIR = 'minecraft:air'
WALK_BLOCKERS = ('_door','_stairs','_bed','ladder','_fence_gate')
SOFT = {AIR,'minecraft:cave_air','minecraft:void_air'}
RESULTS=[]

class NBTWriter:
    """Vanilla StructureTemplate compound encoder; tagged fields from the original NBT schema."""
    def __init__(self): self.b=io.BytesIO()
    def add(self,fmt,n):self.b.write(struct.pack('>'+fmt,n))
    def string(self,s):
        raw=s.encode('utf-8');self.add('H',len(raw));self.b.write(raw)
    def tag(self,typ,name,value):
        self.add('b',typ);self.string(name);self.payload(typ,value)
    def payload(self,typ,v):
        if typ==1:self.add('b',v)
        elif typ==2:self.add('h',v)
        elif typ==3:self.add('i',v)
        elif typ==4:self.add('q',v)
        elif typ==5:self.add('f',v)
        elif typ==6:self.add('d',v)
        elif typ==7:self.add('i',len(v));self.b.write(v)
        elif typ==8:self.string(v)
        elif typ==9:
            typ2=10 if not v or isinstance(v[0],dict) else 3
            self.add('b',typ2);self.add('i',len(v))
            for item in v:self.payload(typ2,item)
        elif typ==10:
            for k,item in v.items():
                if k=='pos' or k=='size':t=9
                elif k in ('state','DataVersion'):t=3
                elif k in ('palette','blocks','entities'):t=9
                elif k in ('Name',):t=8
                elif k=='Properties':t=10
                elif isinstance(item,str):t=8
                elif isinstance(item,dict):t=10
                elif isinstance(item,list):t=9
                elif isinstance(item,int):t=3
                else:raise TypeError((k,type(item)))
                self.tag(t,k,item)
            self.add('b',0)
        elif typ==11:
            self.add('i',len(v));[self.add('i',i) for i in v]
        else:raise ValueError('NBT type '+str(typ))
    def structure(self,root):
        self.tag(10,'',root)
        return gzip.compress(self.b.getvalue(),mtime=0)

class Template:
    def __init__(self,path):
        self.path=path
        self.data=nbt_codec.load(path)
        self.size=tuple(self.data['size']); self.palette=self.data['palette']
        self.original=self.data['blocks'];self.at={tuple(b['pos']):dict(b) for b in self.original}
        self.pidx={self.pkey(p):i for i,p in enumerate(self.palette)}
        self.changes=collections.Counter()
        self.original_sha=hashlib.sha256(path.read_bytes()).hexdigest()
    @staticmethod
    def pkey(p): return (p['Name'],tuple(sorted(p.get('Properties',{}).items())))
    def state(self,name,props=None):
        p={'Name':name}
        if props is not None:p['Properties']=props
        key=self.pkey(p)
        if key not in self.pidx:self.pidx[key]=len(self.palette);self.palette.append(p)
        return self.pidx[key]
    def name(self,pos):
        if pos not in self.at:return AIR
        return self.palette[self.at[pos]['state']]['Name']
    def prop(self,pos):return self.palette[self.at[pos]['state']].get('Properties',{}) if pos in self.at else {}
    def inbounds(self,pos):return all(0<=x<length for x,length in zip(pos,self.size))
    def empty(self,pos):return self.inbounds(pos) and self.name(pos) in SOFT
    def put(self,pos,name,props=None,only_air=True,reason='furniture'):
        if not self.inbounds(pos):return False
        if only_air and not self.empty(pos):return False
        if not only_air and self.name(pos)==name and (props is None or self.prop(pos)==props):return False
        s=self.state(name,props)
        if pos in self.at:self.at[pos]['state']=s
        else:self.at[pos]={'pos':list(pos),'state':s}
        self.changes[reason]+=1
        return True
    def walllike(self,pos):
        n=self.name(pos)
        if n in SOFT:return False
        return any(k in n for k in ('terracotta','sandstone','calcite','diorite','concrete','planks','stripped_', 'bricks'))
    def floor(self,pos):
        x,y,z=pos
        n=self.name((x,y-1,z))
        return n not in SOFT and not any(k in n for k in ('_stairs','_bed','_fence','_door','glass','_slab','lantern','torch','_leaves'))
    def walkable(self,pos):
        if not self.empty(pos) or not self.empty((pos[0],pos[1]+1,pos[2])) or not self.floor(pos):return False
        return all(not any(k in self.name((pos[0]+dx,pos[1],pos[2]+dz)) for k in WALK_BLOCKERS)
                   for dx,dz in ((0,0),))
    def dangerous_near(self,pos,radius=2):
        x,y,z=pos
        for xx in range(x-radius,x+radius+1):
            for zz in range(z-radius,z+radius+1):
                if abs(x-xx)+abs(z-zz)>radius+1:continue
                n=self.name((xx,y,zz))
                if any(k in n for k in WALK_BLOCKERS):return True
        return False
    def add_door(self,x,y,z,name='minecraft:spruce_door',facing='north'):
        if not self.empty((x,y,z)) or not self.empty((x,y+1,z)):return False
        common={'facing':facing,'hinge':'left','open':'false','powered':'false'}
        if self.put((x,y,z),name,dict(common,half='lower'),reason='bedroom_door'):
            assert self.put((x,y+1,z),name,dict(common,half='upper'),reason='bedroom_door')
            return True
        return False
    def commit(self):
        if not self.changes:return None
        # Existing order preserved; only new placements appended.
        self.data['blocks']=list(self.at.values())
        content=NBTWriter().structure(self.data)
        self.path.write_bytes(content)
        roundtrip=nbt_codec.load(self.path)
        assert tuple(roundtrip['size'])==self.size
        assert len(roundtrip['blocks'])==len(self.at)
        assert len({tuple(x['pos']) for x in roundtrip['blocks']})==len(self.at)
        assert all(0<=s['state']<len(roundtrip['palette']) for s in roundtrip['blocks'])
        return hashlib.sha256(content).hexdigest()

def timber(name):
    return 'minecraft:spruce_planks' if any(x in name for x in ('spruce','sandstone','white_terracotta')) else 'minecraft:dark_oak_planks'

def preferred_wood(t):
    c=collections.Counter(t.name(pos) for pos in t.at)
    return 'minecraft:dark_oak_planks' if c['minecraft:dark_oak_planks']>c['minecraft:spruce_planks'] else 'minecraft:spruce_planks'

def preferred_wall(t):
    c=collections.Counter(t.name(pos) for pos in t.at)
    candidates=('minecraft:white_terracotta','minecraft:smooth_sandstone','minecraft:calcite','minecraft:light_gray_terracotta','minecraft:polished_diorite','minecraft:polished_andesite')
    return max(candidates,key=lambda k:c[k]) if any(c[k] for k in candidates) else 'minecraft:white_terracotta'

def fix_fences(t):
    # In 1.21.1 fences must serialize N/E/S/W connections, or isolated posts appear.
    directions={'north':(0,-1),'east':(1,0),'south':(0,1),'west':(-1,0)}
    changes=[]
    for (x,y,z),block in t.at.items():
        n=t.name((x,y,z))
        if not n.endswith('_fence'):continue
        props={'waterlogged':t.prop((x,y,z)).get('waterlogged','false')}
        for side,(dx,dz) in directions.items():
            other=t.name((x+dx,y,z+dz))
            same=other.endswith('_fence')
            solid=any(k in other for k in ('_planks','_log','_bricks','_terracotta','_concrete','_sandstone','calcite','_wall')) and not ('slab' in other or 'stairs' in other)
            props[side]='true' if same or solid else 'false'
        if props!=t.prop((x,y,z)):changes.append(((x,y,z),n,props))
    for xyz,n,props in changes:t.put(xyz,n,props,only_air=False,reason='fence_connections')

def trim_consistency(t):
    # Replace large *perimeter-only* brightly coloured terracotta bands with timber beams.
    # Keep isolated accent blocks/sect identity; only repeated course-wide patterns change.
    if not any(k in t.path.name for k in ('dorm','main','archive','meditation','inn','residence','cottage','guesthouse','teahouse','smithy','apothecary','shop','house','hall')):
        return
    wood=preferred_wood(t)
    count=collections.Counter()
    for (x,y,z) in t.at:
        n=t.name((x,y,z))
        if n.endswith('_terracotta') and n not in ('minecraft:white_terracotta','minecraft:light_gray_terracotta','minecraft:brown_terracotta') and y>=6:
            count[(y,n)]+=1
    for (y,n),quantity in count.items():
        if quantity<28:continue
        # Require the candidate accent material to be a horizontal row at the outer wall.
        positions=[p for p in t.at if p[1]==y and t.name(p)==n]
        edge=sum(1 for x,_,z in positions if x<=7 or z<=7 or x>=t.size[0]-8 or z>=t.size[2]-8)
        if edge/len(positions)<0.9:continue
        for pos in positions:
            # No wall plane extended into interior; hue bands become coherent timber ledgers.
            t.put(pos,wood,only_air=False,reason='timber_trim')

def add_dorm_partitions(t):
    if '_dorm' not in t.path.stem or t.size[0] !=31 or t.size[2]!=34:return
    bedheads=[(x,y,z) for x,y,z in t.at if t.name((x,y,z)).endswith('_bed') and t.prop((x,y,z)).get('part')=='head']
    if not any(y==2 and x in (11,19) and z in (19,21) for x,y,z in bedheads):return
    wood=preferred_wood(t);wall=preferred_wall(t)
    # Ground-level rear dorm gets a central hallway, two inward-opening bedrooms.
    # Only add if every new wall location was empty (prevents overwriting stairs/fixtures).
    planned=[]
    for x in range(7,24):
        for y in range(2,7):
            if x in (13,21) and y in (2,3):continue
            planned.append((x,y,18))
    for z in range(19,28):
        for y in range(2,7):planned.append((15,y,z))
    if any(not t.empty(p) for p in planned):return
    for x,y,z in planned:
        name=(wood if y==6 else 'minecraft:stripped_spruce_log' if x in (7,15,23) else wall)
        if wood=='minecraft:dark_oak_planks' and 'stripped_' in name:name='minecraft:stripped_dark_oak_log'
        t.put((x,y,z),name,reason='interior_partition')
    # Make doors facing hallway; Minecraft uses north/south orientation for wall z-constant.
    t.add_door(13,2,18,facing='north',name='minecraft:dark_oak_door' if wood=='minecraft:dark_oak_planks' else 'minecraft:spruce_door')
    t.add_door(21,2,18,facing='north',name='minecraft:dark_oak_door' if wood=='minecraft:dark_oak_planks' else 'minecraft:spruce_door')

def add_inn_partitions(t):
    if not (t.path.stem=='city_grand_inn' and t.size[0]>=37):return
    wall=preferred_wall(t);wood=preferred_wood(t)
    # Two guest rooms at the back of the ground floor. Preserve front hall/stair circulation.
    planned=[]
    for x in range(8,32):
        for y in range(2,7):
            if x in (14,25) and y in (2,3):continue
            planned.append((x,y,24))
    for z in range(25,32):
        for y in range(2,7):planned.append((19,y,z))
    if any(not t.empty(p) for p in planned):return
    for x,y,z in planned:
        t.put((x,y,z),wood if y==6 or x==19 else wall,reason='interior_partition')
    for x in (14,25):t.add_door(x,2,24,name='minecraft:dark_oak_door',facing='north')

def furniture_by_beds(t):
    wood=preferred_wood(t);bed_heads=[]
    for pos,b in list(t.at.items()):
        name=t.name(pos)
        if name.endswith('_bed') and t.prop(pos).get('part')=='head':bed_heads.append(pos)
    if not bed_heads:return
    for x,y,z in sorted(bed_heads):
        # At most one side table and one upright shelf per bed, set away from the bed and doors.
        furniture=[]
        for dx,dz in ((-2,0),(2,0),(-2,-1),(2,-1),(0,-2),(-3,1),(3,1),(0,3)):
            p=(x+dx,y,z+dz)
            if not t.walkable(p) or t.dangerous_near(p,1):continue
            if any(abs(p[0]-q[0])+abs(p[2]-q[2])<=2 for q in furniture):continue
            furniture.append(p)
            if len(furniture)>=2:break
        if furniture:
            p=furniture[0]
            t.put(p,'minecraft:barrel',{'facing':'up','open':'false'},reason='bedside_storage')
            # A little potted green atop the side table if free and ceiling clear.
            if t.empty((p[0],p[1]+1,p[2])) and ((x+z)//3)%3==0:
                t.put((p[0],p[1]+1,p[2]),'minecraft:potted_fern',reason='bedroom_decor')
        if len(furniture)>1:
            p=furniture[1]
            if t.empty((p[0],p[1]+1,p[2])):
                t.put(p,'minecraft:bookshelf',reason='bedroom_bookshelf')
                if ((x+z)//2)%2==0:t.put((p[0],p[1]+1,p[2]),'minecraft:bookshelf',reason='bedroom_bookshelf')
    # Furnish larger bed buildings with a shared central reading table near a blank wall.
    if len(bed_heads)>=4 and '_dorm' in t.path.stem:
        for y in sorted(set(p[1] for p in bed_heads)):
            # Central communal space in front; no placement on important doorway/stair path.
            for x,z in ((17,12),(19,13),(13,12)):
                pos=(x,y,z)
                if t.walkable(pos) and not t.dangerous_near(pos,2):
                    t.put(pos,'minecraft:lectern',{'facing':'north','has_book':'false','powered':'false'},reason='common_room')
                    break

def furnish_halls(t):
    stem=t.path.stem
    group=None
    if '_archive' in stem:group='archive'
    elif '_meditation' in stem:group='meditation'
    elif '_main' in stem or stem in ('city_prefecture','village_assembly_hall'):group='hall'
    elif any(n in stem for n in ('teahouse','tea_pavilion','city_inn','village_guesthouse')):group='tea'
    elif any(n in stem for n in ('apothecary','herbalist')):group='apothecary'
    if not group:return
    wood=preferred_wood(t)
    # Find the first inhabited floor with a large contiguous interior carpetless room.
    for y in (2,9):
        xs,zs=t.size[0],t.size[2]
        pts=[]
        # Exclude edges, stairs and doors; sample central region, not every unused air block.
        for z in range(max(5,zs//3),min(zs-5,2*zs//3+1)):
            for x in range(max(5,xs//3),min(xs-5,2*xs//3+1)):
                if t.walkable((x,y,z)) and not t.dangerous_near((x,y,z),2):pts.append((x,y,z))
        if len(pts)<12:continue
        cx,cz=xs//2,zs//2
        pts.sort(key=lambda p:(p[0]-cx)**2+(p[2]-cz)**2)
        placed=0
        for p in pts:
            if placed>= (2 if group=='hall' else 3):break
            if any(abs(p[0]-q[0])+abs(p[2]-q[2])<4 for q in t._furnished_halls):continue
            x,_,z=p
            if group=='archive':
                n='minecraft:lectern' if placed==0 else 'minecraft:bookshelf';props={'facing':'north','has_book':'false','powered':'false'} if n.endswith('lectern') else None
                ok=t.put(p,n,props,reason='study_furnishing')
            elif group=='meditation':
                ok=t.put(p,'minecraft:red_carpet' if placed%2 else 'minecraft:orange_carpet',reason='meditation_mat')
            elif group=='tea':
                ok=t.put(p,'minecraft:barrel',{'facing':'up','open':'false'},reason='inn_table')
                # A slab and stairs are only placed on clear floor adjacent to table.
                near=(x+1,y,z)
                if t.walkable(near) and not t.dangerous_near(near,1):
                    t.put(near,'minecraft:spruce_stairs',{'facing':'west','half':'bottom','shape':'straight','waterlogged':'false'},reason='inn_seating')
            elif group=='apothecary':
                ok=t.put(p,'minecraft:brewing_stand',{'has_bottle_0':'false','has_bottle_1':'false','has_bottle_2':'false'},reason='medicine_workspace') if placed==0 else t.put(p,'minecraft:barrel',{'facing':'up','open':'false'},reason='medicine_workspace')
            else:
                ok=t.put(p,'minecraft:lectern',{'facing':'north','has_book':'false','powered':'false'},reason='hall_furnishing') if placed==0 else t.put(p,'minecraft:barrel',{'facing':'up','open':'false'},reason='hall_furnishing')
            if ok:t._furnished_halls.append(p);placed+=1
        break

def enhance_one(path):
    t=Template(path);t._furnished_halls=[]
    fix_fences(t)
    trim_consistency(t)
    add_dorm_partitions(t)
    add_inn_partitions(t)
    furniture_by_beds(t)
    furnish_halls(t)
    sha=t.commit()
    return {'path':str(path.relative_to(ROOT)), 'source_sha':t.original_sha, 'result_sha':sha,'changes':dict(t.changes),'size':list(t.size),'blocks':len(t.at)}

def main():
    if (ROOT/'ARCHITECTURE_V1264_MANIFEST.json').exists():
        print('Already patched: ARCHITECTURE_V1264_MANIFEST.json exists. No NBT files changed.')
        return
    paths=sorted(DATA.rglob('*.nbt'))
    groups=collections.defaultdict(list)
    for p in paths:groups[hashlib.sha256(p.read_bytes()).hexdigest()].append(p)
    errors=[]; unique_edited=0; changes=collections.Counter()
    for i,items in enumerate(groups.values()):
        rep=items[0]
        try:
            data=enhance_one(rep)
            if data['result_sha']:
                payload=rep.read_bytes()
                for alias in items[1:]:alias.write_bytes(payload)
                unique_edited+=1
                for k,v in data['changes'].items():changes[k]+=v
                for path in items:
                    RESULTS.append(dict(data,path=str(path.relative_to(ROOT)),aliasOf='' if path==rep else str(rep.relative_to(ROOT))))
        except Exception as e:
            errors.append({'path':str(rep.relative_to(ROOT)),'error':repr(e)})
            print('ERROR',rep,e,file=sys.stderr,flush=True)
        if i%30==0:print('processed',i,'unique',len(groups),'edited',unique_edited,flush=True)
    summary={'all_structure_files':len(paths),'distinct_original_templates':len(groups),'modified_unique_templates':unique_edited,'modified_file_copies':len(RESULTS),'block_changes':dict(changes),'errors':errors,'details':RESULTS}
    (ROOT/'qa_v1264_architecture_report.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8')
    print('SUMMARY',json.dumps({k:v for k,v in summary.items() if k!='details'},ensure_ascii=False),flush=True)
    if errors:raise SystemExit(1)
if __name__=='__main__': main()
