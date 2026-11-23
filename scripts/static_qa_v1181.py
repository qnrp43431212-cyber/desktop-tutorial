#!/usr/bin/env python3
# Static NBT / catalog validator for MURIM LIFE. Not an actual Minecraft test.
import json,collections,hashlib,sys,csv,gzip,struct
from pathlib import Path
R=Path(__file__).resolve().parents[1]
class NBT:
 def __init__(self,b):self.b=memoryview(gzip.decompress(b));self.o=0
 def r(self,n):q=self.b[self.o:self.o+n];assert len(q)==n;self.o+=n;return q.tobytes()
 def byte(self):return self.r(1)[0]
 def int(self):return struct.unpack('>i',self.r(4))[0]
 def string(self):return self.r(struct.unpack('>H',self.r(2))[0]).decode('utf-8')
 def val(self,t):
  if t==1:return struct.unpack('>b',self.r(1))[0]
  if t==2:return struct.unpack('>h',self.r(2))[0]
  if t==3:return self.int()
  if t==4:return struct.unpack('>q',self.r(8))[0]
  if t==5:return struct.unpack('>f',self.r(4))[0]
  if t==6:return struct.unpack('>d',self.r(8))[0]
  if t in (7,11,12):
   size=self.int();assert 0<=size<10000000
   if t==7:return list(self.r(size))
   if t==11:return list(struct.unpack('>'+str(size)+'i',self.r(4*size)))
   return list(struct.unpack('>'+str(size)+'q',self.r(8*size)))
  if t==8:return self.string()
  if t==9:
   typ=self.byte();n=self.int();assert 0<=n<5000000
   return [self.val(typ) for _ in range(n)]
  if t==10:
   out={}
   while (k:=self.byte())!=0:
    name=self.string();out[name]=self.val(k)
   return out
  raise ValueError(t)
 def root(self):assert self.byte()==10;self.string();r=self.val(10);assert self.o==len(self.b);return r

nbt_files=sorted((R/'src/main/resources/data').glob('*/structure/**/*.nbt'))
assert len(nbt_files)>=344,len(nbt_files)
issues=[]; counts=collections.Counter();templates={}; per_file=[]
for index,file in enumerate(nbt_files):
    rel=file.relative_to(R/'src/main/resources/data');ns=rel.parts[0]; id=ns+':'+str(Path(*rel.parts[2:]).with_suffix('')).replace('\\','/')
    try:
        obj=NBT(file.read_bytes()).root();size=tuple(obj['size']);blocks=obj['blocks'];palette=obj['palette']
        assert len(size)==3 and min(size)>0 and max(size)<=1024,('size',size)
        assert len(palette)>0
        positions=set();states={}
        for j,block in enumerate(blocks):
            xyz=tuple(block['pos']);si=block['state']
            assert len(xyz)==3 and all(0<=v<size[d] for d,v in enumerate(xyz)),('pos',xyz,size)
            assert 0<=si<len(palette),('palette',si,len(palette))
            assert xyz not in positions,('duplicate',xyz)
            positions.add(xyz)
            state=palette[si]
            assert 'Name' in state,('no Name',si)
            if state['Name'].endswith('_door') or state['Name'].endswith('_bed'):
                states[xyz]=(state['Name'],state.get('Properties',{}))
        # Every lower/upper door must have a compatible partner.
        for (x,y,z),(name,props) in states.items():
            suffix=name.rsplit(':',1)[-1]
            if suffix.endswith('_door'):
                half=props.get('half')
                assert half in ('upper','lower'),('door half',name,(x,y,z))
                other=states.get((x,y+(1 if half=='lower' else -1),z))
                assert other is not None and other[0]==name and other[1].get('half')!=half,('door pair',name,(x,y,z))
        counts['total_blocks']+=len(blocks);counts['doors']+=sum(n.endswith('_door') for n,p in states.values())
        templates[id]=size
        per_file.append({'id':id,'x':size[0],'y':size[1],'z':size[2],'blocks':len(blocks),'palette':len(palette),'sha256':hashlib.sha256(file.read_bytes()).hexdigest()})
    except Exception as e:
        issues.append({'id':id,'error':repr(e)})
manifest=json.loads((R/'src/main/resources/murimworldgen/sites.json').read_text())
site_errors=[];refs=0;regions=0
for site in manifest['sites']:
    width,depth=site['size'];regions+=1
    for piece in site['pieces']:
        refs+=1;template=piece['template'];dim=templates.get(template)
        if dim is None:site_errors.append({'site':site['id'],'missing':template});continue
        if piece['x']<0 or piece['z']<0 or piece['x']+dim[0]>width+4 or piece['z']+dim[2]>depth+4:
            site_errors.append({'site':site['id'],'outside':template,'coordinates':piece,'size':dim})
    for paint in site['paints']:
        a=paint['a'];b=paint['b']
        if any(a[i]>b[i] for i in range(3)) or a[0]<0 or a[2]<0 or b[0]>=width or b[2]>=depth:
            site_errors.append({'site':site['id'],'invalid_paint':paint})
# Worldgen config shape and references, independent parse
worldgen=(R/'src/main/resources/data/murimlife_arch/worldgen')
json_files=sorted(worldgen.rglob('*.json'))
for path in json_files:
    try: json.loads(path.read_text())
    except Exception as e:site_errors.append({'worldgen':str(path),'error':str(e)})
# Single-mod metadata guard
mods=list(R.glob('src/main/**/neoforge.mods.toml'))
java=list((R/'src/main/java').rglob('*.java'))
mod_anno=[]
for file in java:
    txt=file.read_text()
    if '@Mod(' in txt:mod_anno.append(str(file.relative_to(R)))
report={'phase':'Independent NBT/source static audit, not runtime',
 'nbt_count':len(nbt_files),'nbt_errors':issues,
 'worldgen_json_count':len(json_files),'sites':regions,'piece_references':refs,'site_errors':site_errors,
 'mods_toml_count':len(mods),'mod_annotations':mod_anno,
 'counts':dict(counts),
 'limits':['No NeoForge compile due unavailable Gradle distribution/dependencies','No Minecraft in-game movement/lighting tests','No guaranteed protection against all other mods or later player edits']}
qa=R/'qa/v1181';qa.mkdir(parents=True,exist_ok=True)
(qa/'static_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
with (qa/'structures_635_csv.csv').open('w',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=per_file[0]);writer.writeheader();writer.writerows(per_file)
print(json.dumps({k:v for k,v in report.items() if k not in ('nbt_errors','site_errors','limits')},ensure_ascii=False,indent=2))
print('errors',len(issues),len(site_errors))
if issues or site_errors or len(mods)!=1 or len(mod_anno)!=1:sys.exit(1)
