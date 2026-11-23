import os,gzip,struct,zlib,sys,json,collections,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
PATHS=list(ROOT.rglob('*.nbt'))
class Reader:
 def __init__(self,b):self.b=memoryview(b);self.p=0
 def pull(self,n):
  if self.p+n>len(self.b):raise ValueError('truncated')
  a=self.b[self.p:self.p+n];self.p+=n;return a
 def i(self,t):return struct.unpack('>'+t,self.pull(struct.calcsize('>'+t)))[0]
 def s(self):return self.pull(self.i('H')).tobytes().decode('utf8')
 def val(self,t):
  if t==1:return self.i('b')
  if t==2:return self.i('h')
  if t==3:return self.i('i')
  if t==4:return self.i('q')
  if t==5:return self.i('f')
  if t==6:return self.i('d')
  if t==7:return self.pull(self.i('i')).tobytes()
  if t==8:return self.s()
  if t==9:
   typ=self.i('b');n=self.i('i');return [self.val(typ) for i in range(n)]
  if t==10:
   res={}
   while typ:=self.i('b'):
    key=self.s();res[key]=self.val(typ)
   return res
  if t==11:
   n=self.i('i');return [self.i('i') for _ in range(n)]
  if t==12:
   n=self.i('i');return [self.i('q') for _ in range(n)]
  raise ValueError('tag '+str(t))
 def parse(self):
  t=self.i('b');name=self.s();return self.val(t)
def load(p):
 raw=p.read_bytes()
 if raw[:2]==b'\x1f\x8b':raw=gzip.decompress(raw)
 elif raw[:1]==b'x':raw=zlib.decompress(raw)
 return Reader(raw).parse()
if __name__=='__main__':
 files=[];error=[];all_blocks=collections.Counter();counts=collections.Counter();issues=[]; examples=[]
 for p in PATHS:
  try:
   d=load(p)
   if not isinstance(d,dict):raise ValueError('non compound')
   size=d.get('size',[]);palette=d.get('palette',[]);blocks=d.get('blocks',[])
   if len(size)!=3 or not palette or not isinstance(blocks,list):
    issues.append((p.name,'bad schema',len(size),len(palette),len(blocks)))
   invalid=[b for b in blocks if len(b.get('pos',[]))!=3 or len(size)==3 and any(k<0 or k>=size[i] for i,k in enumerate(b['pos'])) or b['state']<0 or b['state']>=len(palette)]
   if invalid:issues.append((p.name,'bad position/state',len(invalid)))
   pos=[tuple(b['pos']) for b in blocks];dups=len(pos)-len(set(pos))
   if dups:issues.append((p.name,'duplicate coordinates',dups))
   names=collections.Counter(palette[b['state']].get('Name','') for b in blocks)
   for n,c in names.items():all_blocks[n]+=c
   counts[len(blocks)//1000*1000]+=1
   grids={tuple(b['pos']):palette[b['state']] for b in blocks}; 
   def solid(x):
    n=grids.get(tuple(x),{}).get('Name','minecraft:air')
    return n not in {'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:water','minecraft:lava','minecraft:torch','minecraft:wall_torch','minecraft:lantern','minecraft:chain','minecraft:glass_pane','minecraft:oak_sign','minecraft:oak_wall_sign'}
   floating_stairs=[];floating_chairs=[];stairs=[];doors=[];walls=[]
   for xyz,blk in grids.items():
    n=blk.get('Name','')
    if n.endswith('_stairs'):
     stairs.append(xyz)
     if xyz[1]>0 and not solid((xyz[0],xyz[1]-1,xyz[2])):floating_stairs.append(xyz)
    elif n.endswith('_door') and blk.get('Properties',{}).get('half','lower')=='lower':
     doors.append(xyz)
     if xyz[1]>0 and not solid((xyz[0],xyz[1]-1,xyz[2])):floating_chairs.append(xyz)
    elif 'wall' in n:walls.append(xyz)
   files.append({'path':str(p.relative_to(ROOT)),'name':p.name,'size':size,'blocks':len(blocks),'palette':len(palette),'stairs':len(stairs),'floating_stairs':len(floating_stairs),'doors':len(doors),'unsupported_doors':len(floating_chairs),'walls':len(walls),'stair_positions':floating_stairs[:20]})
  except Exception as e:error.append((p.name,str(e)))
 print('files',len(files),'errors',len(error),'issues',len(issues),'stair_floating',sum(f['floating_stairs'] for f in files),'unattached_doors',sum(f['unsupported_doors'] for f in files))
 print('errors',error[:10]);print('issues',issues[:15]);print('blocks_common',all_blocks.most_common(32))
 print('worst stairs',[(f['name'],f['floating_stairs'],f['stairs']) for f in sorted(files,key=lambda f:f['floating_stairs'],reverse=True)[:15]])
 print('sizes',collections.Counter(tuple(f['size']) for f in files).most_common(10))
 (ROOT/'qa_v124_nbt_baseline.json').write_text(json.dumps({'files':files,'error':error,'issues':issues,'block_types':all_blocks},ensure_ascii=False,indent=2),encoding='utf8')
